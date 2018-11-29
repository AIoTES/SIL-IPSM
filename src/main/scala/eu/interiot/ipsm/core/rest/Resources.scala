/**
  * Copyright 2016-2018 Universitat Politècnica de València
  * Copyright 2016-2018 Università della Calabria
  * Copyright 2016-2018 Prodevelop, SL
  * Copyright 2016-2018 Technische Universiteit Eindhoven
  * Copyright 2016-2018 Fundación de la Comunidad Valenciana para la Investigación,
  *                     Promoción y Estudios Comerciales de Valenciaport
  * Copyright 2016-2018 Rinicom Ltd
  * Copyright 2016-2018 Association pour le développement de la formation
  *                     professionnelle dans le transport
  * Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.
  * Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.
  * Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences
  * Copyright 2016-2018 Azienda Sanitaria Locale TO5
  * Copyright 2016-2018 Alessandro Bassi Consulting SARL
  * Copyright 2016-2018 Neways Technologies B.V.
  *
  * See the NOTICE file distributed with this work for additional information
  * regarding copyright ownership.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

package eu.interiot.ipsm.core.rest

import java.sql.Timestamp
import java.util.Calendar

import eu.interiot.ipsm.core.alignments._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.typesafe.scalalogging.LazyLogging
import eu.interiot.ipsm.core.alignments.AlignmentValidator
import eu.interiot.ipsm.core.channels.ChannelManager
import eu.interiot.ipsm.core.datamodel._
import eu.interiot.ipsm.core._

import scala.util.{Failure, Success, Try}
import scala.xml.XML

trait Resources extends JsonSupport with XMLSupport with LazyLogging {

  private def uuid() = {
    java.util.UUID.randomUUID.toString
  }

  implicit val system: ActorSystem
  implicit val executionContext: ExecutionContext

  def createChannel(chanConf: ChannelConfig): Route = {

    //TODO: do we really need to impose the restriction below?
    if (ChannelManager.checkIfExists(chanConf.sink)) {
      complete(StatusCodes.BadRequest, ErrorResponse(
        s"""Channel with output ID="${chanConf.sink}" already exists"""
      ))
    }

    // if not provided the kafka consumer group for the channel gets initialized to a fresh UUID
    val conf = if (chanConf.optConsumerGroup.isEmpty) {
      chanConf.copy(optConsumerGroup = Some(uuid()))
    } else {
      chanConf
    }

    val chanUUID = uuid()

    val saved: Future[ActorMaterializer] = ChannelManager.materializeChannel(chanUUID, conf, system)

    onComplete(saved) {
      case Success(done) =>
        val res = ChannelManager.createChannel(done, conf, chanUUID)
        onComplete(res) {
          case Success(channelData) =>
            complete(
              StatusCodes.Created,
              SuccessResponse(message = s"Channel ${channelData.id} created successfully")
            )
          case Failure(exc) =>
            complete(
              StatusCodes.InternalServerError,
              ErrorResponse(s"Channel creation failed: ${exc.getMessage}")
            )
        }
      case Failure(exc) =>
        complete(
          StatusCodes.InternalServerError,
          ErrorResponse(s"Channel creation failed: ${exc.getMessage}")
        )
    }
  }

  def deleteChannel(id: Int): Route = ChannelManager.channels.get(id) match {
    case Some((_, _)) =>
      val res = ChannelManager.deleteChannel(id)
      onComplete(res) {
        case Success(_) =>
          complete(
            StatusCodes.OK,
            SuccessResponse(s"Channel $id successfully closed")
        )
        case Failure(exc) =>
          complete(
            StatusCodes.InternalServerError,
            ErrorResponse(s"""Closing channel "$id" FAILED:  ${exc.getMessage}""")
          )
      }
    case None =>
      complete(
        StatusCodes.BadRequest,
        ErrorResponse(s"""Channel with identifier "$id" not found, already deleted?""")
      )
  }

  def listChannels(): Route = {
    complete(
      StatusCodes.OK,
      for (p <- ChannelManager.channels.toList) yield {
        val tc = p._2._2
        ChannelInfo(tc.source,
          tc.inpAlignmentName,
          tc.inpAlignmentVersion,
          tc.outAlignmentName,
          tc.outAlignmentVersion,
          tc.sink,
          p._1,
          s"""${tc.source} -> ({${tc.inpAlignmentName}}:${tc.inpAlignmentVersion} -> {${tc.outAlignmentName}}:${tc.outAlignmentVersion}) -> ${tc.sink}""",
          tc.uuid,
          tc.consumerGroup,
          tc.parallelism
        )
      }
    )
  }

  def listAlignments(): Route = {
    val alignsFuture : Future[Seq[AlignmentData]] = AlignmentsManager.getAlignmentsData

    onComplete(alignsFuture) { aligns =>
      complete(StatusCodes.OK, for (alignData <- aligns.get) yield {
        AlignmentInfo(s"[${alignData.name}] [${alignData.version}] : ${alignData.sourceOntologyURI} -> ${alignData.targetOntologyURI}",
          alignData.id,
          alignData.name,
          alignData.date,
          alignData.sourceOntologyURI,
          alignData.targetOntologyURI,
          alignData.version,
          alignData.creator,
          alignData.description
        )
      })
    }
  }

  private def alignmentDefinitionsMatch(xSrc: String, ySrc: String): Boolean = {
    //TODO: eventually some kind of "Jena-based RDF source normalization" should be done here
    md5Hash(XML.loadString(xSrc).toString) == md5Hash(XML.loadString(ySrc).toString)
  }

  def addAlignment(alignConf: AlignmentConfig): Route = {
    logger.debug(s"Request to add alignment with with ${alignConf.alignmentID}received")
    val errList = AlignmentValidator(alignConf.xmlSource)
    if (errList.nonEmpty) {
      val s = "\n\t- "
      complete(
        StatusCodes.BadRequest,
        ErrorResponse(s"Error validating alignment with ${alignConf.alignmentID}:$s ${errList.mkString(s)}")
      )
    } else {
      // new alignment is syntactically valid, we need to see if it's not a "duplicate"
      val duplicateCheckFuture = AlignmentsManager.findAlignmentDataById(alignConf.alignmentID)
      onComplete(duplicateCheckFuture) {
        case Success(alignDataOpt) =>
          if (alignDataOpt.isEmpty) {
            val newAlignmentData = AlignmentData(
              -1,
              new Timestamp(Calendar.getInstance().getTime.getTime),
              alignConf.name,
              alignConf.sourceOntologyURI,
              alignConf.targetOntologyURI,
              alignConf.version,
              alignConf.creator,
              alignConf.description,
              alignConf.xmlSource
            )
            val res = AlignmentsManager.addAlignment(newAlignmentData)
            onComplete(res) {
              case Success(inserted) =>
                complete(
                  StatusCodes.Created,
                  SuccessResponse(message = s"Alignment with ${inserted.alignmentID} uploaded successfully")
                )
              case Failure(r) =>
                complete(
                  StatusCodes.BadRequest,
                  ErrorResponse(s"""Error inserting alignment with ${alignConf.alignmentID}: ${r.getMessage}""")
                )
            }
          } else {
            // there exists an alignment with the same AlignmentID(name, version)
            val eSrc = alignDataOpt.get.xmlSource
            val nSrc = alignConf.xmlSource
            if (alignmentDefinitionsMatch(eSrc, nSrc)) {
              complete(
                StatusCodes.AlreadyReported,
                SuccessResponse(message = s"Alignment with ${alignConf.alignmentID} already exists")
              )
            } else {
              complete(
                StatusCodes.Conflict,
                ErrorResponse(message = s"Alignment with ${alignConf.alignmentID} already exists but has a different content")
              )
            }
          }
        case Failure(r) =>
          complete(
            StatusCodes.BadRequest,
            ErrorResponse(s"""Error inserting alignment with ${alignConf.alignmentID}: ${r.getMessage}""")
          )
      }
    }
  }

  def deleteAlignment(id: AlignmentID): Route = {

    val dependentChannels = ChannelDAO.countChannelsByAlignmentId(id.name, id.version)

    onComplete(dependentChannels) {
      case Success(0) =>
        val res : Future[Option[AlignmentData]] = AlignmentsManager.findAlignmentDataById(id)
        onComplete(res) {
          case Success(r) =>
            r match {
              case Some(_) =>
                val affectedRowCount: Future[Int] = AlignmentsManager.deleteAlignmentById(id)
                onComplete(affectedRowCount) {
                  case Success(_) =>
                    complete(
                      StatusCodes.OK,
                      SuccessResponse(s"""Alignment with $id successfully deleted""")
                    )
                  case Failure(exc) =>
                    complete(
                      StatusCodes.InternalServerError,
                      ErrorResponse(s"""Deleting alignment with $id FAILED: ${exc.getMessage}""")
                    )
                }
              case None =>
                complete(
                  StatusCodes.BadRequest,
                  ErrorResponse(s"""Alignment with $id not found""")
                )
            }
          case Failure(exc) =>
            complete(
              StatusCodes.InternalServerError,
              ErrorResponse(s"""Deleting alignment with $id FAILED: ${exc.getMessage}""")
            )
        }
      case Success(_) =>
        complete(
          StatusCodes.BadRequest,
          ErrorResponse(s"""Alignment with $id is used by an active channel""")
        )
      case Failure(exc) =>
        complete(
          StatusCodes.InternalServerError,
          ErrorResponse(s"""Deleting alignment with $id FAILED: ${exc.getMessage}""")
        )
    }

  }

  def convertAlignment(conf: AlignmentConfig): Route = {
    logger.debug(s"Request to convert alignment to RDF/XML received")
    val errList = AlignmentValidatorXML(conf.xmlSource)
    if (errList.nonEmpty) {
      complete(
        StatusCodes.BadRequest,
        ErrorResponse(s"Error validating XML alignment with ${errList.mkString("\n\t- ")}")
      )
    } else {
      val f: Future[String] = Future {
        AlignmentConverter.convertFormatToRDFXML(conf.xmlSource)
      }
      onComplete(f) {
        case Success(rdfXmlStr) =>
          val ad = AlignmentData(-1, null, "", "", "", "", "", "", rdfXmlStr) // scalastyle:ignore
          complete(StatusCodes.Created, ad)
        case Failure(exc) =>
          complete(
            StatusCodes.InternalServerError,
            ErrorResponse(s"""Converting alignment format to RDF/XML FAILED: ${exc.getMessage}""")
          )
      }
    }
  }

  def convertAlignmentCellsToTTL(conf: AlignmentConfig): Route = {
    val f: Future[String] = Future {
      AlignmentConverter.convertCellFormat(
        new Alignment(conf.xmlSource),
        CellFormat.Turtle)
    }
    onComplete(f) {
      case Success(rdfXmlStr) =>
        val ad = AlignmentData(-1, null, "", "", "", "", "", "", rdfXmlStr) // scalastyle:ignore
        complete(StatusCodes.Created, ad)
      case Failure(exc) =>
        complete(
          StatusCodes.InternalServerError,
          ErrorResponse(s"""Converting alignment cell format to TTL FAILED: ${exc.getMessage}""")
        )
    }
  }

  def getAlignment(id: AlignmentID): Route = {
    val res : Future[Option[AlignmentData]] = AlignmentsManager.findAlignmentDataById(id)

    onComplete(res) {
      case Success(r) =>
        r match {
          case Some(_) =>
           complete(r.get)
          case None =>
            complete(
              StatusCodes.BadRequest,
              ErrorResponse(s"""Alignment with $id not found""")
            )
        }
      case Failure(exc) =>
        complete(
          StatusCodes.InternalServerError,
          ErrorResponse(s"""Getting alignment with $id FAILED: ${exc.getMessage}""")
        )
    }
  }

  def translate(data: TranslationData): Route = {
    val alignOpts = data.alignIDs.map(id => AlignmentsManager.getAlignment(id))
    logger.debug("Alignment option list: {}", alignOpts)
    if (alignOpts.contains(None)) {
      val errId = data.alignIDs.zip(alignOpts).collectFirst({case n if n._2.isEmpty => n._1}).get
      completeWithError(s"Cannot instantiate alignment with $errId")
    } else {
      Try(new Message(data.graphStr)) match {
        case Success(message) =>
          Try(message.getPayload) match {
            case Success(inpGraph) =>
              Try(alignOpts.foldLeft(inpGraph)((graph, alOpt) => {
                alOpt.get.execute(graph)
              })) match {
                case Success(resGraph) =>
                  message.setPayload(resGraph)
                  complete(
                    StatusCodes.OK,
                    TranslationResponse(
                      "Message translation successful",
                      message.serialize
                    )
                  )
                case Failure(exc) =>
                  completeWithError("Exception applying alignment", exc)
              }
            case Failure(exc) => exc match {
              case _:EmptyPayloadException =>
                logger.trace("Message with empty payload received")
                complete(
                  StatusCodes.OK,
                  TranslationResponse(
                    "Message with empty payload received - no translation applied",
                    data.graphStr
                  )
                )
              case _ =>
                completeWithError("Exception extracting message payload", exc)
            }
          }
        case Failure(exc) =>
          completeWithError("Exception parsing message", exc)
      }

    }
  }

  private def completeWithError(msg: String, exc: Throwable = null): Route = { // scalastyle:ignore
    val errMsg = if (exc != null) {
      s"$msg: ${exc.getMessage}"
    } else {
      msg
    }
    logger.error(errMsg)
    complete(
      StatusCodes.BadRequest,
      TranslationResponse(errMsg, "")
    )
  }

  def validateAlignment(alignConf: AlignmentConfig): Route = {
    logger.debug(s"Request to validate alignment with with ${alignConf.alignmentID}received")
    val errList = AlignmentValidator(alignConf.xmlSource)
    if (errList.nonEmpty) {
      val s = "\n\t- "
      complete(
        StatusCodes.BadRequest,
        ErrorResponse(s"Error validating alignment with ${alignConf.alignmentID}:$s ${errList.mkString(s)}")
      )
    } else {
        complete(
          StatusCodes.Created,
          SuccessResponse(message = s"Alignment with ${alignConf.alignmentID} validated successfully")
        )
    }
  }

}

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

import akka.http.javadsl.model.HttpMethods._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server._
import akka.util.Timeout
import buildinfo.BuildInfo
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import eu.interiot.ipsm.core._
import eu.interiot.ipsm.core.datamodel._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.matching.Regex

trait RestApi extends Resources with JsonSupport with XMLSupport {

  implicit val executionContext: ExecutionContext
  implicit val timeout: Timeout = Timeout(10.seconds)

  val idRE: Regex = """\S+""".r

  private val corsSettings = CorsSettings
    .defaultSettings
    .withAllowCredentials(true)
    .withAllowedMethods(Seq(GET, POST, DELETE, PUT, HEAD, OPTIONS).asJava)

  val api: Route =
    cors(corsSettings) {
      path("channels") {
        get {
          listChannels()
        } ~
          post {
            entity(as[ChannelConfig]) { conf =>
              createChannel(conf)
            }
          }
      } ~
        path("channels" / IntNumber) { id =>
          delete {
            deleteChannel(id)
          }
        } ~
        path("alignments") {
          get {
            listAlignments()
          } ~
            post {
              entity(as[AlignmentConfig]) { conf =>
                addAlignment(conf)
              }
            }
        } ~
        path("validate") {
            post {
              entity(as[AlignmentConfig]) { conf =>
                validateAlignment(conf)
              }
            }
        } ~
        path("convert") {
          post {
            entity(as[AlignmentConfig]) { conf =>
              convertAlignment(conf)
            }
          }
        } ~
      //TODO: Use Segment, instead of "TTL" if we ever offer conversion to cell formats other than TTL
        path("convert" / "TTL") {
          post {
            entity(as[AlignmentConfig]) { conf =>
              convertAlignmentCellsToTTL(conf)
            }
          }
        } ~
        path("alignments" / Segment / Segment) { (name, version) =>
            get {
                getAlignment(AlignmentID(name, version))
            } ~
              delete {
                  deleteAlignment(AlignmentID(name, version))
              }
        } ~
        pathPrefix("swagger") {
          pathEnd {
            redirect("/swagger/", StatusCodes.TemporaryRedirect)
          } ~
            pathSingleSlash {
              getFromResource("swagger/index.html")
            } ~
            getFromResourceDirectory("swagger")
        } ~
        path("translation") {
          post {
            entity(as[TranslationData]) { data =>
              translate(data)
            }
          }
        } ~
        path("version") {
          get {
            complete(StatusCodes.OK, VersionResponse(
              s"${BuildInfo.name}",
              s"${BuildInfo.version}"
            ))
          }
        } ~
        path("terminate") {
          for (reaper <- system.actorSelection("/user/reaper").resolveOne()) {
            reaper ! Reaper.AllDone
          }
          complete(StatusCodes.OK, SuccessResponse("IPSM shutdown initiated"))
        } ~
        path("logging" / Segment) { level =>
          post {
            import ch.qos.logback.classic.{Level, LoggerContext}
            import org.slf4j.LoggerFactory
            val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
            val newLevel = level.toLowerCase match {
              case "all" => Level.ALL
              case "trace" => Level.TRACE
              case "debug" => Level.DEBUG
              case "info" => Level.INFO
              case "warn" => Level.WARN
              case "error" => Level.ERROR
              case _ => Level.OFF
            }
            val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
            rootLogger.setLevel(newLevel)
            complete(StatusCodes.OK, LoggingResponse(
              "IPSM root logging level set",
              s"${rootLogger.getEffectiveLevel}"
            ))
          }
        } ~
        path("logging") {
          get {
            import ch.qos.logback.classic.LoggerContext
            import org.slf4j.LoggerFactory
            val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
            val level = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getEffectiveLevel
            complete(StatusCodes.OK, LoggingResponse(
              "IPSM root logging level retrieved",
              s"$level"
            ))
          }
        }
    }

}

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

package eu.interiot.ipsm.core.channels

import java.util.Calendar

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.scalalogging.LazyLogging
import eu.interiot.ipsm.core._
import eu.interiot.ipsm.core.alignments.AlignmentsManager
import eu.interiot.ipsm.core.config.KafkaBootstrap
import eu.interiot.ipsm.core.datamodel._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object ChannelManager extends LazyLogging {

  import akka.kafka.scaladsl.Consumer
  import akka.kafka.{ConsumerSettings, Subscriptions}
  import org.apache.kafka.clients.consumer.ConsumerConfig
  import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

  import collection.mutable.{Map => MMap}

  val channels: MMap[Int, (ActorMaterializer, ChannelData)] = MMap()


  def loadPredefChannels(implicit system: ActorSystem): Unit = {
    val channelsFuture: Future[Seq[ChannelData]] = ChannelDAO.getChannels

    channelsFuture.foreach {
      channelData => {
        for (chan <- channelData.toList) loadPredef(chan, system)
      }
    }
  }

  private def loadPredef(chanData : ChannelData, system: ActorSystem) : Unit = {

    val chanConf = ChannelConfig(chanData.source,
      chanData.inpAlignmentName,
      chanData.inpAlignmentVersion,
      chanData.outAlignmentName,
      chanData.outAlignmentVersion,
      chanData.sink,
      Some(chanData.consumerGroup),
      Some(chanData.parallelism)
    )
    val saved: Future[ActorMaterializer] = ChannelManager.materializeChannel(chanData.uuid, chanConf, system)

    saved.andThen {
      case Success(done) =>
        channels(chanData.id) = (done, chanData)
      case Failure(exc) =>
        logger.error("Creating predefined channel failed {} failed: {}", chanData.id, exc.getMessage)
    }

  }

  private lazy val bootstrap: String = {
    //noinspection FieldFromDelayedInit
    val bs = Main.params.bootstrap.getOrElse(KafkaBootstrap.default)
    val hostname = s"${bs.hostname}:${bs.port}"
    logger.info("Kafka bootstrap server: {}", hostname)
    hostname
  }

  private val decider: Supervision.Decider = {
    case NonFatal(_) =>
      Supervision.Resume
    case _ =>
      Supervision.Stop
  }

  //noinspection ScalaStyle
  // scalastyle:off method.length
  def materializeChannel(chanUUID: String,
                         chanConf: ChannelConfig,
                         actorSystem: ActorSystem): Future[ActorMaterializer] = Future {

    val localMat = ActorMaterializer(ActorMaterializerSettings(actorSystem)
      .withSupervisionStrategy(decider))(actorSystem)

    val producerSettings = ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(bootstrap)

    val producer = producerSettings.createKafkaProducer()

    val consumerSettings = ConsumerSettings(actorSystem, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrap)
      .withGroupId(chanConf.optConsumerGroup.get)
      .withProperty("dual.commit.enabled", "false")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

    //TODO: make it predefined
    val identityAlignmentId = AlignmentID("", "")

    val inpAlignment = if (chanConf.inpAlignmentId == identityAlignmentId) {
      null // scalastyle:ignore
    } else {
      AlignmentsManager
        .getAlignment(chanConf.inpAlignmentId)
        .getOrElse(throw new Exception("Input alignment instantiation FAILED"))
    }

    val outAlignment = if (chanConf.outAlignmentId == identityAlignmentId) {
      null // scalastyle:ignore
    } else {
      AlignmentsManager
        .getAlignment(chanConf.outAlignmentId)
        .getOrElse(throw new Exception("Output alignment instantiation FAILED"))
    }

    val parallelism = {
      val arg = chanConf.parallelism.getOrElse(1)
      if (arg > 0) {
        arg
      } else {
        1
      }
    }

    Consumer.committableSource(consumerSettings, Subscriptions.topics(chanConf.source))
      .mapAsync(parallelism)(msg => Future {
        val rec = msg.record
        val startTime = Calendar.getInstance.getTimeInMillis
        logger.debug("topic: {}, partition: {}, value: {}, offset: {}", rec.topic(), rec.partition(), rec.value(), rec.offset())
        val message = new Message(msg.record.value())
        Try(message.getPayload) match {
          case Success(inpGraph) =>
            val outGraph = (inpAlignment, outAlignment) match {
              case (null, null) => // scalastyle:ignore
                logger.trace("Both input and output are IDENTITY alignments - graph unchanged")
                inpGraph
              case (null, outA) => // scalastyle:ignore
                logger.trace("IDENTITY input alignment - applying only output alignment")
                outA.execute(inpGraph)
              case (inpA, null) => // scalastyle:ignore
                logger.trace("IDENTITY output alignment - applying only input alignment")
                inpA.execute(inpGraph)
              case (inpA, outA) =>
                logger.trace("Applying both input and output alignment")
                outA.execute(inpA.execute(inpGraph))
            }
            (outGraph, message, msg.committableOffset, startTime)
          case Failure(exc) => exc match {
            case _:EmptyPayloadException =>
              logger.trace("Message with empty payload received")
              (null, message, msg.committableOffset, startTime) // scalastyle:ignore
            case _ =>
              logger.error("Exception extracting message payload: {}", exc.getMessage)
              throw exc
          }
        }
      })
      .mapAsync(parallelism)( out => Future {
        val (model, message, offset, st) = out
        if (model != null) {
          logger.trace("Setting the translated graph as the payload")
          message.setPayload(model)
        }
        logger.debug("Returning translated message to Kafka: {}", message.serialize)
        ProducerMessage.Message(new ProducerRecord[Array[Byte], String](
          chanConf.sink,
          message.serialize
        ), (offset, st))
      })
      .via(Producer.flexiFlow(producerSettings))
      .alsoTo(Sink.foreach { msg =>
        val (_, st) = msg.passThrough
        producer.send(new ProducerRecord(
          s"mon-$chanUUID",
          0,
          null: Array[Byte], // scalastyle:ignore
          st.toString + ";" + Calendar.getInstance.getTimeInMillis.toString
        ))
      })
      .mapAsync(parallelism) { result =>
        result.passThrough._1.commitScaladsl()
      }
      .runWith(Sink.ignore)(localMat)
    localMat
  }
  // scalastyle:on

  def checkIfExists(sink: String): Boolean = {
    channels.values.exists(conf => conf._2.sink == sink)
  }

  def createChannel(mat: ActorMaterializer, chanConf: ChannelConfig, chanUUID: String): Future[ChannelData] = {
     val res = ChannelDAO.addChannel(chanConf, chanUUID)

     res.andThen {
        case Success(channelData) =>
          channels(channelData.id) = (mat, channelData)
        case Failure(exc) =>
          mat.shutdown()
          throw exc
     }
  }

  def deleteChannel(id: Int): Future[Int] =  {
    ChannelDAO.deleteChannelById(id) andThen {
      case Success(_) =>
        channels(id)._1.shutdown()
        if (channels(id)._1.isShutdown) {
          channels.remove(id)
        }
      case Failure(e) =>
        e.printStackTrace()
        throw e
    }
  }

}

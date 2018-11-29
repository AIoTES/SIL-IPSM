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

package eu.interiot.ipsm.core

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import eu.interiot.ipsm.core.alignments.AlignmentsManager
import eu.interiot.ipsm.core.channels.ChannelManager

import scala.util.{Failure, Success}
import eu.interiot.ipsm.core.datamodel._
import org.rogach.scallop._
import eu.interiot.ipsm.core.rest.RestApi
import eu.interiot.ipsm.core.config.KafkaBootstrap

import scala.concurrent.ExecutionContextExecutor

object Main extends App with RestApi {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val bootstrap: ScallopOption[KafkaBootstrap] = opt()(KafkaBootstrap.hostnameConverter)
    verify()
  }

  val params = new Conf(args)

  IPSMModel.initializeSchema onComplete {
    case Success(_) =>
      logger.info(s"IPSM database initialized successfully")
      ChannelManager.loadPredefChannels
      logger.info(s"Loaded channels from database")
      AlignmentsManager.loadPredefAlignments()
      logger.info(s"Loaded alignments from database")

      Http().bindAndHandle(handler = api, interface = host, port = port) onComplete {
        case Success(binding) =>
          logger.info(s"REST interface bound to http:/${binding.localAddress}")
          system.actorOf(Props(new Reaper(IPSMModel.db, binding)), name = "reaper")
        case Failure(ex) =>
          logger.error(s"REST interface could not bind to $host:$port", ex.getMessage)
          logger.info("Shutting down alignment repository")
          IPSMModel.db.close()
          logger.info("Shutting down IPSM")
          system.terminate()
      }
    case Failure(ex) =>
      logger.error(s"DB initialization failed: ${ex.getMessage}")
      IPSMModel.db.close()
      logger.error("Shutting down IPSM")
      system.terminate()
  }

}

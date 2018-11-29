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

package eu.interiot.ipsm.core.config

import com.typesafe.config.ConfigFactory
import org.rogach.scallop.{ArgType, ValueConverter}

import scala.reflect.runtime.universe
import scala.util.matching.Regex

object KafkaBootstrap {
  private val config = ConfigFactory.load()
  private val kafkaHost = config.getString("ipsm-kafka.host")
  private val kafkaPort = config.getInt("ipsm-kafka.port")
  val default = KafkaBootstrap(kafkaHost, kafkaPort)
  val hostnameConverter: ValueConverter[KafkaBootstrap] = new ValueConverter[KafkaBootstrap] {
    val nameRgx:Regex = """([a-z0-9\.]+):(\d+)""".r
    // parse is a method, that takes a list of arguments to all option invokations:
    // for example, "-a 1 2 -a 3 4 5" would produce List(List(1,2),List(3,4,5)).
    // parse returns Left with error message, if there was an error while parsing
    // if no option was found, it returns Right(None)
    // and if option was found, it returns Right(...)
    def parse(s:List[(String, List[String])]): Either[String, Option[KafkaBootstrap]] = s match {
      case (_, nameRgx(hname, port) :: Nil) :: Nil =>
        Right(Some(KafkaBootstrap(hname, port.toInt)))
      case Nil =>
        Right(None)
      case _ =>
        Left("Provide a valid Kafka server hostname and port")
    }

    // some "magic" to make typing work
    val tag: universe.TypeTag[KafkaBootstrap] = reflect.runtime.universe.typeTag[KafkaBootstrap]
    val argType: ArgType.V = org.rogach.scallop.ArgType.SINGLE
  }
}

case class KafkaBootstrap(hostname: String, port: Int) {
  override def toString: String = s"$hostname:$port"
}

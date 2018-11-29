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

package eu.interiot.ipsm.core.message.test

import eu.interiot.ipsm.core.datamodel.Message
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class MessagePayloadTests extends FlatSpec with Matchers {

  val fnames = List(
    "inp/demoMsg1",
    "inp/demoMsg1CO",
    "inp/demoMsg2",
    "inp/demoMsg3",
    "inp/demoMsg3CO",
    "inp/demoMsg34",
    "inp/demoMsg34CO",
    "inp/demoMsg5",
    "inp/demoMsg6"
  )

  fnames.foreach { name =>
    s"""Message "data/$name.json"""" should "parse into (non-null) Jena Dataset" in {
      val jsonFile = s"data/$name.json"
      val jsoSource = Source.fromResource(jsonFile).getLines().mkString("\n")
      val dataSet = new Message(jsoSource)
      dataSet shouldNot be (null)  // scalastyle:ignore
    }

    it should """contain payload""" in {
      val jsonFile = s"data/$name.json"
      val jsonSource = Source.fromResource(jsonFile).getLines().mkString("\n")
      val dataSet = new Message(jsonSource)
      dataSet.getPayload shouldNot be (null)  // scalastyle:ignore
    }
  }

}

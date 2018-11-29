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

package eu.interiot.ipsm.core.alignments.test

import java.io.{File, FileOutputStream, PrintWriter}

import com.typesafe.scalalogging.LazyLogging
import eu.interiot.ipsm.core.datamodel.Message
import eu.interiot.ipsm.core.alignments.Alignment
import org.apache.jena.rdf.model.ModelFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class AlignmentApplicationTest extends FlatSpec with Matchers with LazyLogging {

  val fnames = List(
    ("weight.UniversAAL_CO", "CO_FIWARE"),
    ("bloodPressure.UniversAAL_CO", "CO_FIWARE"),
    ("Noatum", "NOATUM_CO"),
    ("bc-activity", "BodyCloud_CO"),
    ("bc-bloodPressure", "BodyCloud_CO"),
    ("bc-questionnaire", "BodyCloud_CO"),
    ("bc-weight", "BodyCloud_CO")
  )

  fnames.foreach { pair =>
    val (name, alignment) = pair

    s"inp/$name.json" should s"""succesfully translate via $alignment""" in {

      val jsonSource = Source.fromResource(s"data/inp/$name.json").getLines().mkString("\n")

      val xmlFile = s"data/alignments/$alignment.rdf"
      val xmlSource = Source.fromResource(xmlFile).getLines().mkString("\n")

      val message = new Message(jsonSource)
      val inpGraph = message.getPayload

      val inpGraphCopy = if (logger.underlying.isDebugEnabled) {
        ModelFactory.createDefaultModel().add(inpGraph)
      } else {
        null // scalastyle:ignore
      }

      val inpPayloadStr = if (logger.underlying.isDebugEnabled) {
        message.serializePayload
      } else {
        ""
      }

      val align = new Alignment(xmlSource)
      val outGraph = align.execute(inpGraph)
      message.setPayload(outGraph)

      new File("target/output").mkdirs()
      val writer = new PrintWriter(new FileOutputStream(s"target/output/$name.$alignment.json", false))
      writer.write(message.serialize)
      writer.close()

      logger.debug(
        s"""Translating "${pair._1}" via "${pair._2}"
           |SOURCE PAYLOAD:
           |$inpPayloadStr
           |${"-" * 80}
           |""".stripMargin)

      outGraph should not be null
      // TODO: The test below should eventually check wheather the outGraph is isomorphic to a predefined result
      // but there are some problems with checking it with Apache Jena ...
      outGraph.isIsomorphicWith(inpGraphCopy) should be (false)
    }
  }
}

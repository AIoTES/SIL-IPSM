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

import org.scalatest.{FlatSpec, Matchers}
import eu.interiot.ipsm.core.alignments.AlignmentValidator

import scala.io.Source

class AlignmentValidationTest extends FlatSpec with Matchers {

  val fnames = List(
    "UniversAAL_CO",
    "CO_FIWARE",
    "NOATUM_CO",
    "BodyCloud_CO"
  )

  fnames.foreach { name =>
    s"data/alignments/$name.rdf" should s"""validate against structure restrictions""" in {
      val xmlFile = s"data/alignments/$name.rdf"
      val xmlSource = Source.fromResource(xmlFile).getLines().mkString("\n")
     AlignmentValidator.validateWithJena(xmlSource) shouldEqual None
    }

    it should  """have valid alignment steps: """ + s"data/alignments/$name.rdf" in {
      val xmlFile = s"data/alignments/$name.rdf"
      val xmlSource = Source.fromResource(xmlFile).getLines().mkString("\n")
      val av = new AlignmentValidator(xmlSource)
      av.validateSteps() shouldEqual None
    }

    it should "for each cell have all <transformation> param variables contained in <entity1>: " + s"data/alignments/$name.rdf" in {
      val xmlFile = s"data/alignments/$name.rdf"
      val xmlSource = Source.fromResource(xmlFile).getLines().mkString("\n")
      val av = new AlignmentValidator(xmlSource)
      av.validateParamVariables() shouldEqual List()
    }

    it should "for each cell have all <entity2> variables contained in <entity1> and <return>: " + s"data/alignments/$name.rdf" in {
      val xmlFile = s"data/alignments/$name.rdf"
      val xmlSource = Source.fromResource(xmlFile).getLines().mkString("\n")
      val av = new AlignmentValidator(xmlSource)
      av.validateEntity2Variables() shouldEqual List()
    }

  }

}

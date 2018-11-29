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
import eu.interiot.ipsm.core.alignments.{Alignment, AlignmentValidator}
import org.scalatest.{FlatSpec, Matchers}


class AlignmentCompilerTest extends FlatSpec with Matchers with LazyLogging {
  val fnames = List(
     "BodyCloud_CO",
     "CO_FIWARE",
     "NOATUM_CO",
     "UniversAAL_CO"
  )

  fnames.foreach { an =>
    val alignmentName = an.replace("/","_")
    val alignment_path = getClass.getResource(s"/data/alignments/$an.rdf").getPath
    val alignment = xml.XML.loadFile(alignment_path)

    val validationResult = AlignmentValidator(alignment.toString)
    if (validationResult.nonEmpty) {
      println()
      for (err <- validationResult) {
        println(s"Alignment '$an':\n")
        println(s"\tERROR: $err")
      }
      System.exit(1)
    }

    new File("target/output/alignments").mkdirs()
    val writer = new PrintWriter(new FileOutputStream(s"target/output/alignments/$alignmentName.sparql", false))

    writer.write(s"#${"=" * 79}\n")
    writer.write(s"# $alignmentName\n")
    writer.write(s"#${"=" * 79}\n")
    val align = new Alignment(alignment.toString())
    for (cId <- align.alignmentCells.keys) {
      val cell = align.alignmentCells(cId)
      writer.write(cell.toString)
      writer.write(s"#${"=" * 79}\n")
      s"""$alignmentName "${cell.cellId}"""" should "successfully compile to SPARQL" in {
        val cbn = cell != null
        cbn shouldEqual true
      }
    }
    writer.close()
  }
}

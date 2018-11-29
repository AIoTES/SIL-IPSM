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

package eu.interiot.ipsm.core.alignments

import java.io.ByteArrayInputStream

import eu.interiot.ipsm.core.alignments.CellFormat.CellFormat
import eu.interiot.ipsm.core.config.Prefixes
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory, QuerySolution}
import org.apache.jena.rdf.model.{Model, ModelFactory, Resource}

import scala.collection.JavaConverters._
import scala.xml.{Node, XML}


class Alignment(val xmlSource: String) {

  private[alignments] val (xmlCells, stepSeq) = getXmlAlignmentData(xmlSource)
  private[alignments] val (rdfCells, cellFormat, prefixMap) = getRdfAlignmentData(xmlSource)

  private[alignments] val alignmentCells = {
    val cells = {
      for (id <- xmlCells.keys) yield {
        id -> new AlignmentCell(xmlCells(id), rdfCells(id), id, cellFormat, prefixMap)
      }
    }.toMap
    cells
  }

  override def toString: String = {
    val cellUpdates = for (key <- alignmentCells.keys) yield {
      s"\n${alignmentCells(key).toString}\n"
    }
    cellUpdates.mkString("\n") + outputSep +
      s"""\nAlignment steps:\n$outputSep\n${stepSeq.mkString("\n")}\n$outputSep\n"""
  }

  def execute(model: Model): Model = {
    var result = model
    for (step <- stepSeq) {
      result = alignmentCells(step).applyCell(result)
    }
    result
  }

  private def getXmlAlignmentData(alignmentSrc: String): (Map[String, Node], Seq[String]) = {
    val alignmentXML = XML.loadString(alignmentSrc)
    val cellNodesXML = (alignmentXML \ "Alignment" \ "map" \ "Cell").filter(_.namespace == Prefixes("align"))
    val cellsXML = {
      for (c <- cellNodesXML) yield {
        val id = cellNameRE.replaceFirstIn(getAttribute(c, Prefixes("rdf"), "about"), "$1")
        id -> c
      }
    }.toMap
    val steps = (alignmentXML \ "Alignment" \ "steps" \ "step").filter(_.namespace == Prefixes("sripas"))
    val stepSeq: Seq[String] = for (step <- steps) yield {
      getAttribute(step, Prefixes("sripas"), "cell").replace(Prefixes("sripas"), "")
    }
    (cellsXML, stepSeq)
  }

  private def getRdfAlignmentData(alignmentSrc: String): (Map[String, Resource], CellFormat, Map[String, String]) = {
    val model = ModelFactory.createDefaultModel()
    model.read(new ByteArrayInputStream(xmlSource.getBytes()), Prefixes("sripas"))
    val cellsRDF = {
      for (c <- getCells(model)) yield {
        val id = (c.getNameSpace + c.getLocalName)
          .replace(Prefixes("sripas"), "")
          .replace("http://www.inter-iot.eu/", "")
        id -> c
      }
    }.toMap
    (cellsRDF, getCellFormat(model), model.getNsPrefixMap.asScala.toMap)
  }

  private def getCells(model: Model): List[Resource] = {
    val selectQueryStr =
      """|
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>
         |PREFIX sripas: <http://www.inter-iot.eu/sripas#>
         |SELECT ?c
         |WHERE {
         |  ?a rdf:type align:Alignment.
         |  ?a align:map ?c .
         |}
         |""".stripMargin
    val query = QueryFactory.create(selectQueryStr)
    val executionFactory = QueryExecutionFactory.create(query, model)
    val answers = executionFactory.execSelect()
    val result = answers.asScala.toList map { sol: QuerySolution =>
      sol.get("c").asResource()
    }
    executionFactory.close()
    result
  }

  private def getCellFormat(model: Model): CellFormat = {
    val selectQueryStr =
      """|
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>
         |PREFIX sripas: <http://www.inter-iot.eu/sripas#>
         |SELECT ?cf
         |WHERE {
         |  ?a rdf:type align:Alignment.
         |  ?a sripas:cellFormat ?cf .
         |}
         |""".stripMargin
    val query = QueryFactory.create(selectQueryStr)
    val executionFactory = QueryExecutionFactory.create(query, model)
    val answers = executionFactory.execSelect()
    val result = answers.asScala.toSet map { sol: QuerySolution =>
      s"""${sol.get("cf").asResource().getLocalName}"""
    }
    executionFactory.close()
    result.headOption.getOrElse("rdfxml") match {
      case "turtle" => CellFormat.Turtle
      case _ => CellFormat.RdfXml
    }
  }

}

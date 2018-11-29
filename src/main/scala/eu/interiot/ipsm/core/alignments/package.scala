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

import eu.interiot.ipsm.core.config.Prefixes

import scala.xml.{Node, NodeSeq}

package object alignments {

  type Triplet = (String, String, String)
  type Triplets = List[Triplet]
  private[alignments] def tripletToString(t: Triplet): String = {
    s"\t${t._1} ${t._2} ${t._3} ."
  }

  object CellFormat extends Enumeration {
    type CellFormat = Value
    val Turtle, RdfXml = Value
    private val turtleURI = "http://inter-iot.eu/sripas#turtle"
    private val rdfxmlURI = "http://inter-iot.eu/sripas#rdfxml"
    implicit class CellFormatOps(cf: CellFormat) {
      def getUri: String = cf match {
        case Turtle => turtleURI
        case RdfXml => rdfxmlURI
      }
    }
    def fromUri(uri: String): Option[CellFormat] = uri match {
      case `turtleURI` => Option(Turtle)
      case `rdfxmlURI` => Option(RdfXml)
      case _ => Option.empty
    }
  }

  private[alignments] val cellNameRE = s"""${Prefixes("sripas")}(.+)$$""".r
  private[alignments] val nodeRe = s"""${Prefixes("sripas")}(.+)$$""".r
  private[alignments] val entityRe = s"""${Prefixes("align")}entity(.+)$$""".r
  private[alignments] val typingsRe = s"""${Prefixes("sripas")}typings$$""".r
  private[alignments] val datatypeRe = "^\\s*([^\\^]+)\\^\\^(\\S*)\\s*$".r

  private[alignments] val outputSep = "=" * 79

  private[alignments] def getAttribute(node: Node, prefix: String, name: String) : String = {
    var attrValue = ""
    node.attribute(prefix, name) match {
      case Some(attr) =>
        attrValue = attr.text
      case None =>
        attrValue = node.attribute(name).getOrElse(Seq()).text
    }
    attrValue
  }

  private[alignments] def cellFunctionParams(ns: NodeSeq): Seq[CellFunctionParam] = {
    val pars = for {
      p <- ns
    }  yield {
      val parOrder = p.attribute(Prefixes("sripas"), "order") match {
        case Some(_) => p.attribute(Prefixes("sripas"), "order")
        case None => p.attribute("order")
      }
      val parAbout = p.attribute(Prefixes("sripas"), "about") match {
        case Some(_) => p.attribute(Prefixes("sripas"), "about")
        case None => p.attribute("about")
      }
      val parVal = p.attribute(Prefixes("sripas"), "val") match {
        case Some(_) => p.attribute(Prefixes("sripas"), "val")
        case None => p.attribute("val")
      }
      val parTyp = p.attribute(Prefixes("sripas"), "datatype") match {
        case Some(_) => p.attribute(Prefixes("sripas"), "datatype")
        case None => p.attribute("datatype")
      }
      (parOrder, parAbout, parVal, parTyp)
    }
    pars.toList.collect({ // anonymous pattern matching function
      case (Some(order), Some(variable), _, _) =>
        CellFunctionVarParam(order.text.toInt, variable.text)
      case (Some(order), None, Some(value), Some(valTyp)) =>
        CellFunctionValParam(order.text.toInt,value.text, Some(valTyp.text))
      case (Some(order), None, Some(value), None) =>
        CellFunctionValParam(order.text.toInt,value.text)
    }).sortBy(_.position)
  }

  private[alignments] def cellFunctionBinding(nodes: NodeSeq): String = {
    // assumption: <return> element is valid, in particular nodes.size == 1
    getAttribute(nodes.head, Prefixes("sripas"), "about")
  }

  private[alignments] def functionName(node: Node): String = {
    // assumption: XML function node is valid
    getAttribute(node, Prefixes("sripas"), "about")
  }

}

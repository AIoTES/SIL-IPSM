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

import eu.interiot.ipsm.core.datamodel.{AlignmentConfig, AlignmentData}
import eu.interiot.ipsm.core.config.Prefixes
import akka.http.scaladsl.marshalling.{Marshaller, _}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.xml.XML

trait XMLSupport {

  implicit val um : Unmarshaller[HttpEntity, AlignmentConfig] = {
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaTypes.`application/xml`).mapWithCharset { (data, charset) ⇒
      val input =
        if (charset == HttpCharsets.`UTF-8`) {
          data.utf8String
        } else {
          data.decodeString(charset.nioCharset.name)
        }
      val xml = XML.loadString(input)

      AlignmentConfig((xml \ "Alignment" \ "title").filter(_.namespace == Prefixes("dc")).text,
        (xml \ "Alignment" \ "onto1" \ "Ontology" \ ("@{" + Prefixes("rdf") + "}about")).text,
        (xml \ "Alignment" \ "onto2" \ "Ontology" \ ("@{" + Prefixes("rdf") + "}about")).text,
        (xml \ "Alignment" \ "version").filter(_.namespace == Prefixes("exmo")).text,
        (xml \ "Alignment" \ "creator").filter(_.namespace == Prefixes("dc")).text,
        (xml \ "Alignment" \ "description").filter(_.namespace == Prefixes("dc")).text, input
      )
    }
  }

  private val `xml` =  MediaType.applicationWithFixedCharset("xml", HttpCharsets.`UTF-8`)

  implicit def alignmentMarshaller: ToEntityMarshaller[AlignmentData] = Marshaller.oneOf(
    Marshaller.withFixedContentType(`xml`) { align ⇒
      HttpEntity(`xml`, align.xmlSource)
    })

}

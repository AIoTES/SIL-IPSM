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

object Prefixes {

  import collection.mutable.{Map => MMap}

  val prefs: MMap[String, String] = MMap(
    "sripas" -> "http://www.inter-iot.eu/sripas#",
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "ont1" -> "http://www.example.org/ontology1#",
    "ont2" -> "http://www.example.org/ontology2#",
    "ont3" -> "http://www.example.org/ontology3#",
    "ont4" -> "http://www.example.org/ontology4#",
    "ont5" -> "http://www.example.org/ontology5#",
    "ont6" -> "http://www.example.org/ontology6#",
    "owl" -> "http://www.w3.org/2002/07/owl#",
    "xsd" -> "http://www.w3.org/2001/XMLSchema#",
    "iot-lite" -> "http://purl.oclc.org/NET/UNIS/fiware/iot-lite#",
    "ns" -> "http://creativecommons.org/ns#",
    "georss" -> "http://www.georss.org/georss/",
    "OrientDB" -> "http://www.dewi.org/WDA.owl#OrientDB",
    "ACS" -> "http://www.dewi.org/ACS.owl#",
    "WDA" -> "http://www.dewi.org/WDA.owl#",
    "terms" -> "http://purl.org/dc/terms/",
    "xml" -> "http://www.w3.org/XML/1998/namespace#",
    "wgs84_pos" -> "http://www.w3.org/2003/01/geo/wgs84_pos#",
    "DUL" -> "http://www.loa-cnr.it/ontologies/DUL.owl#",
    "foaf" -> "http://xmlns.com/foaf/0.1/",
    "dc" -> "http://purl.org/dc/elements/1.1/",
    "my_port" -> "http://example.sripas.org/ontologies/port.owl#",
    "exmo" -> "http://exmo.inrialpes.fr/align/ext/1.0/#",
    "align" -> "http://knowledgeweb.semanticweb.org/heterogeneity/alignment#",
    "iiot" -> "http://inter-iot.eu/GOIoTP#",
    "iiotex" -> "http://inter-iot.eu/GOIoTPex#"
  )

  def apply(key: String): String = prefs(key)

  def get(key: String): Option[String] = prefs.get(key)

  def set(key: String, prefix: String): Unit = {
    prefs += (key -> prefix)
  }

}

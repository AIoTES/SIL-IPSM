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

package eu.interiot.ipsm.core.datamodel

import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.{JsonLDWriteContext, Lang, RDFDataMgr, RDFFormat}
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

import com.github.jsonldjava.core.JsonLdOptions
import org.apache.jena.riot.system.RiotLib

case class EmptyPayloadException(private val message: String = "", private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

class Message(jsonLdStr: String) {

  private val URIBaseINTERIoT = "http://inter-iot.eu/"
  private val URIBaseMessage: String = URIBaseINTERIoT + "message/"
  private val URImessagePayloadGraph = URIBaseMessage + "payload"

  private val messageDataset = readDatasetFromJSONLD(jsonLdStr)

  def getPayload: Model = {
    if (messageDataset.containsNamedModel(URImessagePayloadGraph)) {
      messageDataset.getNamedModel(URImessagePayloadGraph)
    } else {
      throw EmptyPayloadException()
    }
  }

  def setPayload(model: Model): Unit = {
    //messageDataset.replaceNamedModel(URImessagePayloadGraph, model)
    //Replace resulted in empty payload graph. Add works as replace.
    messageDataset.addNamedModel(URImessagePayloadGraph, model)
  }

  def serialize: String = writeDatasetToJSONLD()

  def serializePayload: String = payloadAsJsonLd()

  def serializePayloadAsRdfXml: String = payloadAsRdfXml()

  private def readDatasetFromJSONLD(jsonLdString: String): Dataset = {
    val jenaDataset = DatasetFactory.create
    val inStream = new ByteArrayInputStream(jsonLdString.getBytes)
    RDFDataMgr.read(jenaDataset, inStream, Lang.JSONLD)
    jenaDataset
  }

  private def payloadAsRdfXml(): String = {
    val outStream = new ByteArrayOutputStream
    RDFDataMgr.write(outStream, messageDataset.getNamedModel(URImessagePayloadGraph), Lang.RDFXML)
    outStream.toString
  }

  private def payloadAsJsonLd(): String = {
    val outStream = new ByteArrayOutputStream
    RDFDataMgr.write(outStream, messageDataset.getNamedModel(URImessagePayloadGraph), Lang.JSONLD)
    outStream.toString
  }

  private def writeDatasetToJSONLD(): String = {
    val writer = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_COMPACT_PRETTY)

    val outStream = new ByteArrayOutputStream

    writer.write(outStream,
      messageDataset.asDatasetGraph(),
      RiotLib.prefixMap(messageDataset.asDatasetGraph()),
      null, // scalastyle:ignore
      Message.jsonLDWriteContext)

    outStream.toString
  }

}

object Message {
  def apply(jsonLdStr: String): Message = new Message(jsonLdStr)

  private val jsonLDWriteContext = {
    val tContext = new JsonLDWriteContext()
    val opts = new JsonLdOptions()
    //      opts.setEmbed(false)
    //      opts.setExplicit(true)
    //      opts.setOmitDefault(false)
    opts.setUseRdfType(true)
    tContext.setOptions(opts)
    tContext
  }
}
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

import java.sql.Timestamp

final case class ChannelConfig(source: String,
                               inpAlignmentName: String,
                               inpAlignmentVersion: String,
                               outAlignmentName: String,
                               outAlignmentVersion: String,
                               sink: String,
                               optConsumerGroup: Option[String] = None,
                               parallelism: Option[Int] = None) {
  def inpAlignmentId: AlignmentID = AlignmentID(inpAlignmentName, inpAlignmentVersion)
  def outAlignmentId: AlignmentID = AlignmentID(outAlignmentName, outAlignmentVersion)
}

final case class ChannelInfo(source: String,
                             inpAlignmentName: String,
                             inpAlignmentVersion: String,
                             outAlignmentName: String,
                             outAlignmentVersion: String,
                             sink: String, id: Int,
                             descId: String,
                             uuid: String,
                             consumerGroup: String,
                             parallelism: Int)

final case class ChannelData(source: String,
                             inpAlignmentName: String,
                             inpAlignmentVersion: String,
                             outAlignmentName: String,
                             outAlignmentVersion: String,
                             sink: String,
                             id: Int,
                             uuid: String,
                             consumerGroup: String,
                             parallelism: Int)

final case class SuccessResponse(message: String)

final case class ErrorResponse(message: String)

final case class LoggingResponse(message: String, level: String)

final case class VersionResponse(name: String, version: String)


final case class AlignmentInfo(descId: String,
id: Int,
name: String,
date: Timestamp,
sourceOntologyURI: String,
targetOntologyURI: String,
version: String,
creator: String,
description: String)

final case class AlignmentConfig(name: String,
sourceOntologyURI: String,
targetOntologyURI: String,
version: String,
creator: String,
description: String,
xmlSource: String) {
  def alignmentID: AlignmentID = AlignmentID(name, version)
}

final case class AlignmentData(id: Int,
date: Timestamp,
name: String,
sourceOntologyURI: String,
targetOntologyURI: String,
version: String,
creator: String,
description: String,
xmlSource: String) {
  def alignmentID: AlignmentID = AlignmentID(name, version)
}

final case class TranslationData(alignIDs: List[AlignmentID], graphStr: String)

final case class TranslationResponse(message: String, graphStr: String)

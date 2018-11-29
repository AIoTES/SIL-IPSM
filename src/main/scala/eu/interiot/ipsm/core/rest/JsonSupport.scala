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

import java.sql.Timestamp

//import eu.interiot.ipsm.core._
import eu.interiot.ipsm.core.datamodel._
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsValue, JsonFormat, RootJsonFormat}

object IPSMJsonProtocol extends DefaultJsonProtocol {
  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    def write(obj: Timestamp): JsValue = JsNumber(obj.getTime)

    def read(json: JsValue): Timestamp = json match {
      case JsNumber(time) => new Timestamp(time.toLong)
      case _ => throw DeserializationException("Date expected")
    }
  }
}

trait JsonSupport {

  import IPSMJsonProtocol._

  implicit val errorFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse)
  implicit val responseFormat: RootJsonFormat[SuccessResponse] = jsonFormat1(SuccessResponse)
  implicit val loggingResponseFormat: RootJsonFormat[LoggingResponse] = jsonFormat2(LoggingResponse)
  implicit val versionResponseFormat: RootJsonFormat[VersionResponse] = jsonFormat2(VersionResponse)
  implicit val alignmentInfoFormat: RootJsonFormat[AlignmentInfo] = jsonFormat9(AlignmentInfo)
  implicit val channelConfigFormat: RootJsonFormat[ChannelConfig] = jsonFormat8(ChannelConfig)
  implicit val channelInfoFormat: RootJsonFormat[ChannelInfo] = jsonFormat11(ChannelInfo)
  implicit val alignemntIDFormat: RootJsonFormat[AlignmentID] = jsonFormat2(AlignmentID)
  implicit val translationDataFormat: RootJsonFormat[TranslationData] = jsonFormat2(TranslationData)
  implicit val translationResponseFormat: RootJsonFormat[TranslationResponse] = jsonFormat2(TranslationResponse)

}

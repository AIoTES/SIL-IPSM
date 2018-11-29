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

object ChannelDAO {

  import scala.concurrent.Future
  import IPSMModel.dc.profile.api._

  def findChannelById(id: Int) : Future[Option[ChannelData]] = {
    val q = IPSMModel.channels.filter{ _.id === id }
    val action = q.result.headOption
    IPSMModel.db.run(action)
  }

  def countChannelsByAlignmentId(name: String, version: String) : Future[Int] = {
    val q = IPSMModel.channels.filter{chan => {
      (chan.inpAlignmentName === name && chan.inpAlignmentVersion === version) ||
        (chan.outAlignmentName === name && chan.outAlignmentVersion === version)
    }}.distinct.length
    val action = q.result
    IPSMModel.db.run(action)
  }

  def deleteChannelById(id: Int) : Future[Int] = {
    val q = IPSMModel.channels.filter{ _.id === id }
    val action = q.delete
    IPSMModel.db.run(action)
  }

  def addChannel(channelConf: ChannelConfig, chanUUID: String): Future[ChannelData] = {
    val newChannel = ChannelData(channelConf.source,
      channelConf.inpAlignmentName,
      channelConf.inpAlignmentVersion,
      channelConf.outAlignmentName,
      channelConf.outAlignmentVersion,
      channelConf.sink,
      -1,
      chanUUID,
      channelConf.optConsumerGroup.get,
      if (channelConf.parallelism.isDefined) {
        channelConf.parallelism.get
      } else {
        1
      }
    )
    IPSMModel.db.run(IPSMModel.channelInsertQuery += newChannel)
  }

  def getChannels: Future[Seq[ChannelData]] = {
    val allChannelsAction: DBIO[Seq[ChannelData]] = IPSMModel.channels.result
    IPSMModel.db.run(allChannelsAction)
  }

}

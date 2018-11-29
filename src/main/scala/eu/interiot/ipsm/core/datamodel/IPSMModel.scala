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

import scala.concurrent.ExecutionContext.Implicits.global
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcBackend, JdbcProfile}
import slick.jdbc.meta.MTable
import slick.lifted.{Index, ProvenShape}

import scala.concurrent.Future

object IPSMModel {

  val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("ipsm-db-sqlite")
  val db: JdbcBackend#DatabaseDef = dc.db

  import dc.profile.api._

  class Alignments(tag: Tag) extends Table[AlignmentData](tag, "alignment") {
    def id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def date: Rep[Timestamp] = column[Timestamp]("DATE")
    def name: Rep[String] = column[String]("NAME")
    def sourceOntologyURI: Rep[String] = column("SOURCE_ONTO_URI")
    def targetOntologyURI: Rep[String] = column("TARGET_ONTO_URI")
    def version: Rep[String] = column("VERSION")
    def creator: Rep[String] = column("CREATOR")
    def description: Rep[String] = column("DESCRIPTION")
    def alignment: Rep[String] = column("ALIGNMENT")
    def idx: Index = index("idx_nv", (name, version), unique = true)
    // scalastyle:off method.name
    def * : ProvenShape[AlignmentData] = (
      id,
      date,
      name,
      sourceOntologyURI,
      targetOntologyURI,
      version,
      creator,
      description,
      alignment
    ) <> (AlignmentData.tupled, AlignmentData.unapply)
    // scalastyle:on
  }

  val alignments: TableQuery[Alignments] = TableQuery[Alignments]

  val alignmentInsertQuery = alignments returning alignments.map(_.id) into ((align, id) => align.copy(id = id))

  // scalastyle:off public.methods.have.type
  class Channels(tag: Tag) extends Table[ChannelData](tag, "channel") {
    def id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def source: Rep[String] = column("SOURCE")
    def sink: Rep[String] = column("SINK")
    def inpAlignmentName: Rep[String] = column("INP_ALIGNMENT_NAME")
    def inpAlignmentVersion: Rep[String] = column("INP_ALIGNMENT_VERSION")
    def outAlignmentName: Rep[String] = column("OUT_ALIGNMENT_NAME")
    def outAlignmentVersion: Rep[String] = column("OUT_ALIGNMENT_VERSION")
    def kafkaGroup: Rep[String] = column("KAFKA_GROUP")
    def parallelism: Rep[Int] = column("PARALLELISM")
    def uuid: Rep[String] = column("UUID")
    // scalastyle:off method.name
    def * : ProvenShape[ChannelData] = (
      source,
      inpAlignmentName,
      inpAlignmentVersion,
      outAlignmentName,
      outAlignmentVersion,
      sink,
      id,
      uuid,
      kafkaGroup,
      parallelism
    ) <> (ChannelData.tupled, ChannelData.unapply)
    // scalastyle:on
  }
  // scalastyle:on

  val channels: TableQuery[Channels] = TableQuery[Channels]


  val channelInsertQuery = channels returning channels.map(_.id) into ((channel, id) => channel.copy(id = id)) // scalastyle:ignore

  def initializeSchema: Future[Unit] = {

    def createTableIfNotExists(tables: Vector[MTable]): Future[Unit] = {
      if (!tables.exists(_.name.name == alignments.baseTableRow.tableName)) {
        db.run(alignments.schema.create)
      }
      if (!tables.exists(_.name.name == channels.baseTableRow.tableName)) {
        db.run(channels.schema.create)
      } else {
        Future(Unit)
      }
    }

    db.run(MTable.getTables).flatMap(createTableIfNotExists)
  }
}

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

import com.typesafe.scalalogging.LazyLogging
import eu.interiot.ipsm.core.datamodel.{AlignmentDAO, AlignmentData, AlignmentID}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object AlignmentsManager extends LazyLogging {
  import collection.mutable.{Map => MMap}

  private val alignments = MMap[String, Alignment]()

  def getAlignment(id: AlignmentID): Option[Alignment] = {
    val alignmentOpt = alignments.get(id.toIndex)
    if (alignmentOpt.isDefined) {
      alignmentOpt
    } else {
      val alignmentFuture = AlignmentDAO.findAlignmentById(id)
      Await.ready(alignmentFuture, Duration.Inf)
      alignmentFuture.value.get match {
        case Success(alignDataOpt) =>
          if (alignDataOpt.isDefined) {
          val alignData = alignDataOpt.get
            val index = alignData.alignmentID.toIndex
            alignments += (index -> new Alignment(alignData.xmlSource))
            alignments.get(index)
          } else {
            logger.error("Alignment with {} not found", id)
          None
      }
        case Failure(exc) =>
          logger.error("Exception looking for alignment: {}", exc.getMessage)
          None
    }
  }
  }

  /**
    * Add a new alignment
    * @param alignData alignment data
    */
  def addAlignment(alignData: AlignmentData): Future[AlignmentData] = {
    val res = AlignmentDAO.addAlignment(alignData)

    res.andThen {
      case Success(alignmentData) =>
        alignments += (alignmentData.alignmentID.toIndex -> new Alignment(alignmentData.xmlSource))
      case _ =>
    }
  }

  /**
    * Get alignments (predefined and inserted at runtime)
    * @return Set of tuples (source ontology URI, target ontology URI
    */
  def getAlignmentsData: Future[Seq[AlignmentData]] = {
    AlignmentDAO.getAlignments
  }

  def findAlignmentDataById(id: AlignmentID): Future[Option[AlignmentData]] = {
    AlignmentDAO.findAlignmentById(id)
  }

  def deleteAlignmentById(id: AlignmentID): Future[Int] = {
    AlignmentDAO.deleteAlignmentById(id) andThen {
      case Success(_) =>
        alignments.remove(id.toIndex)
      case _ =>
    }
  }

  def loadPredefAlignments(): Unit = {
    val alignmentsFuture: Future[Seq[AlignmentData]] = AlignmentDAO.getAlignments

    alignmentsFuture.foreach {
      alignData => {
        for (align <- alignData.toList) {
          alignments += (align.alignmentID.toIndex -> new Alignment(align.xmlSource))
        }
      }
    }
  }

}

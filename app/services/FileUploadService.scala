/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.RunnableGraph
import connectors.FileUploadConnector
import play.Logger
import play.mvc.Http.Status._
import flow.investorDetailsCSVFlow
import util.Util


import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, NotFoundException }

object FileUploadService extends FileUploadService {
  override lazy val fileUploadConnector = FileUploadConnector
}

trait FileUploadService {

  final val EMPTY_STRING = ""
  val fileUploadConnector: FileUploadConnector

  implicit val system = ActorSystem("InvestorDetailsFlow")
  implicit val materializer = ActorMaterializer()

  def createEnvelope(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[String] = {
    fileUploadConnector.createEnvelope().map {
      result =>
        result.header("Location").getOrElse("").replaceAll("""[:.\-a-z].+\/file-upload\/envelopes\/""", EMPTY_STRING)
    }.recover {
      case e: Exception => Logger.warn(s"[FileUploadService][createEnvelope] Error ${e.getMessage} received.")
        EMPTY_STRING
    }
  }

  def getEnvelopeStatus(envelopeID: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    fileUploadConnector.getEnvelopeStatus(envelopeID).map {
      result => result.status match {
        case OK => result
        case _ => Logger.warn(s"[FileUploadService][checkEnvelopeStatus] Error ${result.status} received.")
          result
      }
    }.recover {
      case e:NotFoundException => {
        Logger.warn(s"[FileUploadService][checkEnvelopeStatus] Error ${e.getMessage} received for envelope Id $envelopeID"
          + "Returning Ok 200 with no data.")
        HttpResponse(OK)
      }
      case e: Exception => Logger.warn(s"[FileUploadService][checkEnvelopeStatus] Error ${e.getMessage} received.")
        HttpResponse(INTERNAL_SERVER_ERROR)
    }
  }

  def closeEnvelope(envelopeID: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    fileUploadConnector.closeEnvelope(envelopeID).map {
      result => result.status match {
        case CREATED => {
        }
          result
        case _ => {
          Logger.warn(s"[FileUploadService][closeEnvelope] Error ${result.status} received.")
        }
          result
      }
    }.recover {

      case e: Exception => {
        Logger.warn(s"[FileUploadService][closeEnvelope] Error ${e.getMessage} received.")
      }
        HttpResponse(INTERNAL_SERVER_ERROR)
    }
  }

  def deleteFile(envelopeID: String, fileID: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    fileUploadConnector.deleteFile(envelopeID, fileID).map {
      result => result.status match {
        case OK =>
          result
        case _ => Logger.warn(s"[FileUploadService][deleteFile] Error ${result.status} received.")
          result
      }
    }.recover {
      case e: Exception => Logger.warn(s"[FileUploadService][deleteFile] Error ${e.getMessage} received.")
        HttpResponse(INTERNAL_SERVER_ERROR)
    }
  }

  def getFileData(envelopeID: String, fileID: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    fileUploadConnector.getFileData(envelopeID, fileID).map {
      result =>
        result.status match {
        case OK => printBytesString(result.body, fileID)
          result
        case _ => Logger.warn(s"[FileUploadService][getFileData] Error ${result.status} received.")
          result
      }
    }.recover {
      case e:NotFoundException => {
        Logger.warn(s"[FileUploadService][getFileData] Error ${e.getMessage} received for envelope Id $envelopeID"
          + "Returning Ok 200 with no data.")
        HttpResponse(OK)
      }
      case e: Exception => Logger.warn(s"[FileUploadService][getFileData] Error ${e.getMessage} received.")
        HttpResponse(INTERNAL_SERVER_ERROR)
    }
  }

  def printBytesString(data: String, fileId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Unit ={
    Logger.info(s" FILE DATA INSERT TO DATABASE")
    val flow = investorDetailsCSVFlow(Util.fileSource(data), Util.investorDetailsSink(fileId), Util.validationErrorsSink(fileId))
    val run = RunnableGraph.fromGraph(flow).run
    val combinedRunResult = Future.sequence(List(run._1, run._2))
    combinedRunResult map { result =>
      Logger.warn("Processing completed for file: " + fileId + "\n")
    }
  }

}

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

package util

import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import flow.{CSV, ErrorSink, OutputSink}
import models.{InvestorDetails, ValidationError}
import play.api.libs.json.Json
import services.InvestorService

import scala.concurrent.ExecutionContext

/**
  * @author David O'Riordan
  */
object Util {

  /*
   * CSV converter may add surrounding quotation marks to a field if it contains a comma (or whatever the separator is)
   */

  val quot = "\""
  val unquote: String => String = { data =>
    if (data.startsWith(quot) && data.endsWith(quot))
      data.stripPrefix(quot).stripSuffix(quot)
    else
      data
  }

  /*
   * Convert field from input to canonical form for validation and transformation
   */
  val canonicalize: String => String = unquote andThen { s => s.trim }

  def splitRowIntoColumns(row: String): Seq[String] = {
    // Ned to take into account commas within quoted strings are not separators,
    // hence a simple split won't work
    // Instead we recursively consume the row as follows
    // - if next non-whitespace character is a quotation mark,, then skip to next quotation mark
    // including the full quoted text in the output
    // - Find next comma  or \n and include text up to but not including that separator
    import scala.annotation.tailrec
    @tailrec def split(remainingRowData: String, columns: Seq[String]): Seq[String] = {
      val trimmed = remainingRowData.trim
      val nextCh = trimmed.headOption.getOrElse('\n')
      nextCh match {
        case '\n' => columns // done
        case ',' => // only whitespace on this column
          split(trimmed.drop(1), columns :+ remainingRowData.takeWhile(_ != ','))
        case '\"' =>
          // span the quoted part before seeking closing separator
          val quotedValue = "\"" + trimmed.drop(1).takeWhile(_ != '\"')
          // postQuotedPValue is part between closing quotation and next separator...
          // should really be empty if properly formatted CSV....
          val postQuotedValue = trimmed.substring(quotedValue.length).takeWhile(c => c != ',' && c != '\n')
          val value = quotedValue + postQuotedValue
          val remainder = trimmed.drop(value.length + 1) // skip to next column i.e. over the value plus separator we just found
          split(remainder, columns :+ value)
        case _ =>
          val value = remainingRowData.takeWhile(c => c != ',' && c != '\n')
          val remainder = remainingRowData.drop(value.length + 1)
          split(remainder, columns :+ value)
      }
    }
    split(row, List())
  }

  import java.nio.file.{Paths, Path}
  val outputFileOptions=Set(
    java.nio.file.StandardOpenOption.CREATE,
    java.nio.file.StandardOpenOption.WRITE,
    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)

  /*def investorDetailsSink(fileId: String): OutputSink = {
    val path = Paths.get(fileId + ".out")
    val sink = FileIO.toPath(path,outputFileOptions)
    Flow[InvestorDetails]
      .map { investor =>
        val json = Json.toJson(investor)
        ByteString(json.toString + "\n")
      }
      .toMat(sink)(Keep.right)
  }*/

  def investorDetailsSink(fileId: String)(implicit ec: ExecutionContext): OutputSink = {
    Flow[InvestorDetails]
      .map { investor =>
        InvestorService.saveInvestorDetails(investor)
      }
      .toMat(Sink.ignore)(Keep.right)
  }

  def validationErrorsSink(fileId: String): ErrorSink = {
    val path = Paths.get(fileId + ".err")
    val sink = FileIO.toPath(path, outputFileOptions)
    Flow[List[ValidationError]]
      .map { errors =>
        // output list of errors as stream of Json objects on separate lines rather than in an array
        val asJsonStreamLines = errors.foldLeft[String]("") { (acc, err) =>
          val errAsJson = Json.toJson(err)
          acc + errAsJson.toString + "\n"
        }
        ByteString(asJsonStreamLines)
      }
      .toMat(sink)(Keep.right)
  }

  /*def validationErrorsSink(fileId: String): ErrorSink = {
    Flow[List[ValidationError]]
      .map { errors =>
        // output list of errors as stream of Json objects on separate lines rather than in an array
        val asJsonStreamLines = errors.foldLeft[String]("") { (acc, err) =>
          val errAsJson = Json.toJson(err)
          acc + errAsJson.toString + "\n"
        }
        ByteString(asJsonStreamLines)
      }
      .toMat(Sink.ignore)(Keep.right)
  }*/

  def fileSource(data: String): CSV.File = {
    Source.single(ByteString(data))
  }
}

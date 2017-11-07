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

import models.ValidationError

import scala.util.matching.Regex

/**
  * @author David O'Riordan
  */
object RowValidator {

  // NB these  regexes are probably over-simplified, should be aligned with UI and DES checks
  val intRegex = """^(\d+)$""".r
  val decimalAmountRegex = """^(\d+(?:\.\d+)?)$""".r
  val nameRegex = """^[A-za-z- ']+$""".r
  val addrLineRegex = """^[a-zA-Z0-9\"&,. ]+$""".r
  val postcodeRegex = """^[A-Za-z]{1,2}[0-9][0-9A-Za-z]? [0-9][A-Za-z]{2}$""".r
  val countryCodeRegex = """^[A-Z]{2}$""".r // two letter ISO country code per https://en.wikipedia.org/wiki/List_of_postal_codes
  val yesOrNoRegex = """^(yes)|(no)$""".r

  val baseRules=List(
    ColumnValidationRule("First Name", false, Some(nameRegex)),
    ColumnValidationRule("Last Name", true, Some(nameRegex)),
    ColumnValidationRule("Address Line 1", false, Some(addrLineRegex)),
    ColumnValidationRule("Address Line 1", false, Some(addrLineRegex)),
    ColumnValidationRule("Address Line 3", true, Some(addrLineRegex)),
    ColumnValidationRule("Postcode", true, Some(postcodeRegex)),
    ColumnValidationRule("Country", false, Some(countryCodeRegex)),
    ColumnValidationRule("Nominee Flag", false, Some(yesOrNoRegex)))

  val DateIssuedFormat = "dd-MM-yyyy"
  val dif = new java.text.SimpleDateFormat(DateIssuedFormat)
  dif.setLenient(false)

  private val dateIssuedValidityCheck: String => Option[String] = { date =>
    try {
      dif.parse(date.trim())
      // TODO any range validity checks required?
      None
    } catch {
      case ex: Throwable => Some("Invalid date")
    }
  }

  val holdingRules=List(
    ColumnValidationRule("Date Issued", false, customRule = Some(dateIssuedValidityCheck)),
    ColumnValidationRule("Description"),
    ColumnValidationRule("Amount", false, Some(decimalAmountRegex)),
    ColumnValidationRule("Number", false, Some(intRegex)))

  val numBaseColumns = baseRules.length
  val numSingleHoldingColumns = holdingRules.length
  val notEnoughColumnsErrorStr = s"The row must have at least ${numBaseColumns} columns"
  val invalidNumColumnsErrorStr = s"Invalid number of columns in row: there must be ${numBaseColumns} columns followed by exactly ${numSingleHoldingColumns} columns for each holding"

  def validate(columns: Seq[String], rowNumber: Int): List[ValidationError] = {
    if (columns.size  < numBaseColumns)
      List(ValidationError(rowNumber, columns.size, notEnoughColumnsErrorStr))
    else if ((columns.size - numBaseColumns) % numSingleHoldingColumns != 0)
      List(ValidationError(rowNumber, columns.size, invalidNumColumnsErrorStr))
    else {
      val baseErrors = validateBaseDetails(columns.take(numBaseColumns), rowNumber)
      val holdingErrors = validateHoldingDetails(columns.drop(numBaseColumns), rowNumber)
      baseErrors ++ holdingErrors
    }
  }

  private def validateBaseDetails(columns: Seq[String], rowNumber: Int): List[ValidationError] = {
    val dataColumnRuleZipped = columns.zipWithIndex.zip(baseRules)
    val validationResults = dataColumnRuleZipped map { case ((data, columnNumber), rule) =>
      rule.validate(data).map { err => ValidationError(rowNumber, columnNumber, err) }
    }
    // collect any errors
    validationResults collect {
      case Some(error) => error
    } toList
  }

  private def validateHoldingDetails(holdingDetailsColumns: Seq[String], rowNumber: Int): List[ValidationError] = {
    val holdingWithIndexZipped = holdingDetailsColumns.grouped(numSingleHoldingColumns).zipWithIndex
    holdingWithIndexZipped flatMap { case (holding, holdingNumber) =>
      val dataColumnRuleZipped = holding.zipWithIndex.zip(holdingRules)
      val validationResults = dataColumnRuleZipped map { case ((data,holdingColumnIndex), rule) =>
        val columnNumber = numBaseColumns + (holdingNumber * numSingleHoldingColumns) + holdingColumnIndex
        rule.validate(data).map { err => ValidationError(rowNumber, columnNumber, err) }
      }
      // collect any errors
      validationResults collect {
        case Some(error) => error
      }
    } toList
  }

  case class ColumnValidationRule(
    cName: String,
    isOptional: Boolean = false,
    regex: Option[Regex] = None,
    maxLength: Option[Int] = None,
    customRule: Option[String => Option[String]] = None)
  {
    private def check(f: => Boolean, error: String): Option[String] = if (f) Some(error) else None
    private def checkRequired: Option[String] = check(!isOptional, "This column cannot be empty")
    private def checkLength(data: String): Option[String] = check(maxLength.map(data.length > _).getOrElse(false), "Data too long")

    private def checkRegex(data: String): Option[String] = for(
      pattern <- regex;
      err <- data match {
        case pattern(_*) => None
        case _ => Some("Invalid format")
      }
    ) yield err // err is None (i.e. no error) if either no pattern supplied or the pattern matches the data

    private def customCheck(data: String): Option[String] = for(
      check <- customRule;
      err <- check(data)
    ) yield err

    def validate(data: String):  Option[String] =
      if (data.isEmpty)
        checkRequired
      else
        checkLength(data) orElse checkRegex(data) orElse customCheck(data)
  }
}

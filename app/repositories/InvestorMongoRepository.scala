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

package repositories

import models.InvestorDetails
import play.api.libs.json.Format
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Writes.StringWrites
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait InvestorRepository extends Repository[InvestorDetails, String]{
  def createInvestor(investorDetails: InvestorDetails)
}

class InvestorMongoRepository(implicit mongo: () => DB)
  extends ReactiveRepository[InvestorDetails, String]("investors", mongo, InvestorDetails.mongoFormats, Format(StringReads, StringWrites))
    with InvestorRepository {

  override def indexes = Seq(
    Index(Seq("aField" -> IndexType.Ascending), name = Some("aFieldUniqueIdx"), unique = true, sparse = true),
    Index(Seq("anotherField" -> IndexType.Ascending), name = Some("anotherFieldIndex"))
  )

  def createInvestor(investorDetails: InvestorDetails): Unit = {
    insert(investorDetails)
  }

  def dropCollection: Future[Unit] = collection.drop()
}
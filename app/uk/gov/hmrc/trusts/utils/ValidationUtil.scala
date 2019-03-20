/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.utils

import org.joda.time.DateTime
import uk.gov.hmrc.trusts.models.{IndividualDetailsType, TrusteeType}
import uk.gov.hmrc.trusts.services.TrustsValidationError

import scala.annotation.tailrec

trait ValidationUtil {

  def isNotFutureDate(date : DateTime,path : String, key : String ) : Option[TrustsValidationError] = {
    if (isAfterToday(date)) {
      Some(TrustsValidationError(s"$key must be today or in the past.", path))
    } else {
      None
    }
  }

  def isNotFutureDate(date : Option[DateTime],path : String, key : String ) : Option[TrustsValidationError]= {
    if (date.isDefined && isAfterToday(date.get)) {
      isNotFutureDate(date.get, path, key)
    } else {
      None
    }
  }

  def isAfterToday(date :DateTime): Boolean = {
    date.isAfter(new DateTime().plusDays(1).withTimeAtStartOfDay())
  }

  type ListOfItemsAtIndex = List[(String, Int)]

  def findDuplicates(ninos : ListOfItemsAtIndex) : ListOfItemsAtIndex = {

    @tailrec
    def findDuplicateHelper(remaining : ListOfItemsAtIndex, duplicates : ListOfItemsAtIndex) : ListOfItemsAtIndex = {
      remaining match {
        case Nil => {
          duplicates
        }
        case head :: tail =>
          if (tail.exists(x => x._1 == head._1)) {
            findDuplicateHelper(tail, head :: duplicates)
          } else {
            findDuplicateHelper(tail, duplicates)
          }
      }
    }

    findDuplicateHelper(ninos, Nil)
  }


   def getTrusteesUtrWithIndex(trustees: List[TrusteeType]) = {
    val utrList: List[(String, Int)] = trustees.zipWithIndex.flatMap {
      case (trustee, index) =>
        trustee.trusteeOrg.flatMap { x =>
          x.identification.utr.map { y => (y, index) }
        }
    }
    utrList
  }


   def getTrusteesNinoWithIndex(trustees: List[TrusteeType]) = {
    val ninoList: List[(String, Int)] = trustees.zipWithIndex.flatMap {
      case (trustee, index) =>
        trustee.trusteeInd.flatMap { x =>
          x.identification.nino.map { y => (y, index) }
        }
    }
    ninoList
  }


   def getIndBenificiaryNinoWithIndex(indBenificiaries: List[IndividualDetailsType]) = {
    val ninoList: List[(String, Int)] = indBenificiaries.zipWithIndex.flatMap {
      case (indv, index) =>
        indv.identification.map { x =>
          x.nino.map { y => (y, index) }
        }
    }.toList.flatten
    ninoList
  }

  def getIndBenificiaryPassportNumberWithIndex(indBenificiaries: List[IndividualDetailsType]) = {
    val passportNumberList: List[(String, Int)] = indBenificiaries.zipWithIndex.flatMap {
      case (indv, index) =>
        indv.identification.map { x =>
          x.passport.map { y => (y.number, index) }
        }
    }.toList.flatten
    passportNumberList
  }

  def isNotTrust(currentTypeOfTrust: String, typeOfTrust: TypeOfTrust.Value) = currentTypeOfTrust == typeOfTrust.toString


}

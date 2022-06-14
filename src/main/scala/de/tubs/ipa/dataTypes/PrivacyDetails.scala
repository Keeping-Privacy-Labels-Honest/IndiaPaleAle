package de.tubs.ipa.dataTypes

import de.tubs.ipa.dataTypes.Purpose.knownPurposes
import collection.mutable.{Map => MMap}

case class DataCategory(dataCategory: String,
                        dataTypes: List[String],
                        identifier: String)

object Purpose {

  val knownPurposes: Map[String, String] = Map(
    "DEVELOPERS_ADVERTISING" -> "Developer's Advertising or Marketing",
    "THIRD_PARTY_ADVERTISING" -> "Third-Party Advertising",
    "PRODUCT_PERSONALIZATION" -> "Product Personalization",
    "OTHER_PURPOSES" -> "Other Purposes",
    "APP_FUNCTIONALITY" -> "App Functionality",
    "ANALYTICS" -> "Analytics"
  )

}

case class Purpose(identifier: String,
                   purpose: String,
                   dataCategories: Seq[DataCategory]) {

  assert(knownPurposes.contains(identifier))

}

trait PrivacyType {
  val identifier: String
  val description: String
  val privacyType: String
}

case class DataUsedToTrackYou(override val identifier: String,
                              override val description: String,
                              override val privacyType: String,
                              dataCategory: List[DataCategory])
    extends PrivacyType {

  assert(identifier == "DATA_USED_TO_TRACK_YOU")

}

trait DataSomehowRelatedToYou extends PrivacyType {
  val purposes: List[Purpose]
}

case class DataLinkedToYou(override val identifier: String,
                           override val description: String,
                           override val privacyType: String,
                           override val purposes: List[Purpose])
    extends DataSomehowRelatedToYou {
  assert(identifier == "DATA_LINKED_TO_YOU")
}

case class DataNotLinkedToYou(override val identifier: String,
                              override val description: String,
                              override val privacyType: String,
                              override val purposes: List[Purpose])
    extends DataSomehowRelatedToYou {

  assert(identifier == "DATA_NOT_LINKED_TO_YOU")

}

case class DataNotCollected(override val identifier: String,
                            override val description: String,
                            override val privacyType: String)
    extends PrivacyType

case class PrivacyDetails(managePrivacyChoicesUrl: Option[String],
                          privacyTypes: List[PrivacyType]) {

  def getDataUsedToTrackYouCount: Int =
    privacyTypes.count(_.isInstanceOf[DataUsedToTrackYou])
  def getDataLinkedToYouCount: Int =
    privacyTypes.count(_.isInstanceOf[DataLinkedToYou])
  def getDataNotLinkedToYouCount: Int =
    privacyTypes.count(_.isInstanceOf[DataNotLinkedToYou])
  def getDataNotCollectedCount: Int =
    privacyTypes.count(_.isInstanceOf[DataNotCollected])

  def printPurposeOverview(): Unit = {
    println("Data Linked To You")
    println(
      privacyTypes
        .filter(_.isInstanceOf[DataLinkedToYou])
        .map(_.asInstanceOf[DataLinkedToYou])
        .flatMap(_.purposes.map(elem => (elem.identifier, elem.purpose)))
        .toSet
        .mkString("\n"))
    println()
    println("Data Not Linked To You")
    println(
      privacyTypes
        .filter(_.isInstanceOf[DataNotLinkedToYou])
        .map(_.asInstanceOf[DataNotLinkedToYou])
        .flatMap(_.purposes.map(elem => (elem.identifier, elem.purpose)))
        .toSet
        .mkString("\n"))
  }

  def getPrivacyTypeDataCategorySet(privacyType: String): Set[String] = {
    privacyTypes.flatMap {
      case DataLinkedToYou(_, _, _, purposes)
          if privacyType == "DATA_LINKED_TO_YOU" =>
        purposes.flatMap(_.dataCategories).flatMap(_.dataTypes)
      case DataNotLinkedToYou(_, _, _, purposes)
          if privacyType == "DATA_NOT_LINKED_TO_YOU" =>
        purposes.flatMap(_.dataCategories).flatMap(_.dataTypes)
      case DataUsedToTrackYou(_, _, _, dataCategories)
          if privacyType == "DATA_USED_TO_TRACK_YOU" =>
        dataCategories.flatMap(_.dataTypes)
      case _ => List()
    }.toSet
  }

  def getPurposeCountMap(privacyType: String): Map[String, Int] = {
    val purposes = privacyTypes.flatMap {
      case DataLinkedToYou(_, _, _, purposes)
          if privacyType == "DATA_LINKED_TO_YOU" =>
        purposes.map(_.purpose)
      case DataNotLinkedToYou(_, _, _, purposes)
          if privacyType == "DATA_NOT_LINKED_TO_YOU" =>
        purposes.map(_.purpose)
      case _ => List()
    }
    purposes.toSet.map { purpose: String =>
      purpose -> purposes.count(_ == purpose)
    }.toMap
  }

}

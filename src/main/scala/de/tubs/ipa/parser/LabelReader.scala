package de.tubs.ipa.parser

import de.tubs.ipa.dataTypes.{
  DataCategory,
  DataLinkedToYou,
  DataNotCollected,
  DataNotLinkedToYou,
  DataSomehowRelatedToYou,
  DataUsedToTrackYou,
  PrivacyDetails,
  PrivacyType,
  Purpose
}
import de.tubs.ipa.utility.FilesystemInteraction
import spray.json._

object LabelReader extends DefaultJsonProtocol {

  implicit val dataCategoryFormat: RootJsonFormat[DataCategory] = jsonFormat3(
    DataCategory)

  implicit val purposeFormat: RootJsonFormat[Purpose] = jsonFormat3(
    Purpose.apply)

  implicit val dataNotCollectedFormat: RootJsonFormat[DataNotCollected] =
    jsonFormat3(DataNotCollected)

  implicit object PrivacyTypeFormat extends RootJsonFormat[PrivacyType] {
    override def read(json: JsValue): PrivacyType = {
      json.asJsObject.fields("identifier").asInstanceOf[JsString].value match {
        case "DATA_USED_TO_TRACK_YOU" =>
          DataUsedToTrackYouFormat.read(json)
        case "DATA_LINKED_TO_YOU" | "DATA_NOT_LINKED_TO_YOU" =>
          DataSomehowRelatedToYouFormat.read(json)
        case "DATA_NOT_COLLECTED" =>
          json.convertTo[DataNotCollected]
        case x =>
          throw new RuntimeException(s"unknown privacy type identifier $x")
      }
    }

    override def write(obj: PrivacyType): JsValue =
      throw new NotImplementedError()
  }

  implicit object DataSomehowRelatedToYouFormat
      extends RootJsonFormat[DataSomehowRelatedToYou] {
    override def read(json: JsValue): DataSomehowRelatedToYou = {
      assert(
        json.asJsObject
          .fields("dataCategories")
          .asInstanceOf[JsArray]
          .elements
          .isEmpty)
      val identifier =
        json.asJsObject.fields("identifier").asInstanceOf[JsString].value
      val description =
        json.asJsObject.fields("description").asInstanceOf[JsString].value
      val privacyType =
        json.asJsObject.fields("privacyType").asInstanceOf[JsString].value
      val purposes = json.asJsObject
        .fields("purposes")
        .asInstanceOf[JsArray]
        .elements
        .map(_.convertTo[Purpose])
        .toList
      json.asJsObject.fields("identifier").asInstanceOf[JsString].value match {
        case "DATA_LINKED_TO_YOU" =>
          DataLinkedToYou(identifier, description, privacyType, purposes)
        case "DATA_NOT_LINKED_TO_YOU" =>
          DataNotLinkedToYou(identifier, description, privacyType, purposes)
      }
    }

    override def write(obj: DataSomehowRelatedToYou): JsValue =
      throw new NotImplementedError()
  }

  implicit object DataUsedToTrackYouFormat
      extends RootJsonFormat[DataUsedToTrackYou] {
    override def read(json: JsValue): DataUsedToTrackYou = {
      // Data used to track you does not fill the purposes array
      assert(
        json.asJsObject
          .fields("purposes")
          .asInstanceOf[JsArray]
          .elements
          .isEmpty)
      DataUsedToTrackYou(
        json.asJsObject.fields("identifier").asInstanceOf[JsString].value,
        json.asJsObject.fields("description").asInstanceOf[JsString].value,
        json.asJsObject.fields("privacyType").asInstanceOf[JsString].value,
        json.asJsObject
          .fields("dataCategories")
          .asInstanceOf[JsArray]
          .elements
          .map(_.convertTo[DataCategory])
          .toList
      )
    }

    override def write(obj: DataUsedToTrackYou): JsValue =
      throw new NotImplementedError()
  }

  implicit val privacyDetailsFormat: RootJsonFormat[PrivacyDetails] =
    jsonFormat2(PrivacyDetails.apply)

  def readPrivacyLabel(file: String): PrivacyDetails = {
    val obj = JsonParser(FilesystemInteraction.readFile(file)).asJsObject
    assert(obj.fields.contains("data"),
           "privacy label json needs to have the field data")
    assert(obj.fields("data").asInstanceOf[JsArray].elements.length == 1,
           "only expect single data element in privacy label json dump")
    val single =
      obj.fields("data").asInstanceOf[JsArray].elements.head.asJsObject
    assert(single.fields.contains("attributes"),
           "data element needs to contain field attributes")
    assert(
      single.fields("attributes").asJsObject.fields.contains("privacyDetails"),
      "attributes needs to contain privacy details")
    single
      .fields("attributes")
      .asJsObject
      .fields("privacyDetails")
      .convertTo[PrivacyDetails]
  }

  def readPrivacyLabelSet(folder: String): PrivacyDetails = {
    PrivacyDetails(None,
                   FilesystemInteraction
                     .files(folder)
                     .map(readPrivacyLabel)
                     .flatMap(_.privacyTypes)
                     .toList)
  }

}

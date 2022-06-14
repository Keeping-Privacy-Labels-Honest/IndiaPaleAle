package de.tubs.ipa.privacyLabels

import de.tubs.ipa.utility.FilesystemInteraction
import spray.json.{DefaultJsonProtocol, JsArray, JsonParser, RootJsonFormat}

case class PrivacyLabelSet(label: String,
                           absolutePath: String,
                           expectedCount: Int)

object PrivacyLabelSet extends DefaultJsonProtocol {

  implicit val privacyLabelSetFormat: RootJsonFormat[PrivacyLabelSet] =
    jsonFormat3(PrivacyLabelSet.apply)

  def readInLabelList(jsonFile: String): Seq[PrivacyLabelSet] = {
    JsonParser(FilesystemInteraction.readFile(jsonFile))
      .asInstanceOf[JsArray]
      .elements
      .map(_.convertTo[PrivacyLabelSet])
  }

}

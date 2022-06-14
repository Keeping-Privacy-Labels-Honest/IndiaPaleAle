package de.tubs.ipa.ThreeUDownloader

import spray.json.{
  DefaultJsonProtocol,
  JsArray,
  JsBoolean,
  JsNumber,
  JsObject,
  JsValue,
  RootJsonFormat,
  enrichAny
}
import wvlet.log.LogSupport

case class ThreeUAppData(versionid: String,
                         icon: String,
                         sort: Int,
                         itemid: String,
                         shortversion: String,
                         downloaded: String,
                         isfull: Int,
                         version: String,
                         id: Int,
                         slogancolor: String,
                         slogan: String,
                         appname: String,
                         md5: String,
                         sourceid: String,
                         path: String,
                         minversion: String,
                         sizebyte: String,
                         longversion: String,
                         pkagetype: Int)

case class ThreeUResponse(success: Boolean,
                          ttype: Int,
                          list: List[ThreeUAppData],
                          co: Int)
    extends LogSupport {

  def merge(other: ThreeUResponse): ThreeUResponse = {
    if (ttype != other.ttype)
      throw new RuntimeException(
        s"only merge responses of the same type got $ttype vs ${other.ttype}")
    if (success != other.success)
      throw new RuntimeException("only merge two successfull responses")
    ThreeUResponse(success, ttype, list ++ other.list, co)
  }

}

object ThreeUResponseReader extends DefaultJsonProtocol {

  implicit val threeUAppDataFormat: RootJsonFormat[ThreeUAppData] =
    jsonFormat19(ThreeUAppData)

  implicit object ThreeUResponseFormat extends RootJsonFormat[ThreeUResponse] {
    override def read(json: JsValue): ThreeUResponse = {
      val fields = json.asJsObject.fields
      ThreeUResponse(
        fields("success").isInstanceOf[JsBoolean].booleanValue(),
        fields("type").asInstanceOf[JsNumber].value.toInt,
        fields("list")
          .asInstanceOf[JsArray]
          .elements
          .map(_.convertTo[ThreeUAppData])
          .toList,
        fields("co").asInstanceOf[JsNumber].value.toInt
      )
    }

    override def write(obj: ThreeUResponse): JsValue = {
      JsObject(
        ("success", JsBoolean(obj.success)),
        ("type", JsNumber(obj.ttype)),
        ("list", JsArray(obj.list.map(_.toJson))),
        ("co", JsNumber(obj.co))
      )
    }
  }

}

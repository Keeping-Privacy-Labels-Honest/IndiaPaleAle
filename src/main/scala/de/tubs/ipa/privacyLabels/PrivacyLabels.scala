package de.tubs.ipa.privacyLabels

import scalaj.http.Http
import spray.json._
import wvlet.log.LogSupport

import scala.annotation.tailrec
import scala.util.Random

object BadTokenException extends Throwable

object TimeoutRequiredException extends Throwable

object PrivacyLabels extends LogSupport {
  //credit for finding the endpoint to download labels to https://github.com/facundoolano/app-store-scraper/blob/master/lib/privacy.js

  var token : String = getToken

  private def getToken: String = {
    val appid = 1225867923 //this is candycrush - I do not expect it to get out of store too soon
    val result = Http(s"https://apps.apple.com/de/app/id$appid").header("User-Agent","Wget/1.12 (linux-gnu)").asString
    if(result.code == 403) {
      println(result)
      throw BadTokenException
    }
    val tokenString = result
    //println(tokenString)
    val regexp = "token%22%3A%22([^%]+)%22%7D".r.unanchored
    val token = tokenString.body match {
      case regexp(token) => token
      //case _ => throw BadTokenException
    }
    token
  }

  @tailrec
  final def getPrivacyLabel(appid: Int, tries : Int = 4): JsObject = {
    if(tries == 0) {
      throw TimeoutRequiredException
    }
    val randomWait = Random.nextInt(10000)
    info(s"randomly sleeping for $randomWait")
    Thread.sleep(randomWait)
    /**/
    info(s"requesting https://amp-api.apps.apple.com/v1/catalog/DE/apps/$appid?platform=web&fields=privacyDetails&l=en-us")
    val res = Http(
      s"https://amp-api.apps.apple.com/v1/catalog/DE/apps/$appid?platform=web&fields=privacyDetails&l=en-us")
      .headers(
        List(
          "Origin" -> "https://apps.apple.com",
          "Authorization" -> s"Bearer $token"
        )).asString

      if(res.code == 403) {
        throw BadTokenException
      } else {
        val body = res.body
        if(body.contains("Too Many Requests")) {
          val CHILLTIME = 60000
          Thread.sleep(CHILLTIME)
          warn(s"too many requests - chilling for $CHILLTIME sec")
          getPrivacyLabel(appid, tries -1)
        }  else {
          res.body.parseJson
            .asJsObject
        }
      }

  }

}

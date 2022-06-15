package de.tubs.ipa.ThreeUDownloader

import com.nimbusds.jose.util.StandardCharset
import de.halcony.argparse.{Parser, ParsingResult}
import de.tubs.ipa.ThreeUDownloader.ThreeUResponseReader.{ThreeUResponseFormat, threeUAppDataFormat}
import de.tubs.ipa.parser.LabelReader.listFormat
import de.tubs.ipa.privacyLabels.{BadTokenException, PrivacyLabels, TimeoutRequiredException}
import scalaj.http.{Http, HttpOptions}
import spray.json.{JsArray, JsonParser, enrichAny}
import wvlet.log.LogSupport

import java.io.{File, FileWriter}
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import scala.collection.mutable.ListBuffer

case class DownloadParameter(ttype: String,
                             remd: List[String] = List("71","73","74","6"),
                             sort: String = "1",
                             model: String = "101",
                             isJail: String = "0",
                             isAuth: String = "1",
                             specialId: String = "0",
                             hd: String = "0",
                             ts: String = "0",
                             u3: String = "3u",
                             countryid: String = "51") {

  def getParameter(page: Int,cat : Int): Seq[(String, String)] =
    getParameter(page.toString, cat)

  def getParameter(page: String,cat : Int): Seq[(String, String)] = {
    Seq(
      ("model", model),
      ("isJail", isJail),
      ("isAuth", isAuth),
      ("sort", sort),
      ("type", ttype),
      ("remd", remd(cat)),
      ("page", page),
      ("specialid", specialId),
      ("hd", hd),
      ("ts", ts),
      ("u3", u3),
      ("countryid", countryid)
    )
  }

}

object ThreeU extends LogSupport {

  val parser: Parser = Parser("3u", "interaction with the 3u web API")
    .addSubparser(Parser(""))
    .addSubparser(
      Parser("statistics", "get statistics on the downloaded app list")
        .addPositional("folder", "the folder containing the jsons")
        .addDefault[ParsingResult => Unit]("func", statisticsMain))
    .addSubparser(
      Parser("get-privacy-labels", "download the privacy labels")
        .addPositional("folder", "the folder containing the jsons")
        .addDefault[ParsingResult => Unit]("func", downloadPrivacyLabelsMain))
    .addSubparser(
      Parser("download", "download json files for category and page")
        .addPositional("category", "the category to download as csv or all")
        .addPositional("until", "the page until which to download")
        .addPositional("folder", "where to store the results")
        .addDefault[ParsingResult => Unit]("func", downloadMain))

  private val URL = "http://app.pcres.3u.com/app_list.action"

  private val CATEGORIES: Map[String, DownloadParameter] = Map(
    "lifestyle" -> DownloadParameter("105"),
    "productivity" -> DownloadParameter("110"),
    "social-networking" -> DownloadParameter("103"),
    "photo-and-video" -> DownloadParameter("109"),
    "music" -> DownloadParameter("108"),
    "books" -> DownloadParameter("111"),
    "references" -> DownloadParameter("117"),
    "weather" -> DownloadParameter("112"),
    "health-and-fitness" -> DownloadParameter("120"),
    "finance" -> DownloadParameter("114"),
    "education" -> DownloadParameter("119"),
    "utilities" -> DownloadParameter("107"),
    "shopping" -> DownloadParameter("150"),
    "navigation" -> DownloadParameter("104"),
    "entertainment" -> DownloadParameter("106"),
    "travel" -> DownloadParameter("116"),
    "business" -> DownloadParameter("118"),
    "sports" -> DownloadParameter("123"),
    "news" -> DownloadParameter("113"),
    "catalogue" -> DownloadParameter("122"),
    "food-and-drink" -> DownloadParameter("115"),
    "medical" -> DownloadParameter("121"),
    "games" -> DownloadParameter("0", remd = List("41","43","44"), sort = "2")
  )

  def readFile(path: String,
               encoding: Charset = StandardCharset.UTF_8): String = {
    val encoded = Files.readAllBytes(Paths.get(path))
    new String(encoded, encoding)
  }

  def getJson(category: String, page: Int, cat : Int): String = {
    CATEGORIES.get(category) match {
      case Some(value) =>
        Http(URL)
          .postForm(value.getParameter(page, cat))
          .option(HttpOptions.readTimeout(10000))
          .asString
          .body
      case None =>
        throw new RuntimeException(s"the category $category does not exist")
    }
  }

  def downloadPrivacyLabelsMain(pargs: ParsingResult): Unit = {
    val folder = pargs.get[String]("folder")
    val finished = ListBuffer[String]()
    val files =
      new File(folder).listFiles().filter(_.getAbsolutePath.endsWith(s".json"))
    files.foreach { file =>
      val responses = JsonParser(readFile(file.getAbsolutePath))
        .asInstanceOf[JsArray]
        .elements
        .map(
          _.convertTo[ThreeUResponse]
        )
      info(s"downloading ${file.getAbsolutePath}")
      val fileName = file.getAbsolutePath.split("/").last.split("\\.").head
      val out = s"$folder/$fileName/"
      new File(out).mkdirs()
      val ids = responses.flatMap(_.list.map(_.itemid)).toSet
      ids.foreach { id =>
        val idFile = new File(s"$out/$id.json")
        val download = if (idFile.exists()) {
          val content = readFile(idFile.getAbsolutePath)
          content.trim == "NA" || content.contains("API capacity exceeded")
        } else {
          true
        }
        if (download) {
          val json = try {
            val ret = Some(PrivacyLabels.getPrivacyLabel(id.toInt))
            info(s"trying to download PL for $id : SUCC")
            ret
          } catch {
            case TimeoutRequiredException =>
              throw new RuntimeException(s"even waiting did not help - better try even later")
            case BadTokenException =>
              error("apple throws a hissifit and won't give us the token")
              throw new RuntimeException(s"we cannot continue without token - start at a later date, finished categories ${finished.toList}/current : ${fileName}")
            case x: Throwable =>
              error(s"trying to download PL for $id : ${x.getMessage}")
              None
          }
          val fw = new FileWriter(idFile)
          try {
            json match {
              case Some(value) => fw.write(value.prettyPrint)
              case None => fw.write("NA")
            }
          } finally {
            fw.flush()
            fw.close()
          }
        } else {
          info(s"$id already has proper privacy label")
        }
      }
      finished.addOne(fileName)
    }
  }

  def statisticsMain(pargs: ParsingResult): Unit = {
    val files = new File(pargs.get[String]("folder"))
      .listFiles()
      .filter(_.getAbsolutePath.endsWith(".json"))
    val overallApps = ListBuffer[String]()
    val overallTtypes = ListBuffer[Int]()
    files.foreach { file =>
      val fileName = file.getAbsolutePath.split("/").last
      info(s"Statistics $fileName")
      val responses = JsonParser(readFile(file.getAbsolutePath))
        .asInstanceOf[JsArray]
        .elements
        .map(
          _.convertTo[ThreeUResponse]
        )
      val ids = responses.flatMap(_.list.map(_.itemid)).toSet
      val ttypes = responses.map(_.ttype).toSet
      info(s"distinct apps: ${ids.size}")
      info(s"distinct types: ${ttypes.size}")
      overallApps.addAll(ids)
      overallTtypes.addAll(ttypes)
    }
    info("General")
    info(s"distinct apps:${overallApps.toSet.size}")
    info(s"distinct types:${overallTtypes.toSet.size}")
  }

  def downloadMain(pargs: ParsingResult): Unit = {
    val maxPage = pargs.get[String]("until").toInt
    val categories = pargs.get[String]("category") match {
      case "all" => CATEGORIES.keySet
      case x => x.split(",").toSet
    }
    val where = pargs.get[String]("folder")
    categories.foreach { category =>
      info(s"starting downloads for $category")
      val downloads = ListBuffer[ThreeUResponse]()
      var currentPage = 0
      try {
        var currentCat = -1
        var content : String = ""
        var parameter : Seq[(String,String)] = List()
        while (currentCat + 1 != CATEGORIES(category).remd.length) {
          currentPage = 0
          currentCat += 1
          var continue = true
          try {
            while (currentPage != maxPage && continue) {
              currentPage += 1
              info(s"downloading $currentPage/$maxPage of $category")
              parameter = CATEGORIES(category).getParameter(currentPage,currentCat)
              content = getJson(category, currentPage, currentCat)
              val response = JsonParser(content).convertTo[ThreeUResponse]
              if (!response.success) {
                info(s"Response success was not true in response:\n ${response.toJson.prettyPrint}")
                continue = false
              } else {
                info("success")
              }
              if (response.list.isEmpty) {
                info(s"Response contained empty list - assuming we reached the end of the rope here laddy")
                continue = false
              } else {
                info(s"we downloaded information for ${response.list.length} apps")
              }
              downloads.addOne(response)
            }
          } catch {
            case x: Throwable =>
              info(s"we encountered ${x.getMessage} stopping this remd now")
              continue = false
          }
        }
      } catch {
        case x: Throwable =>
          error(
            s"Downloading $category at page $currentPage/$maxPage resulted in ${x.getMessage}")
      } finally {
        if (downloads.nonEmpty) {
          new File(where).mkdirs()
          val fw = new FileWriter(new File(s"$where/$category.json"))
          try {
            val apps = downloads.map(_.list.length).sum
            info(s"we have downloaded information for $apps apps of $category")
            fw.write(downloads.toList.toJson.prettyPrint)
          } finally {
            fw.flush()
            fw.close()
          }
        }
      }
    }
  }

}

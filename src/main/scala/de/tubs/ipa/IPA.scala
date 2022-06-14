package de.tubs.ipa

import de.halcony.argparse.{Parser, ParsingException, ParsingResult}
import de.tubs.ipa.ThreeUDownloader.ThreeU
import de.tubs.ipa.dataTypes.{IPAMeta, MetaBoolean, MetaDict, MetaInteger, MetaString, MetaUnknown}
import de.tubs.ipa.privacyLabels.{PrivacyLabelSet, PrivacyLabels, Visualization}
import wvlet.log.LogSupport

import java.io.{BufferedWriter, File, FileWriter}
import scala.io.Source

object IPA extends LogSupport {

  private val parser: Parser = Parser(
    "IPA",
    "utility to analyze and work with information contained in .ipa files")
    .addSubparser(ThreeU.parser)
    .addSubparser(
      Parser(
        "meta",
        "extract the app meta data of the given file/or files in given folder")
        .addPositional("elements", "a csv list of wanted elements")
        .addPositional("path",
                       "path to a single .ipa or a folder containing .ipas")
        .addFlag(
          "header",
          "e",
          "header",
          "whether to print a h(e)ader line with the requested attributes")
        .addDefault[ParsingResult => Unit]("func", appIdsMain, "the function"))
    .addSubparser(
      Parser("privacyLabels", "actions related to privacy labels")
        .addSubparser(
          Parser("download",
                 "download the privacy labels related to a given csv of apps")
            .addPositional(
              "csv",
              "a csv of apps to download, needs a header with 'itemId'")
            .addPositional(
              "out",
              "the folder where to store the downloaded privacy labels")
            .addDefault[ParsingResult => Unit]("func",
                                               downloadPrivacyLabelsMain,
                                               "the function")))

  def main(argv: Array[String]): Unit = {
    try {
      val pargv = parser.parse(argv)
      pargv.get[ParsingResult => Unit]("func")(pargv)
    } catch {
      case _: ParsingException =>
    }
  }

  private def generateVisualization(outFolder : String, setDescriptionJson : String) : Visualization = {
    new Visualization(outFolder, PrivacyLabelSet.readInLabelList(setDescriptionJson))
  }

  private def translateActions(actionCsv : String) : List[String] = {
    actionCsv.split(",").flatMap {
      case "all" => List("purposes")
      case x => List(x)
    }.toList
  }

  private def plottingPrep(pargs : ParsingResult) : (Visualization,List[String]) = {
    (generateVisualization(pargs.get[String]("outFolder"), pargs.get[String]("setDescriptionJson")),
      translateActions(pargs.get[String]("which")))
  }

  private def plotAllSetsMain(pargs : ParsingResult) : Unit = {
    val (visualization, plotActions) = plottingPrep(pargs)
    plotActions.foreach {
      case "types" => visualization.generateAllPrivacyCategoryStatistics()
      case "types-purposes" => visualization.generateAllPrivacyTypePurposeStatistic()
      case "types-data-categories" => visualization.generateAllPrivacyTypeDataCategories()
      case "summary-data-categories" => visualization.generateSummaryPrivacyTypeDataCategoriesStatistic()
    }
  }

  private def plotSingleSetMain(pargs : ParsingResult) : Unit = {
    val (visualization, plotActions) = plottingPrep(pargs)
    val label = pargs.get[String]("label")
    plotActions.foreach {
      case "types" => visualization.generateSinglePrivacyCategoryStatistics(label)
      case "types-purposes" => visualization.generateSinglePrivacyTypePurposeStatistic(label)
      case "types-data-categories" => visualization.generateSinglePrivacyTypeDataCategoriesStatistic(label)
      case "summary-data-categories" => throw new RuntimeException("This action only makes sense for all")
    }
  }

  private def downloadPrivacyLabelsMain(pargs: ParsingResult): Unit = {
    val csv = Source.fromFile(pargs.get[String]("csv"))
    val csvParsed = try {
      csv
        .getLines()
        .map { line =>
          line.split(",").toList
        }
        .toList
    } finally {
      csv.close()
    }
    val posItemId = csvParsed.head.indexOf("itemId")
    if (posItemId == -1) {
      throw new RuntimeException(
        "the provided csv must have a header line containing 'itemId'")
    }
    if (!new File(pargs.get[String]("out")).isDirectory) {
      throw new RuntimeException(
        "the provided out folder has to be a directory")
    }
    csvParsed.tail.foreach { line =>
      val itemId = line.apply(posItemId)
      try {
        info(s"getting privacy label of ${line.mkString("(", ",", ")")}")
        val labels = PrivacyLabels.getPrivacyLabel(itemId.toInt).prettyPrint
        val bw: BufferedWriter = new BufferedWriter(
          new FileWriter(new File(s"${pargs.get[String]("out")}/$itemId.json")))
        try {
          bw.write(labels)
        } finally {
          bw.flush()
          bw.close()
        }
      } catch {
        case _: Throwable =>
          error(
            s"unable to download privacy label for ${line.mkString("(", ",", ")")}")
      }
    }
  }

  private def appIdsMain(pargs: ParsingResult): Unit = {
    val path: String = pargs.get[String]("path")
    val ipaMetas: Seq[IPAMeta] = if (new File(path).isDirectory) {
      new File(path)
        .listFiles()
        .filter(_.getAbsolutePath.endsWith(".ipa"))
        .toIndexedSeq
        .flatMap { file =>
          try {
            List(IPAMeta.fromIpa(file.getAbsolutePath))
          } catch {
            case x: Throwable =>
              error(x.getMessage)
              List()
          }
        }
    } else {
      assert(new File(path).isFile && new File(path).getAbsolutePath
               .endsWith(".ipa"),
             "can only process .ipa files")
      try {
        List(IPAMeta.fromIpa(new File(path).getAbsolutePath))
      } catch {
        case x: Throwable =>
          error(x.getMessage)
          List()
      }
    }
    val keys: Seq[String] = pargs.get[String]("elements").split(",").toSeq
    if (pargs.get[Boolean]("header")) {
      println(keys.mkString(","))
    }
    ipaMetas.foreach { elem =>
      val values = keys.map { key =>
        elem.dict.get(key) match {
          case MetaUnknown(label, _) => s"?$label?".replaceAll(",","")
          case MetaInteger(value)    => value.toString.replaceAll(",","")
          case MetaDict(_)           => "<dictionary>"
          case MetaString(value)     => value.replaceAll(",","")
          case MetaBoolean(value)    => value.toString.replaceAll(",","")
        }
      }
      println(values.mkString(","))
    }
  }
}

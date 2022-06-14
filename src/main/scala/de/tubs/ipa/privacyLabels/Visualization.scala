package de.tubs.ipa.privacyLabels

import de.tubs.ias.ppm.bargraph._
import de.tubs.ias.ppm.tikzGeneral._
import de.tubs.ipa.dataTypes.{PrivacyDetails, PrivacyType}
import de.tubs.ipa.parser.LabelReader
import de.tubs.ipa.utility.FilesystemInteraction
import wvlet.log.LogSupport

import collection.mutable.{ListBuffer, Map => MMap}

class Visualization(outFolder: String, labelSet: Seq[PrivacyLabelSet])
    extends LogSupport {

  val mapping
    : Map[String,
          (Color, Color, Int, Int, PrivacyDetails, Seq[PrivacyDetails])] = {
    val colorWheelFill = new ColorWheel()
    val colorWheelBorder = new ColorWheel()
    val availableColors = 30
    var counter = 0
    if (labelSet.length > availableColors) {
      warn(
        s"the provided label set is larger than $availableColors only the first 15 are used (or supported color wise)")
    }
    labelSet
      .slice(0, availableColors)
      .map { elem =>
        val (colorFill, colorBorder) = if (counter < 15) {
          val color = colorWheelFill.getNextColor
          (color, color)
        } else {
          (White, colorWheelBorder.getNextColor)
        }
        counter += 1
        val privacyLabels: Seq[PrivacyDetails] = FilesystemInteraction
          .files(elem.absolutePath)
          .map(LabelReader.readPrivacyLabel)
        val fileCount = FilesystemInteraction.files(elem.absolutePath).length
        val emptyLabels = privacyLabels.count(_.privacyTypes.isEmpty)
        info(
          s"reading in ${elem.absolutePath}, fileCount: $fileCount, emptyLabels : $emptyLabels")
        val missing = (elem.expectedCount - fileCount) + emptyLabels
        elem.label -> (colorFill, colorBorder, missing, elem.expectedCount,
        PrivacyDetails(None, privacyLabels.flatMap(_.privacyTypes).toList),
        privacyLabels)
      }
      .toMap
  }

  private def generateCoordinatesAllPurposes(privacyDetails: PrivacyDetails,
                                             missing: Int): List[Coordinate] = {
    List(
      Coordinate(privacyDetails.getDataUsedToTrackYouCount.toString,
                 "Data Used to Track You"),
      Coordinate(privacyDetails.getDataLinkedToYouCount.toString,
                 "Data Linked to You"),
      Coordinate(privacyDetails.getDataNotLinkedToYouCount.toString,
                 "Data Not Linked to You"),
      Coordinate(privacyDetails.getDataNotCollectedCount.toString,
                 "Data Not Collected"),
      Coordinate(missing.toString, "No Label Provided")
    )
  }

  private def privacyDetailsToPurposeCountCoordinates(
      privacyDetails: PrivacyDetails,
      privacyType: String): List[Coordinate] = {
    privacyDetails
      .getPurposeCountMap(privacyType)
      .map {
        case (purpose, count) => Coordinate(count.toString, purpose)
      }
      .toList
  }

  private def privacyDetailsToDataCategoryCoordinates(
      privacyDetailsIndiv: Seq[PrivacyDetails],
      privacyType: String): List[Coordinate] = {
    val dataCategorySets =
      privacyDetailsIndiv.map(_.getPrivacyTypeDataCategorySet(privacyType))
    dataCategorySets
      .flatMap(_.toList)
      .toSet
      .map { category =>
        Coordinate(dataCategorySets.count(_.contains(category)).toString,
                   category)
      }
      .toList
  }

  def generateAllPrivacyCategoryStatistics(): Unit = {
    val texFile = s"$outFolder/AllPrivacyTypes.tex"
    val svgFile = s"$outFolder/AllPrivacyTypes.svg"
    val pair = mapping.map {
      case (key,
            (colorFill, colorBorder, missing, expected, privacyDetails, _)) =>
        val coordinates =
          generateCoordinatesAllPurposes(privacyDetails, missing)
        (Plot(colorBorder, 0.1, colorFill, coordinates, Some(key)), expected)
    }.toList
    val plots = pair.map(_._1)
    val expected = pair.map(_._2).max
    new BarGraph(
      svgFile,
      Axis(BarOrientation.horizontal,
           0.2,
           5,
           width = Some(40),
           height = Some(45),
           xmin = Some(0),
           xmax = Some(expected)),
      plots
    ).createPictureTex(texFile)
    TikzSVG.compile(texFile)
  }

  def generateAllPrivacyTypePurposeStatistic(): Unit = {
    val relevantPrivacyTypes =
      List("DATA_LINKED_TO_YOU", "DATA_NOT_LINKED_TO_YOU")
    relevantPrivacyTypes.foreach { privacyType =>
      val texFile = s"$outFolder/AllPrivacyPurposesOf$privacyType.tex"
      val svgFile = s"$outFolder/AllPrivacyPurposesOf$privacyType.svg"
      val pair = mapping.map {
        case (key, (colorFill, colorBorder, _, expected, privacyDetails, _)) =>
          val coordinates =
            privacyDetailsToPurposeCountCoordinates(privacyDetails, privacyType)
          (Plot(colorBorder, 0.2, colorFill, coordinates, Some(key)), expected)
      }.toList
      val plots = pair.map(_._1)
      val expected = pair.map(_._2).max
      new BarGraph(
        svgFile,
        Axis(BarOrientation.horizontal,
             0.2,
             5,
             width = Some(40),
             height = Some(60),
             xmin = Some(0),
             xmax = Some(expected)),
        plots
      ).createPictureTex(texFile)
      TikzSVG.compile(texFile)
    }
  }

  def generateAllPrivacyTypeDataCategories(): Unit = {
    var maxExpected = 0
    val relevantPrivacyTypes = List("DATA_LINKED_TO_YOU",
                                    "DATA_NOT_LINKED_TO_YOU",
                                    "DATA_USED_TO_TRACK_YOU")
    relevantPrivacyTypes.foreach { privacyType =>
      val dataCategoryPlots: MMap[String, ListBuffer[Plot]] = MMap()
      mapping.foreach {
        case (key,
              (colorFill, colorBorder, _, expected, _, privacyDetailsIndiv)) =>
          if (expected > maxExpected) {
            maxExpected = expected
          }
          privacyDetailsToDataCategoryCoordinates(privacyDetailsIndiv,
                                                  privacyType).foreach {
            coordinate =>
              if (dataCategoryPlots.contains(coordinate.y)) {
                dataCategoryPlots(coordinate.y).addOne(
                  Plot(colorBorder, 0.2, colorFill, List(coordinate), Some(key))
                )
              } else {
                dataCategoryPlots.addOne(
                  coordinate.y -> ListBuffer(
                    Plot(colorBorder,
                         0.2,
                         colorFill,
                         List(coordinate),
                         Some(key)))
                )
              }
          }
      }
      dataCategoryPlots.foreach {
        case (dataCategory, plots) =>
          val texFile =
            s"$outFolder/All${dataCategory.trim}DataCategoryOfPrivacyType${privacyType}.tex"
          val svgFile =
            s"$outFolder/All${dataCategory.trim}DataCategoryOfPrivacyType${privacyType}.svg"
          new BarGraph(
            svgFile,
            Axis(BarOrientation.horizontal,
                 0.2,
                 5,
                 width = Some(20),
                 height = Some(15),
                 xmin = Some(0),
                 xmax = Some(maxExpected)),
            plots.toList
          ).createPictureTex(texFile)
          TikzSVG.compile(texFile)
      }

    }
  }

  def generateSinglePrivacyCategoryStatistics(labelName: String): Unit = {
    val texFile = s"$outFolder/Single${labelName}privacyTypes.tex"
    val svgFile = s"$outFolder/Single${labelName}privacyTypes.svg"
    val (colorBorder, colorFill, missing, expected, privacyDetails, _) =
      mapping(labelName)
    val coordinates = generateCoordinatesAllPurposes(privacyDetails, missing)
    new BarGraph(
      svgFile,
      Axis(BarOrientation.horizontal,
           0.2,
           10,
           xmin = Some(0),
           xmax = Some(expected)),
      List(Plot(colorBorder, 0.1, colorFill, coordinates, Some(labelName)))
    ).createPictureTex(texFile)
    TikzSVG.compile(texFile)
  }

  def generateSinglePrivacyTypePurposeStatistic(labelName: String): Unit = {
    val relevantPrivacyTypes =
      List("DATA_LINKED_TO_YOU", "DATA_NOT_LINKED_TO_YOU")
    val (_, _, _, _, privacyDetails, _) = this.mapping(labelName)
    relevantPrivacyTypes.foreach { privacyType =>
      val texFile =
        s"$outFolder/Single${labelName}PrivacyPurposesOf$privacyType.tex"
      val svgFile =
        s"$outFolder/Single${labelName}PrivacyPurposesOf$privacyType.svg"
      val coordinates =
        privacyDetailsToPurposeCountCoordinates(privacyDetails, privacyType)
      new BarGraph(
        svgFile,
        Axis(BarOrientation.horizontal, 0.2, 10, xmin = Some(0)),
        List(Plot(Black, 0.1, Black, coordinates, Some(labelName)))
      ).createPictureTex(texFile)
      TikzSVG.compile(texFile)
    }
  }

  def generateSinglePrivacyTypeDataCategoriesStatistic(
      labelName: String): Unit = {
    val relevantPrivacyTypes = List("DATA_LINKED_TO_YOU",
                                    "DATA_NOT_LINKED_TO_YOU",
                                    "DATA_USED_TO_TRACK_YOU")
    val (_, _, _, _, _, privacyDetailsIndiv) = this.mapping(labelName)
    relevantPrivacyTypes.foreach { privacyType =>
      val texFile =
        s"$outFolder/Single${labelName}DataCategoriesOf$privacyType.tex"
      val svgFile =
        s"$outFolder/Single${labelName}DataCategoriesOf$privacyType.svg"
      val coordinates =
        privacyDetailsToDataCategoryCoordinates(privacyDetailsIndiv,
                                                privacyType)
      new BarGraph(
        svgFile,
        Axis(BarOrientation.horizontal,
             0.2,
             5,
             xmin = Some(0),
             height = Some(20)),
        List(Plot(Black, 0.1, Black, coordinates, Some(labelName)))
      ).createPictureTex(texFile)
      TikzSVG.compile(texFile)
    }
  }

  def generateSummaryPrivacyTypeDataCategoriesStatistic(): Unit = {
    val relevantPrivacyTypes = List("DATA_LINKED_TO_YOU",
                                    "DATA_NOT_LINKED_TO_YOU",
                                    "DATA_USED_TO_TRACK_YOU")
    val privacyTypesIndiv = mapping.flatMap(_._2._6)
    val maxExpected = mapping.map(_._2._4).sum
    relevantPrivacyTypes.foreach { privacyType =>
      val texFile = s"$outFolder/SummaryDataCategoriesOf$privacyType.tex"
      val svgFile = s"$outFolder/SummaryDataCategoriesOf$privacyType.svg"
      val plot =
        Plot(Black,
             0.2,
             Black,
             privacyDetailsToDataCategoryCoordinates(privacyTypesIndiv.toList,
                                                     privacyType),
             Some("Summary"))
      new BarGraph(
        svgFile,
        Axis(BarOrientation.horizontal,
             0.2,
             5,
             xmin = Some(0),
             height = Some(20),
             xmax = Some(maxExpected)),
        List(plot)
      ).createPictureTex(texFile)
      TikzSVG.compile(texFile)
    }
  }

}

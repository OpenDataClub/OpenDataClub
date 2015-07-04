package com.opendataclub.scrapers.ine

import sys.process._
import java.net.URL
import java.io.File
import scala.io.Source
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import com.opendataclub.models.ExternalDataSource
import com.opendataclub.models.DataImport
import com.opendataclub.models.DataImport
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import com.opendataclub.scrapers.Scraper
import scala.util.Try
import scala.util.Success

/**
 * @author juanignaciosl
 * ine.es
 *   -> "EPA. Ocupados (miles)". "Información detallada"
 *   -> "Resultados"
 *   -> "Resultados nacionales" (trimestrales), "Ocupados"
 *   -> "Ocupados por sexo y grupo de edad. Valores absolutos y porcentajes respecto del total de cada sexo" (http://www.ine.es/dynt3/inebase/es/index.htm?padre=982&capsel=985)
 *   -> Download, CSV: http://www.ine.es/jaxiT3/files/es/4076c.csv?t=4076&nocab=1
 */
class IneEpaScraper extends Scraper {

  val downloadedFilePath = "tmp/epaQuarterSexAge.csv"

  def run(externalDataSource: ExternalDataSource): Try[DataImport] = {
    downloadEpaQuarterSexAge(externalDataSource.downloadUrl, downloadedFilePath)
    val intervalsAndValuesPerRange = parseEpaQuarterSexAgeFile()
    Success(new DataImport(externalDataSource, intervalsAndValuesPerRangeToJson(intervalsAndValuesPerRange)))
  }

  private def downloadEpaQuarterSexAge(downloadUrl: String, downloadedFilePath: String = downloadedFilePath): Unit = {
    fileDownloader(downloadUrl, downloadedFilePath)
  }

  private def fileDownloader(url: String, filename: String) = {
    new URL(url) #> new File(filename) !!
  }

  private def parseEpaQuarterSexAgeFile(downloadedFilePath: String = downloadedFilePath): (List[Interval], List[(Range, List[String])]) = {
    val epaContent = Source.fromFile(downloadedFilePath).getLines().toList.drop(3).dropRight(1)

    val quarters = extractQuartersFromHeader(epaContent.head)
    val totals = epaContent.drop(1).map(extractTotals(_))

    (quarters, totals)
  }

  private def extractQuartersFromHeader(headersLine: String): List[Interval] = {
    headersLine.split(",").drop(9).dropRight(1).map { quarter =>
      val yearAndQuarter = quarter.split("T")
      val beginning = new DateTime(yearAndQuarter(0).toInt, yearAndQuarter(1).toInt * 3, 1, 0, 0)
      new Interval(beginning, 3.months)
    }.toList
  }

  private def extractTotals(epaContentLine: String): (Range, List[String]) = {
    val cells = epaContentLine.split(",").zipWithIndex.filter(_._2 % 2 == 0).map(_._1).toList
    val extractionPattern = """De (\d*) a (\d*) años""".r
    val extractionPatternMax = """De (\d*) y más años""".r
    val ageRange = cells.last match {
      case extractionPattern(minAge, maxAge) => minAge.toInt until maxAge.toInt
      case extractionPatternMax(minAge)      => minAge.toInt until Int.MaxValue
    }
    (ageRange, cells.dropRight(1).map(_.replaceAll("\\.", "")))
  }

  implicit val intervalWrites = new Writes[Interval] {
    def writes(interval: Interval) = Json.obj(
      "start" -> interval.start,
      "end" -> interval.end)
  }

  implicit val rangeWrites = new Writes[Range] {
    def writes(range: Range) = Json.obj(
      "start" -> range.start,
      "end" -> range.end)
  }

  implicit def tuple2[A: Writes, B: Writes]: Writes[(A, B)] = new Writes[(A, B)] {
    def writes(o: (A, B)): JsValue = Json.arr(o._1, o._2)
  }

  private def intervalsAndValuesPerRangeToJson(intervalsAndValuesPerRange: (List[Interval], List[(Range, List[String])])): JsValue = {
    Json.obj(
        "intervals" -> Json.toJson(intervalsAndValuesPerRange._1),
        "valuesPerRange" -> Json.toJson(intervalsAndValuesPerRange._2)
    )
  }

}
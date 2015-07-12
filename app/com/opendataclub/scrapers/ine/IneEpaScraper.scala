package com.opendataclub.scrapers.ine

import sys.process._
import java.net.URL
import java.io.File
import scala.io.Source
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import com.opendataclub.models.ExternalDataSource
import com.opendataclub.models.DataImport
import com.opendataclub.models.DataImportRepository
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import com.opendataclub.scrapers.Scraper
import scala.util.Try
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import slick.lifted._
import slick.driver.PostgresDriver.api._
import com.github.tototoshi.slick.PostgresJodaSupport._
import slick.backend.DatabaseConfig
import scala.concurrent.Future
import slick.driver.JdbcProfile
import com.opendataclub.models.DataTable
import com.opendataclub.models.DataImportId
import com.opendataclub.models.TransientDataImport
import com.opendataclub.models.DataTableRepository

/**
 * ine.es
 *   -> "EPA. Ocupados (miles)". "Información detallada"
 *   -> "Resultados"
 *   -> "Resultados nacionales" (trimestrales), "Ocupados"
 *   -> "Ocupados por sexo y grupo de edad. Valores absolutos y porcentajes respecto del total de cada sexo" (http://www.ine.es/dynt3/inebase/es/index.htm?padre=982&capsel=985)
 *   -> Download, CSV: http://www.ine.es/jaxiT3/files/es/4076c.csv?t=4076&nocab=1
 */
class IneEpaScraper extends Scraper {

  val downloadedFilePath = "tmp/epaQuarterSexAge.csv"
  val groups = List("Ambos sexos", "Ambos sexos (%)", "Hombres", "Hombres (%)", "Mujeres", "Mujeres (%)")

  def run(externalDataSource: ExternalDataSource, dbConfig: DatabaseConfig[JdbcProfile], dataImportRepository: DataImportRepository, dataTableRepository: DataTableRepository): Future[(DataImport, DataTable)] = {
    new URL(externalDataSource.downloadUrl) #> new File(downloadedFilePath) !!

    for {
      intervalsAndValuesPerRange <- new IneEpaScraperToScala().parseEpaQuarterSexAgeFile(downloadedFilePath)
      dataImport <- dataImportRepository.put(new TransientDataImport(externalDataSource, new IneEpaScraperJsonConverter().intervalsAndValuesPerRangeToJson(intervalsAndValuesPerRange)))
      schemaAndName <- new DatabaseStorage().store(dbConfig, dataImport.id, intervalsAndValuesPerRange, externalDataSource)
      dataTable <- dataTableRepository.put(new DataTable(dataImport.id, schemaAndName._1, schemaAndName._2))
    } yield (dataImport, dataTable)
  }

  class IneEpaScraperToScala {

    def parseEpaQuarterSexAgeFile(downloadedFilePath: String): Future[(List[Interval], List[(Range, List[Float])])] = {
      Future {
        val epaContent = Source.fromFile(downloadedFilePath).getLines().toList.drop(3)

        // INFO: this contains duplications 
        val quarters = extractQuartersFromHeader(epaContent.head)

        val length = quarters.length

        val ranges = epaContent.dropRight(1).map(extractRange(_))
        val totals = epaContent.drop(1).map(extractTotals(_, length).padTo(length, 0F)).toVector

        (quarters, ranges.zipWithIndex.map { case (r, i) => (r, totals(i)) })
      }
    }

    private def extractQuartersFromHeader(headersLine: String): List[Interval] = {
      headersLine.split(",").drop(9).dropRight(1).map { quarter =>
        val yearAndQuarter = quarter.split("T")
        val beginning = new DateTime(yearAndQuarter(0).toInt, ((yearAndQuarter(1).toInt - 1) * 3) + 1, 1, 0, 0)
        new Interval(beginning, 3.months - 1.day)
      }.toList
    }

    private def extractRange(epaContentLine: String): (Range) = {
      val cells = epaContentLine.split(",").toList
      val total = """Total""".r
      val extractionPattern = """De (\d*) a (\d*) años""".r
      val extractionPatternMax = """De (\d*) y más años""".r
      cells.last match {
        case total()                           => 0 until Int.MaxValue
        case extractionPattern(minAge, maxAge) => minAge.toInt until maxAge.toInt
        case extractionPatternMax(minAge)      => minAge.toInt until Int.MaxValue
      }
    }

    private def extractTotals(epaContentLine: String, length: Int): List[Float] = {
      // removing percentage decimals
      epaContentLine
        .split(",")
        .padTo(length, "0")
        .grouped(2)
        .map { pairOrNot => (if (pairOrNot.length == 2) pairOrNot else Array(pairOrNot(0), 0)) }
        .map { pair => s"${pair(0)}${pair(1)}" }
        .toList
        .dropRight(1).map(_.replaceAll("\\.", "").toFloat / 10)
    }
  }

  class IneEpaScraperJsonConverter {
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

    def intervalsAndValuesPerRangeToJson(intervalsAndValuesPerRange: (List[Interval], List[(Range, List[Float])])): JsValue = {
      Json.obj(
        "intervals" -> Json.toJson(intervalsAndValuesPerRange._1),
        "valuesPerRange" -> Json.toJson(intervalsAndValuesPerRange._2))
    }
  }

  class DatabaseStorage {

    def store(dbConfig: DatabaseConfig[JdbcProfile], dataImportId: DataImportId, intervalsAndValuesPerRange: (List[Interval], List[(Range, List[Float])]), externalDataSource: ExternalDataSource): Future[(String, String)] = {

      lazy val db = dbConfig.db

      // TODO: maybe we should use s"eds_${externalDataSource.id.value}" as schema, for example, instead of public
      val schema = "public"
      val name = s"di_${dataImportId.value}"

      val sqlCheckTableExists = sql"""
      SELECT EXISTS (
          SELECT 1 
          FROM   pg_catalog.pg_class c
          JOIN   pg_catalog.pg_namespace n ON n.oid = c.relnamespace
          WHERE  n.nspname = $schema
          AND    c.relname = $name
          AND    c.relkind = 'r'
      )
      """.as[(Boolean)]

      db.run(sqlCheckTableExists).flatMap { exists =>
        if (!exists(0)) {
          createDataTableContent(dbConfig, intervalsAndValuesPerRange, name).map { _ => (schema, name) }
        } else {
          Future { (schema, name) }
        }
      }
    }

    private def createDataTableContent(dbConfig: DatabaseConfig[JdbcProfile], intervalsAndValuesPerRange: (List[Interval], List[(Range, List[Float])]), name: String): Future[(Int, List[Int])] = {
      lazy val db = dbConfig.db

      val valuesPerRange = intervalsAndValuesPerRange._2

      val columns = columnsFromRanges(valuesPerRange.map(_._1))

      val sqlCreateTable = sqlu"""
      create table #$name (
        "id" SERIAL NOT NULL PRIMARY KEY,
        "measure_group" text not null,
        "interval_end" date not null,
        #${columns.map { column => s"$column numeric(6, 1)" }.mkString(", ")}
      )
      """

      val values = valuesPerInterval(intervalsAndValuesPerRange)

      val inserts = values.map {
        intervalAndValues =>
          sqlu"""
              insert into #$name (interval_end, measure_group, #${columns.mkString(", ")})
              values ('#${intervalAndValues._1.end}', '#${intervalAndValues._2}', #${intervalAndValues._3.mkString(", ")})
              """
      }

      for {
        resultCreate <- db.run(sqlCreateTable)
        resultInserts <- Future.sequence(inserts.map(db.run(_)))
      } yield (resultCreate, resultInserts)
    }

    def columnsFromRanges(ranges: List[Range]): List[String] = {
      ranges.map { range => s"from_${range.min}_to_${range.max + 1}" }
    }

    def valuesPerInterval(intervalsAndValuesPerRange: (List[Interval], List[(Range, List[Float])])): List[(Interval, String, List[Float])] = {
      val intervals = intervalsAndValuesPerRange._1
      val values = intervalsAndValuesPerRange._2.map(_._2)
      val valuesTransposed = values.transpose
      intervals.zipWithIndex.map { case (interval, i) => (interval, groups(i % groups.size), valuesTransposed(i)) }
    }

  }

}
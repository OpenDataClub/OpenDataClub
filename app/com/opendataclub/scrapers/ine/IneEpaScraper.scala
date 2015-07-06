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

  def run(externalDataSource: ExternalDataSource, dbConfig: DatabaseConfig[JdbcProfile], dataImportRepository: DataImportRepository): Future[(DataImport, Option[DataTable])] = {
    new URL(externalDataSource.downloadUrl) #> new File(downloadedFilePath) !!

    for {
      dataImport <- new IneEpaScraperToJson().run(downloadedFilePath, externalDataSource)
      storedDataImport <- dataImportRepository.put(dataImport)
      dataTable <- new DatabaseStorage().store(dbConfig, storedDataImport, externalDataSource)
    } yield (storedDataImport, dataTable)
  }
  
  class IneEpaScraperToJson {

    def run(downloadedFilePath: String, externalDataSource: ExternalDataSource): Future[DataImport] = {
      Future {
        val intervalsAndValuesPerRange = parseEpaQuarterSexAgeFile(downloadedFilePath)
        new DataImport(externalDataSource, intervalsAndValuesPerRangeToJson(intervalsAndValuesPerRange))
      }
    }

    private def parseEpaQuarterSexAgeFile(downloadedFilePath: String): (List[Interval], List[(Range, List[String])]) = {
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
        "valuesPerRange" -> Json.toJson(intervalsAndValuesPerRange._2))
    }

  }

  class DatabaseStorage {

    def store(dbConfig: DatabaseConfig[JdbcProfile], dataImport: DataImport, externalDataSource: ExternalDataSource): Future[Option[DataTable]] = {
      dataImport.id match {
        case Some(id: DataImportId) => store(dbConfig, id, dataImport, externalDataSource)
        case None                   => Future { None }
      }
    }

    private def store(dbConfig: DatabaseConfig[JdbcProfile], dataImportId: DataImportId, dataImport: DataImport, externalDataSource: ExternalDataSource): Future[Option[DataTable]] = {
      val dataTable = new DataTable(dataImportId)

      lazy val db = dbConfig.db
      val schema = dataTable.schema
      val name = dataTable.name

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
        // WIP
        if (!exists(0)) {
          createDataTableContent(dbConfig, dataImport.content, dataTable).map { _ => Some(dataTable) }
        } else {
          Future { Some(dataTable) }
        }
      }
    }

    private def createDataTableContent(dbConfig: DatabaseConfig[JdbcProfile], content: JsValue, dataTable: DataTable): Future[Boolean] = {
      val name = dataTable.name
      // WIP
      lazy val db = dbConfig.db
      val sqlCreateTable = sqlu"""
      create table #$name (
        "id" SERIAL NOT NULL PRIMARY KEY,
        #${columns.mkString(",")}
      )
      """
      val result = db.run(sqlCreateTable)
      result.map { _ => true }
    }

    def columns = {
      // WIP
      List("c1 text", "c2 text")
    }

  }

}
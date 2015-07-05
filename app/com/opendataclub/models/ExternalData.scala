package com.opendataclub.models

import slick.lifted._
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import com.github.tototoshi.slick.PostgresJodaSupport._
import slick.backend.DatabaseConfig
import scala.concurrent.Future
import slick.driver.JdbcProfile
import org.joda.time.DateTime
import slick.lifted.Tag
import com.opendataclub.scrapers.ine.IneEpaScraper
import scala.concurrent.ExecutionContext.Implicits.global
import com.opendataclub.scrapers.Scraper
import com.opendataclub.scrapers.Scraper
import scala.util.Failure
import scala.util.Try
import scala.util.Success

class ExternalDataSourceService(dbConfig: DatabaseConfig[JdbcProfile]) {

  val db = dbConfig.db
  val externalDataSources = slick.lifted.TableQuery[ExternalDataSources]

  def extract(id: Long): Future[(ExternalDataSource, DataImport)] = {
    val edsf = db.run(externalDataSources.filter(_.id === new ExternalDataSourceId(-1L)).take(1).result.head)
    val externalDataSourceAndDataImportAttempt = for {
      eds <- edsf
      diAttempt <- Future { eds.extract }
    } yield (eds, diAttempt)

    externalDataSourceAndDataImportAttempt.map { _ match {
      case (externalDataSource: ExternalDataSource, Success(di)) => db.run(slick.lifted.TableQuery[DataImports] += di); (externalDataSource, di)
      case (externalDataSource: ExternalDataSource, Failure(e)) => throw e
      }
    }
  }
}

case class ExternalDataSourceId(value: Long) extends slick.lifted.MappedTo[Long]

/**
 * @author juanignaciosl
 */
case class ExternalDataSource(sourceId: SourceId, name: String, description: String, url: String, downloadUrl: String, className: String, createdAt: DateTime, updatedAt: DateTime, id: ExternalDataSourceId) {

  def extract: Try[DataImport] = {
    val scraper = Class.forName(className).newInstance()
    scraper match {
      case s: Scraper => s.run(this)
      case _ => Failure(new RuntimeException(s"$className not found"))
    }
  }

}

class ExternalDataSources(tag: Tag) extends Table[ExternalDataSource](tag, "external_data_sources") {
  val sources = slick.lifted.TableQuery[Sources]

  def sourceId = column[SourceId]("source_id")
  def source = foreignKey("external_data_sources_source_fk", sourceId, sources)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  def name = column[String]("name")
  def description = column[String]("description")
  def url = column[String]("url")
  def downloadUrl = column[String]("download_url")
  def className = column[String]("class_name")
  def createdAt = column[DateTime]("created_at")
  def updatedAt = column[DateTime]("updated_at")
  def id = column[ExternalDataSourceId]("id", O.AutoInc, O.PrimaryKey)

  def * = (sourceId, name, description, url, downloadUrl, className, createdAt, updatedAt, id) <> (ExternalDataSource.tupled, ExternalDataSource.unapply)
}
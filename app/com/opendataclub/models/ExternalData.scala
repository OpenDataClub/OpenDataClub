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

class ExternalDataSourceRepository(dbConfig: DatabaseConfig[JdbcProfile]) {

  val db = dbConfig.db
  val externalDataSources = slick.lifted.TableQuery[ExternalDataSources]

  def extract(id: Long): Future[DataImport] = {
    val edsf = db.run(externalDataSources.filter(_.id === new ExternalDataSourceId(-1L)).take(1).result.head)
    for {
      eds <- edsf
      di <- Future { eds.extract }
      whatever <- db.run(slick.lifted.TableQuery[DataImports] += di)
    } yield di
  }
}

case class ExternalDataSourceId(value: Long) extends slick.lifted.MappedTo[Long]

/**
 * @author juanignaciosl
 */
case class ExternalDataSource(sourceId: SourceId, name: String, description: String, url: String, downloadUrl: String, createdAt: DateTime, updatedAt: DateTime, id: ExternalDataSourceId) {

  def extract: DataImport = {
    // TODO: this should be a factory depending on current ExternalDataSource
    val scraper = new IneEpaScraper(this)
    val dataImport = scraper.run

    dataImport
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
  def createdAt = column[DateTime]("created_at")
  def updatedAt = column[DateTime]("updated_at")
  def id = column[ExternalDataSourceId]("id", O.AutoInc, O.PrimaryKey)

  def * = (sourceId, name, description, url, downloadUrl, createdAt, updatedAt, id) <> (ExternalDataSource.tupled, ExternalDataSource.unapply)
}
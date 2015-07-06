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
import play.api.mvc.PathBindable

class ExternalDataSourceService(repository: ExternalDataSourceRepository, dataImportRepository: DataImportRepository) {

  def extract(id: ExternalDataSourceId, dbConfig: DatabaseConfig[JdbcProfile]): Future[(ExternalDataSource, DataImport, Option[DataTable])] = {
    for {
      eds <- repository.get(id)
      extraction <- eds.extract(dbConfig, dataImportRepository)
    } yield (eds, extraction._1, extraction._2)
  }
}

class ExternalDataSourceRepository(dbConfig: DatabaseConfig[JdbcProfile]) extends ReadRepository[ExternalDataSource, ExternalDataSourceId] {
  val db = dbConfig.db

  lazy val externalDataSources = slick.lifted.TableQuery[ExternalDataSources]

  def get(id: ExternalDataSourceId): Future[ExternalDataSource] = {
    db.run(externalDataSources.filter(_.id === new ExternalDataSourceId(-1L)).take(1).result.head)
  }
}

case class ExternalDataSourceId(value: Long) extends slick.lifted.MappedTo[Long]
object ExternalDataSourceId {
  implicit def pathBinder(implicit intBinder: PathBindable[Long]) = new PathBindable[ExternalDataSourceId] {
    override def bind(key: String, value: String): Either[String, ExternalDataSourceId] = {
      Right(new ExternalDataSourceId(value.toLong))
    }
    override def unbind(key: String, id: ExternalDataSourceId): String = {
      id.value.toString
    }
  }
}

case class ExternalDataSource(sourceId: SourceId, name: String, description: String, url: String, downloadUrl: String, className: String, createdAt: DateTime, updatedAt: DateTime, id: ExternalDataSourceId) {
  
	def extract(dbConfig: DatabaseConfig[JdbcProfile], dataImportRepository: DataImportRepository) = {
    scraper.flatMap(_.run(this, dbConfig, dataImportRepository))
  }
  
  private def scraper: Future[Scraper] = {
    Future {
      lazy val scraper = Class.forName(className).newInstance()
      scraper match {
        case s: Scraper => s
        case _          => throw new RuntimeException(s"$className not found")
      }
    }
  }
  
}

class ExternalDataSources(tag: Tag) extends Table[ExternalDataSource](tag, "external_data_sources") {
  lazy val sources = slick.lifted.TableQuery[Sources]

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
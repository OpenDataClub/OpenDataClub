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
import play.api.libs.json.JsValue
import com.opendataclub.postgres.MyPostgresDriver.api.playJsonTypeMapper
import play.api.mvc.PathBindable
import scala.concurrent.ExecutionContext.Implicits.global

class DataImportRepository(dbConfig: DatabaseConfig[JdbcProfile]) extends ReadWriteRepository[TransientDataImport, StoredDataImport, DataImportId] {
  val db = dbConfig.db

  lazy val dataImports = slick.lifted.TableQuery[DataImports]

  def get(id: DataImportId): Future[Option[StoredDataImport]] = {
    db.run(dataImports.filter(_.id === id).take(1).result.headOption.map {
      _ match {
        case Some(dataImport: TransientDataImport) => Some(new StoredDataImport(dataImport, dataImport.id.get))
        case None => None
      }
    })
  }

  def put(dataImport: TransientDataImport): Future[StoredDataImport] = {
    val dataImportId = (dataImports returning dataImports.map(_.id)) += dataImport
    db.run(dataImportId.transactionally).map { id: DataImportId => new StoredDataImport(dataImport, id) }
  }
}

sealed trait DataImport {
  def externalDataSourceId: ExternalDataSourceId
  def createdAt: DateTime
  def content: JsValue
}
case class TransientDataImport(val externalDataSourceId: ExternalDataSourceId, val content: JsValue, val createdAt: DateTime = new DateTime(), val id: Option[DataImportId] = None) extends DataImport {
  def this(externalDataSource: ExternalDataSource, content: JsValue) = this(externalDataSource.id, content, new DateTime, None)
}
class StoredDataImport(val externalDataSourceId: ExternalDataSourceId, val content: JsValue, val createdAt: DateTime, val id: DataImportId) extends DataImport {
  def this(transient: TransientDataImport, id: DataImportId) = this(transient.externalDataSourceId, transient.content, transient.createdAt, id)
}

case class DataImportId(value: Long) extends slick.lifted.MappedTo[Long]
object DataImportId {
  implicit def pathBinder(implicit intBinder: PathBindable[Long]) = new PathBindable[DataImportId] {
    override def bind(key: String, value: String): Either[String, DataImportId] = {
      Right(new DataImportId(value.toLong))
    }
    override def unbind(key: String, id: DataImportId): String = {
      id.value.toString
    }
  }
}

class DataImports(tag: Tag) extends Table[TransientDataImport](tag, "data_imports") {
  lazy val externalDataSources = slick.lifted.TableQuery[ExternalDataSources]

  def externalDataId = column[ExternalDataSourceId]("external_data_source_id")
  def externalData = foreignKey("data_imports_external_data_fk", externalDataId, externalDataSources)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  def createdAt = column[DateTime]("created_at")
  def content = column[JsValue]("content")
  def id = column[DataImportId]("id", O.AutoInc, O.PrimaryKey)

  def * = (externalDataId, content, createdAt, id.?) <> (TransientDataImport.tupled, TransientDataImport.unapply)
}
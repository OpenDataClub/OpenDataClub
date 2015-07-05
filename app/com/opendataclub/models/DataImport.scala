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

class DataImportRepository(dbConfig: DatabaseConfig[JdbcProfile]) extends ReadWriteRepository[DataImport, DataImportId] {
  val db = dbConfig.db

  lazy val dataImports = slick.lifted.TableQuery[DataImports]

  def get(id: DataImportId): Future[DataImport] = {
    db.run(dataImports.filter(_.id === id).take(1).result.head)
  }
  
  def put(dataImport: DataImport): Future[Int] = {
    db.run(slick.lifted.TableQuery[DataImports] += dataImport)
  }
}

case class DataImport(externalDataSourceId: ExternalDataSourceId, createdAt: DateTime, content: JsValue, id: Option[DataImportId]) {
  def this(externalDataSource: ExternalDataSource, content: JsValue)= this(externalDataSource.id, new DateTime, content, None)
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

class DataImports(tag: Tag) extends Table[DataImport](tag, "data_imports") {
  lazy val externalDataSources = slick.lifted.TableQuery[ExternalDataSources]
  
  def externalDataId = column[ExternalDataSourceId]("external_data_source_id")
  def externalData = foreignKey("data_imports_external_data_fk", externalDataId, externalDataSources)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def createdAt = column[DateTime]("created_at")
  def content = column[JsValue]("content")
  def id = column[DataImportId]("id", O.AutoInc, O.PrimaryKey)
  
  def * = (externalDataId, createdAt, content, id.?) <> (DataImport.tupled, DataImport.unapply)
}
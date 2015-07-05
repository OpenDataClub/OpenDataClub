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

case class DataTable(dataImportId: DataImportId, schema: String, name: String, createdAt: DateTime, id: Option[DataTableId]) {
  // TODO: how can we avoid using `.get`?
  def this(dataImport: DataImport, externalDataSource: ExternalDataSource) = this(dataImport.id.get, s"eds_${externalDataSource.id}", s"di_${dataImport.id}", new DateTime, None)
}

case class DataTableId(value: Long) extends slick.lifted.MappedTo[Long]
object DataTableId {
  implicit def pathBinder(implicit intBinder: PathBindable[Long]) = new PathBindable[DataTableId] {
    override def bind(key: String, value: String): Either[String, DataTableId] = {
      Right(new DataTableId(value.toLong))
    }
    override def unbind(key: String, id: DataTableId): String = {
      id.value.toString
    }
  }
}

class DataTables(tag: Tag) extends Table[DataTable](tag, "data_tables") {
  lazy val dataImports = slick.lifted.TableQuery[DataImports]

  def dataImportId = column[DataImportId]("data_import_id")
  def dataImport = foreignKey("data_tables_data_imports_fk", dataImportId, dataImports)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  def schema = column[String]("schema")
  def name = column[String]("name")
  def createdAt = column[DateTime]("created_at")
  def id = column[DataTableId]("id", O.AutoInc, O.PrimaryKey)

  def * = (dataImportId, schema, name, createdAt, id.?) <> (DataTable.tupled, DataTable.unapply)
}
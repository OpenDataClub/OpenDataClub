package com.opendataclub.models

import scala.concurrent.ExecutionContext.Implicits.global
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

class DataTableRepository(dbConfig: DatabaseConfig[JdbcProfile]) extends ReadWriteRepository[DataTable, DataTableId] {
  lazy val db = dbConfig.db

  lazy val dataTables = slick.lifted.TableQuery[DataTables]

  def get(id: DataTableId): Future[DataTable] = {
    db.run(dataTables.filter(_.id === id).take(1).result.head)
  }

  def get(dataImportId: DataImportId): Future[Option[DataTable]] = {
    db.run(dataTables.filter(_.dataImportId === dataImportId).take(1).result.headOption)
  }

  def put(dataTable: DataTable): Future[DataTable] = {
    val dataTableWithId = (dataTables returning dataTables.map(_.id) into ((Table, id) => dataTable.copy(id = Some(id)))) += dataTable
    db.run(dataTableWithId.transactionally)
  }
}

case class DataTable(dataImportId: DataImportId, schema: String, name: String, createdAt: DateTime, id: Option[DataTableId]) {
  // TODO: maybe we should use s"eds_${externalDataSource.id.value}" as schema, for example, instead of public
  def this(dataImportId: DataImportId) = this(dataImportId, "public", s"di_${dataImportId.value}", new DateTime, None)

  def headers(dbConfig: DatabaseConfig[JdbcProfile]): Future[List[DataTableColumn]] = {
    lazy val db = dbConfig.db

    val columnsSql = sql"""
      SELECT column_name
      FROM information_schema.columns
      WHERE table_schema = $schema
      AND table_name   = $name
      """.as[(String)]

    db.run(columnsSql).map(_.toList.map(new DataTableColumn(_)))
  }

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

case class DataTableColumn(name: String)
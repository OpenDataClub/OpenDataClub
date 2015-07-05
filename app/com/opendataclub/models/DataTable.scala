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

class DataTableService(dataTableRepository: DataTableRepository) {
  def dataTable(dataImport: DataImport, externalDataSource: ExternalDataSource): Future[DataTable] = {
    val dataTableFuture = dataTableRepository.get(dataImport.id.get)
    dataTableFuture.flatMap {
      _ match {
        case Some(dt: DataTable) => Future { dt }
        case None => {
          val dataTable = new DataTable(dataImport, externalDataSource)
          dataTableRepository.put(dataTable).map { _ => dataTable }
        }
      }
    }
  }
}

class DataTableRepository(dbConfig: DatabaseConfig[JdbcProfile]) extends ReadWriteRepository[DataTable, DataTableId] {
  lazy val db = dbConfig.db

  lazy val dataTables = slick.lifted.TableQuery[DataTables]

  def get(id: DataTableId): Future[DataTable] = {
    db.run(dataTables.filter(_.id === id).take(1).result.head)
  }

  def get(dataImportId: DataImportId): Future[Option[DataTable]] = {
    db.run(dataTables.filter(_.dataImportId === dataImportId).take(1).result.headOption)
  }

  def put(dataTable: DataTable): Future[Int] = {
    db.run(dataTables += dataTable)
  }
}

case class DataTable(dataImportId: DataImportId, schema: String, name: String, createdAt: DateTime, id: Option[DataTableId]) {
  // TODO: how can we avoid using `.get`? Search the whole proyect for some .get on Option
  // TODO: maybe we should use s"eds_${externalDataSource.id.value}" as schema, for example, instead of public
  def this(dataImport: DataImport, externalDataSource: ExternalDataSource) = this(dataImport.id.get, "public", s"di_${dataImport.id.get.value}", new DateTime, None)
  
  def content(dbConfig: DatabaseConfig[JdbcProfile], dataImport: DataImport): Future[Boolean] = {
    lazy val db = dbConfig.db
    
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
      if(!exists(0)) {
        createDataTableContent(dbConfig, dataImport)
      } else {
        Future { true }
      }
    }
  }
  
  private def createDataTableContent(dbConfig: DatabaseConfig[JdbcProfile], dataImport: DataImport): Future[Boolean] = {
    // WIP
    lazy val db = dbConfig.db
    val sqlCreateTable = sqlu"""
      create table #$name (
        "id" SERIAL NOT NULL PRIMARY KEY,
        #${dataImport.columns.mkString(",")}
      )
      """
    val result = db.run(sqlCreateTable)
    result.map { _ => true }
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
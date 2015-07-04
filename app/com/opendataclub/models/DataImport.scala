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

/**
 * @author juanignaciosl
 */
case class DataImport(externalDataId: Long, createdAt: DateTime, content: JsValue, id: Option[Long]) {
  def this(externalDataSource: ExternalDataSource, content: JsValue)= this(externalDataSource.id, new DateTime, content, None)
}

class DataImports(tag: Tag) extends Table[DataImport](tag, "data_imports") {
  val externalDataSources = slick.lifted.TableQuery[ExternalDataSources]
  
  def externalDataId = column[Long]("external_data_id")
  def externalData = foreignKey("data_imports_external_data_fk", externalDataId, externalDataSources)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def createdAt = column[DateTime]("created_at")
  def content = column[JsValue]("content")
  // TODO: type-safe ids
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  
  def * = (externalDataId, createdAt, content, id.?) <> (DataImport.tupled, DataImport.unapply)
}
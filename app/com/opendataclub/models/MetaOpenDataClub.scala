package com.opendataclub.models

import slick.lifted._
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import com.github.tototoshi.slick.PostgresJodaSupport._
import slick.backend.DatabaseConfig
import scala.concurrent.Future
import slick.driver.JdbcProfile

case class MetaOpenDataClub(val lastDataUpdate: DateTime, createdAt: DateTime, id: Option[Long] = None)

class MetaOpenDataClubs(tag: Tag) extends Table[MetaOpenDataClub](tag, "meta_open_data_club") {
  def lastDataUpdate = column[DateTime]("last_data_update")
  def createdAt = column[DateTime]("created_at")
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  
  def * = (lastDataUpdate, createdAt, id.?) <> (MetaOpenDataClub.tupled, MetaOpenDataClub.unapply)
}

class MetaOpenDataClubRepository(dbConfig: DatabaseConfig[JdbcProfile]) {
  
  val db = dbConfig.db
  val metaOpenDataClubs = slick.lifted.TableQuery[MetaOpenDataClubs]
  
  def lastMeta: Future[MetaOpenDataClub] = {
    db.run(metaOpenDataClubs.sortBy(m => m.createdAt.desc).take(1).result.head)
  }
}
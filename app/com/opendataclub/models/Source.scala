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

/**
 * @author juanignaciosl
 */
case class Source(name: String, url: String, createdAt: DateTime, updatedAt: DateTime, id: Option[Long]) {
  

  
}

class Sources(tag: Tag) extends Table[Source](tag, "sources") {
  def name = column[String]("name")
  def url = column[String]("url")
  def createdAt = column[DateTime]("created_at")
  def updatedAt = column[DateTime]("updated_at")
  // TODO: type-safe ids
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  
  def * = (name, url, createdAt, updatedAt, id.?) <> (Source.tupled, Source.unapply)
}
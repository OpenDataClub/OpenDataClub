package com.opendataclub.controllers

import play.api._
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import com.google.inject.Inject
import com.opendataclub.models._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author juanignaciosl
 */
class HomeController @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  
  def index  = Action.async { implicit request =>
    new MetaOpenDataClubRepository(dbConfig).lastMeta.map(meta => Ok(views.html.index(meta)))
  }
  
}
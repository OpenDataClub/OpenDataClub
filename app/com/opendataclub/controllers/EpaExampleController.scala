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
class EpaExampleController @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  
  def index  = Action.async { implicit request =>
    new ExternalDataSourceRepository(dbConfig).extract(-1).map(di => Ok(views.html.dataImport(di)))
  }
}
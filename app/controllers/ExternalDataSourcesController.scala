package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import com.google.inject.Inject
import com.opendataclub.models._
import scala.concurrent.ExecutionContext.Implicits.global

class ExternalDataSourcesController @Inject() (dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def show(id: ExternalDataSourceId) = Action.async { implicit request =>
    new ExternalDataSourceRepository(dbConfig).get(id)
      .map(externalDataSource => Ok(views.html.externalDataSource(externalDataSource)))
  }
}
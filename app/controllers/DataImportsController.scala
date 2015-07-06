package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import com.google.inject.Inject
import com.opendataclub.models._
import scala.concurrent.ExecutionContext.Implicits.global
import com.opendataclub.models.ExternalDataSourceService

class DataImportsController @Inject() (dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def show(id: com.opendataclub.models.ExternalDataSourceId, dataImportId: com.opendataclub.models.DataImportId) = Action.async { implicit request =>
    val externalDataSourceAndDataImport = for {
      externalDataSource <- new ExternalDataSourceRepository(dbConfig).get(id)
      dataImport <- new DataImportRepository(dbConfig).get(dataImportId)
    } yield (externalDataSource, dataImport)

    externalDataSourceAndDataImport.map { x => Ok(views.html.dataImport(x._1, Some(x._2), None)) }
  }

  def create(id: ExternalDataSourceId) = Action.async { implicit request =>
    new ExternalDataSourceService(new ExternalDataSourceRepository(dbConfig), new DataImportRepository(dbConfig)).extract(id, dbConfig)
      .map(extraction => Ok(views.html.dataImport(extraction._1, Some(extraction._2), extraction._3)))
  }
}
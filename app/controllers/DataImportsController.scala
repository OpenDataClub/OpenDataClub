package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import com.google.inject.Inject
import com.opendataclub.models._
import scala.concurrent.ExecutionContext.Implicits.global
import com.opendataclub.models.ExternalDataSourceService
import scala.concurrent.Future

class DataImportsController @Inject() (dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def show(id: com.opendataclub.models.ExternalDataSourceId, dataImportId: com.opendataclub.models.DataImportId) = Action.async { implicit request =>
    val externalDataSourceAndDataImport = for {
      externalDataSource <- new ExternalDataSourceRepository(dbConfig).get(id)
      dataImport <- new DataImportRepository(dbConfig).get(dataImportId)
    } yield (externalDataSource, dataImport)

    externalDataSourceAndDataImport.map { x => Ok(views.html.dataImport(x._1, x._2, None)) }
  }

  def create(id: ExternalDataSourceId) = Action.async { implicit request =>
    // TODO: two flatMap and then new Futures? Probable refactor
    new ExternalDataSourceService(new ExternalDataSourceRepository(dbConfig), new DataImportRepository(dbConfig), new DataTableRepository(dbConfig)).extract(id, dbConfig)
      .flatMap(extraction => extraction._2 match {
        case Some(x: Future[(DataImport, DataTable)]) => x.flatMap {
          y => Future { Ok(views.html.dataImport(extraction._1, Some(y._1), Some(y._2))) }
        }
        case None => Future {  Ok(views.html.dataImport(extraction._1, None, None)) }
      })
  }
}

// {
//        _.map { dataImportAndDataTable =>
//          Ok(views.html.dataImport(extraction._1, Some(dataImportAndDataTable._1), Some(dataImportAndDataTable._2)))
//        }
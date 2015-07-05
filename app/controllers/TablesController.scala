package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import com.google.inject.Inject
import com.opendataclub.models._
import scala.concurrent.ExecutionContext.Implicits.global

class TablesController @Inject() (dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  
  def show(id: com.opendataclub.models.ExternalDataSourceId, dataImportId: com.opendataclub.models.DataImportId) = Action.async { implicit request =>
    val tableData = for {
      externalDataSource <- new ExternalDataSourceRepository(dbConfig).get(id)
      dataImport <- new DataImportRepository(dbConfig).get(dataImportId)
      dataTable <- new DataTableService(new DataTableRepository(dbConfig)).dataTable(dataImport, externalDataSource)
      dataTableContent <- dataTable.content(dbConfig, dataImport)
    } yield(externalDataSource, dataImport, dataTable, dataTableContent)
    
    tableData.map { x => Ok(views.html.dataTable(x._1, x._2, Some(x._3))) }
  }

}
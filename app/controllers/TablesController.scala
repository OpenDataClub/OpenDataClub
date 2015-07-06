package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import com.google.inject.Inject
import com.opendataclub.models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TablesController @Inject() (dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  
  def show(id: com.opendataclub.models.ExternalDataSourceId, dataImportId: com.opendataclub.models.DataImportId) = Action.async { implicit request =>
    val tableData = for {
      externalDataSource <- new ExternalDataSourceRepository(dbConfig).get(id)
      dataImport <- new DataImportRepository(dbConfig).get(dataImportId)
      dataTable <- new DataTableRepository(dbConfig).get(dataImportId)
      headers <- dataTableHeaders(dataTable)
    } yield(externalDataSource, dataImport, dataTable)
    
    tableData.map { x => Ok(views.html.dataTable(x._1, x._2, x._3)) }
  }
  
  private def dataTableHeaders(dataTable: Option[DataTable]): Future[List[DataTableColumn]] = {
    dataTable match {
      case Some(dt: DataTable) => dt.headers(dbConfig)
      case None => Future { List[DataTableColumn]() }
    }
  }

}
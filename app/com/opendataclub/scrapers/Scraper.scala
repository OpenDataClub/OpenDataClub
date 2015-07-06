package com.opendataclub.scrapers

import com.opendataclub.models.DataImport
import com.opendataclub.models.ExternalDataSource
import scala.util.Try
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import com.opendataclub.models.DataTable
import scala.concurrent.Future
import com.opendataclub.models.DataImportRepository

trait Scraper {
  def run(externalDataSource: ExternalDataSource, dbConfig: DatabaseConfig[JdbcProfile], dataImportRepository: DataImportRepository): Future[(DataImport, Option[DataTable])]
}
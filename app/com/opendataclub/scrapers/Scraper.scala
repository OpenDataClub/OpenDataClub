package com.opendataclub.scrapers

import com.opendataclub.models.DataImport
import com.opendataclub.models.ExternalDataSource
import scala.util.Try

/**
 * @author juanignaciosl
 */
trait Scraper {
  def run(externalDataSource: ExternalDataSource): Try[DataImport] 
}
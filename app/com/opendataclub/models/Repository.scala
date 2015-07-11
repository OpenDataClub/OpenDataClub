package com.opendataclub.models

import scala.concurrent.Future

trait ReadRepository[E, ID] {
  
  // TODO: if a search by id fails, should return None or a failed future?
  def get(id: ID): Future[Option[E]]
  
}

trait WriteRepository[TE, SE] {
  
  def put(e: TE): Future[SE]
  
}

trait ReadWriteRepository[TE, SE, ID] extends ReadRepository[SE, ID] with WriteRepository[TE, SE]
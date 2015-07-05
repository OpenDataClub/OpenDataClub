package com.opendataclub.models

import scala.concurrent.Future

trait ReadRepository[E, ID] {
  
  def get(id: ID): Future[E]
  
}

trait WriteRepository[E] {
  
  def put(e: E): Future[Int]
  
}

trait ReadWriteRepository[E, ID] extends ReadRepository[E, ID] with WriteRepository[E]
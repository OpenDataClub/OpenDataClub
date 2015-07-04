package com.opendataclub.postgres

// see https://github.com/tminglei/slick-pg

import slick.driver.PostgresDriver
import com.github.tminglei.slickpg._
import play.api.libs.json.JsValue
import play.api.libs.json.Json

// INFO: Original extended ExPostgresDriver
trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgDateSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgPlayJsonSupport
                          with PgSearchSupport
                          //with PgPostGISSupport 
                          //with PgNetSupport 
                          //with PgLTreeSupport 
                          {
  override val pgjson = "jsonb" //to keep back compatibility, pgjson's value was "json" by default

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
                           with DateTimeImplicits
                           with JsonImplicits
                           //with NetImplicits
                           //with LTreeImplicits
                           with RangeImplicits
                           with HStoreImplicits
                           with SearchImplicits
                           with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse(_))(s).orNull,
        (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }
}

object MyPostgresDriver extends MyPostgresDriver
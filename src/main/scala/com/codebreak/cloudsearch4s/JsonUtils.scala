package com.codebreak.cloudsearch4s

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JsonUtils {

  def serialize(doc: AnyRef, converter: Module = new DateSupportModule()): String = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(converter)
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
    mapper.writeValueAsString(doc)
  }

  def deserialize[T](json: String, clazz: Class[T], converter: Module = new DateSupportModule()): T = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(converter)
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
    mapper.readValue(json, clazz)
  }

}
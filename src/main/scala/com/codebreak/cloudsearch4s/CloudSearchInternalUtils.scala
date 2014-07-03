package com.codebreak.cloudsearch4s

import java.net.URLEncoder
import com.fasterxml.jackson.databind.PropertyNamingStrategy

private[cloudsearch4s] object CloudSearchInternalUtils {
  private val nameConverter = new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy()
  def getPropertyNames(clazz: Class[_]): Seq[String] = clazz.getDeclaredFields.map(_.getName).filter(!_.startsWith("$")).map(nameConverter.translate)

  def q(value: String): String = "\"" + value + "\""
  def u(value: String): String = URLEncoder.encode(value, "UTF-8")

  implicit class RichStringBuilder(sb: StringBuilder){
    def encode(value: String): StringBuilder = sb.append(u(value))
  }
}

package jp.co.bizreach

import org.apache.lucene.search.{TermQuery, Query, BooleanClause, BooleanQuery}
import org.apache.lucene.util.ToStringUtils
import org.apache.lucene.index.Term
import java.net.URLEncoder

/**
 * Created by naoki.takezoe on 2014/07/24.
 */
package object cloudsearch4s {

  case class FacetParam(field: String, sort: String = "", buckets: Seq[String] = Nil, size: Int = 0)
  case class HighlightParam(field: String, format: String = "", maxPhrases: Int = 0, preTag: String = "", postTag: String = "")

  sealed trait SortParam { val field: String }
  case class Asc(field: String) extends SortParam
  case class Desc(field: String) extends SortParam

  case class CloudSearchResult[T](total: Int, hits: Seq[CloudSearchDocument[T]], facets: Map[String, Seq[Facet]])
  case class CloudSearchDocument[T](id: String, fields: T, highlight: Map[String, String])
  case class Facet(value: String, count: Int)

  case class CloudSearchSettings(searchUrl: String, registerUrl: String, proxy: Option[Proxy] = None)
  case class Proxy(host: String, port: Int)
  case class CloudSearchError(messages: Seq[String])

  /**
   * Extends [[org.apache.lucene.search.BooleanQuery]] to generate SHOULD query as OR.
   * If you want to execute OR query, use this class instead of BooleanQuery.
   */
  class ExtendedBooleanQuery extends BooleanQuery {
    override def toString(field: String): String = {
      val buffer: StringBuilder = new StringBuilder
      val needParens: Boolean = getBoost != 1.0 || getMinimumNumberShouldMatch > 0
      if (needParens) {
        buffer.append("(")
      }
      {
        var i: Int = 0
        while (i < clauses.size) {
          {
            val c: BooleanClause = clauses.get(i)
            if (c.isProhibited) {
              buffer.append("-")
            } else if (c.isRequired) {
              buffer.append("+")
            } else if(i > 0){
              buffer.append("OR ")
            }
            val subQuery: Query = c.getQuery
            if (subQuery != null) {
              if (subQuery.isInstanceOf[BooleanQuery]) {
                buffer.append("(")
                buffer.append(subQuery.toString(field))
                buffer.append(")")
              } else {
                buffer.append(subQuery.toString(field))
              }
            } else {
              buffer.append("null")
            }
            if (i != clauses.size - 1) {
              buffer.append(" ")
            }
          }
          ({
            i += 1; i - 1
          })
        }
      }
      if (needParens) {
        buffer.append(")")
      }
      if (getMinimumNumberShouldMatch > 0) {
        buffer.append('~')
        buffer.append(getMinimumNumberShouldMatch)
      }
      if (getBoost != 1.0f) {
        buffer.append(ToStringUtils.boost(getBoost))
      }
      return buffer.toString
    }
  }

  /**
   * Extends [[org.apache.lucene.search.TermQuery]] to URL encode the term value.
   */
  class URLEncodeTermQuery(term: Term) extends TermQuery(term) {
    override def toString(field: String): String = {
      val buffer: StringBuilder = new StringBuilder
      val term = getTerm()
      if (!(term.field == field)) {
        buffer.append(term.field)
        buffer.append(":")
      }
      buffer.append(URLEncoder.encode(term.text, "UTF-8"))
      buffer.append(ToStringUtils.boost(getBoost))
      buffer.toString
    }
  }

}

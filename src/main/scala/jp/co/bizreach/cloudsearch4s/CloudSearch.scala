package jp.co.bizreach.cloudsearch4s

import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.{HttpGet, HttpPost}
import java.nio.charset.StandardCharsets
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import java.net.URLEncoder
import org.apache.lucene.search.Query
import CloudSearch._

trait CloudSearch {

  def registerIndexByMap(fields: Map[String, Any]): String
  def registerIndicesByMap(fieldsList: Seq[Map[String, Any]]): Seq[String]
  def registerIndex(fields: AnyRef): String
  def registerIndices(fieldsList: Seq[AnyRef]): Seq[String]
  def updateIndexByMap(id: String, fields: Map[String, Any]): Unit
  def updateIndicesByMap(idAndFieldsList: Seq[(String, Map[String, Any])]): Unit
  def updateIndex(id: String, fields: AnyRef): Unit
  def updateIndices(idAndFieldsList: Seq[(String, AnyRef)]): Unit
  def removeIndex(id: String): Unit
  def removeIndices(idList: Seq[String]): Unit
  def search[T](clazz: Class[T], query: Query, fields: Seq[String] = Nil, facets: Seq[FacetParam] = Nil, highlights: Seq[HighlightParam] = Nil,
                sorts: Seq[SortParam] = Nil, start: Int = 0, size: Int = 0): CloudSearchResult[T]

}

class CloudSearchImpl(registerUrl: String, searchUrl: String) extends CloudSearch {
  import CloudSearchInternalUtils._

  def registerIndexByMap(fields: Map[String, Any]): String = {
    registerIndicesByMap(List(fields))(0)
  }

  def registerIndicesByMap(fieldsList: Seq[Map[String, Any]]): Seq[String] = {
    val mapList = fieldsList.map { case fields =>
      Map(
        "type"   -> "add",
        "id"     -> new com.eaio.uuid.UUID().toString,
        "fields" -> fields
      )
    }

    val json = JsonUtils.serialize(mapList)
    executePostRequest(registerUrl, json)

    mapList.map(x => x("id").asInstanceOf[String])
  }

  def registerIndex(fields: AnyRef): String = {
    registerIndices(Seq(fields))(0)
  }

  def registerIndices(fieldsList: Seq[AnyRef]): Seq[String] = {
    registerIndicesByMap(fieldsList.map { fields =>
      JsonUtils.deserialize(JsonUtils.serialize(fields), classOf[Map[String, Any]])
    })
  }

  def updateIndexByMap(id: String, fields: Map[String, Any]): Unit = {
    updateIndicesByMap(Seq((id, fields)))
  }

  def updateIndicesByMap(idAndFieldsList: Seq[(String, Map[String, Any])]): Unit = {
    val json = JsonUtils.serialize(
      idAndFieldsList.map { case (id, fields) =>
        Map(
          "type"   -> "add",
          "id"     -> id,
          "fields" -> fields
        )
      }
    )
    executePostRequest(registerUrl, json)
  }

  def updateIndex(id: String, fields: AnyRef): Unit = {
    updateIndices(Seq((id, fields)))
  }

  def updateIndices(idAndFieldsList: Seq[(String, AnyRef)]): Unit = {
    updateIndicesByMap(idAndFieldsList.map { case (id, fields) =>
      (id, JsonUtils.deserialize(JsonUtils.serialize(fields), classOf[Map[String, Any]]))
    })
  }

  def removeIndex(id: String): Unit = {
    removeIndices(Seq(id))
  }

  def removeIndices(idList: Seq[String]): Unit = {
    val json = JsonUtils.serialize(
      idList.map { id =>
        Map(
          "type" -> "delete",
          "id"   -> id
        )
      }
    )
    executePostRequest(registerUrl, json)
  }

  def search[T](clazz: Class[T], query: Query, fields: Seq[String] = Nil, facets: Seq[FacetParam] = Nil, highlights: Seq[HighlightParam] = Nil,
                sorts: Seq[SortParam] = Nil, start: Int = 0, size: Int = 0): CloudSearchResult[T] = {
    val sb = new StringBuilder()
    sb.append("q=").append(u(query.toString))
    sb.append("&q.parser=lucene")
    if(fields.nonEmpty){
      sb.append("&return=").encode(fields.mkString(","))
    } else if(!clazz.isAssignableFrom(classOf[Map[_, _]])){
      sb.append("&return=").encode(getPropertyNames(clazz).mkString(","))
    }
    // facet
    facets.foreach { facet =>
      sb.append("&facet.").append(facet.field).append("=")
      sb.encode("{")
      sb.encode(Seq(
        if(facet.sort.nonEmpty)    Some(s"sort:${q(facet.sort)}")                           else None,
        if(facet.size > 0)         Some(s"size:${facet.size}")                              else None,
        if(facet.buckets.nonEmpty) Some(s"buckets:[${facet.buckets.map(q).mkString(",")}]") else None
      ).flatten.mkString(","))
      sb.encode("}")
    }
    // highlight
    highlights.foreach { highlight =>
      sb.append("&highlight.").append(highlight.field).append("=")
      sb.encode("{")
      sb.encode(Seq(
        if(highlight.format.nonEmpty)  Some(s"format:${q(highlight.format)}")       else None,
        if(highlight.maxPhrases > 0)   Some(s"max_phrases:${highlight.maxPhrases}") else None,
        if(highlight.preTag.nonEmpty)  Some(s"pre_tag:${q(highlight.preTag)}")      else None,
        if(highlight.postTag.nonEmpty) Some(s"post_tag:${q(highlight.postTag)}")    else None
      ).flatten.mkString(","))
      sb.encode("}")
    }
    // sort
    if(sorts.nonEmpty){
      sb.append("&sort=")
      sb.encode(sorts.map {
        case Asc(field)  => field + " asc"
        case Desc(field) => field + " desc"
      }.mkString(","))
    }
    // start
    if(start > 0){
      sb.append("&start=").append(start)
    }
    // size
    if(size > 0){
      sb.append("&size=").append(size)
    }

    val response = JsonUtils.deserialize(executeGetRequest(searchUrl, sb.toString), classOf[Map[String, Any]])

    CloudSearchResult(
      total = response("hits").asInstanceOf[Map[String, Any]]("found").asInstanceOf[Int],
      hits = response("hits").asInstanceOf[Map[String, Any]]("hit").asInstanceOf[Seq[Map[String, AnyRef]]].map { doc =>
        CloudSearchDocument(
          id        = doc("id").asInstanceOf[String],
          fields    = JsonUtils.deserialize(JsonUtils.serialize(doc("fields")), clazz),
          highlight = doc.get("highlights").map(_.asInstanceOf[Map[String, String]]).getOrElse(Map.empty)
        )
      },
      facets = response.get("facets").map(_.asInstanceOf[Map[String, Map[String, AnyRef]]].map { case (field, map) =>
        field -> map("buckets").asInstanceOf[Seq[Map[String, Any]]].map { bucket =>
          Facet(bucket("value").asInstanceOf[String], bucket("count").asInstanceOf[Int])
        }
      }).getOrElse(Map.empty)
    )
  }

  protected def executePostRequest(url: String, json: String): String  = {
    val post = new HttpPost(url)
    val client = HttpClientBuilder.create().build()
    try {
      val entry = new StringEntity(json, StandardCharsets.UTF_8)
      entry.setContentType("application/json")
      post.setEntity(entry)

      val response = client.execute(post)
      EntityUtils.toString(response.getEntity())
    } finally {
      client.close()
    }
  }

  protected def searchInternal[T](url: String, queryString: String, clazz: Class[T]): CloudSearchResult[T] = {
    val response = JsonUtils.deserialize(executeGetRequest(url, queryString), classOf[Map[String, Any]])

    CloudSearchResult(
      total = response("hits").asInstanceOf[Map[String, Any]]("found").asInstanceOf[Int],
      hits = response("hits").asInstanceOf[Map[String, Any]]("hit").asInstanceOf[Seq[Map[String, AnyRef]]].map { doc =>
        CloudSearchDocument(
          id        = doc("id").asInstanceOf[String],
          fields    = JsonUtils.deserialize(JsonUtils.serialize(doc("fields")), clazz),
          highlight = doc.get("highlights").map(_.asInstanceOf[Map[String, String]]).getOrElse(Map.empty)
        )
      },
      facets = response.get("facets").map(_.asInstanceOf[Map[String, Map[String, AnyRef]]].map { case (field, map) =>
        field -> map("buckets").asInstanceOf[Seq[Map[String, Any]]].map { bucket =>
          Facet(bucket("value").asInstanceOf[String], bucket("count").asInstanceOf[Int])
        }
      }).getOrElse(Map.empty)
    )
  }

  protected def executeGetRequest(url: String, queryString: String): String = {
    val get = new HttpGet(url + "?" + queryString)
    val client = HttpClientBuilder.create().build()
    try {
      val response = client.execute(get)
      EntityUtils.toString(response.getEntity())
    } finally {
      client.close()
    }
  }

}

object CloudSearch {

  def apply(registerUrl: String, searchUrl: String): CloudSearch = new CloudSearchImpl(registerUrl, searchUrl)

  case class FacetParam(field: String, sort: String = "", buckets: Seq[String] = Nil, size: Int = 0)
  case class HighlightParam(field: String, format: String = "", maxPhrases: Int = 0, preTag: String = "", postTag: String = "")

  sealed trait SortParam { val field: String }
  case class Asc(field: String) extends SortParam
  case class Desc(field: String) extends SortParam

  case class CloudSearchResult[T](total: Int, hits: Seq[CloudSearchDocument[T]], facets: Map[String, Seq[Facet]])
  case class CloudSearchDocument[T](id: String, fields: T, highlight: Map[String, String])
  case class Facet(value: String, count: Int)

}
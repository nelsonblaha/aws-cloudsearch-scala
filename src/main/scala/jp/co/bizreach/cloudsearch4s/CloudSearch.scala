package jp.co.bizreach.cloudsearch4s

import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpPost
import java.nio.charset.StandardCharsets
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import java.net.URLDecoder
import org.apache.lucene.search.Query
import CloudSearch._
import org.apache.http.client.config.RequestConfig
import org.apache.http.HttpHost
import org.slf4j.LoggerFactory

trait CloudSearch {

  def registerIndexByMap(fields: Map[String, Any]): Either[CloudSearchError, String]
  def registerIndicesByMap(fieldsList: Seq[Map[String, Any]]): Either[CloudSearchError, Seq[String]]
  def registerIndex(fields: AnyRef): Either[CloudSearchError, String]
  def registerIndices(fieldsList: Seq[AnyRef]): Either[CloudSearchError, Seq[String]]
  def updateIndexByMap(id: String, fields: Map[String, Any]): Either[CloudSearchError, String]
  def updateIndicesByMap(idAndFieldsList: Seq[(String, Map[String, Any])]): Either[CloudSearchError, Seq[String]]
  def updateIndex(id: String, fields: AnyRef): Either[CloudSearchError, String]
  def updateIndices(idAndFieldsList: Seq[(String, AnyRef)]): Either[CloudSearchError, Seq[String]]
  def removeIndex(id: String): Either[CloudSearchError, String]
  def removeIndices(idList: Seq[String]): Either[CloudSearchError, Seq[String]]
  def search[T](clazz: Class[T], query: Query, fields: Seq[String] = Nil, facets: Seq[FacetParam] = Nil, highlights: Seq[HighlightParam] = Nil,
                sorts: Seq[SortParam] = Nil, start: Int = 0, size: Int = 0): Either[CloudSearchError, CloudSearchResult[T]]

}

class CloudSearchImpl(settings: CloudSearchSettings) extends CloudSearch {
  import CloudSearchInternalUtils._

  protected val logger = LoggerFactory.getLogger("com.codebreak.cloudsearch4s.monitor")

  def registerIndexByMap(fields: Map[String, Any]): Either[CloudSearchError, String] = {
    registerIndicesByMap(List(fields)) match {
      case Right(x) => Right(x(0))
      case Left(x)  => Left(x)
    }
  }

  def registerIndicesByMap(fieldsList: Seq[Map[String, Any]]): Either[CloudSearchError, Seq[String]] = {
    val mapList = fieldsList.map { case fields =>
      Map(
        "type"   -> "add",
        "id"     -> new com.eaio.uuid.UUID().toString,
        "fields" -> fields
      )
    }

    val json = JsonUtils.serialize(mapList)
    executeUpdateRequest(json) match {
      case None    => Right(mapList.map(x => x("id").asInstanceOf[String]))
      case Some(x) => Left(x)
    }
  }

  def registerIndex(fields: AnyRef): Either[CloudSearchError, String] = {
    registerIndices(Seq(fields)) match {
      case Right(x) => Right(x(0))
      case Left(x)  => Left(x)
    }
  }

  def registerIndices(fieldsList: Seq[AnyRef]): Either[CloudSearchError, Seq[String]] = {
    registerIndicesByMap(fieldsList.map { fields =>
      JsonUtils.deserialize(JsonUtils.serialize(fields), classOf[Map[String, Any]])
    })
  }

  def updateIndexByMap(id: String, fields: Map[String, Any]): Either[CloudSearchError, String] = {
    updateIndicesByMap(Seq((id, fields))) match {
      case Right(x) => Right(x(0))
      case Left(x)  => Left(x)
    }
  }

  def updateIndicesByMap(idAndFieldsList: Seq[(String, Map[String, Any])]): Either[CloudSearchError, Seq[String]] = {
    val json = JsonUtils.serialize(
      idAndFieldsList.map { case (id, fields) =>
        Map(
          "type"   -> "add",
          "id"     -> id,
          "fields" -> fields
        )
      }
    )
    executeUpdateRequest(json) match {
      case None    => Right(idAndFieldsList.map(_._1))
      case Some(x) => Left(x)
    }
  }

  def updateIndex(id: String, fields: AnyRef): Either[CloudSearchError, String] = {
    updateIndices(Seq((id, fields))) match {
      case Right(x) => Right(x(0))
      case Left(x)  => Left(x)
    }
  }

  def updateIndices(idAndFieldsList: Seq[(String, AnyRef)]): Either[CloudSearchError, Seq[String]] = {
    updateIndicesByMap(idAndFieldsList.map { case (id, fields) =>
      (id, JsonUtils.deserialize(JsonUtils.serialize(fields), classOf[Map[String, Any]]))
    })
  }

  def removeIndex(id: String): Either[CloudSearchError, String] = {
    removeIndices(Seq(id)) match {
      case Right(x) => Right(x(0))
      case Left(x)  => Left(x)
    }
  }

  def removeIndices(idList: Seq[String]): Either[CloudSearchError, Seq[String]] = {
    val json = JsonUtils.serialize(
      idList.map { id =>
        Map(
          "type" -> "delete",
          "id"   -> id
        )
      }
    )
    executeUpdateRequest(json) match {
      case None    => Right(idList)
      case Some(x) => Left(x)
    }
  }

  def search[T](clazz: Class[T], query: Query, fields: Seq[String] = Nil, facets: Seq[FacetParam] = Nil, highlights: Seq[HighlightParam] = Nil,
                sorts: Seq[SortParam] = Nil, start: Int = 0, size: Int = 0): Either[CloudSearchError, CloudSearchResult[T]] = {
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
    if(size >= 0){
      sb.append("&size=").append(size)
    }

    executeSearchRequest(sb.toString, clazz)
  }

  protected def executeUpdateRequest(json: String): Option[CloudSearchError]  = {
    val request = new HttpPost(settings.registerUrl)
    val client  = HttpClientBuilder.create().build()

    settings.proxy.foreach { x =>
      val config = RequestConfig.custom().setProxy(new HttpHost(x.host, x.port)).build()
      request.setConfig(config)
    }

    try {
      val entry = new StringEntity(json, StandardCharsets.UTF_8)
      entry.setContentType("application/json")
      request.setEntity(entry)

      val start    = System.currentTimeMillis
      val response = client.execute(request)
      val end      = System.currentTimeMillis

      val resultJson = EntityUtils.toString(response.getEntity())
      val resultMap = JsonUtils.deserialize(resultJson, classOf[Map[String, AnyRef]])

      val status  = resultMap("status").asInstanceOf[String]
      val adds    = resultMap("adds").asInstanceOf[Int]
      val deletes = resultMap("deletes").asInstanceOf[Int]
      logger.info(s"action:cloudsearch.update\tstatus:${status}\ttime:${end - start}msec\tadds:${adds}\tdeletes:${deletes}")

      status match {
        case "success" => None
        case _ => Some(CloudSearchError(
          messages = resultMap("errors").asInstanceOf[List[Map[String, String]]].map(error => error("message"))
        ))
      }
    } finally {
      request.releaseConnection()
    }
  }

  protected def executeSearchRequest[T](queryString: String, clazz: Class[T]): Either[CloudSearchError, CloudSearchResult[T]] = {
    val request = new HttpPost(settings.searchUrl)
    val client  = HttpClientBuilder.create().build()

    settings.proxy.foreach { x =>
      val config = RequestConfig.custom().setProxy(new HttpHost(x.host, x.port)).build()
      request.setConfig(config)
    }

    val entry = new StringEntity(queryString, StandardCharsets.UTF_8)
    entry.setContentType("application/x-www-form-urlencoded")
    request.setEntity(entry)

    try {
      val start    = System.currentTimeMillis
      val response = client.execute(request)
      val end      = System.currentTimeMillis

      val json = EntityUtils.toString(response.getEntity())
      val responseMap = JsonUtils.deserialize(json, classOf[Map[String, Any]])

      responseMap.get("error") match {
        case Some(_) => {
          Left(CloudSearchError(messages = Seq(responseMap("message").asInstanceOf[String])))
        }
        case None => {
          val result = CloudSearchResult(
            total = responseMap("hits").asInstanceOf[Map[String, Any]]("found").asInstanceOf[Int],
            hits = responseMap("hits").asInstanceOf[Map[String, Any]]("hit").asInstanceOf[Seq[Map[String, AnyRef]]].map { doc =>
              CloudSearchDocument(
                id        = doc("id").asInstanceOf[String],
                fields    = JsonUtils.deserialize(JsonUtils.serialize(doc("fields")), clazz),
                highlight = doc.get("highlights").map(_.asInstanceOf[Map[String, String]]).getOrElse(Map.empty)
              )
            },
            facets = responseMap.get("facets").map(_.asInstanceOf[Map[String, Map[String, AnyRef]]].map { case (field, map) =>
              field -> map("buckets").asInstanceOf[Seq[Map[String, Any]]].map { bucket =>
                Facet(bucket("value").asInstanceOf[String], bucket("count").asInstanceOf[Int])
              }
            }).getOrElse(Map.empty)
          )

          logger.info(s"action:cloudsearch.search\ttime:${end - start}msec\ttotal:${result.total}\thits:${result.hits.size}\tquery:${URLDecoder.decode(queryString, "UTF-8")}")

          Right(result)
        }
      }
    } finally {
      request.releaseConnection()
    }
  }

}

object CloudSearch {

  def apply(settings: CloudSearchSettings): CloudSearch = new CloudSearchImpl(settings)

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

}
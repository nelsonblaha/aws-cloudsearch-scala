package jp.co.bizreach.cloudsearch4s

import CloudSearch._
import jp.co.bizreach.cloudsearch4s

/**
 * Mock for [[cloudsearch4s.CloudSearch]].
 *
 * @param params Excepted query and its result
 */
class CloudSearchMock(params: Map[String, CloudSearchResult[_]] = Map.empty) extends CloudSearchImpl(CloudSearchSettings("", "")) {

  private val actions = scala.collection.mutable.ListBuffer[(String, String)]()

  override protected def executeUpdateRequest(json: String): Option[CloudSearchError]  = {
    actions += ("update" -> json)
    None
  }

  override protected def executeSearchRequest[T](queryString: String, clazz: Class[T]): Either[CloudSearchError, CloudSearchResult[T]] = {
    actions += ("search" -> queryString)
    Right(params(queryString).asInstanceOf[CloudSearchResult[T]])
  }

  /**
   * History of executed request.
   *
   * @return List of tuple which contains HTTP method (GET or POST) and parameter (query string or JSON string)
   */
  def executedRequests: List[(String, String)] = actions.toList

}

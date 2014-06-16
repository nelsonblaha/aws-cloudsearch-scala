cloudsearch4s
========
cloudsearch4s is a Scala client of AWS CloudSearch.

## How to use


## Case class

cloudsearch4s can handle documents as `Map[String, Any]` or case class. If you want to handle documents as case class, you have to define a case class which is mapped to the index in the CloudSearch at first.

```scala
case class Job(
  jobTitle: String,
  jobContent: String,
  salary: Int
)
```

If property count is over 22, you have to define as a normal class not a case class. This limitation will be removed in Scala 2.11.

```scala
class Job(
  val jobTitle: String,
  val jobContent: String,
  val salary: Int,
  ...
)
```

## API Usage

```scala
import jp.co.bizreach.cloudsearch4s.CloudSearch

val registerUrl = "http://xxxx"
val searchUrl   = "http://xxxx"

val cloudsearch = CloudSearch(registerUrl, searchUrl)
```

### Register

```scala
// Register single document by Map
val id: String = cloudsearch.registerIndexByMap(
  Map("job_title" -> "Title", "job_content" -> "Content")
)

// Register multiple documents by Map
val ids: Seq[String] = cloudsearch.registerIndicesByMap(Seq(
  Map("job_title" -> "Title", "job_content" -> "Content"),
  Map("job_title" -> "Title", "job_content" -> "Content")
))

// Register single document by case class
val id: String = cloudsearch.registerIndexByMap(
  Job("Title", "Content")
)

// Register multiple documents by case class
val ids: Seq[String] = cloudsearch.registerIndicesByMap(Seq(
  Job(Title, Content),
  Job(Title, Content)
))
```

### Update

```scala
// Update single document by Map
cloudsearch.updateIndexByMap(
  "091f2b7e-5b3b-4936-ae42-c560655a165f",
  Map("job_title" -> "Title", "job_content" -> "Content")
)

// Update multiple documents by case class
cloudsearch.updateIndices(
  Seq(
    ("091f2b7e-5b3b-4936-ae42-c560655a165f", Job("Title", "Content")),
    ("00dd1a55-0e6d-437a-9e16-8f8b50f45a20", Job("Title", "Content"))
  )
)
```

### Delete

```scala
// Delete single document
cloudsearch.removeIndex("091f2b7e-5b3b-4936-ae42-c560655a165f")

// Delete multiple documents
cloudsearch.removeIndices(Seq(
  "091f2b7e-5b3b-4936-ae42-c560655a165f",
  "00dd1a55-0e6d-437a-9e16-8f8b50f45a20"
))
```

### Search

```scala
val result: CloudSearchResult[Job] = cloudsearch.search(classOf[Job],
  // Query which is assembled using Lucene's query builder API
  new TermQuery(new Term("index_type", "crawl"))
  // Retrieve fields (Optional)
  fields = Seq("job_title", "job_content", "company_name"),
  // Highlight settings (Optional)
  highlights = Seq(HighlightParam("job_title")),
  // Facet search settings (Optional)
  facets = Seq(FacetParam("employment_status"))
)
```

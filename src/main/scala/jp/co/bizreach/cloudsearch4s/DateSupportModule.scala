package jp.co.bizreach.cloudsearch4s

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, Version}
import java.util.{Locale, Date}
import java.sql.Timestamp
import org.joda.time.{LocalDateTime, LocalDate}
import org.joda.time.format.DateTimeFormat
import java.text.SimpleDateFormat

class DateSupportModule extends SimpleModule("MyModule", Version.unknownVersion()){

  addSerializer(classOf[LocalDate], new JsonSerializer[LocalDate] {
    override def serialize(value: LocalDate, generator: JsonGenerator, provider: SerializerProvider): Unit = {
      generator.writeString(value.toString("yyyy-MM-dd'T'00:00:00'Z'", Locale.ENGLISH))
    }
  })

  addSerializer(classOf[LocalDateTime], new JsonSerializer[LocalDateTime] {
    override def serialize(value: LocalDateTime, generator: JsonGenerator, provider: SerializerProvider): Unit = {
      generator.writeString(value.toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH))
    }
  })

  addSerializer(classOf[Timestamp], new JsonSerializer[Timestamp] {
    override def serialize(value: Timestamp, generator: JsonGenerator, provider: SerializerProvider): Unit = {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).format(value)
    }
  })

  addSerializer(classOf[Date], new JsonSerializer[Date] {
    override def serialize(value: Date, generator: JsonGenerator, provider: SerializerProvider): Unit = {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).format(value)
    }
  })

  addDeserializer(classOf[LocalDate], new JsonDeserializer[LocalDate](){
    override def deserialize(parser: JsonParser, context: DeserializationContext): LocalDate = {
      try {
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parseLocalDateTime(parser.getValueAsString).toLocalDate
      } catch {
        case e: IllegalArgumentException =>
          DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").parseLocalDateTime(parser.getValueAsString).toLocalDate
      }
    }
  })

  addDeserializer(classOf[LocalDateTime], new JsonDeserializer[LocalDateTime](){
    override def deserialize(parser: JsonParser, context: DeserializationContext): LocalDateTime = {
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parseLocalDateTime(parser.getValueAsString)
    }
  })

  addDeserializer(classOf[Timestamp], new JsonDeserializer[Timestamp](){
    override def deserialize(parser: JsonParser, context: DeserializationContext): Timestamp = {
      new Timestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).parse(parser.getValueAsString).getTime)
    }
  })

  addDeserializer(classOf[Date], new JsonDeserializer[Date](){
    override def deserialize(parser: JsonParser, context: DeserializationContext): Date = {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).parse(parser.getValueAsString)
    }
  })

}

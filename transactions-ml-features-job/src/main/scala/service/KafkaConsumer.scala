package service

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import model.TransactionObjects.Transaction
import org.apache.avro.generic.GenericRecord
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala._

import scala.collection.JavaConverters._

object KafkaConsumer {

  private class TransactionDeserializer(topic: String, schemaRegistryUrl: String) extends DeserializationSchema[Transaction] {
    @transient private lazy val valueDeserializer = {
      val deserializer: KafkaAvroDeserializer = new KafkaAvroDeserializer()
      val props = Map(
        "schema.registry.url" -> schemaRegistryUrl
      ).asJava
      deserializer.configure(props, false)
      deserializer
    }

    override def deserialize(message: Array[Byte]): Transaction = {
      val record = valueDeserializer.deserialize(topic, message).asInstanceOf[GenericRecord]

      val userId = record.get("user_id").asInstanceOf[Int]
      val timestamp = record.get("transaction_timestamp_millis").asInstanceOf[Long]
      val amount = record.get("amount").asInstanceOf[Float]
      val currency = record.get("currency").toString
      val counterpartId = record.get("counterpart_id").asInstanceOf[Int]
      val ingestionDate = record.get("ingestion_date").toString

      Transaction(userId, timestamp, amount, currency, counterpartId, ingestionDate)
    }

    override def isEndOfStream(nextElement: Transaction): Boolean = false

    override def getProducedType: TypeInformation[Transaction] = TypeExtractor.getForClass(classOf[Transaction])
  }

  def readTransactionData(env: StreamExecutionEnvironment): DataStream[Transaction] = {
    val kafkaSource: KafkaSource[Transaction] = KafkaSource.builder[Transaction]()
      .setBootstrapServers("broker:29092")
      .setTopics("transactions")
      .setGroupId("transactions-ml-features-job-group")
      .setStartingOffsets(OffsetsInitializer.latest())
      .setValueOnlyDeserializer(new TransactionDeserializer("transactions", "http://schema-registry:8081"))
      .build()

    val kafkaTransactions: DataStream[Transaction] = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "transactions-ml-features-job")

    //    kafkaTransactions.print()
    kafkaTransactions
  }

}

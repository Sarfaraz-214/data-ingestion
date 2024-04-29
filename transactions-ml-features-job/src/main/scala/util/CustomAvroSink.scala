package util

import model.TransactionObjects.TransactionPerUser
import org.apache.flink.core.fs.Path
import org.apache.flink.formats.avro.AvroWriters
import org.apache.flink.streaming.api.functions.sink.filesystem.{OutputFileConfig, StreamingFileSink}
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy
import org.apache.flink.streaming.api.scala.DataStream

import java.time.ZoneId

object CustomAvroSink {

  def save(numTransactionsPerUser: DataStream[TransactionPerUser], outputPath: String): Unit = {
    val sink: StreamingFileSink[TransactionPerUser] = StreamingFileSink
      .forBulkFormat(new Path(outputPath), AvroWriters.forReflectRecord(classOf[TransactionPerUser]))
      .withBucketAssigner(new DateTimeBucketAssigner("yyyy-MM-dd", ZoneId.of("UTC")))
      .withRollingPolicy(OnCheckpointRollingPolicy.build())
      .withOutputFileConfig(
        OutputFileConfig
          .builder()
          .withPartPrefix("transactions")
          .withPartSuffix(".avro")
          .build()
      )
      .build()

    numTransactionsPerUser.addSink(sink)
  }

}

package job

import model.TransactionObjects._
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.scala._
import service.CustomTransformer._
import service.KafkaConsumer
import util.{CustomAvroSink, CustomScyllaSink}


object FlinkJobRunner {

  private val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

  def main(args: Array[String]): Unit = {
    val checkpointConfig: CheckpointConfig = env.getCheckpointConfig
    checkpointConfig.setCheckpointInterval(60000)

    val transactionEvents: DataStream[Transaction] = KafkaConsumer.readTransactionData(env)

    val transactionsPerUser: KeyedStream[Transaction, Int] = transactionEvents.keyBy(_.userId)

    val numTransactionsPerUser: DataStream[TransactionPerUser] = calcTransactionsPerUser(transactionsPerUser)

    val partitionedTransactionsPerUser: DataStream[TransactionPerUser] = doPartitioningByUserId(numTransactionsPerUser)
    partitionedTransactionsPerUser.print()

    CustomAvroSink.save(partitionedTransactionsPerUser, "file:///opt/flink/output")

    CustomScyllaSink.save(partitionedTransactionsPerUser)

    env.execute()
  }

}


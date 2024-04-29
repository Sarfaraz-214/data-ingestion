package util

import model.TransactionObjects.TransactionPerUser
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.cassandra.CassandraSink

object CustomScyllaSink {

  def save(numTransactionsPerUser: DataStream[TransactionPerUser]): Unit = {
    val transactionTuples: DataStream[(Int, Int)] = numTransactionsPerUser.map(t => (t.userId, t.totalTransactionsCount))

    //As Scylla DB is fully compatible with Apache Cassandra.
    CassandraSink.addSink(transactionTuples)
      .setQuery("insert into remotebank.transactions_per_user(user_id, total_transactions_count) values (?, ?)")
      .setHost("scylla-db", 9042)
      .build()
  }

}

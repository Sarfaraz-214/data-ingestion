package service

import model.TransactionObjects.{Transaction, TransactionPerUser}
import org.apache.flink.api.common.functions.Partitioner
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object CustomTransformer {

  def calcTransactionsPerUser(transactionsPerUser: KeyedStream[Transaction, Int]): DataStream[TransactionPerUser] = {
    val numTransactionsPerUser: DataStream[TransactionPerUser] = transactionsPerUser.process(

      new KeyedProcessFunction[Int, Transaction, TransactionPerUser] { // Instantiated once per Key/User
        //                     ^ key  ^ event     ^ result

        var stateCounter: ValueState[Int] = _ //A value state per Key, Key in this case is userId

        override def open(parameters: Configuration): Unit = {
          // Initialize all states
          stateCounter = getRuntimeContext
            .getState(new ValueStateDescriptor[Int]("transactions-state", classOf[Int]))
        }

        override def processElement(
                                     event: Transaction,
                                     ctx: KeyedProcessFunction[Int, Transaction, TransactionPerUser]#Context,
                                     out: Collector[TransactionPerUser]): Unit = {
          val numTransactionsForThisTask: Int = stateCounter.value()
          stateCounter.update(numTransactionsForThisTask + 1)
          out.collect(TransactionPerUser(event.userId, stateCounter.value()))
        }
      }
    )

//    numTransactionsPerUser.print()
    numTransactionsPerUser
  }

  def doPartitioningByUserId(numTransactionsPerUser: DataStream[TransactionPerUser]): DataStream[TransactionPerUser] = {
    val partitioner: Partitioner[Int] = new Partitioner[Int] {
      override def partition(key: Int, numPartitions: Int): Int = {
        key.hashCode % numPartitions
      }
    }

    val partitionedStream: DataStream[TransactionPerUser] = numTransactionsPerUser.partitionCustom(
      partitioner,
      event => event.userId
    )

    partitionedStream
  }

}

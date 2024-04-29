package model

object TransactionObjects {

  case class Transaction(userId: Int, transactionTimestampMillis: Long, amount: Float, currency: String, counterpartId: Int, ingestionDate: String)

  case class TransactionPerUser(userId: Int, totalTransactionsCount: Int)

}

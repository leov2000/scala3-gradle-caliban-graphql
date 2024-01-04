package account.graphql

import account.schema.AccountEvent.*
import account.schema.{Account, AccountEvent}
import zio.stream.ZStream
import zio.{Hub, Ref, UIO, URIO, ZIO, ZLayer}

import java.time.ZonedDateTime

object ZAccountService:

  trait AccountService:

    def addAccount(name: String, balance: Float): UIO[Boolean]

    def debitAccount(account: Int, debitAmount: Float): UIO[Boolean]

    def creditAccount(account: Int, creditAmount: Float): UIO[Boolean]

    def getAccount(account: Int): UIO[List[Account]]

    def findAccountHolder(name: String): UIO[List[Account]]

    def deleteAccount(account: Int): UIO[Boolean]

    def accountEvent: ZStream[Any, Nothing, AccountEvent]
  end AccountService

  def addAccount(name: String, balance: Float): URIO[AccountService, Boolean] =
    ZIO.serviceWithZIO(_.addAccount(name, balance))

  def debitAccount(
      account: Int,
      debitAmount: Float
  ): URIO[AccountService, Boolean] =
    ZIO.serviceWithZIO(_.debitAccount(account, debitAmount))

  def creditAccount(
      account: Int,
      creditAmount: Float
  ): URIO[AccountService, Boolean] =
    ZIO.serviceWithZIO(_.creditAccount(account, creditAmount))

  def getAccount(account: Int): URIO[AccountService, List[Account]] =
    ZIO.serviceWithZIO(_.getAccount(account))

  def findAccountHolder(name: String): URIO[AccountService, List[Account]] =
    ZIO.serviceWithZIO(_.findAccountHolder(name))

  def deleteAccount(account: Int): URIO[AccountService, Boolean] =
    ZIO.serviceWithZIO(_.deleteAccount(account))

  def deletedEvents: ZStream[AccountService, Nothing, AccountEvent] =
    ZStream.serviceWithStream(_.accountEvent)

  def make(account: Map[Int, Account]): ZLayer[Any, Nothing, AccountService] =
    ZLayer {
      for
        accountState <- Ref.make(account)
        accountCounter <- Ref.make(0)
        subscribers <- Hub.unbounded[AccountEvent]
      yield new AccountService:

        override def addAccount(name: String, balance: Float): UIO[Boolean] =
          for
            account <- accountCounter.getAndUpdate(_ + 1)
            _ <- accountState
              .getAndUpdate { state =>
                state + (account -> Account(
                  name,
                  account,
                  balance,
                  ZonedDateTime.now()
                ))
              }
          yield true

        override def debitAccount(
            account: Int,
            debitAmount: Float
        ): UIO[Boolean] =
          accountState
            .modify(state =>
              if state(account).accountNumber == account then
                (
                  true,
                  state + (account -> state(account)
                    .copy(balance = state(account).balance + debitAmount))
                )
              else (false, state)
            )
            .tap(debited => ZIO.when(debited)(subscribers.publish(DEBIT)))

        override def creditAccount(
            account: Int,
            creditAmount: Float
        ): UIO[Boolean] =
          accountState
            .modify(state =>
              if state(account).accountNumber == account then
                (
                  true,
                  state + (account -> state(account)
                    .copy(balance = state(account).balance - creditAmount))
                )
              else (false, state)
            )
            .tap(credited => ZIO.when(credited)(subscribers.publish(CREDIT)))

        override def getAccount(accountNumber: Int): UIO[List[Account]] =
          accountState.get
            .map(
              _.values
                .collect {
                  case r @ Account(_, a, _, _) if a == accountNumber => r
                }
                .toList
            )

        override def findAccountHolder(name: String): UIO[List[Account]] =
          accountState.get
            .map(
              _.values
                .collect {
                  case r @ Account(n, _, _, _) if n == name => r
                }
                .toList
            )

        override def deleteAccount(account: Int): UIO[Boolean] =
          accountState
            .modify(state =>
              if state(account).accountNumber == account then
                (true, state.removed(account))
              else (false, state)
            )

        override def accountEvent: ZStream[Any, Nothing, AccountEvent] =
          ZStream.scoped(subscribers.subscribe).flatMap(ZStream.fromQueue(_))
    }
end ZAccountService

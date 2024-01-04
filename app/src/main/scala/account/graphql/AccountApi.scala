package account.graphql

import caliban.*
import caliban.schema.Annotations.GQLDeprecated
import caliban.schema.Schema
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Schema.auto.*
import caliban.{GraphQL, RootResolver}
import account.graphql.ZAccountService.AccountService
import account.schema.{Account, AccountEvent}
import com.typesafe.scalalogging.LazyLogging
import account.graphql.AccountEnvironment.AccountEnv
import zio.*
import zio.URIO
import zio.stream.ZStream

object AccountApi extends LazyLogging:

  case class AddAccountArgs(name: String, balance: Float)

  case class FindByAccountArgs(account: Int)

  case class FindByUserArgs(name: String)

  case class AccountBalanceUpdateArgs(account: Int, amount: Float)

  case class DeleteAccountArgs(account: Int)

  case class Queries(
      account: FindByAccountArgs => URIO[AccountService, List[Account]],
      @GQLDeprecated("This field will be deprecated Q4/2023")
      accountHolder: FindByUserArgs => URIO[AccountService, List[Account]]
  )

  case class Mutations(
      addAccount: AddAccountArgs => URIO[AccountService, Boolean],
      creditAccount: AccountBalanceUpdateArgs => URIO[AccountService, Boolean],
      debitAccount: AccountBalanceUpdateArgs => URIO[AccountService, Boolean],
      deleteAccount: DeleteAccountArgs => URIO[AccountService, Boolean]
  )

  case class Subscriptions(
      accountEvents: ZStream[AccountService, Nothing, AccountEvent]
  )

  given findByAccountArgsSchema: Schema[AccountEnv, FindByAccountArgs] =
    Schema.gen
  given addAcountArgsSchema: Schema[AccountEnv, AddAccountArgs] = Schema.gen
  given queriesSchema: Schema[AccountEnv, Queries] = Schema.gen
  given mutationsSchema: Schema[AccountEnv, Mutations] = Schema.gen
  given eventsSchema: Schema[AccountEnv, AccountEvent] = Schema.gen
  given subSchema: Schema[AccountEnv, Subscriptions] = Schema.gen

  val api: GraphQL[AccountEnv] =
    graphQL(
      RootResolver(
        Queries(
          account = args => ZAccountService.getAccount(args.account),
          accountHolder = args => ZAccountService.findAccountHolder(args.name)
        ),
        Mutations(
          addAccount =
            args => ZAccountService.addAccount(args.name, args.balance),
          creditAccount =
            args => ZAccountService.creditAccount(args.account, args.amount),
          debitAccount =
            args => ZAccountService.debitAccount(args.account, args.amount),
          deleteAccount = args => ZAccountService.deleteAccount(args.account)
        ),
        Subscriptions(ZAccountService.deletedEvents)
      )
    )

  logger.info(api.render)

end AccountApi

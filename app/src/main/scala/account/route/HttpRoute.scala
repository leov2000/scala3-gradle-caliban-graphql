package account.route

import account.*
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.server.Route
import caliban.{CalibanError, GraphQLInterpreter}
import account.graphql.AccountApi
import account.graphql.AccountEnvironment.AccountEnv
import account.route.Directive.graphqlRoute
import zio.{Runtime, Unsafe}

import scala.concurrent.ExecutionContext

class HttpRoute(using
    system: ActorSystem,
    runtime: Runtime[AccountEnv],
    ec: ExecutionContext
):

  val interpreter: GraphQLInterpreter[AccountEnv, CalibanError] =
    Unsafe.unsafe { u =>
      given uns: Unsafe = u
      runtime.unsafe.run(AccountApi.api.interpreter).getOrThrow()
    }

  val route: Route = graphqlRoute(interpreter)
end HttpRoute

package account.route

import caliban.interop.tapir.{HttpInterpreter, WebSocketInterpreter}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.stream.Materializer
import caliban.{GraphQLInterpreter, PekkoHttpAdapter}
import sttp.tapir.json.circe.*
import zio.Runtime

import scala.concurrent.ExecutionContext

object Directive:

  def graphqlRoute[R, E](
      interpreter: GraphQLInterpreter[R, E]
  )(using
      runtime: Runtime[R],
      ec: ExecutionContext,
      materializer: Materializer
  ): Route =
    val adapter: PekkoHttpAdapter = PekkoHttpAdapter.default

    path("api" / "graphql") {
      adapter.makeHttpService(HttpInterpreter(interpreter))
    } ~ path("ws" / "graphql") {
      adapter.makeWebSocketService(WebSocketInterpreter(interpreter))
    } ~ path("altair") {
      getFromResource("altair.html")
    }
  end graphqlRoute
end Directive

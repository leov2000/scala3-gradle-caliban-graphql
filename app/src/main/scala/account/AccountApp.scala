package account

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import account.graphql.ZAccountService
import account.graphql.AccountEnvironment.AccountEnv
import account.route.HttpRoute
import zio.{Runtime, Unsafe}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object AccountApp extends App:

  given system: ActorSystem = ActorSystem()
  given executionContext: ExecutionContextExecutor = system.dispatcher

  given runtime: Runtime[AccountEnv] =
    Unsafe.unsafe(u =>
      given uns: Unsafe = u
      Runtime.unsafe.fromLayer(ZAccountService.make(Map()))
    )

  val bindingFuture =
    Http().newServerAt("localhost", 8088).bind(new HttpRoute().route)
  println(s"Server online at http://localhost:8088/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete { _ =>
      system.terminate()
    }
end AccountApp

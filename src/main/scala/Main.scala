import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import zio._
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import sttp.model.StatusCode
import sttp.tapir.query
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter._
import sttp.tapir.ztapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto._

object Short extends ZIOAppDefault {
  implicit override val runtime = zio.Runtime.default

  sealed abstract class DomainError(message: String)

  case class Error(message: String) extends DomainError(message)
  object Error {
    implicit val errorEncoder: Encoder[Error] = deriveEncoder[Error]
    implicit val errorDecoder: Decoder[Error] = deriveDecoder[Error]
  }

  sealed abstract class DomainErrorSecond(message: String)
      extends DomainError(message)

  case class ErrorSecond(message: String) extends DomainErrorSecond(message)
  object ErrorSecond {
    implicit val errorEncoder: Encoder[ErrorSecond] = deriveEncoder[ErrorSecond]
    implicit val errorDecoder: Decoder[ErrorSecond] = deriveDecoder[ErrorSecond]
  }

  case class Response(message: String)
  object Response {
    implicit val errorEncoder: Encoder[Response] = deriveEncoder[Response]
    implicit val errorDecoder: Decoder[Response] = deriveDecoder[Response]
  }

  val authAction: ZIO[Any, DomainError, Unit] = ZIO.unit

  val responseEndpoint =
    endpoint.post
      .securityIn(auth.bearer[String]())
      .errorOut(
        oneOf[DomainError](
          oneOfVariant(
            StatusCode.BadRequest,
            jsonBody[Error].description("ERROR")
          )
        )
      )
      .zServerSecurityLogic(s => authAction)
      .errorOutVariants(
        oneOfVariant(
          StatusCode.BadRequest,
          jsonBody[ErrorSecond].description("ERROR")
        )
      )
      .in("response")
      .in(query[String]("key"))
      .out(jsonBody[Response])

  val attach = VertxZioServerInterpreter().route(
    responseEndpoint.serverLogic(_ =>
      key =>
        ZIO.consoleWith(_.printLine("123")).orDie *> ZIO.fail(
          ErrorSecond("FAIL")
        )
    )
  )

  override def run =
    ZIO.scoped(
      ZIO
        .acquireRelease(
          ZIO
            .attempt {
              val vertx = Vertx.vertx()
              val server = vertx.createHttpServer()
              val router = Router.router(vertx)
              attach(router)
              server.requestHandler(router).listen(8089)
            }
            .flatMap(_.asRIO)
        ) { server =>
          ZIO.consoleWith(_.printLine("PRINT")).orDie *>
            ZIO.attempt(server.close()).flatMap(_.asRIO).orDie
        }
        *> ZIO.never
    )
}

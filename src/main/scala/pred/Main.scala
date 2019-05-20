package pred

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {
  val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  logger.info("BEGIN - Constructor of class Main from pred package.")
/*
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
*/
  implicit val system = ActorSystem("barclpred-service")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  val shortTimeout = 10.seconds
  val listenPort = 8081

  val serverSource = Http().bind(interface = "0.0.0.0", port = listenPort)

  val accur :Double = 0.8432

  val reqHandler: HttpRequest => Future[HttpResponse] = {

    case req@HttpRequest(HttpMethods.POST, Uri.Path("/recalc"), _, ent, _)
    =>
      logger.info("request (0) "+req.uri+" - "+req.method)
      Future.successful {
        HttpResponse(StatusCodes.OK,
          entity = "Jello 0 POST")
      }

    case req@HttpRequest(HttpMethods.GET, Uri.Path("/accur"), _, ent, _)
    =>
      logger.info("request (1) "+req.uri+" - "+req.method)
      val mlpcModel1 = new MlpcModel1
      Future.successful {
        HttpResponse(StatusCodes.OK,
          entity = "Test set accuracy = ["+mlpcModel1.accur+"]")
      }

    case httpReq : HttpRequest =>
      logger.info("request (2) "+httpReq.uri+" - "+httpReq.method)
      Future.successful {
        HttpResponse(
          StatusCodes.NotFound,
          entity = HttpEntity(`text/plain` withCharset `UTF-8`, "<html><body>Not found!</body></html>")
        )
      }
  }

  val bindingFuture: Future[Http.ServerBinding] =
    serverSource.to(Sink.foreach { connection =>
      connection.handleWithAsyncHandler(reqHandler)
    }).run()

}
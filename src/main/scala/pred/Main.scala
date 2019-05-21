package pred

import org.slf4j.{Logger, LoggerFactory}
import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._


object Main extends App {
  val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  logger.info("BEGIN - Constructor of class Main from pred package.")

  val mlpcModel1 = new MlpcModel1

  implicit val system = ActorSystem("barclpred-service")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  //val shortTimeout = 10.seconds
  val listenPort = 8081
  val serverSource = Http().bind(interface = "192.168.122.219", port = listenPort) //0.0.0.0

  val reqHandler: HttpRequest => Future[HttpResponse] = {

    case req@HttpRequest(HttpMethods.POST, Uri.Path("/recalc"), _, ent, _)
    => logger.info("request (0) "+req.uri+" - "+req.method)
      Future.successful {
        HttpResponse(StatusCodes.OK, entity = "Hello 0 POST")
      }

    case req@HttpRequest(HttpMethods.GET, Uri.Path("/accur"), _, ent, _)
    => logger.info("request (1) "+req.uri+" - "+req.method)

      try {
        val resAccur = mlpcModel1.getPredictionByModel
        logger.info("resAccur="+resAccur)
      } catch {
        case ex: Throwable => logger.info(ex.getLocalizedMessage)
      }

      Future.successful {
        HttpResponse(StatusCodes.OK, entity = "Test set accuracy = ["+mlpcModel1.getPredictionByModel+"]")
      }

    case httpReq : HttpRequest =>
      logger.info("request (2) "+httpReq.uri+" - "+httpReq.method)
      Future.successful {
        HttpResponse(
          StatusCodes.NotFound, entity = HttpEntity(`text/plain` withCharset `UTF-8`, "<html><body>Not found!</body></html>")
        )
      }
  }

  val bindingFuture: Future[Http.ServerBinding] =
    serverSource.to(Sink.foreach { connection =>
      connection.handleWithAsyncHandler(reqHandler)
    }).run()

}
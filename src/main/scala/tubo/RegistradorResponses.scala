package tubo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.scaladsl.Flow

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by Gustavo on 10-Jul-17.
  */
class RegistradorResponses(actorSystem: ActorSystem, paralelismo: Int = 8) {
	private val esAsinc = false
	private val blockingIoDispatcher = this.actorSystem.dispatchers.lookup("tubo.blocking-io-dispatcher")


	val flujo: Flow[(Try[HttpResponse], RegistradorRequests.Etiqueta), HttpResponse, NotUsed] = {
		if (this.esAsinc) Flow[(Try[HttpResponse], RegistradorRequests.Etiqueta)].mapAsyncUnordered(paralelismo)(t => funcAsinc(t._1, t._2))
		else Flow.fromFunction(t => funcSinc(t._1, t._2))
	}

	def funcSinc(tResponse: Try[HttpResponse], etiqueta: RegistradorRequests.Etiqueta): HttpResponse = {
		tResponse match {
			case Success(respuesta) => respuesta
			case Failure(error) => HttpResponse(StatusCodes.BadGateway, entity = error.getMessage)
		}

	}

	def funcAsinc(tResponse: Try[HttpResponse], etiqueta: RegistradorRequests.Etiqueta): Future[HttpResponse] = {
		tResponse match {
			case Failure(error) => Future.successful(HttpResponse(StatusCodes.BadGateway, entity = error.getMessage))
			case Success(respuesta) => Future {
				respuesta
			}(blockingIoDispatcher)
		}
	}
}


package tubo

import akka.NotUsed
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.scaladsl.Flow

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by Gustavo on 10-Jul-17.
  */
class RegistradorResponses(paralelismo: Int = 8) {
	val esAsinc = false

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
			case Success(respuesta) => Future.successful(respuesta)
			case Failure(error) => Future.successful(HttpResponse(StatusCodes.BadGateway, entity = error.getMessage))
		}

	}

}


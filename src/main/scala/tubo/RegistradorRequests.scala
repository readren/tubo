package tubo

import akka.NotUsed
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Flow

import scala.concurrent.Future

object RegistradorRequests {
	type Etiqueta = Long
}

/**
  * Created by Gustavo on 10-Jul-17.
  */
class RegistradorRequests(paralelismo: Int = 8) {

	import RegistradorRequests._

	val flujo: Flow[HttpRequest, (HttpRequest, Etiqueta), NotUsed] =
		Flow[HttpRequest].mapAsyncUnordered(paralelismo)(func)

	def func(request: HttpRequest): Future[(HttpRequest, Etiqueta)] = {
		Future.successful((request, 1))
	}
}

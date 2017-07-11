package tubo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Flow

import scala.concurrent.Future

object RegistradorRequests {
	type Etiqueta = Long
}

/**
  * Created by Gustavo on 10-Jul-17.
  */
class RegistradorRequests(actorSystem: ActorSystem, paralelismo: Int = 8) {
	import RegistradorRequests._

	private val blockingIoDispatcher = this.actorSystem.dispatchers.lookup("tubo.blocking-io-dispatcher")

	val flujo: Flow[HttpRequest, (HttpRequest, Etiqueta), NotUsed] =
		Flow[HttpRequest].mapAsyncUnordered(paralelismo)(func)

	private def func(request: HttpRequest): Future[(HttpRequest, Etiqueta)] = {
		Future {
			(request, 1L)
		}(blockingIoDispatcher)
	}
}

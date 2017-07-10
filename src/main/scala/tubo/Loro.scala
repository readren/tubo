package tubo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.HttpResponse

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
  * Created by Gustavo on 10-Jul-17.
  */
object Loro {

	private implicit val actorSystem = ActorSystem("Loro")
	private val log = actorSystem.log

	def main(args: Array[String]): Unit = {
		val http = Http()(actorSystem)
		implicit val materializer = ActorMaterializer.create(actorSystem) // TODO investigar si conviene pasar el actor system en lugar del contexto de este actor.


		val config = actorSystem.settings.config
		val interface = config.getString("loro.interface")
		val port = config.getInt("loro.port")

		val flow = akka.http.scaladsl.server.RouteResult.route2HandlerFlow(this.handler)
		val fServerBinding = http.bindAndHandle(flow, interface, port)

		fServerBinding.onComplete {
			case Success(_) =>
				log.info(s"escuchando en $interface:$port")
			case Failure(e) =>
				if (NonFatal(e)) {
					log.error(e, "No logré apoderarme de {}:{}", interface, port)
				}
		}(actorSystem.dispatcher)


	}

	def handler: Route = {
		extractRequest { request =>
			(post | put) {
				log.debug("method={}, entity={}", request.method.value, request.entity.toString)
				complete(request.entity)
			} ~ (get | delete) {
				val uri = request.uri.toString()
				val mensaje = s"method=${request.method.value}, uri=$uri"
				log.debug(mensaje)
				complete(mensaje)
			}
		}
	}

}

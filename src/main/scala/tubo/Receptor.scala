package tubo

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.control.NonFatal


object Receptor {
	def props(supervisor: ActorRef): Props = Props(new Receptor(supervisor))

	case object Apagar

}

/**
  * Arma el receptor de HTTP-requests y se queda esperando eternamente que le envíen la señal de apagarse.
  */
class Receptor(supervisor: ActorRef) extends Actor with ActorLogging {

	import Receptor._

	private val actorSystem = this.context.system
	private val http: HttpExt = Http()(this.actorSystem)
	private implicit val materializer = ActorMaterializer.create(this.context) // TODO investigar si conviene pasar el actor system en lugar del contexto de este actor.

	private var fServerBinding: Future[Http.ServerBinding] = _

	override def preStart: Unit = {
		val config = this.context.system.settings.config
		val interface = config.getString("tubo.interface")
		val port = config.getInt("tubo.port")
		val hostDestino = config.getString("tubo.destino.host")
		val puertoDestino = config.getInt("tubo.destino.puerto")

		val registradorRequests = new RegistradorRequests(this.actorSystem)
		val registradorResponses = new RegistradorResponses(this.actorSystem)
		val emisor: Emisor[RegistradorRequests.Etiqueta] = new Emisor(http, hostDestino, puertoDestino, registradorRequests.flujo, registradorResponses.flujo)(this.materializer)
		this.fServerBinding = this.http.bindAndHandle(emisor.flujo, interface, port)
		//		this.fServerBinding = this.http.bindAndHandle(this.handler, interface, port)

		this.fServerBinding.onComplete {
			case Success(_) =>
				log.info(s"escuchando en $interface:$port")
			case Failure(e) =>
				if (NonFatal(e)) {
					log.error(e, "No logré apoderarme de {}:{}", interface, port)
					this.self ! Apagar
				}
		}(this.context.dispatcher)

	}

	override def receive: Receive = {
		case Apagar =>
			log.info("El actor {} ha pedido que me apague", this.sender)
			this.apagarServidorHttp {
				() => supervisor ! Supervisor.ApagarTodo
			}
	}

	private def apagarServidorHttp(cuandoApagado: () => Unit): Unit = {
		implicit val ec = this.context.dispatcher
		// notar que a esta altura el fServerBinding seguramente ya esta completo
		this.fServerBinding.onComplete {
			case Failure(_) => cuandoApagado()
			case Success(binding) =>
				this.http.shutdownAllConnectionPools().onComplete { _ =>
					binding.unbind().onComplete(_ => cuandoApagado())
				}
		}
	}

	//	def handler: Route = {
	//		extractRequest { request =>
	//			(post | put) {
	//				complete(request.entity)
	//			} ~ (get | delete) {
	//				val uri = request.uri.toString()
	//				complete(s"method=${request.method.value}, uri=$uri")
	//			}
	//		}
	//	}

}

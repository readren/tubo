package tubo

import akka.actor.{Actor, ActorRef, Props}

object Supervisor {

	case object ApagarTodo

}

class Supervisor extends Actor {

	import Supervisor._

	private val fachadaReceptor: ActorRef = context.actorOf(Receptor.props(this.self), "fachadaReceptor")

	override def preStart(): Unit = {
	}

	def receive: Receive = {
		// Dado que este es el actor supervisor, apagarlo implica terminar el ActorSystem y con ello la aplicación completa.
		case ApagarTodo => context.stop(self)
	}
}


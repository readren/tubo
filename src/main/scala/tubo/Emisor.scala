package tubo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Source}
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, FlowShape}

import scala.util.Try

/**
  * Created by Gustavo on 10-Jul-17.
  */
class Emisor[T](http: HttpExt, hostDestino: String, puertoDestino: Int, registradorRequests: Flow[HttpRequest, (HttpRequest, T), Any], registradorResponses: Flow[(Try[HttpResponse], T), HttpResponse, Any])(implicit am: ActorMaterializer) {

	private val poolClientFlow = this.http.cachedHostConnectionPool[T](this.hostDestino, this.puertoDestino)

	private val grafo = GraphDSL.create(registradorRequests, registradorResponses, this.poolClientFlow)((_, _, c) => c) {
		implicit builder =>
			(registradorRequestsShape, registradorResponsesShape, poolClientFlowShape) =>
				import GraphDSL.Implicits._

				registradorRequestsShape.out ~> poolClientFlowShape.in
				poolClientFlowShape.out ~> registradorResponsesShape.in

				FlowShape[HttpRequest, HttpResponse](registradorRequestsShape.in, registradorResponsesShape.out)
	}

	val flujo: Flow[HttpRequest, HttpResponse, HostConnectionPool] = Flow.fromGraph(this.grafo)

}

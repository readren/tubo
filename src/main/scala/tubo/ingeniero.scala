package tubo

import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, FlowShape}

import scala.util.Try

/**
  * Created by Gustavo on 10-Jul-17.
  */
object ingeniero {

	def armarFlujoHaciaYDesdeElDestino[T](
											 http: HttpExt,
											 hostDestino: String,
											 puertoDestino: Int
										 )(
											 implicit am: ActorMaterializer
										 ): Flow[(HttpRequest, T), (Try[HttpResponse], T), HostConnectionPool] = {
		http.cachedHostConnectionPool[T](hostDestino, puertoDestino)
	}

	def armarFlujoDesdeYHaciaElOrigen[T](
											registradorRequests: Flow[HttpRequest, (HttpRequest, T), Any],
											destino: Flow[(HttpRequest, T), (Try[HttpResponse], T), HostConnectionPool],
											registradorResponses: Flow[(Try[HttpResponse], T), HttpResponse, Any]
										)(
											implicit am: ActorMaterializer
										): Flow[HttpRequest, HttpResponse, HostConnectionPool] = {

		val grafo = GraphDSL.create(registradorRequests, registradorResponses, destino)((_, _, c) => c) {
			implicit builder =>
				(registradorRequestsShape, registradorResponsesShape, poolClientFlowShape) =>
					import GraphDSL.Implicits._

					registradorRequestsShape.out ~> poolClientFlowShape.in
					poolClientFlowShape.out ~> registradorResponsesShape.in

					FlowShape[HttpRequest, HttpResponse](registradorRequestsShape.in, registradorResponsesShape.out)
		}
		Flow.fromGraph(grafo)
	}
}

package hercules.actors.notifiers.rest

import hercules.protocols.HerculesMainProtocol.NotificationUnitMessage

import scala.concurrent.Future
import scalaj.http.{ HttpResponse }

/**
 * Created by johda411 on 2015-03-18.
 * Implement this trait to get an interface for sending messages to Http requests.
 */
trait RestClient {

  /**
   * Implement this function with behaviour appropriate to the http requests you want
   * to make. This might mean not on passing on certain messages if they are not conforming
   * to the implementation you want.
   * @param message to transform to a http request.
   * @return A Future of a HttpResponse with response codes and possible payloads.
   */
  def mapMessageToEndPoint(message: NotificationUnitMessage): Future[HttpResponse[String]]

}

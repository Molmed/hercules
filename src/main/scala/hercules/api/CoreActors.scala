package hercules.api

import akka.actor.ActorRef
import akka.util.Timeout

import hercules.actors.utils.MasterLookup
import hercules.api.services._

import spray.routing.HttpService

/**
 * This trait contains the actors that make up our application; it can be mixed in with
 * ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
 */
trait CoreActors extends MasterLookup {
  this: Core =>

  lazy val cluster = getMasterClusterClient(system, getDefaultConfig, getDefaultClusterClient)
  lazy val services = List[HerculesService](
    new StatusService {
      def actorRefFactory = system
      implicit val clusterClient = cluster
      implicit val to = timeout
    },
    new DemultiplexingService {
      def actorRefFactory = system
      implicit val clusterClient = cluster
      implicit val to = timeout
    },
    new SwaggerService with HerculesService {
      def actorRefFactory = system
    })
}
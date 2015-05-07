package hercules.api

import hercules.actors.utils.MasterLookup
import hercules.api.services._

/**
 * This trait contains the actors and/or service traits that make up our application; it can be mixed in with
 * ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
 */
trait CoreActors extends MasterLookup {
  this: Core =>

  /**
   * Lazily looks up the cluster client for the system
   */
  lazy val cluster = getMasterClusterClient(system, getDefaultConfig, getDefaultClusterClient)
  /**
   * We keep the services we will provide in a list. When setting up the API, the routes of all services
   * will be concatenated to form the complete route.
   */
  lazy val services = List[HerculesService](
    new StatusService {
      def actorRefFactory = system
      implicit val clusterClient = cluster
      implicit val to = timeout
    },
    // TODO Temporarily disabled. /JD 20150507
    //    new DemultiplexingService {
    //      def actorRefFactory = system
    //      implicit val clusterClient = cluster
    //      implicit val to = timeout
    //    },
    new SwaggerService with HerculesService {
      def actorRefFactory = system
    })
}
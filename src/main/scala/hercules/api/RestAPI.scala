package hercules.api

/**
 * The cake pattern allows us to start the API by just mixing in the traits
 * that build up the API
 */
object RestAPI extends BootedCore with CoreActors with Api with Web
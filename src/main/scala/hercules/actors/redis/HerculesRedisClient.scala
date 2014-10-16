package hercules.actors.redis

import redis.RedisClient
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Success, Failure }
import redis.actors.NoConnectionException
import akka.actor.Actor
import akka.actor.Props
import hercules.entities.ProcessingUnit
import akka.event.LoggingReceive
import hercules.actors.HerculesActor
import scala.concurrent.Future

object HerculesRedisClient {

  /**
   * @param unit the processing unit to a to the redis instance
   * @param redis implicity tries to fetch a redis client
   * @return the length of the list after push wrapped in a future.
   */
  def sendProcessingUnitToRedis(
    unit: ProcessingUnit)(implicit redis: RedisClient): Future[Long] = {

    // @TODO This is all just a sketch right now, but we'll have 
    // to fix this later

    val task = "process_flowcell" // Check with @Guillermo what the task name is

    // Argument should be a key-value pairs to parse into a dictionary.
    // It's important that these are correct, because otherwise Guillermos
    // Celery stuff will go nuts!
    val args = List("arg1:value1", "arg2:value2")

    val pushUnitToRedis =
      redis.rpush[String](
        "some_key",
        s"{'task': $task, 'args': {${args.mkString(",")}}")

    pushUnitToRedis
  }

  def props(): Props = {
    Props(new HerculesRedisClient())
  }

  object HerculesRedisClientProtocol {
    case class SendProcessingUnitToRedis(unit: ProcessingUnit)
  }

}

/**
 * TODO This is just a template for how Hercules can interface with Redis.
 * We will use this to send messages to a redis instance which will then
 * automatically kick of best practice pipelines.
 *
 * This should somehow interface with a delivery actor that once it's done
 * should send a message here (and to any other services that needs to be
 * notified).
 */
class HerculesRedisClient() extends HerculesActor {

  import HerculesRedisClient.HerculesRedisClientProtocol._

  //@TODO Make configurable
  implicit val redis = RedisClient(host = "localhost")

  def receive = LoggingReceive {
    case message: SendProcessingUnitToRedis =>
      try {
        val response = HerculesRedisClient.sendProcessingUnitToRedis(message.unit)
        response.onComplete {
          case Success(reply) =>
            log.info(s"Successfully send $message.unit to Redis.")
          case Failure(reason) =>
            log.warning(
              s"Failed to send $message.unit to Redis." +
                " Will try to resend it in one minute.")
            context.system.scheduler.scheduleOnce(60.seconds, self, message)
        }
      } catch {
        case NoConnectionException => {
          context.system.scheduler.scheduleOnce(60.seconds, self, message)
          log.warning("Lost connection to Redis." +
            " Will attempt to resend in 60 seconds")
        }
      }
  }

}
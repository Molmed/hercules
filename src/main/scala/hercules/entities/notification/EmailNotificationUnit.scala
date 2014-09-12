package hercules.entities.notification

object EmailNotificationUnit {
  
  def sendNotification(
    unit: EmailNotificationUnit
  ): Boolean = {
    println("Sending message: " + unit.message)
    true
  }
  
}
/**
 * Provides a base for representing an email notification unit
 */
class EmailNotificationUnit(
  val message: String,
  var attempts: Int = 0) extends NotificationUnit {}

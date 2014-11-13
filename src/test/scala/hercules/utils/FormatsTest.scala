package hercules.utils

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.util.Date
import java.util.Calendar

class FormatsTest extends FlatSpec with Matchers {

  "Formats" should "give us formats on the form: yyyy-MM-dd HH:mm:ss" in {
    val expected = "2014-11-12 17:53:00"
    val calendar = Calendar.getInstance()
    calendar.clear()
    // Note that month starts with 0 for jan.
    calendar.set(2014, 10, 12, 17, 53, 00)
    val date = calendar.getTime()
    val actual = Formats.date.format(date)
    assert(expected.toString() === actual.toString())
  }

}
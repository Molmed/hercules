package hercules.utils

/**
 *
 *
 *
 * Created by johda411 on 2015-03-09.
 */
object VersionUtils {

  /**
   * Report back the Hercules version as set in the manifest file
   */
  lazy val herculesVersion: String = {
    val versionFromJar = this.getClass.getPackage.getImplementationVersion
    if (versionFromJar == null)
      "development version (this should not be seen in production mode)"
    else
      versionFromJar
  }

}

package hercules.utils

import java.io.File

import org.apache.commons.io.FileUtils

/**
 * Created by johda411 on 2015-03-05.
 * Some utilities for deleting files and dirs (recursively)
 */
object DeleteUtils {

  /**
   * Will delete all files and directories in the seq recursively
   * @param filesAndDirsToDelete delete these (if they exist)!
   */
  def deleteFilesAndDirs(filesAndDirsToDelete: Seq[File]): Unit = {
    filesAndDirsToDelete.foreach(x => {
      if (x.exists())
        if (x.isDirectory())
          FileUtils.deleteDirectory(x)
        else
          x.delete()
    })
  }
}

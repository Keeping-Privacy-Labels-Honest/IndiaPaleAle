package de.tubs.ipa.utility

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import com.nimbusds.jose.util.StandardCharset

object FilesystemInteraction {
  def readFile(path: String,
               encoding: Charset = StandardCharset.UTF_8): String = {
    val encoded = Files.readAllBytes(Paths.get(path))
    new String(encoded, encoding)
  }

  def files(path: String): Seq[String] = {
    assert(new File(path).isDirectory, "the provided path must be a folder")
    new File(path)
      .listFiles()
      .filter(_.isFile)
      .filter(_.getAbsolutePath.endsWith(".json"))
      .map(_.getAbsolutePath)
  }

}

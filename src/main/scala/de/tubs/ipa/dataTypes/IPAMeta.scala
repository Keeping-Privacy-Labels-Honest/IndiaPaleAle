package de.tubs.ipa.dataTypes

import de.tubs.ipa.parser.{CustomXML, iTunesMetadataParser}

import java.io.File
import java.util.zip.ZipFile
import scala.language.reflectiveCalls
import scala.xml.Elem

object IPAMeta {

  private def using[T <: { def close(): Unit }, U](resource: T)(
      block: T => U): U = {
    try {
      block(resource)
    } finally {
      if (resource != null) {
        resource.close()
      }
    }
  }

  def fromIpa(path: String): IPAMeta = {
    using(new ZipFile(new File(path))) { zipFile =>
      val metadata = Option(zipFile.getEntry("iTunesMetadata.plist"))
      metadata match {
        case Some(data) =>
          val xml: Elem = CustomXML.load(zipFile.getInputStream(data))
          new IPAMeta(xml)
        case None =>
          throw new RuntimeException(
            s"file $path does not have an iTunesMetaData.plist")
      }

    }
  }

}

class IPAMeta(metaXml: Elem) {

  val dict: MetaDict = {
    assert(metaXml.child.length == 1)
    iTunesMetadataParser.parseElement(metaXml.child.head.asInstanceOf[Elem])
  }.asInstanceOf[MetaDict]

}

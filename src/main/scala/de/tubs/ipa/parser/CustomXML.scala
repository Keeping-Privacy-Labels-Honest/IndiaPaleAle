package de.tubs.ipa.parser

import javax.xml.parsers.SAXParserFactory
import scala.xml.factory.XMLLoader
import scala.xml.{Elem, SAXParser}

object CustomXML extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val factory = SAXParserFactory.newInstance()
    //factory.setFeature("http://xml.org/sax/features/validation", false)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",
                       false)
    //factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    //factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    factory.newSAXParser()
  }
}

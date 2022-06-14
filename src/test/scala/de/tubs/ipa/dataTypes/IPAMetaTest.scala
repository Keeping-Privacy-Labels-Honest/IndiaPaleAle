package de.tubs.ipa.dataTypes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IPAMetaTest extends AnyWordSpec with Matchers {

  "fromIpa" should {
    "return a IPAMeta object with the meta xml" in {
      val ipa = IPAMeta.fromIpa("./resources/Snaptube_2.2.1.ipa")
      ipa.dict.get("softwareVersionBundleId") shouldBe MetaString("com.sptube.browser")
    }
  }

}

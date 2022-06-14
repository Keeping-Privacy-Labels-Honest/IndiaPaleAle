package de.tubs.ipa.privacyLabels

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PrivacyLabelsTest extends AnyWordSpec with Matchers {

  "just testing" should {
    "dostuff" in {
      println(PrivacyLabels.getPrivacyLabel(1522899035))
    }
  }

}

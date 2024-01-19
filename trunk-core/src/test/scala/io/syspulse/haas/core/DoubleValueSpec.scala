package io.syspulse.haas.core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
//import org.scalatest.TryValues._
import scala.io.Source

import io.syspulse.skel.util.Util
import java.math.BigInteger

class DoubleValueSpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  "DoubleValueSpec" should {
    "10.1 - 10.0 == 0" in {
      val d = 10.0 - 10.0
      DoubleValue.isZero(d) should === (true)
    }

    "1000000.0 - 1000000.0 == 0" in {
      val d = 1000000.0 - 1000000.0
      DoubleValue.isZero(d) should === (true)
    }

    "100.0 == 100.0 " in {
      val d1 = 100.0
      val d2 = 100.0
      DoubleValue.==(d1,d2) should === (true)
    }
  }
  
}
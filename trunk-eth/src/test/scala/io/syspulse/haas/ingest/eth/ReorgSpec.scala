package io.syspulse.haas.ingest.eth

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
//import org.scalatest.TryValues._

import io.syspulse.skel.util.Util

import io.syspulse.haas.ingest.Config
import io.syspulse.haas.ingest.eth

class ReorgSpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  "ReorgSpec (old Reorg Flow)" should {
    
    "find reorg (1,2,1) with depth=2" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2)
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1 should === ((true,false))
      val r2 = p.isReorg("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2 should === ((true,false))
      
      val r4 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111111","timestamp":"0x904","transactions":[]}}""")
      r4 should === ((true,true))
    }

    "find reorg (1,2,2) with depth=2" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2)
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1 should === ((true,false))
      val r2 = p.isReorg("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2 should === ((true,false))
      
      val r4 = p.isReorg("""{"result":{"number":"0x2","hash":"0x2222222","timestamp":"0x904","transactions":[]}}""")
      r4 should === ((true,true))
    }

    "not find reorg (1,1,1) with depth=2 (Duplicates)" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2)
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1 should === ((true,false))
      
      val r2 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r2 should === ((false,false))
      
      val r4 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r4 should === ((false,false))
    }

    "not find reorg (1,2,3,1) with depth=2" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2)
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1 should === ((true,false))
      val r2 = p.isReorg("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2 should === ((true,false))
      val r3 = p.isReorg("""{"result":{"number":"0x3","hash":"0x3333","timestamp":"0x903","transactions":[]}}""")
      r3 should === ((true,false))

      val r4 = p.isReorg("""{"result":{"number":"0x1","hash":"0x1111111","timestamp":"0x904","transactions":[]}}""")
      r4 should === ((true,false))
    }

    
  }  
}
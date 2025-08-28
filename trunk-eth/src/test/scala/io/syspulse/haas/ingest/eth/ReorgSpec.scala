package io.syspulse.haas.ingest.eth

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
//import org.scalatest.TryValues._

import io.syspulse.skel.util.Util

import io.syspulse.haas.ingest.Config

import io.syspulse.haas.ingest.eth
import io.syspulse.haas.reorg.ReorgBlock2

class ReorgSpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  "ReorgSpec (reorgFlow=reorg1)" should {
    
    "find reorg (1,2,1) with depth=2" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg1")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)
      
      val r2 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2._1 should === (true)
      r2._2.size should === (0)
      
      val r4 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111111","timestamp":"0x904","transactions":[]}}""")
      info(s"r4=${r4._2}")
      r4._1 should === (true)
      r4._2.size should === (2)
      r4._2(0).num should === (2)
      r4._2(1).num should === (1)
    }

    "find reorg (1,2,2) with depth=1" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg1")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)
      
      val r2 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2._1 should === (true)
      r2._2.size should === (0)
      
      val r4 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222222","timestamp":"0x904","transactions":[]}}""")
      info(s"r4=${r4._2}")
      r4._1 should === (true)
      r4._2.size should === (1)
      r4._2(0).num should === (2)      

    }

    "not find reorg (1,1,1) (Duplicates)" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg1")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)
      
      val r2 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")      
      r2._1 should === (false)
      r2._2.size should === (0)
      
      val r4 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")      
      r4._1 should === (false)
      r4._2.size should === (0)
    }

    "not find reorg (1,2,3,1) (default depth=2)" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg1")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
            
      //
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)

      val r2 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2._1 should === (true)
      r2._2.size should === (0)

      val r3 = p.reorg.track("""{"result":{"number":"0x3","hash":"0x3333","timestamp":"0x903","transactions":[]}}""")
      r3._1 should === (true)
      r3._2.size should === (0)

      val r4 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111111","timestamp":"0x904","transactions":[]}}""")
      r4._1 should === (true)
      r4._2.size should === (0)
    }
        
  } 

  // ---------------------------------------------------------------------------------------------------------------
  "ReorgSpec (reorgFlow=reorg2)" should {
    
    "find reorg (1,2,1) with depth=2" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg2")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)
      
      val r2 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2._1 should === (true)
      r2._2.size should === (0)
      
      val r4 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111111","timestamp":"0x904","transactions":[]}}""")
      info(s"r4=${r4._2}")
      r4._1 should === (true)
      r4._2.size should === (2)
      r4._2(0).num should === (2)
      r4._2(1).num should === (1)
    }

    "find reorg (1,2,2) with depth=1" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg2")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)
      
      val r2 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2._1 should === (true)
      r2._2.size should === (0)
      
      val r4 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222222","timestamp":"0x904","transactions":[]}}""")
      info(s"r4=${r4._2}")
      r4._1 should === (true)
      r4._2.size should === (1)
      r4._2(0).num should === (2)      

    }

    "find reorg (1,1,1) (Duplicates)" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg2")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
      
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)
      
      val r2 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")      
      r2._1 should === (true)
      r2._2.size should === (0)
      
      val r4 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")      
      r4._1 should === (true)
      r4._2.size should === (0)
    }

    "not find reorg (1,2,3,1) (default depth=2)" in {
      val config = Config(feed = "",output = "null://",blockReorg = 2,reorgFlow = "reorg2")
      val p = new eth.flow.rpc3.PipelineBlock(config)
      p.reorg.depth should === (2)
            
      //
      val r1 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111","timestamp":"0x901","transactions":[]}}""")
      r1._1 should === (true)
      r1._2.size should === (0)

      val r2 = p.reorg.track("""{"result":{"number":"0x2","hash":"0x2222","timestamp":"0x902","transactions":[]}}""")
      r2._1 should === (true)
      r2._2.size should === (0)

      val r3 = p.reorg.track("""{"result":{"number":"0x3","hash":"0x3333","timestamp":"0x903","transactions":[]}}""")
      r3._1 should === (true)
      r3._2.size should === (0)

      val r4 = p.reorg.track("""{"result":{"number":"0x1","hash":"0x1111111","timestamp":"0x904","transactions":[]}}""")
      r4._1 should === (true)
      r4._2.size should === (0)
    }

    "find reorg (20786680,20786681,20786680) with depth=2" in {
      val reorg = new ReorgBlock2(2)
      
      val r1 = reorg.track(20786680,"0x74b99f772cf68feebd4135cfa12be1918f4de8b761197f8fc3d0834322373ced",0,0)
      r1._1 should === (true)
      r1._2.size should === (0)
      
      val r2 = reorg.track(20786681,"0x74b99f772cf68feebd4135cfa12be1918f4de8b761197f8fc3d0834322373ced",0,0)
      r2._1 should === (true)
      r2._2.size should === (0)
      
      val r4 = reorg.track(20786680,"0x2e7848f43111e4c84dc1f4141f369a41499e2c1203287d5d373b550c49ac1b38",0,0)
      info(s"r4=${r4._2}")
      r4._1 should === (true)
      r4._2.size should === (2)
      r4._2(0).num should === (20786681)
      r4._2(1).num should === (20786680)
    }
  } 
}
package io.syspulse.haas.ingest.eth.flow.rpc

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
//import org.scalatest.TryValues._
import scala.io.Source

import io.syspulse.skel.util.Util
import java.math.BigInteger

class LastBlockSpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  "LastBlock" should {
    "have history == lag" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100, lag = 2)
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.size() should === (0)

      lastBlock.commit(1,"0x1")
      lastBlock.size() should === (1)
      lastBlock.last() should === (List(Block(1,"0x1")))
      lastBlock.commit(2,"0x2")
      lastBlock.last() should === (List(Block(2,"0x2"),Block(1,"0x1")))
      lastBlock.size() should === (2)
      lastBlock.commit(3,"0x3")
      lastBlock.last() should === (List(Block(3,"0x3"),Block(2,"0x2"),Block(1,"0x1")))
      lastBlock.size() should === (3)
      lastBlock.commit(4,"0x4")
      lastBlock.last() should === (List(Block(4,"0x4"),Block(3,"0x3"),Block(2,"0x2")))
      lastBlock.size() should === (3)
      lastBlock.commit(5,"0x5")
      lastBlock.last() should === (List(Block(5,"0x5"),Block(4,"0x4"),Block(3,"0x3")))
      lastBlock.size() should === (3)
    }

    "not find reorg on start" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100)      
      lastBlock.next() should === (1)

      val r1 = lastBlock.isReorg(1,"0x111")

      r1 should === (List.empty)
    }

    "not find reorg on first block (lag=0)" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 0)      
      lastBlock.next() should === (1)

      lastBlock.commit(1,"0x111")

      val r1 = lastBlock.isReorg(1,"0x111")

      r1 should === (List.empty)
    }

    "not find reorg on duplicate block repeat (lag=0)" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 0)      
      lastBlock.next() should === (1)
      
      lastBlock.commit(1,"0x111")
      lastBlock.commit(1,"0x111")
      lastBlock.commit(1,"0x111")

      val r1 = lastBlock.isReorg(1,"0x111")

      r1 should === (List.empty)
    }

    "not find reorg on duplicate block repeat (lag=2)" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 0)      
      lastBlock.next() should === (1)
      
      lastBlock.commit(1,"0x111")
      lastBlock.commit(1,"0x111")
      lastBlock.commit(1,"0x111")

      val r1 = lastBlock.isReorg(1,"0x111")

      r1 should === (List.empty)
      lastBlock.size() should === (1)
    }

    "not find reorg on normal block sequence with lag == 0" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 0)      
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")      
      val r1 = lastBlock.isReorg(2,"0x222")
      r1 should === (List.empty)

      lastBlock.commit(2,"0x222")      
      val r2 = lastBlock.isReorg(3,"0x333")
      r2 should === (List.empty)
    }

    "not find reorg on normal block sequence with lag == 1" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 1) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")      
      val r1 = lastBlock.isReorg(2,"0x222")
      r1 should === (List.empty)      

      lastBlock.commit(2,"0x222")      
      val r2 = lastBlock.isReorg(3,"0x333")
      r2 should === (List.empty)
    }

    "not commit same block" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 1) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")      
      lastBlock.next() should === (2)
      lastBlock.committed() should === (1)

      lastBlock.commit(1,"0x111")      
      lastBlock.next() should === (2)
      lastBlock.committed() should === (1)
      
    }

    "not find reorg on normal block sequence (10) with lag == 3" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 3) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      for(i <- 1 to 10) {
        lastBlock.commit(i,s"0x${i}${i}${i}")      
        val r = lastBlock.isReorg(i + 1,s"0x${i+1}${i+1}${i+1}")
        r should === (List.empty)
                
      }
    }
        
    "find reorg (deep=1) with lag == 2" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 2) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")      
      lastBlock.commit(2,"0x222")
      
      val r3 = lastBlock.isReorg(2,"0x222_2")
      r3 should === (List(Block(2,"0x222")))
    }

    "find reorg (deep=1) with lag == 2 and throttle=1000" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 2) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")   
      lastBlock.commit(1,"0x111")
      lastBlock.commit(1,"0x111")
      lastBlock.commit(1,"0x111")
      
      val r3 = lastBlock.isReorg(1,"0x111_2")
      r3 should === (List(Block(1,"0x111")))
    }

    "reorg (deep=1) with lag == 2" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 2) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")
      lastBlock.commit(2,"0x222")
      
      val r3 = lastBlock.isReorg(2,"0x222_2")
      val r4 = lastBlock.reorg(r3)
      r3 should === (List(Block(2,"0x222")))
      r4 should === (List(Block(1,"0x111")))

      lastBlock.next() should === (3)
    }

    "find reorg (deep=2) with lag == 2" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 2) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")      
      lastBlock.commit(2,"0x222")
      lastBlock.commit(3,"0x333")      
      
      val r3 = lastBlock.isReorg(2,"0x222_2")
      r3 should === (List(Block(3,"0x333"),Block(2,"0x222")))
    }

    "reorg (deep=2) with lag == 2" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 2) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.commit(1,"0x111")
      lastBlock.commit(2,"0x222")
      lastBlock.commit(3,"0x333")      
      
      val r3 = lastBlock.isReorg(2,"0x222_2")
      val r4 = lastBlock.reorg(r3)
      r3 should === (List(Block(3,"0x333"),Block(2,"0x222")))
      r4 should === (List(Block(1,"0x111")))

      lastBlock.commit(2,"0x222_2")
      lastBlock.next() should === (3)
      lastBlock.committed() should === (2)
      lastBlock.last() should === (List(Block(2,"0x222_2"),Block(1,"0x111")))
    }

    "preserve history between instantiations" in {
      val lastBlock1 = new LastBlock()
      lastBlock1.reset()
      lastBlock1.set(1,1,100, lag = 2)
      lastBlock1.commit(1,"0x111")
      lastBlock1.commit(2,"0x222")
      
      lastBlock1.last() should === (List(Block(2,"0x222"),Block(1,"0x111")))

      val lastBlock2 = new LastBlock()
      lastBlock2.set(1,1,100, lag = 2)
      lastBlock2.last() should === (List(Block(2,"0x222"),Block(1,"0x111")))
    }
      
    "beacon reorg flow (deep=1) with lag == 2" in {
      val lastBlock = new LastBlock()
      lastBlock.reset()
      
      lastBlock.set(1,1,100,lag = 2) 
      lastBlock.next() should === (1)
      lastBlock.committed() should === (-1)

      lastBlock.isReorg(1,"0x111") should === (List())
      lastBlock.commit(1,"0x111")
      lastBlock.isReorg(1,"0x111") should === (List())
      lastBlock.commit(1,"0x111")
      lastBlock.isReorg(1,"0x111") should === (List())
      lastBlock.commit(1,"0x111")
      
      lastBlock.isReorg(2,"0x222") should === (List())
      lastBlock.commit(2,"0x222")
      lastBlock.isReorg(2,"0x222") should === (List())
      lastBlock.commit(2,"0x222")
      lastBlock.isReorg(2,"0x222") should === (List())
      lastBlock.commit(2,"0x222")
      
      val r3 = lastBlock.isReorg(2,"0x222_2")
      r3 should === (List(Block(2,"0x222")))
      val r4 = lastBlock.reorg(r3)      
      r4 should === (List(Block(1,"0x111")))
      lastBlock.commit(2,"0x222_2")

      lastBlock.next() should === (3)

      lastBlock.isReorg(2,"0x222_2") should === (List())
      lastBlock.commit(2,"0x222_2")

    }   
  }
  
}
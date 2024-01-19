package io.syspulse.haas.ingest

import java.util.concurrent.atomic.AtomicLong

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

class CursorBlock(file:String = "BLOCK",lag:Int = 0) {
  private val log = Logger(this.getClass)

  override def toString() = s"${current} [${blockStart} : ${blockEnd}]"

  var stateFile = file
  private var current:Long = 0
  var blockStart:Long = 0
  var blockEnd:Long = 0

  def read(newStateFile:String = file):String = this.synchronized {
    stateFile = newStateFile
    // can be string lile ("latest")
    os.read(os.Path(stateFile,os.pwd))    
  }

  def write(current:Long) = this.synchronized {
    os.write.over(os.Path(file,os.pwd),current.toString)    
  }
  
  def init(blockStart:Long, blockEnd:Long) = {
    this.synchronized {
      this.current = blockStart
      this.blockStart = blockStart
      this.blockEnd = blockEnd
    }    
  }

  def set(current:Long) = this.synchronized {
    this.current = current    
  }

  def get() = this.synchronized {
    this.current
  }

  def next() = this.synchronized {
    current + 1
  }

  def commit(block:Long) = this.synchronized {
    current = block + 1
    write(current)
  }

}


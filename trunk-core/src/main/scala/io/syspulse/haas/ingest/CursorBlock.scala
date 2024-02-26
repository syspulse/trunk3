package io.syspulse.haas.ingest

import java.util.concurrent.atomic.AtomicLong

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

class CursorBlock(file:String = "BLOCK",lag:Int = 0)(implicit config:Config) {

  case class CursorFile(file:String) {
    private val log = Logger(this.getClass)

    val datastore = if(config.datastore.isBlank() || file.startsWith("/")) "" else config.datastore + "/"  
    val stateFile = s"${datastore}${file}"

    if(!os.exists(os.Path(stateFile,os.pwd))) {
      // create file
      write(0L)
    }

    def read():String = this.synchronized {
      // can be string lile ("latest")
      os.read(os.Path(stateFile,os.pwd))
    }

    def write(current:Long) = this.synchronized {
      os.write.over(os.Path(stateFile,os.pwd),current.toString)    
    }
  }
  

  override def toString() = s"${current} [${blockStart} : ${blockEnd}]"

  private var cursor = CursorFile(file)
  private var current:Long = 0
  var blockStart:Long = 0
  var blockEnd:Long = 0
    
  def setFile(newStateFile:String) = this.synchronized {
    cursor = CursorFile(newStateFile)
    this
  }

  def read():String = this.synchronized {
    // can be string lile ("latest")
    cursor.read()
  }

  def write(current:Long) = this.synchronized {
    cursor.write(current)
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


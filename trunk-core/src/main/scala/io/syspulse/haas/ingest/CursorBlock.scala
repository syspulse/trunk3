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
  var blockEnd:Long = Int.MaxValue
  var blockList:Seq[Long] = Seq() // special option to read only the list of blocks
  var blockListIndex = 0
    
  def setList(blocks:Seq[Long]) = this.synchronized {
    blockList = blocks
    blockEnd = blockList.last
  }
  
  def setFile(newStateFile:String) = this.synchronized {
    if(newStateFile.isBlank())
      this
    else
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
      if(blockList.size > 0) {
        // if list is specified, init it
        blockListIndex = 0
        this.current = blockList(blockListIndex)
        this.blockStart = blockList(blockListIndex)
        this.blockEnd = blockList.last
      } else {
        this.current = blockStart
        this.blockStart = blockStart
        this.blockEnd = blockEnd
      }
    }    
  }

  def set(current:Long) = this.synchronized {
    this.current = current    
  }

  def get() = this.synchronized {
    current
  }

  def next() = this.synchronized {
    if(blockList.size > 0) {
      if(blockListIndex >= blockList.size && blockListIndex >= blockEnd) {
        // beyond, should not come here
        -1L
      } else {
        current = blockList(blockListIndex)
        blockListIndex = blockListIndex + 1
        current
      }
      
    } else
      current + 1
  }

  def commit(block:Long) = this.synchronized {
    if(blockList.size > 0) {
      // don't change
    } else {
      current = block + 1
    }
    write(current)
  }

}


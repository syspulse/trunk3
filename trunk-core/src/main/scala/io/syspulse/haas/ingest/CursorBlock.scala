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
      try {
        os.read(os.Path(stateFile,os.pwd))
      } catch {
        case e:Exception =>
          log.warn(s"failed to read cursor: ${stateFile}: ${e.getMessage}")
          ""
      }
    }

    def write(current:Long) = this.synchronized {
      try {
        os.write.over(os.Path(stateFile,os.pwd),current.toString)    
      } catch {
        case e:Exception =>
          log.warn(s"failed to write cursor: ${stateFile}: ${e.getMessage}")
      }
    }
  }
  

  override def toString() = if(blockList.size > 0)
    s"${current} [${blockList.mkString(",")}] : ${blockEnd} (${file})"
    else
    s"${current} [${blockStart} : ${blockEnd}] (${file})"

  private var cursor = CursorFile(file)
  private var current:Long = 0
  private var lastBlock:Long = 0
  var blockStart:Long = 0
  var blockEnd:Long = Int.MaxValue
  var blockList:Seq[Long] = Seq() // special option to read only the list of blocks
  var blockListIndex = 0
    
  def setList(blocks:Seq[Long]) = this.synchronized {
    blockList = blocks.sorted
    blockEnd = blockList.last
  }

  def getList() = this.synchronized {
    if(blockList.size > 0) {
      // take list taking into ccount index
      blockList.drop(blockListIndex)
    } else
      Seq()    
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
        
        // ATTENTION: This logic may change
        this.blockEnd = blockList.last
        //this.blockEnd = blockEnd
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

  def last():Long = this.synchronized {
    lastBlock
  }

  def commit(block:Long) = this.synchronized {
    lastBlock = block
    if(blockList.size > 0) {
      current = block
      blockListIndex = blockList.indexOf(block) + 1
    } else {
      current = block + 1
    }
    write(current)
  }

}


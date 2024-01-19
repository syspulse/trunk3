package io.syspulse.haas.ingest.eth.flow.lake

import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import com.github.tototoshi.csv._

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._

import spray.json._
import DefaultJsonProtocol._
import java.util.concurrent.TimeUnit

import io.syspulse.haas.serde._
import io.syspulse.haas.core.{ Block, Transaction, TokenTransfer, Event, Tx }

import io.syspulse.haas.ingest.eth.EthURI

import io.syspulse.haas.ingest.Decoder

trait LakeDecoder[T] extends Decoder[T,Block,Transaction,TokenTransfer,Event,Tx] {

  protected val log = Logger(s"${this}")
  
  import BlockJson._
  import TransactionJson._
  import TokenTransferJson._
  import EventJson._
  import TxJson._

  def parseBlock(data:String):Seq[Block] = {
    if(data.isEmpty()) return Seq()

    try {
      // check it is JSON
      if(data.stripLeading().startsWith("{")) {
        val block = data.parseJson.convertTo[Block]
        
        val ts = block.ts        
        Seq(block)

      } else {
        // assume CSV
        // ignore header
        // number,hash,parent_hash,nonce,sha3_uncles,logs_bloom,transactions_root,state_root,receipts_root,miner,difficulty,total_difficulty,size,extra_data,gas_limit,gas_used,timestamp,transaction_count,base_fee_per_gas
        if(data.stripLeading().startsWith("number")) {
          Seq.empty
        } else {
          val block = data.split(",",-1).toList match {
            case number :: hash :: parent_hash :: nonce :: sha3_uncles :: logs_bloom :: 
                 transactions_root :: state_root :: receipts_root :: 
                 miner :: difficulty :: total_difficulty :: 
                 size :: extra_data :: gas_used :: gas_limit :: 
                 timestamp :: transaction_count :: base_fee_per_gas :: Nil =>
                
                 val ts = timestamp.toLong
                 //latestTs.set(ts * 1000L)

                 Seq(Block(
                    number.toLong,
                    hash,parent_hash, nonce, 
                    sha3_uncles,logs_bloom,
                    transactions_root,state_root, receipts_root,
                    miner, BigInt(difficulty), BigInt(total_difficulty),
                    size.toLong,
                    extra_data,
                    gas_limit.toLong,gas_used.toLong,

                    ts,
                    transaction_count.toLong,
                    OptionEmpty(base_fee_per_gas).map(_.toLong)
                  ))            
                  
            case _ => 
              log.error(s"failed to parse: '${data}'")
              Seq()
          }
          block
        }
      }
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }

  def parseTransaction(data:String):Seq[Transaction] = {
    if(data.isEmpty()) return Seq()

    try {
      // check it is JSON
      if(data.stripLeading().startsWith("{")) {
        val tx = data.parseJson.convertTo[Transaction]
                        
        Seq(tx)
      } else {
        if(data.stripLeading().startsWith("hash")) {
          Seq.empty
        } else {
          val tx = data.split(",",-1).toList match {
            // New EIP-1155 Transaction Type
            case block_timestamp :: transaction_index :: hash :: block_number :: from_address :: to_address :: 
                 gas :: gas_price :: input :: value :: 
                 nonce :: max_fee_per_gas :: max_priority_fee_per_gas :: transaction_type :: 
                 receipt_cumulative_gas_used :: receipt_gas_used :: receipt_contract_address :: 
                 receipt_root :: receipt_status :: receipt_effective_gas_price :: Nil =>
                
                 val ts = block_timestamp.toLong
                 //latestTs.set(ts * 1000L)

                 Seq(Transaction(
                    ts,
                    transaction_index.toInt,
                    hash,
                    block_number.toLong,
                    from_address.toLowerCase(),
                    OptionEmpty(to_address.toLowerCase()),
                    gas.toLong,
                    BigInt(gas_price),
                    input,
                    BigInt(value),

                    nonce.toLong,
                    OptionEmpty(max_fee_per_gas).map(BigInt(_)),
                    OptionEmpty(max_priority_fee_per_gas).map(BigInt(_)), 
                    
                    OptionEmpty(transaction_type).map(_.toInt), 

                    receipt_cumulative_gas_used.toLong, 
                    receipt_gas_used.toLong, 
                    OptionEmpty(receipt_contract_address), 
                    OptionEmpty(receipt_root), 
                    OptionEmpty(receipt_status).map(_.toInt),
                    OptionEmpty(receipt_effective_gas_price).map(BigInt(_))
                  ))
            
            case _ => 
              log.error(s"failed to parse: '${data}'")
              Seq()
          }
          tx
        }
      }
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }

  def parseTx(data:String):Seq[Tx] = {
    if(data.isEmpty()) return Seq()

    try {
      // check it is JSON
      if(data.stripLeading().startsWith("{")) {
        val tx = data.parseJson.convertTo[Tx]
                        
        Seq(tx)
      } else {
        // CSV is not supported !
        log.error(s"CSV is not supported for Tx: '${data}'")
        Seq()
      }
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }
  
  def parseTokenTransfer(data:String):Seq[TokenTransfer] = {
    if(data.isEmpty()) return Seq()

    try {
      // check it is JSON
      if(data.stripLeading().startsWith("{")) {
        val tt = data.parseJson.convertTo[TokenTransfer]
        
        Seq(tt)

      } else {
        // assume CSV
        // ignore header
        // 
        if(data.stripLeading().startsWith("token_address")) {
          Seq.empty
        } else {
          val tt = data.split(",",-1).toList match {
            case block_timestamp :: block_number :: tokenAddress :: from_address :: to_address :: value :: 
                 transaction_hash :: log_index :: Nil =>
            
              // ATTENTION: Stupid ethereum-etl insert '\r' !
              val ts = block_timestamp.trim.toLong
              //latestTs.set(ts * 1000L)

              Seq( TokenTransfer(
                ts,block_number.toLong,tokenAddress,from_address,to_address,BigInt(value),
                transaction_hash,log_index.toInt
              ))
            case _ => 
              log.error(s"failed to parse: '${data}'")
              Seq()
          }
          tt
        }
      }
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }

  
  def parseEventLog(data:String):Seq[Event] = {
    if(data.isEmpty()) return Seq()

    try {
      // check it is JSON
      if(data.stripLeading().startsWith("{")) {
        val tt = data.parseJson.convertTo[Event]
        
        Seq(tt)

      } else {
        // log_index,transaction_hash,transaction_index,block_hash,block_number,block_timestamp,address,data,topics
        // topics is the list: ,"0xe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c,0x000000000000000000000000e33c8e3a0d14a81f0dd7e174830089e82f65fc85"

        if(data.stripLeading().startsWith("log_index")) {
          Seq.empty
        } else {
          val tt = CSVParser.parse(data,'\\',',','\"') match {
            case Some(d) => d match {
              case block_timestamp :: block_number :: address :: data :: transaction_hash ::
                   topics :: log_index :: transaction_index :: Nil =>
                  
                val ts = block_timestamp.trim.toLong
                //latestTs.set(ts * 1000L)

                Seq(Event(
                  ts,
                  block_number.toLong,                  
                  address,
                  data,
                  transaction_hash,
                  topics.split(",").toList,                  
                  log_index.toInt,
                  transaction_index.toInt,
                ))
              case _ => 
                log.error(s"failed to parse: '${data}'")
                Seq()
            }

            case _ => 
              log.error(s"failed to parse: '${data}'")
              Seq()
          }
          tt
        }
      }
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }

}

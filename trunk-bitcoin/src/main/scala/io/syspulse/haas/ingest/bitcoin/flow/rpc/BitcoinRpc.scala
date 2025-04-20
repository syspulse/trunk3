package io.syspulse.haas.ingest.bitcoin.flow.rpc

import com.typesafe.scalalogging.Logger
import spray.json._
import DefaultJsonProtocol._

import io.syspulse.skel.Ingestable
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{Try,Success,Failure}

import io.syspulse.haas.ingest.bitcoin.{Block,Tx}
import io.syspulse.haas.ingest.bitcoin.BitcoinURI

case class BitcoinRpcRequest(
  jsonrpc: String = "1.0",
  id: String = "rpc",
  method: String,
  params: Seq[JsValue]
)

case class BitcoinRpcResponse(
  result: JsValue,
  error: Option[JsValue],
  id: String
)

// Script Signature
case class RpcScriptSig(
  asm: String,                   // Assembly representation
  hex: String                    // Hex representation
)

// Block Transaction Input (vin)
case class RpcTransactionInput(
  txid: Option[String],          // Previous tx hash
  vout: Option[Int],            // Output index in previous tx
  coinbase: Option[String],      // Coinbase data (only for coinbase txs)
  scriptSig: Option[RpcScriptSig],  // Script signature
  txinwitness: Option[Seq[String]], // Witness data
  sequence: Long                 // Sequence number
)

// Script Public Key
case class RpcScriptPubKey(
  asm: String,                  // Assembly representation
  desc: String,                 // Descriptor
  hex: String,                  // Hex representation
  address: Option[String],      // Bitcoin address
  `type`: String               // Script type
)

// Transaction Output (vout)
case class RpcTransactionOutput(
  value: BigInt,            // Value in BTC
  n: Int,                       // Output index
  scriptPubKey: RpcScriptPubKey    // Output script
)

// Transaction
case class RpcTransaction(
  txid: String,                 // Transaction ID
  hash: String,                 // Transaction hash
  version: Int,                 // Version number
  size: Int,                    // Size in bytes
  vsize: Int,                   // Virtual size
  weight: Int,                  // Weight
  locktime: Long,               // Lock time
  vin: Seq[RpcTransactionInput],   // Inputs
  vout: Seq[RpcTransactionOutput], // Outputs
  fee: Option[BigInt]           // Transaction fee (if available)
) extends Ingestable {
  def id = txid
  
  def getToAddresses(): Seq[String] = {
    vout.flatMap(_.scriptPubKey.address)
  }

  def isCoinbase(): Boolean = {
    vin.exists(_.coinbase.isDefined)
  }

  def getTotalOutputValue(): BigInt = {
    vout.map(_.value).sum
  }

  def getOutputsWithAddresses(): Seq[(String, BigInt)] = {
    vout.flatMap(out => out.scriptPubKey.address.map(addr => (addr, out.value)))
  }
}

// Block
case class RpcBlock(
  hash: String,                 // Block hash
  confirmations: Int,           // Number of confirmations
  height: Int,                  // Block height
  version: Long,                // Version
  versionHex: String,           // Version in hex
  merkleroot: String,           // Merkle root
  time: Long,                   // Block time
  mediantime: Long,            // Median time
  nonce: Long,                 // Nonce
  bits: String,                // Bits
  difficulty: BigInt,          // Mining difficulty
  chainwork: String,           // Chain work
  nTx: Int,                    // Number of transactions
  previousblockhash: String,   // Previous block hash
  nextblockhash: String,       // Next block hash
  strippedsize: Int,          // Stripped size
  size: Int,                   // Block size
  weight: Int,                 // Block weight
  tx: Seq[RpcTransaction]         // Transactions
) extends Ingestable

// Blockchain Info Response
case class RpcBlockchainInfo(
  chain: String,                  // Current network name (main, test, regtest)
  blocks: Int,                    // Current number of blocks
  headers: Int,                   // Current number of headers
  bestblockhash: String,         // Hash of the currently best block
  difficulty: BigInt,            // Current difficulty
  mediantime: Long,              // Median time for the current best block
  verificationprogress: Double,   // Estimate of verification progress [0..1]
  initialblockdownload: Boolean,  // Whether node is in initial block download mode
  chainwork: String,             // Total amount of work in active chain, in hexadecimal
  size_on_disk: Long,            // Estimated size of the block and undo files
  pruned: Boolean                // Whether the blocks are subject to pruning
)

object RpcJsonProtocol extends DefaultJsonProtocol {
  // Format definitions for nested objects first
  implicit val scriptSigFormat: RootJsonFormat[RpcScriptSig] = jsonFormat2(RpcScriptSig)
  implicit val scriptPubKeyFormat: RootJsonFormat[RpcScriptPubKey] = jsonFormat5(RpcScriptPubKey)
  
  // Format for TransactionInput with optional fields
  implicit val transactionInputFormat: RootJsonFormat[RpcTransactionInput] = jsonFormat6(RpcTransactionInput)

  // Format for TransactionOutput
  implicit val transactionOutputFormat: RootJsonFormat[RpcTransactionOutput] = jsonFormat3(RpcTransactionOutput)

  // Format for Transaction with optional fee field
  implicit val transactionFormat: RootJsonFormat[RpcTransaction] = jsonFormat10(RpcTransaction)
  
  // Format for Block
  implicit val blockFormat: RootJsonFormat[RpcBlock] = jsonFormat19(RpcBlock)

  implicit val rpcRequestFormat: RootJsonFormat[BitcoinRpcRequest] = jsonFormat4(BitcoinRpcRequest.apply)
  implicit val rpcResponseFormat: RootJsonFormat[BitcoinRpcResponse] = jsonFormat3(BitcoinRpcResponse.apply)

  implicit val blockchainInfoFormat: RootJsonFormat[RpcBlockchainInfo] = jsonFormat11(RpcBlockchainInfo)
}


class BitcoinRpc(uri: String)(implicit system: ActorSystem) {  
  val log = Logger(getClass)

  import RpcJsonProtocol._
  import system.dispatcher

  val bitcoinUri = BitcoinURI(uri)

  private val http = Http()
  private val credentials = BasicHttpCredentials(bitcoinUri.username, bitcoinUri.password)
  private val timeout = 30.seconds  

  private def rpcCall(method: String, params: Seq[JsValue]): Future[JsValue] = {
    val request = BitcoinRpcRequest(method = method, params = params)
    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = bitcoinUri.url,
      entity = HttpEntity(ContentTypes.`application/json`, request.toJson.toString)
    ).addCredentials(credentials)

    http.singleRequest(httpRequest)
      .flatMap { response =>
        if (response.status.isSuccess()) {
          Unmarshal(response.entity).to[String]
            .map(_.parseJson.convertTo[BitcoinRpcResponse])
            .map { rpcResponse =>
              rpcResponse.error match {
                case Some(error) => throw new Exception(s"RPC error: ${error.toString}")
                case None => rpcResponse.result
              }
            }
        } else {
          throw new Exception(s"HTTP error: ${response.status}")
        }
      }
      .recover { 
        case e: Exception => 
          log.error(s"RPC call failed: ${e.getMessage}")
          throw e
      }
  }

  def getTransaction(txid: String): Future[RpcTransaction] = {
    rpcCall("getrawtransaction", Seq(JsString(txid), JsNumber(1))).map(_.convertTo[RpcTransaction])
  }

  def getBlock(hash: String): Future[RpcBlock] = {
    rpcCall("getblock", Seq(JsString(hash), JsNumber(2))).map(_.convertTo[RpcBlock])
  }

  def getBlockByHeight(height: Int): Future[RpcBlock] = {
    rpcCall("getblockhash", Seq(JsNumber(height)))
      .map(_.convertTo[String])
      .flatMap(hash => getBlock(hash))
  }

  def getBlockchainInfo(): Future[RpcBlockchainInfo] = {
    rpcCall("getblockchaininfo", Seq())
      .map(_.convertTo[RpcBlockchainInfo])      
  }

  def getLatestBlock(): Future[(Long,String)] = {
    getBlockchainInfo()
      .map(info => (info.blocks,info.bestblockhash))
  }


  def parseBlock(data: String): Try[RpcBlock] = {
    Try {
      val json = data.parseJson
      json match {
        case obj: JsObject =>
          // Handle both raw block data and RPC response format
          val blockJson = if(obj.fields.contains("result")) {
            obj.fields("result")  // RPC response format
          } else {
            json  // Raw block data
          }
          blockJson.convertTo[RpcBlock]
        case _ =>
          throw new Exception(s"Invalid JSON format: '${data}'")
      }
    }
  }
 

  def decodeBlock(data: String): Seq[Tx] = {
    try {
      val block = parseBlock(data) match {
        case Success(b) => b
        case Failure(e) => 
          log.error(s"failed to parse block: ${e.getMessage}")
          return Seq.empty
      }

      val ts = block.time * 1000L

      val b = Block(
        ts = ts,
        i = block.height,
        hash = block.hash,
        phash = block.previousblockhash,        
      )
      
      block.tx.view.zipWithIndex.map { case (t,i) => {
        // For coinbase transactions, use "COINBASE" as from address
        val from = if(t.isCoinbase()) {
          "COINBASE"
        } else {
          t.vin.headOption.flatMap(_.txid).getOrElse("UNKNOWN")
        }

        // Get first output address, fallback to "UNKNOWN" if not available
        val to = t.vout.headOption.flatMap(_.scriptPubKey.address).getOrElse("UNKNOWN")
        
        Tx(
          ts = ts,
          txid = t.txid,
          hash = t.hash,
          from = from,
          to = to,
          v = t.vout.headOption.map(_.value).getOrElse(BigInt(0)),
          fee = t.fee,
          block = b,
          i = Some(i)
        )
      }}.toIndexedSeq  // Convert view back to concrete sequence
    } catch {
      case e: Exception =>
        log.error(s"failed to decode block: ${e.getMessage}")
        Seq.empty
    }
  }
}

package io.haas.ingest.solana.flow.rpc

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import spray.json._

import io.haas.ingest.Config
import io.haas.ingest.solana.flow.rpc.SolanaRpcJson._
import io.haas.ingest.solana.Transaction

class SolanaRpcParsingSpec extends AnyWordSpec with Matchers {

  "SolanaRpcJson" should {
    
    "parse error responses from RSP.json" in {
      val text = scala.io.Source.fromResource("RSP.json").mkString
      val js = text.parseJson

      js match {
        case JsArray(items) =>
          // The goal is to ensure spray-json can deserialize the payload without crashing.
          items.foreach { item =>
            try {
              item.convertTo[RpcBlockResult]
            } catch {
              case e: spray.json.DeserializationException =>
                // Make failures actionable: include the deserialization message and where it breaks.
                val top = e.getStackTrace.take(5).mkString("\n")
                val debug = item match {
                  case jsObj: JsObject =>
                    val res = jsObj.fields.get("result")
                    val stages = List(
                      ("RpcBlockResult", () => item.convertTo[RpcBlockResult]),
                      ("RpcBlock", () => res.get.convertTo[RpcBlock]),
                      ("RpcTransaction(first)", () => res.get.asJsObject.fields("transactions").convertTo[JsArray].elements.head.convertTo[RpcTransaction]),
                      ("RpcTransaction(find-fail)", () => {
                        val txs = res.get.asJsObject.fields("transactions").convertTo[JsArray].elements
                        var failure: Option[String] = None

                        for ((tx, idx) <- txs.zipWithIndex if failure.isEmpty) {
                          try {
                            tx.convertTo[RpcTransaction]
                          } catch {
                            case ex: spray.json.DeserializationException =>
                              val txObj = tx.asJsObject
                              val metaDebug =
                                try {
                                  txObj.fields("meta").convertTo[RpcMeta]
                                  "meta: OK"
                                } catch {
                                  case ex2: spray.json.DeserializationException =>
                                    val metaJs = txObj.fields("meta").asJsObject
                                    def attempt[A](label: String)(f: => A): String =
                                      try {
                                        f
                                        s"$label: OK"
                                      } catch {
                                        case ex3: spray.json.DeserializationException =>
                                          s"$label: FAIL(${ex3.getMessage})"
                                      }

                                    val details = List(
                                      attempt("computeUnitsConsumed") {
                                        metaJs.fields("computeUnitsConsumed").convertTo[Long]
                                      },
                                      attempt("err") { metaJs.fields.get("err").map(_.convertTo[Option[JsValue]]).getOrElse(None) },
                                      attempt("fee") { metaJs.fields("fee").convertTo[Long] },
                                      attempt("innerInstructions") { metaJs.fields("innerInstructions").convertTo[Option[Array[RpcInnerInstruction]]] },
                                      attempt("loadedAddresses") { metaJs.fields("loadedAddresses").convertTo[RpcLoadedAddresses] },
                                      attempt("logMessages") { metaJs.fields("logMessages").convertTo[Option[Array[String]]] },
                                      attempt("postBalances") { metaJs.fields("postBalances").convertTo[Array[Long]] },
                                      attempt("postTokenBalances") { metaJs.fields("postTokenBalances").convertTo[Array[RpcPostTokenBalance]] },
                                      attempt("preTokenBalances") { metaJs.fields("preTokenBalances").convertTo[Array[RpcPostTokenBalance]] },
                                      attempt("rewards") { metaJs.fields.get("rewards").map(_.convertTo[Option[Array[RpcReward]]]).getOrElse(None) },
                                      attempt("status") { metaJs.fields("status").convertTo[RpcStatus] }
                                    ).mkString(" | ")

                                    s"meta: FAIL: ${ex2.getMessage} | $details"
                                }
                              val txDebug =
                                try {
                                  txObj.fields("transaction").convertTo[RpcTransactionTx]
                                  "transaction: OK"
                                } catch {
                                  case ex2: spray.json.DeserializationException => s"transaction: FAIL: ${ex2.getMessage}"
                                }
                              val vDebug =
                                try {
                                  txObj.fields.get("version").map(_.convertTo[JsValue]).getOrElse(JsNull)
                                  "version: OK"
                                } catch {
                                  case ex2: spray.json.DeserializationException => s"version: FAIL: ${ex2.getMessage}"
                                }

                              failure = Some(s"idx=$idx msg=${ex.getMessage} | $metaDebug | $txDebug | $vDebug")
                          }
                        }

                        failure.getOrElse("all-ok")
                      })
                    )
                    stages
                      .map { case (name, f) =>
                        try {
                          val v = f()
                          s"$name: OK: ${Option(v).map(_.toString).getOrElse("")}".trim
                        } catch {
                          case ex: spray.json.DeserializationException =>
                            s"$name: FAILED: ${ex.getMessage}"
                        }
                      }
                      .mkString("\n  ", "\n  ", "")
                  case _ =>
                    "item is not a JsObject"
                }

                fail(s"Failed to parse one RSP item: ${e.getMessage}\n${e.toString}\n${top}\n$debug")
            }
          }

        case other =>
          fail(s"RSP.json must contain a JSON array, got: ${other.getClass.getName}")
      }
    }    

    "parse SOL-408538997 transactions into 1213 Transaction objects" in {
      val text = scala.io.Source.fromResource("SOL-408538997.json").mkString
      val blockResult = text.parseJson.convertTo[RpcBlockResult]

      val config = Config(feed = "https://rpc.test", output = "null://")
      val pipeline = new PipelineTransaction(config)

      val txs: Seq[Transaction] = blockResult.result.toSeq.flatMap(pipeline.transform)
      txs.size should be(1213)
    }

    "parse SOL-408538997-jsonparsed transactions into 1213 Transaction objects" in {
      val text = scala.io.Source.fromResource("SOL-408538997-jsonparsed.json").mkString
      val blockResult = text.parseJson.convertTo[RpcBlockResult]

      val config = Config(feed = "https://rpc.test", output = "null://")
      val pipeline = new PipelineTransaction(config)

      val txs: Seq[Transaction] = blockResult.result.toSeq.flatMap(pipeline.transform)
      txs.size should be(1213)
    }
  }
}


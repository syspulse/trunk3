package io.haas.ingest.solana.flow.rpc

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import spray.json._
import io.haas.ingest.Config
import io.haas.ingest.solana.flow.rpc.SolanaRpcJson._
import io.haas.ingest.solana.Transaction
import io.haas.ingest.solana.SolanaJson._

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
                      ("RpcTransaction(first)", () => res.get.asJsObject.fields("transactions").asInstanceOf[JsArray].elements.head.convertTo[RpcTransaction]),
                      ("RpcTransaction(find-fail)", () => {
                        val txs = res.get.asJsObject.fields("transactions").asInstanceOf[JsArray].elements
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
                                        metaJs.fields("computeUnitsConsumed") match {
                                          case JsNumber(n) => n.toLong
                                          case other => deserializationError(s"Expected JsNumber for computeUnitsConsumed but got: $other")
                                        }
                                      },
                                      attempt("err") { metaJs.fields.get("err").getOrElse(JsNull) },
                                      attempt("fee") {
                                        metaJs.fields("fee") match {
                                          case JsNumber(n) => n.toLong
                                          case other => deserializationError(s"Expected JsNumber for fee but got: $other")
                                        }
                                      },
                                      attempt("innerInstructions") { metaJs.fields.getOrElse("innerInstructions", JsNull) },
                                      attempt("loadedAddresses") { metaJs.fields("loadedAddresses").convertTo[RpcLoadedAddresses] },
                                      attempt("logMessages") { metaJs.fields.getOrElse("logMessages", JsNull) },
                                      attempt("postBalances") { metaJs.fields.getOrElse("postBalances", JsNull) },
                                      attempt("postTokenBalances") { metaJs.fields.getOrElse("postTokenBalances", JsNull) },
                                      attempt("preTokenBalances") { metaJs.fields.getOrElse("preTokenBalances", JsNull) },
                                      attempt("rewards") { metaJs.fields.get("rewards").getOrElse(JsNull) },
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
                              val vDebug = "version: OK"

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

    "parse SOL-408538997-jsonparsed transactions into 1213 Transaction objects (primary)" in {
      val text = scala.io.Source.fromResource("SOL-408538997-jsonparsed.json").mkString
      val blockResult = text.parseJson.convertTo[RpcBlockResult]

      val config = Config(feed = "https://rpc.test", output = "null://")
      val pipeline = new PipelineTransaction(config)

      val txs: Seq[Transaction] = blockResult.result.toSeq.flatMap(pipeline.transform)
      txs.size should be(1213)

      // jsonParsed must populate parsed instructions for at least some transactions.
      txs.exists(_.ins.exists(_.parsed.isDefined)) shouldBe true
    }

    "parse SOL-408538997 (non-jsonParsed) transactions into 1213 Transaction objects (compat)" in {
      val text = scala.io.Source.fromResource("SOL-408538997.json").mkString
      val blockResult = text.parseJson.convertTo[RpcBlockResult]

      val config = Config(feed = "https://rpc.test", output = "null://")
      val pipeline = new PipelineTransaction(config)

      val txs: Seq[Transaction] = blockResult.result.toSeq.flatMap(pipeline.transform)
      txs.size should be(1213)
    }

    "parse SOL-399514308-jsonparsed and find BAaNb... with 3 instructions" in {
      // This jsonParsed fixture can be large; keep it as a classpath resource.
      val text = scala.io.Source.fromResource("SOL-399514308-jsonparsed.json").mkString
      val blockResult = text.parseJson.convertTo[RpcBlockResult]

      val source = "BAaNbWqNcr358iXAYHzD5sjuADBCNpjwE137fZqYxdRp"

      val config = Config(feed = "https://rpc.test", output = "null://")
      val pipeline = new PipelineTransaction(config)

      val txs: Seq[Transaction] = blockResult.result.toSeq.flatMap(pipeline.transform)
      txs.nonEmpty shouldBe true

      val txOpt = txs.find { t =>
        t.ins.exists { i =>
          i.parsed.exists { p =>
            p.`type` == "transfer" &&
            p.info.exists(_.fields.get("source").contains(JsString(source)))
          }
        }
      }

      txOpt shouldBe defined

      val tx = txOpt.get
      tx.ins.length shouldBe 3

      // 2 ComputeBudget instructions (raw) + 1 system transfer (parsed)
      tx.ins(0).programId shouldBe Some("ComputeBudget111111111111111111111111111111")
      tx.ins(1).programId shouldBe Some("ComputeBudget111111111111111111111111111111")

      val i2 = tx.ins(2)
      i2.programId shouldBe Some("11111111111111111111111111111111")
      i2.parsed.map(_.`type`) shouldBe Some("transfer")
      i2.parsed.flatMap(_.info).map(_.fields.get("source")) shouldBe Some(Some(JsString(source)))
    }

    "roundtrip Transaction json (toJson -> parse -> convertTo) preserves key fields" in {
      val text = scala.io.Source.fromResource("SOL-399514308-jsonparsed.json").mkString
      val blockResult = text.parseJson.convertTo[RpcBlockResult]

      val source = "BAaNbWqNcr358iXAYHzD5sjuADBCNpjwE137fZqYxdRp"
      val config = Config(feed = "https://rpc.test", output = "null://")
      val pipeline = new PipelineTransaction(config)

      val txs: Seq[Transaction] = blockResult.result.toSeq.flatMap(pipeline.transform)

      val txOpt = txs.find { t =>
        t.ins.exists { i =>
          i.parsed.exists { p =>
            p.`type` == "transfer" &&
            p.info.exists(_.fields.get("source").contains(JsString(source)))
          }
        }
      }
      txOpt shouldBe defined

      val tx0 = txOpt.get
      val json0 = tx0.toJson.compactPrint

      // ensure instruction field names are compact
      json0.contains("\"accounts\"") shouldBe false
      json0.contains("\"programId\"") shouldBe false
      json0.contains("\"program\"") shouldBe false
      json0.contains("\"stackHeight\"") shouldBe false
      json0.contains("\"parsed\"") shouldBe false
      json0.contains("\"acc\"") shouldBe true
      json0.contains("\"pid\"") shouldBe true
      json0.contains("\"pro\"") shouldBe true // at least for system transfer
      json0.contains("\"sth\"") shouldBe true
      json0.contains("\"dat\"") shouldBe true
      json0.contains("\"typ\"") shouldBe true
      json0.contains("\"info\"") shouldBe true

      val tx1 = json0.parseJson.convertTo[Transaction]

      info(s"tx0: ${tx0.toJson.compactPrint}")

      tx1.sig shouldBe tx0.sig
      tx1.sts shouldBe tx0.sts
      tx1.ver shouldBe tx0.ver
      tx1.b shouldBe tx0.b
      tx1.h shouldBe tx0.h
      tx1.i shouldBe tx0.i

      tx1.acc.length shouldBe tx0.acc.length
      tx1.ins.length shouldBe tx0.ins.length

      tx1.ins.length shouldBe 3
      tx1.ins(0).programId shouldBe Some("ComputeBudget111111111111111111111111111111")
      tx1.ins(1).programId shouldBe Some("ComputeBudget111111111111111111111111111111")

      val i2 = tx1.ins(2)
      i2.programId shouldBe Some("11111111111111111111111111111111")
      i2.parsed.map(_.`type`) shouldBe Some("transfer")
      i2.parsed.flatMap(_.info).map(_.fields.get("source")) shouldBe Some(Some(JsString(source)))
    }
  }
}


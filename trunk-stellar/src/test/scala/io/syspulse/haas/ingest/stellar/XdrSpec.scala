package io.syspulse.haas.ingest.stellar

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues._

import org.stellar.sdk.xdr._
import org.stellar.sdk._

import io.syspulse.skel.util.Util

class XdrSpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  // https://laboratory.stellar.org/#xdr-viewer
  "Xdr" should {
    
    // "repeat SDK xdr test" in {
    //   val xdr ="AAAAAERmsKL73CyLV/HvjyQCERDXXpWE70Xhyb6MR5qPO3yQAAAAZAAIbkEAACD7AAAAAAAAAAN43bSwpXw8tSAhl7TBtQeOZTQAXwAAAAAAAAAAAAAAAAAAAAEAAAABAAAAAP1qe44j+i4uIT+arbD4QDQBt8ryEeJd7a0jskQ3nwDeAAAAAAAAAADdVhDVFrUiS/jPrRpblXY4bAW9u4hbRI2Hhw+2ATsFpQAAAAAtPWvAAAAAAAAAAAGPO3yQAAAAQHGWVHCBsjTyap/OY9JjPHmzWtN2Y2sL98aMERc/xJ3hcWz6kdQAwjlEhilItCyokDHCrvALZy3v/1TlaDqprA0=";
      
    //   val transactionEnvelope:TransactionEnvelope  = TransactionEnvelope.fromXdrBase64(xdr)
    //   val r = transactionEnvelope.getV0().getTx().getSeqNum().getSequenceNumber().getInt64()
    //   r should === (2373025265623291L)
    // }

    // // https://horizon-testnet.stellar.org/transactions/c53b04644f40e38582e262bedf0451250c8499b949ea19a0f82cd1528a3616a5/operations?cursor=\u0026limit=10\u0026order=asc
    // "parse Payment result_xdr" in {
    //   val xdr ="AAAAAAAAAGQAAAAAAAAAAQAAAAAAAAABAAAAAAAAAAA=";
      
    //   val transactionResult:TransactionResult = TransactionResult.fromXdrBase64(xdr)
    //   val r = transactionResult.getResult().getDiscriminant
    //   val f = transactionResult.getFeeCharged().getInt64()
    //   val rr = transactionResult.getResult().getResults()(0).getTr().getPaymentResult().getDiscriminant()

    //   val t = transactionResult.getResult().getResults()(0).getTr().getDiscriminant()

    //   //info(s">>> ${r},${f}:${t}=${rr}")
    //   t should === (OperationType.PAYMENT)
    //   r should === (TransactionResultCode.txSUCCESS)
    //   f should === (100)
    //   rr should === (PaymentResultCode.PAYMENT_SUCCESS)
    // }

    // "parse invokeHostFunction result_xdr" in {
    //   val xdr ="AAAAAAABnrwAAAAAAAAAAQAAAAAAAAAYAAAAAKSRuPmDvwcGCY2vZUHNRGYSxUrLvvy8n49frN+kdeA2AAAAAA==";
      
    //   val transactionResult:TransactionResult = TransactionResult.fromXdrBase64(xdr)
    //   val r = transactionResult.getResult().getDiscriminant
    //   val f = transactionResult.getFeeCharged().getInt64()
    //   val t = transactionResult.getResult().getResults()(0).getTr().getDiscriminant()

    //   val rr = transactionResult.getResult().getResults()(0).getTr().getInvokeHostFunctionResult().getDiscriminant()
    //   val s = Util.hex(transactionResult.getResult().getResults()(0).getTr().getInvokeHostFunctionResult().getSuccess().getHash())
    //   val s_xdr = transactionResult.getResult().getResults()(0).getTr().getInvokeHostFunctionResult().getSuccess().toXdrBase64()

    //   info(s">>> ${r},${f}:${t}=${rr},${s} (${s_xdr})")
    // }

    // "parse invokeHostFunction envelope_xdr" in {
    //   val xdr ="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";
      
    //   val envelope_xdr:TransactionEnvelope = TransactionEnvelope.fromXdrBase64(xdr)
    //   val oo = envelope_xdr.getV1().getTx().getOperations()
      
    //   val r  = oo.map(o => o.getBody().getDiscriminant()).toList

    //   info(s">>> ${oo.toSeq}: ${r}")
    // }

    "parse contract Events result_meta_xdr" in {
      val xdr ="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";
      
      val result_meta_xdr:TransactionMeta = TransactionMeta.fromXdrBase64(xdr)
      val v3 = result_meta_xdr.getV3()
      val soroban = v3.getSorobanMeta()

      info(s"result_meta_xdr = ${result_meta_xdr}")
      
      val events = soroban.getEvents()
      info(s"events = ${events}")

      val aa  = events.map(e => {
        val contract = e.getContractID()
        Address.fromContract(contract.getHash())
      }).toList
      info(s">>> ${aa}")

      val dd  = events.map(e => {
        val data = e.getBody().getV0().getData().toXdrBase64()
        data
      }).toList
      info(s">>> ${dd}")

      val tt  = events.map(e => {
        val tt = e.getBody().getV0().getTopics().map(_.toXdrBase64())
        tt.toList
      }).toList
      info(s">>> ${tt}")
      

    }

  }
  
}
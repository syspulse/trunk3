package io.syspulse.haas.ingest.stellar

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues._

import  org.stellar.sdk.xdr._

import io.syspulse.skel.util.Util

class XdrSpec extends AnyWordSpec with Matchers {
  //val testDir = this.getClass.getClassLoader.getResource(".").getPath + "../../../"

  // https://laboratory.stellar.org/#xdr-viewer
  "Xdr" should {
    
    "repeat SDK xdr test" in {
      val xdr ="AAAAAERmsKL73CyLV/HvjyQCERDXXpWE70Xhyb6MR5qPO3yQAAAAZAAIbkEAACD7AAAAAAAAAAN43bSwpXw8tSAhl7TBtQeOZTQAXwAAAAAAAAAAAAAAAAAAAAEAAAABAAAAAP1qe44j+i4uIT+arbD4QDQBt8ryEeJd7a0jskQ3nwDeAAAAAAAAAADdVhDVFrUiS/jPrRpblXY4bAW9u4hbRI2Hhw+2ATsFpQAAAAAtPWvAAAAAAAAAAAGPO3yQAAAAQHGWVHCBsjTyap/OY9JjPHmzWtN2Y2sL98aMERc/xJ3hcWz6kdQAwjlEhilItCyokDHCrvALZy3v/1TlaDqprA0=";
      
      val transactionEnvelope:TransactionEnvelope  = TransactionEnvelope.fromXdrBase64(xdr)
      val r = transactionEnvelope.getV0().getTx().getSeqNum().getSequenceNumber().getInt64()
      r should === (2373025265623291L)
    }

    // https://horizon-testnet.stellar.org/transactions/c53b04644f40e38582e262bedf0451250c8499b949ea19a0f82cd1528a3616a5/operations?cursor=\u0026limit=10\u0026order=asc
    "parse Payment result_xdr" in {
      val xdr ="AAAAAAAAAGQAAAAAAAAAAQAAAAAAAAABAAAAAAAAAAA=";
      
      val transactionResult:TransactionResult = TransactionResult.fromXdrBase64(xdr)
      val r = transactionResult.getResult().getDiscriminant
      val f = transactionResult.getFeeCharged().getInt64()
      val rr = transactionResult.getResult().getResults()(0).getTr().getPaymentResult().getDiscriminant()

      val t = transactionResult.getResult().getResults()(0).getTr().getDiscriminant()

      //info(s">>> ${r},${f}:${t}=${rr}")
      t should === (OperationType.PAYMENT)
      r should === (TransactionResultCode.txSUCCESS)
      f should === (100)
      rr should === (PaymentResultCode.PAYMENT_SUCCESS)
    }

    "parse invokeHostFunction result_xdr" in {
      val xdr ="AAAAAAABnrwAAAAAAAAAAQAAAAAAAAAYAAAAAKSRuPmDvwcGCY2vZUHNRGYSxUrLvvy8n49frN+kdeA2AAAAAA==";
      
      val transactionResult:TransactionResult = TransactionResult.fromXdrBase64(xdr)
      val r = transactionResult.getResult().getDiscriminant
      val f = transactionResult.getFeeCharged().getInt64()
      val t = transactionResult.getResult().getResults()(0).getTr().getDiscriminant()

      val rr = transactionResult.getResult().getResults()(0).getTr().getInvokeHostFunctionResult().getDiscriminant()
      val s = Util.hex(transactionResult.getResult().getResults()(0).getTr().getInvokeHostFunctionResult().getSuccess().getHash())
      val s_xdr = transactionResult.getResult().getResults()(0).getTr().getInvokeHostFunctionResult().getSuccess().toXdrBase64()

      info(s">>> ${r},${f}:${t}=${rr},${s} (${s_xdr})")
    }

  }
  
}
package sms.smpp.gateway.mt;

import org.apache.log4j.*;
import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

/**
 * Implements simple PDU listener which handles PDUs received from SMSC.
 * It puts the received requests into a queue and discards all received
 * responses. Requests then can be fetched (should be) from the queue by
 * calling to the method <code>getRequestEvent</code>.
 * @see Queue
 * @see ServerPDUEvent
 * @see ServerPDUEventListener
 * @see SmppObject
 */
public class SenderPDUEventListener
    extends SmppObject implements ServerPDUEventListener {
  /**
   * logger
   */
  private static Logger logger = Logger.getLogger("sms.smpp.gateway.mt");
  private Session session;

  public SenderPDUEventListener(Session session) {
    this.session = session;
  }

  public void handleEvent(ServerPDUEvent event) {
    PDU pdu = event.getPDU();

    if (pdu.isRequest()) {
      logger.info("async request received, but this is error. " + pdu.debugString());
    }
    else if (pdu.isResponse()) {
      if (pdu.getCommandId() == Data.SUBMIT_SM_RESP) {
        SubmitSMResp resp = (SubmitSMResp) pdu;

        logger.info("async request received " + resp.debugString());

        if (pdu.isOk()) {
        }
        else if (pdu.getCommandStatus() == Data.ESME_RSYSERR) {
        }
        else {
        }
      }
      else if (pdu.getCommandId() == Data.QUERY_SM_RESP) {
        QuerySMResp resp = (QuerySMResp) pdu;

        logger.info("async request receivede " + resp.debugString());
      }
      else if (pdu.getCommandId() == Data.ENQUIRE_LINK_RESP) {
        logger.debug("async request received " + pdu.debugString());
      }
      else {
        // Else
        logger.info("async response received " + pdu.debugString());
      }
    }
    else {
      logger.info("pdu of unknown class (not request nor " +
                  "response) received, discarding " +
                  pdu.debugString());
    }
  }
}

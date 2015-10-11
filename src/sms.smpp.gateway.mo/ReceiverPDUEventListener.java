package sms.smpp.gateway.mo;

import org.apache.log4j.*;
import com.logica.smpp.*;
import com.logica.smpp.pdu.*;
import com.logica.smpp.util.*;

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
public class ReceiverPDUEventListener
    extends SmppObject implements ServerPDUEventListener {

  /**
   * logger
   */
  private static Logger logger = Logger.getLogger("com.omnitel.sms.smpp.gateway.mo");
  Session session;
  Queue requestPdu = new Queue(Config.MAX_QUEUE_SIZE);

  public ReceiverPDUEventListener(Session session) {
    this.session = session;
  }

  public void handleEvent(ServerPDUEvent event) {
    PDU pdu = event.getPDU();

    if (pdu.isRequest()) {
      logger.info("async request received " + pdu.debugString());
      synchronized (requestPdu) {
        requestPdu.enqueue(pdu);
        requestPdu.notify();
      }
    }
    else if (pdu.isResponse()) {
      if (pdu.getCommandId() == Data.ENQUIRE_LINK_RESP) {
        logger.debug("async response received " + pdu.debugString());
      }
      else {
        logger.info("async response received " + pdu.debugString());
      }
    }
    else {
      logger.info("pdu of unknown class (not request nor response) received, discarding " + pdu.debugString());
    }
  }

  public PDU getRequestPdu(long timeout) {
    PDU pdu = null;

    synchronized (requestPdu) {
      if (requestPdu.isEmpty()) {
        try {
          requestPdu.wait(timeout);
        }
        catch (InterruptedException e) {
          // ignoring, actually this is what we're waiting for
        }
      }

      if (!requestPdu.isEmpty()) {
        pdu = (PDU) requestPdu.dequeue();
      }
    }

    return pdu;
  }

  public void notifyQueue() {
    synchronized (this.requestPdu) {
      this.requestPdu.notify();
    }
  }
}

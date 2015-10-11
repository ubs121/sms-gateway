
package sms.smpp.gateway.mo;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;

import org.apache.log4j.*;
import com.logica.smpp.*;
import com.logica.smpp.pdu.*;
import com.omnitel.util.*;

public class Client
    implements Runnable {

  private static Logger logger = Logger.getLogger("sms.smpp.gateway.mo");

  private String lineSeparator = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction(
      "line.separator"));
  private Thread thread;
  static Session session = null;

  ReceiverPDUEventListener pduListener = null;
  Vector rule = new Vector();
  long lastmodified = 0;

  public Client() {
    this.thread = new Thread(this);
    this.thread.start();
  }

  public void run() {

    while (true) {
      com.logica.smpp.pdu.PDU pdu = null;

      if (this.session == null || !this.session.isBound()) {
        this.smppBind();
      }
      else {
        if (this.checkModifiedRule()) {
          this.loadRule();
        }

        pdu = this.pduListener.getRequestPdu(Config.QUEUE_TIMEOUT);

        if (pdu != null) {
          if (pdu instanceof DeliverSM) {
            DeliverSM deliverSm = (DeliverSM) pdu;

            if (deliverSm.getEsmClass() == Data.SM_SMSC_DLV_RCPT_TYPE) {
              try {
                DeliverSMResp resp = this.smppReport(deliverSm);

                logger.debug(resp.debugString());
                this.session.respond(resp);
              }
              catch (Exception ex) {
                ex.printStackTrace();
                logger.info(ex);
              }
            }
            else {
              try {
                DeliverSMResp resp = this.smppDeliver(deliverSm);

                logger.info(resp.debugString());
                this.session.respond(resp);
              }
              catch (Exception ex) {
                ex.printStackTrace();
                logger.info(ex);
              }
            }
          }
          else {
            logger.info("Received request PDU:" + pdu.debugString());
          }
        }
        else {
          this.smppEnquireLink();
        }
      }
    }
  }

  private boolean checkModifiedRule() {
    File file = new File(Config.MO_RULE_PATH);

    if (file.lastModified() != this.lastmodified ) {
      this.lastmodified = file.lastModified();
      return true;
    }

    return false;
  }

  private void loadRule() {
    InputStream is = null;
    Properties props = new Properties();

    try {
      is = new FileInputStream(Config.MO_RULE_PATH);
      props.load(is);
    }
    catch (Exception e) {
      logger.error("Can't read the mo rule properties file.");
      return;
    }

    Enumeration propNames = props.propertyNames();
    Vector vt = new Vector();

    while (propNames.hasMoreElements()) {
      String name = (String) propNames.nextElement();

      if (name.endsWith(".prefix")) {
        String targetName = name.substring(0, name.lastIndexOf("."));
        String prefix = props.getProperty(targetName + ".prefix");
        String host = props.getProperty(targetName + ".host");
        String port = props.getProperty(targetName + ".port");
        String url = props.getProperty(targetName + ".url");
        Pattern pattern = Pattern.compile(prefix + "\\d*");
        Hashtable ht = new Hashtable();

        if (host == null || url == null) continue;

        if (port == null) port = "80";

        ht.put("pattern", pattern);
        ht.put("host", host);
        ht.put("port", port);
        ht.put("url", url);
        vt.add(ht);
      }
    }

    this.rule = vt;
  }

  private DeliverSMResp smppDeliver(DeliverSM pdu) {
    DeliverSMResp resp = (DeliverSMResp) pdu.getResponse();
    String srcAddr = pdu.getSourceAddr().getAddress();
    String destAddr = pdu.getDestAddr().getAddress();
    String text = pdu.getShortMessage();

    for (int i = 0; i < this.rule.size(); i++) {
      Hashtable ht = (Hashtable)this.rule.get(i);
      Pattern pattern = (Pattern)ht.get("pattern");
      String host = (String)ht.get("host");
      String port = (String)ht.get("port");
      String url = (String)ht.get("url");

      if (pattern.matcher(destAddr).matches()) {
        try {
//          url += "?srcAddr=" + srcAddr + "&destAddr=" + destAddr + "&text=" + URLEncoder.encode(text, "EUC_KR");
          url = url.replace("#1", srcAddr);
          url = url.replace("#2", destAddr);
          url = url.replace("#3", URLEncoder.encode(text, "EUC_KR"));

          URL u = new URL("http", host, Integer.parseInt(port), url);
          URLConnection urlCon = u.openConnection();

          urlCon.setConnectTimeout(2000);
          urlCon.setReadTimeout(2000);
          urlCon.connect();
          urlCon.getContent();
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }

        break;
      }
    }

    return resp;
  }

  private DeliverSMResp smppReport(DeliverSM pdu) {
    DeliverSMResp resp = (DeliverSMResp) pdu.getResponse();

    return resp;
  }

  private void smppBind() {
    try {
      BindRequest request = null;
      BindResponse response = null;
      TCPIPConnection connection = null;

      request = new BindReceiver();
      connection = new TCPIPConnection(Config.SMSC_ADDRESS, Config.SMSC_PORT);
      connection.setReceiveTimeout(20 * 1000);
      this.session = new Session(connection);

      // set values
      request.setSystemId(Config.SYSTEM_ID);
      request.setPassword(Config.PASSWORD);
      request.setSystemType(Config.SYSTEM_TYPE);
      request.setInterfaceVersion( (byte) 0x34);
      request.setAddressRange(new AddressRange(Config.TON, Config.NPI, Config.ADDRESS_RANGE));

      // send the request
      logger.info("Request " + request.debugString());
      this.pduListener = new ReceiverPDUEventListener(this.session);
      response = this.session.bind(request, this.pduListener);
      logger.info("Response " + response.debugString());
    }
    catch (Exception e) {
      e.printStackTrace();
      logger.info("Exception:" + e);
    }

    if (this.session == null || !this.session.isBound()) {
      try {
        this.thread.sleep(Config.SMS_RECOVER_TIME);
      }
      catch (InterruptedException ex) {
      }
    }
  }

  private void smppUnbind() {
    try {
      // send the request
      logger.info("Going to unbind.");
      this.session.getConnection().setCommsTimeout(1000);
      this.session.getConnection().setReceiveTimeout(1000);

      if (this.session.getReceiver().isReceiver()) {
        logger.info("It can take a while to stop the receiver.");
      }
      UnbindResp response = this.session.unbind();

      if (response != null) {
        logger.info("Unbind response " + response.debugString());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      logger.info("Unbind operation failed. " + e);
    }
    finally {
      this.session = null;
    }
  }

  private void smppEnquireLink() {
    try {
      EnquireLink request = new EnquireLink();
      EnquireLinkResp response;

      request.setSequenceNumber(0);
      logger.debug("Request " + request.debugString());
      this.session.enquireLink(request);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      this.smppUnbind();
    }
  }

  /**
   *
   * @param args
   */
  public static void main(String[] args) {
    logger.addAppender(new ConsoleAppender(new SimpleLayout()));

    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd");
    PrintStream ps = Loger.getLoger(Config.LOG_SERVER_PATH + "err.", formatter);

    if (ps != null) {
      System.setErr(ps);
    }

    try {
      DailyRollingFileAppender appender = new DailyRollingFileAppender(new UserLayout(new SimpleDateFormat("HH:mm:ss")),
          Config.LOG_SERVER_PATH + "log", "'.'yyyy-MM-dd");

      logger.addAppender(appender);
      logger.setLevel(Level.toLevel(Config.LOG_SERVER_LEVEL));
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }

    Client client = new Client();
  }
}

/*
 * Copyright (c) 2003 ubs121.
 *
 */

package sms.smpp.gateway.mt;

import java.io.*;
import java.text.*;

import org.apache.log4j.*;
import com.logica.smpp.*;
import com.logica.smpp.pdu.*;
import com.omnitel.util.*;

public class Client
    implements Runnable {

  private static Logger logger = Logger.getLogger("sms.smpp.gateway.mt");
  private String lineSeparator = (String) java.security.AccessController.
      doPrivileged(
          new sun.security.action.GetPropertyAction("line.separator"));
  private Thread thread;
  static Session session = null;
  SenderPDUEventListener pduListener = null;
  HttpServer httpServer = null;

  public Client() {
    this.thread = new Thread(this);
    this.thread.start();
    this.httpServer = new HttpServer(this);
  }

  public void run() {
    while (true) {
      if (this.session == null || !this.session.isBound()) {
        this.smppBind();
      }
      else {
        try {
          this.thread.sleep(Config.LINK_TIME);
        }
        catch (InterruptedException ex) {
        }

        this.smppEnquireLink();
      }
    }
  }

  private void smppBind() {
    try {
      BindRequest request = null;
      BindResponse response = null;
      TCPIPConnection connection = null;

      request = new BindTransmitter();
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
      this.pduListener = new SenderPDUEventListener(this.session);
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

  protected synchronized void smppSubmit(SubmitSM request) throws Exception {
    logger.info("Request " + request.debugString());

    this.session.submit(request);
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
      DailyRollingFileAppender appender = new DailyRollingFileAppender(new
          UserLayout(new SimpleDateFormat("HH:mm:ss")),
          Config.LOG_SERVER_PATH + "log",
          "'.'yyyy-MM-dd");

      logger.addAppender(appender);
      logger.setLevel(Level.toLevel(Config.LOG_SERVER_LEVEL));
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }

    Client client = new Client();
  }
}

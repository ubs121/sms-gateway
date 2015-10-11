package sms.smpp.gateway.mt;

import java.io.*;
import java.net.*;

import org.apache.log4j.*;

public class HttpServer
    extends Thread {

  private static Logger logger = Logger.getLogger("sms.smpp.gateway.mt");
  /**
   * line separator
   */
  private String lineSeparator = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction(
      "line.separator"));

  private Thread thread;
  private ServerSocket serverSocket;
  private Client client;

  public HttpServer(Client client) {
    this.client = client;
    this.thread = new Thread(this);
    this.thread.start();
  }

  public void run() {
    try {
      this.openServerSocket();

      while (true) {
        Socket socket = this.serverSocket.accept();

        logger.debug("Connect from " + socket.getInetAddress().getHostAddress());

        new HttpWorker(this.client, socket);
      }
    }
    catch (IOException e) {
      if (this.serverSocket != null && !this.serverSocket.isClosed()) {
        e.printStackTrace();
      }
    }
  }

  private void openServerSocket() throws IOException {
    this.serverSocket = new ServerSocket(Config.HTTP_PORT);
    logger.info("Open port " + Config.HTTP_PORT);
  }

  private void closeServerSocket() {
    try {
      if (this.serverSocket != null) {
        this.serverSocket.close();
      }
    }
    catch (IOException e) {
    }
    finally {
      this.serverSocket = null;
    }
  }
}

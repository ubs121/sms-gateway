package sms.smpp.gateway.mt;

import java.io.*;
import java.net.*;
import java.util.*;

import com.logica.smpp.pdu.*;

public class HttpWorker
    extends Thread {
  private Socket socket;
  private Thread thread;
  private BufferedReader br;
  private BufferedWriter bw;
  private Client client;

  public HttpWorker(Client client, Socket socket) {
    this.client = client;
    this.socket = socket;
    this.thread = new Thread(this);
    this.thread.start();
  }

  public void run() {
    String sTmp = null, sUri = null;
    boolean bFirst = true;

    try {
      openSocket();

      while ( (sTmp = br.readLine()) != null) {
        if (bFirst) {
          sUri = sTmp;
        }

        bFirst = false;

        if (sTmp.equals("")) {
          break;
        }
      }

      if (sUri != null && sUri.toLowerCase().startsWith("get /sms_send")) {
        String sSender = null, sReceiver = null, sText = null;
        String[] saTmp = null;
        Hashtable htTmp = new Hashtable();

        //Get /sms_send?sender=97699119876&receiver=97699116789&text=this%20is%20test HTTP/1.0
        sTmp = sUri.substring("get /sms_send?".length());
        //sender=97699119876&receiver=97699116789&text=this%20is%20test
        sTmp = sTmp.substring(0, sTmp.lastIndexOf(" "));
        saTmp = sTmp.split("&");

        if (saTmp.length != 3) {
          throw new HttpResponceException("701");
        }

        for (int i = 0; i < saTmp.length; i++) {
          String[] saTmp2 = null;

          sTmp = saTmp[i];
          saTmp2 = sTmp.split("=");

          htTmp.put(saTmp2[0].toLowerCase(), saTmp2[1]);
        }

        sSender = (String) htTmp.get("sender");
        sReceiver = (String) htTmp.get("receiver");
        sText = (String) htTmp.get("text");

        if (sSender == null || sReceiver == null || sText == null) {
          throw new HttpResponceException("702");
        }

        sText = URLDecoder.decode(sText, "EUC_KR");

        this.sendSms(sSender, sReceiver, sText);

        try {
          bw.write("HTTP/1.1 200 OK");
          bw.newLine();
          bw.newLine();
          bw.flush();
        }
        catch (Exception e) {
        }
      }
    }
    catch (HttpResponceException ex) {
      //HTTP/1.1 200 OK
      try {
        bw.write("HTTP/1.1 " + ex + " OK");
        bw.newLine();
        bw.newLine();
        bw.flush();
      }
      catch (Exception e) {
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      try {
        bw.write("HTTP/1.1 700 OK");
        bw.newLine();
        bw.newLine();
        bw.flush();
      }
      catch (Exception e) {
      }
    }
    finally {
      closeSocket();
    }
  }

  private void sendSms(String sSender, String sReceiver, String sText) throws Exception {
    int nTotalPage = 0;
    SubmitSM request = new SubmitSM();
    Calendar cal = Calendar.getInstance();

    nTotalPage = (int) (sText.length() / 153) + ( (sText.length() % 153) == 0 ? 0 : 1);

    for (int i = 0; i < nTotalPage; i++) {
      request.setId( (int) System.currentTimeMillis());
      request.setSequenceNumber( (int) System.currentTimeMillis());

      if (nTotalPage == 1) {
        request.setShortMessage(sText);
      }
      else {
        byte[] bHeader = new byte[6];

        //05 00 03 15 02 01
        bHeader[0] = 0x05;
        bHeader[1] = 0x00;
        bHeader[2] = (byte) cal.get(Calendar.MINUTE);
        bHeader[3] = (byte) cal.get(Calendar.SECOND);
        bHeader[4] = (byte) nTotalPage;
        bHeader[5] = (byte) (i + 1);

        if (i == (nTotalPage - 1)) {
          byte[] bTmp = new byte[6 + sText.substring(i * 153).length()];

          System.arraycopy(bHeader, 0, bTmp, 0, 6);
          System.arraycopy(sText.substring(i * 153).getBytes(), 0, bTmp, 6, sText.substring(i * 153).length());

          request.setShortMessage(new String(bTmp));
        }
        else {
          byte[] bTmp = new byte[159];

          System.arraycopy(bHeader, 0, bTmp, 0, 6);
          System.arraycopy(sText.substring(i * 153, (i + 1) * 153).getBytes(), 0, bTmp, 6, 153);

          request.setShortMessage(new String(bTmp));
        }
      }

      request.setSourceAddr(Config.TON, Config.NPI, sSender);
      request.setDestAddr(Config.TON, Config.NPI, sReceiver);

      this.client.smppSubmit(request);
    }
  }

  private void openSocket() throws IOException {
    this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    this.bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
  }

  private void closeSocket() {
    try {
      this.br.close();
      this.bw.close();
      this.socket.close();
    }
    catch (Exception e) {
    }
    finally {
      this.br = null;
      this.bw = null;
      this.socket = null;
    }
  }
}

class HttpResponceException
    extends Exception {
  private String sResponceCode = null;

  public HttpResponceException(String sResponseCode) {
    this.sResponceCode = sResponseCode;
  }

  public String toString() {
    return this.sResponceCode;
  }
}

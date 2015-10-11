package sms.smpp.gateway.mt;

import java.io.*;

import com.util.*;

public class Config {

  /**
   * program full path
   */
  private static String fullPath = (String) java.security.AccessController.
      doPrivileged(
          new sun.security.action.GetPropertyAction("fullPath"));

  /**
   * property
   */
  private static MyProperties myProperty;
  /**
   * log path
   */
  public static String LOG_SERVER_PATH;
  /**
   * log level
   */
  public static String LOG_SERVER_LEVEL;
  public static int LINK_TIME;
  public static int SMS_PER_SECOND;
  public static int SMS_RECOVER_TIME;
  public static int ENQUIRELINK_TIME;
  /**
   * SMSC port
   */
  public static int SMSC_PORT;
  /**
   *
   */
  public static String SMSC_ADDRESS;
  public static String SYSTEM_ID;
  public static String PASSWORD;
  public static String SYSTEM_TYPE;
  public static byte TON;
  public static byte NPI;
  public static String ADDRESS_RANGE;
  public static int HTTP_PORT;

  static {
    String addr;

    if (fullPath == null) {
      fullPath = "..";
    }

    Config.myProperty = new MyProperties(fullPath + "/property/mt.properties");
    Config.LOG_SERVER_PATH = fullPath + Config.myProperty.getStringProperty("LOG_SERVER_PATH", "/log/mt/");
    Config.LOG_SERVER_LEVEL = Config.myProperty.getStringProperty("LOG_SERVER_LEVEL", "INFO");
    Config.LINK_TIME = Config.myProperty.getIntProperty("LINK_TIME", 30) * 1000;
    Config.SMS_PER_SECOND = Config.myProperty.getIntProperty("SMS_PER_SECOND", 10);
    Config.SMS_RECOVER_TIME = Config.myProperty.getIntProperty("SMS_RECOVER_TIME", 30) * 1000;
    Config.SMSC_PORT = Config.myProperty.getIntProperty("SMSC_PORT", 3702);

    Config.SMSC_ADDRESS = Config.myProperty.getStringProperty("SMSC_ADDR", "192.11.200.203");

    Config.SYSTEM_ID = Config.myProperty.getStringProperty("SYSTEM_ID", "id");
    Config.PASSWORD = Config.myProperty.getStringProperty("PASSWORD", "pwd");
    Config.SYSTEM_TYPE = Config.myProperty.getStringProperty("SYSTEM_TYPE", "app");
    Config.TON = (byte) Config.myProperty.getIntProperty("TON", 1);
    Config.NPI = (byte) Config.myProperty.getIntProperty("NPI", 1);
    Config.ADDRESS_RANGE = Config.myProperty.getStringProperty("ADDRESS_RANGE", "1");

    Config.ENQUIRELINK_TIME = Config.myProperty.getIntProperty("ENQUIRELINK_TIME", 10); // 10 min
    Config.HTTP_PORT = Config.myProperty.getIntProperty("HTTP_PORT", 80);
  }

  /**
   * Save string property
   * @param name property name
   * @param value property value
   * @throws IOException
   */
  public static void write(String name, String value) throws IOException {
    synchronized (Config.myProperty) {
      Config.myProperty.setStringProperty(name, value);
      Config.myProperty.save(name);
    }
  }

  /**
   * Save int property
   * @param name property name
   * @param value property value
   * @throws IOException
   */
  public static void write(String name, int value) throws IOException {
    write(name, Integer.toString(value));
  }
}

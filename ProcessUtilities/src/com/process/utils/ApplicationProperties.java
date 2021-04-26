package com.process.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApplicationProperties
{
  static private String propertiesFileName = System.getProperty("user.home") + File.separator + "cloudez-api.properties";
  static private Properties prop = new Properties();
  static
  {
    try
    {
      InputStream inputStream = new FileInputStream(new File(propertiesFileName));
      prop.load(inputStream);
    }
    catch (IOException ioe)
    {
      System.out.println("Error in Runner Application");
    }
  }

  public static String getProperties(String key)
  {
    return prop.getProperty(key);
  }
}

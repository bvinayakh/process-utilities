package com.process.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationProperties
{
  static private Logger log = LoggerFactory.getLogger(ApplicationProperties.class);
  static private String propertiesFileName = System.getProperty("user.home") + File.separator + "cloudez-api.properties";
  static private Properties prop = new Properties();
  static private String deploymentMode = System.getenv("aws_credential_type");

  static private boolean isPropertiesFile = false;

  static
  {
    if ((deploymentMode == null) || (!deploymentMode.equalsIgnoreCase("eks")))
    {
      log.info("Loading properties from ::: " + System.getProperty("user.home"));
      try
      {
        InputStream inputStream = new FileInputStream(new File(propertiesFileName));
        prop.load(inputStream);
        isPropertiesFile = true;
      }
      catch (IOException ioe)
      {
        log.info("Error reading properties. " + ioe.getMessage());
      }
    }
  }

  public static String getProperties(String key)
  {
    String property = null;
    if (isPropertiesFile) property = prop.getProperty(key);
    else
      property = System.getenv(key);
    if (property == null) log.debug("Property {" + key + "} not found");
    return property;
  }
}

package com.process.containers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import org.simpleyaml.exceptions.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.process.Runner;
import com.process.utils.ApplicationProperties;
import com.process.utils.JSONOM;

public class HelmCmd
{
  static private Logger log = LoggerFactory.getLogger(HelmCmd.class);

  private boolean useKubeconfig = false;
  private String helmBinaryLocation = ApplicationProperties.getProperties("helm_executable_location");

  private Runner runner = null;

  private JSONOM mapper = null;
  private ObjectNode parentNode = null;

  public HelmCmd()
  {
    mapper = new JSONOM();
    runner = new Runner();

    useKubeconfig = Boolean.valueOf(ApplicationProperties.getProperties("use_kubeconfig"));
    log.debug("kubeconfig is : " + useKubeconfig);
  }

  public JsonNode helmRepoUpdate() throws IOException, InterruptedException
  {
    StringBuffer updateCmd = new StringBuffer();
    updateCmd.append(helmBinaryLocation + " ");
    updateCmd.append("repo" + " ");
    updateCmd.append("update" + " ");
    return runner.runExec(updateCmd);
  }

  // read releases in the current helm chart
  public JsonNode helmList() throws IOException, InterruptedException
  {
    parentNode = mapper.createObjectNode();
    ObjectNode resultNode = mapper.createObjectNode();
    ObjectNode warningNode = mapper.createObjectNode();
    ObjectNode errorNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(helmBinaryLocation + " ");
    cmd.append("list" + " ");
    cmd.append("-a -A" + " ");
    cmd.append("-o" + " " + "json" + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config_file_location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();
    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      JsonNode root = mapper.readTree(decodedResult);
      if (root.isArray()) parentNode.putArray("Result").addPOJO(root);
      else
        parentNode.putPOJO("Result", resultNode);
    }
    if (warning.length() > 0)
    {
      String decodedWarning = new String(Base64.getDecoder().decode(warning));
      warningNode.putPOJO("Message", decodedWarning);
      parentNode.putPOJO("Warning", warningNode);
    }
    if (error.length() > 0)
    {
      String decodedError = new String(Base64.getDecoder().decode(error));
      errorNode.putPOJO("Message", decodedError);
      parentNode.putPOJO("Error", errorNode);
    }
    return parentNode;
  }

  public JsonNode helmUninstall(String releaseName, String namespace) throws IOException, InterruptedException
  {
    StringBuffer cmd = new StringBuffer();
    cmd.append(helmBinaryLocation + " ");
    cmd.append("uninstall" + " ");
    cmd.append(releaseName + " ");
    cmd.append("--namespace" + " " + namespace + " ");
    if (Boolean.valueOf(ApplicationProperties.getProperties("debug_enabled"))) cmd.append("--debug" + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config_file_location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    System.out.println("outputnode");

    String result = outputNode.get("Result").asText();
    System.out.println("result:" + result);
    String warning = outputNode.get("Warning").asText();
    System.out.println("warning:" + warning);
    String error = outputNode.get("Error").asText();
    parentNode = mapper.createObjectNode();

    if (result.length() > 0) parentNode.putPOJO("Result", mapper.writeValueAsString(new String(Base64.getDecoder().decode(result))));
    if (warning.length() > 0)
    {
      String decodedWarning = new String(Base64.getDecoder().decode(warning));
      ObjectNode warningNode = mapper.createObjectNode();
      warningNode.putPOJO("Message", decodedWarning);
      parentNode.putPOJO("Warning", warningNode);
    }
    if (error.length() > 0)
    {
      String decodedError = new String(Base64.getDecoder().decode(error));
      ObjectNode errorNode = mapper.createObjectNode();
      errorNode.putPOJO("Message", decodedError);
      parentNode.putPOJO("Error", errorNode);
    }

    return parentNode;
  }

  public JsonNode helmShowValues(String chartName) throws IOException, InterruptedException
  {
    helmRepoUpdate();

    ObjectNode resultNode = mapper.createObjectNode();
    ObjectNode warningNode = mapper.createObjectNode();
    ObjectNode errorNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(helmBinaryLocation + " ");
    cmd.append("show" + " ");
    cmd.append("values" + " ");
    cmd.append(chartName + " ");
    cmd.append("--insecure-skip-tls-verify" + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config_file_location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    parentNode = mapper.createObjectNode();
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();

    if (result.length() > 0)
    {
      System.out.println("Fetching values from chart:::" + chartName);
      String decodedResult = new String(Base64.getDecoder().decode(result));
      ObjectNode valuesNode = mapper.createObjectNode();
      Yaml yaml = new Yaml();
      InputStream inputStream = new ByteArrayInputStream(decodedResult.getBytes());
      Map<String, Object> obj = (Map<String, Object>) yaml.load(inputStream);
      Iterator<String> mapIterator = obj.keySet().iterator();
      while (mapIterator.hasNext())
      {
        String key = mapIterator.next();
        valuesNode.putPOJO(key, obj.get(key));
      }
      resultNode.putPOJO("Chart", chartName);
      resultNode.putPOJO("Values", valuesNode);
      parentNode.putPOJO("Result", resultNode);
    }
    if (warning.length() > 0)
    {
      String decodedWarning = new String(Base64.getDecoder().decode(warning));
      warningNode.putPOJO("Message", decodedWarning);
      parentNode.putPOJO("Warning", warningNode);
    }
    if (error.length() > 0)
    {
      String decodedError = new String(Base64.getDecoder().decode(error));
      errorNode.putPOJO("Message", decodedError);
      parentNode.putPOJO("Error", errorNode);
    }

    return parentNode;
  }


  /*
   * releaseName: Helm Release Name
   */
  public JsonNode helmUpgrade(String releaseName, String repoName, String chartName, String namespace, ArrayList<String> parameters)
      throws IOException, InterruptedException, InvalidConfigurationException
  {
    // update helm repo on the instance before installing/upgrading helm chart
    helmRepoUpdate();

    StringBuffer cmd = new StringBuffer();
    cmd.append(helmBinaryLocation + " ");
    cmd.append("upgrade" + " ");
    cmd.append("--install" + " ");
    cmd.append(releaseName + " ");
    cmd.append(repoName + "/" + chartName + " ");
    cmd.append("-f" + " " + releaseName + "-values.yaml" + " ");
    cmd.append("--namespace" + " " + namespace + " ");
    // adding command line parameters if present
    Iterator<String> parametersIterator = parameters.iterator();
    while (parametersIterator.hasNext())
    {
      String parameter = parametersIterator.next();
      cmd.append("--set" + " " + parameter + " ");
    }
    cmd.append("-o" + " " + "json" + " ");
    if (Boolean.valueOf(ApplicationProperties.getProperties("debug_enabled"))) cmd.append("--debug" + " ");

    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config_file_location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();
    parentNode = mapper.createObjectNode();

    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      JsonNode root = mapper.readTree(decodedResult);
      ObjectNode resultNode = mapper.createObjectNode();
      resultNode.putPOJO("Name", root.get("name"));
      JsonNode infoNode = root.get("info");
      JsonNode chartNode = root.get("chart");
      JsonNode metadataNode = chartNode.get("metadata");
      resultNode.putPOJO("Status", infoNode.get("status"));
      resultNode.putPOJO("Description", infoNode.get("description"));
      resultNode.putPOJO("ChartName", metadataNode.get("name"));
      resultNode.putPOJO("ChartVersion", metadataNode.get("version"));
      parentNode.putPOJO("Result", resultNode);
    }
    if (warning.length() > 0)
    {
      String decodedWarning = new String(Base64.getDecoder().decode(warning));
      ObjectNode warningNode = mapper.createObjectNode();
      warningNode.putPOJO("Message", decodedWarning);
      parentNode.putPOJO("Warning", warningNode);
    }
    if (error.length() > 0)
    {
      String decodedError = new String(Base64.getDecoder().decode(error));
      ObjectNode errorNode = mapper.createObjectNode();
      errorNode.putPOJO("Message", decodedError);
      parentNode.putPOJO("Error", errorNode);
    }

    return parentNode;
  }

  public JsonNode statusRelease(String releaseName, String namespace) throws IOException, InterruptedException
  {
    StringBuffer cmd = new StringBuffer();
    cmd.append(helmBinaryLocation + " ");
    cmd.append("list" + " ");
    cmd.append("-a -A" + " ");
    cmd.append("-o" + " " + "json" + " ");
    if (Boolean.valueOf(ApplicationProperties.getProperties("debug_enabled"))) cmd.append("--debug" + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config_file_location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();
    parentNode = mapper.createObjectNode();

    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      if (isValidJSON(decodedResult))
      {
        JsonNode mapNode = mapper.getJsonNode(decodedResult);
        Iterator<JsonNode> resultNodeIterator = mapNode.iterator();
        ObjectNode resultNode = mapper.createObjectNode();
        while (resultNodeIterator.hasNext())
        {
          JsonNode node = resultNodeIterator.next();
          if ((node.get("name").asText().equalsIgnoreCase(releaseName)) && (node.get("namespace").asText().equalsIgnoreCase(namespace)))
          {
            resultNode.putPOJO("Status", mapper.writeValueAsString(node.get("status")));
            resultNode.putPOJO("Release", mapper.writeValueAsString(node.get("name")));
            resultNode.putPOJO("Namespace", mapper.writeValueAsString(node.get("namespace")));
          }
        }
        parentNode.putPOJO("Result", resultNode);
      }
      else
        parentNode.putPOJO("Result", result);
    }
    if (warning.length() > 0)
    {
      String decodedWarning = new String(Base64.getDecoder().decode(warning));
      ObjectNode warningNode = mapper.createObjectNode();
      warningNode.putPOJO("Message", decodedWarning);
      parentNode.putPOJO("Warning", warningNode);
    }
    if (error.length() > 0)
    {
      String decodedError = new String(Base64.getDecoder().decode(error));
      ObjectNode errorNode = mapper.createObjectNode();
      errorNode.putPOJO("Message", decodedError);
      parentNode.putPOJO("Error", errorNode);
    }

    return parentNode;
  }


  public JsonNode helmSearchRepo(String repoName) throws IOException, InterruptedException
  {
    helmRepoUpdate();

    parentNode = mapper.createObjectNode();
    ObjectNode resultNode = mapper.createObjectNode();
    ObjectNode warningNode = mapper.createObjectNode();
    ObjectNode errorNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(helmBinaryLocation + " ");
    cmd.append("search" + " ");
    cmd.append("repo" + " ");
    if (!repoName.equalsIgnoreCase("no_filter"))
    {
      cmd.append("-r" + " " + repoName + " ");
    }
    cmd.append("-l" + " ");
    cmd.append("-o" + " " + "json");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();
    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      JsonNode root = mapper.readTree(decodedResult);
      if (root.isArray()) parentNode.putArray("Result").addPOJO(root);
      else
        parentNode.putPOJO("Result", resultNode);
    }
    if (warning.length() > 0)
    {
      String decodedWarning = new String(Base64.getDecoder().decode(warning));
      warningNode.putPOJO("Message", decodedWarning);
      parentNode.putPOJO("Warning", warningNode);
    }
    if (error.length() > 0)
    {
      String decodedError = new String(Base64.getDecoder().decode(error));
      errorNode.putPOJO("Message", decodedError);
      parentNode.putPOJO("Error", errorNode);
    }
    return parentNode;
  }

  // to add repo to helm index
  public JsonNode repoAdd(String repoName, String repoUrl, String repoUser, String repoPassword, Boolean isAuth) throws IOException, InterruptedException
  {
    parentNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(helmBinaryLocation + " ");
    cmd.append("repo" + " ");
    cmd.append("add" + " ");
    cmd.append(repoName + " ");
    cmd.append(repoUrl + " ");
    if (isAuth)
    {
      cmd.append("--username" + " " + repoUser + " ");
      cmd.append("--password" + " " + repoPassword + " ");
    }
    JsonNode cmdOutputNode = runner.runExec(cmd);
    String result = cmdOutputNode.get("Result").asText();
    String warning = cmdOutputNode.get("Warning").asText();
    String error = cmdOutputNode.get("Error").asText();

    parentNode.putPOJO("Result", result);
    parentNode.putPOJO("Warning", warning);
    parentNode.putPOJO("Error", error);

    return parentNode;
  }

  public boolean isValidJSON(final String json)
  {
    boolean valid = false;
    try
    {
      JsonParser parser = mapper.getJsonFactory().createJsonParser(json);
      valid = true;
    }
    catch (JsonParseException jpe)
    {
      // logger.debug("Invalid JSON " + jpe.getMessage());
      System.err.println("Invalid JSON " + jpe.getMessage());
    }
    catch (IOException ioe)
    {
      // logger.debug("Error reading JSON or Invalid JSON " + ioe.getMessage());
      System.err.println("Error reading JSON or Invalid JSON " + ioe.getMessage());
    }

    return valid;
  }

  public static void main(String[] args)
  {
    try
    {
      System.out.println(new HelmCmd().statusRelease("airflowdemo201", "cloudez-demo"));
    }
    catch (IOException | InterruptedException e)
    {
      e.printStackTrace();
    }
  }
}

package com.process.containers;

import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.process.Runner;
import com.process.utils.ApplicationProperties;
import com.process.utils.JSONOM;

public class KubectlCmd
{
  private String kubeCtlBinaryLocation = ApplicationProperties.getProperties("kubectl.executable.location");
  private boolean useKubeconfig = Boolean.valueOf(ApplicationProperties.getProperties("use.kubeconfig"));

  private Runner runner = null;

  private JSONOM mapper = null;
  private ObjectNode parentNode = null;

  public KubectlCmd()
  {
    mapper = new JSONOM();
    runner = new Runner();
  }

  public JsonNode createNamespace(String account, String region, String clusterName, String namespace) throws IOException, InterruptedException
  {
    parentNode = mapper.createObjectNode();

    ObjectNode resultNode = mapper.createObjectNode();
    ObjectNode warningNode = mapper.createObjectNode();
    ObjectNode errorNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(kubeCtlBinaryLocation + " ");
    cmd.append("create" + " ");
    cmd.append("namespace" + " ");
    cmd.append(namespace + " ");
    cmd.append("--context=" + "arn:aws:eks:" + region + ":" + account + ":cluster/" + clusterName + " ");
    cmd.append("-o json" + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config.file.location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();

    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      JsonNode root = mapper.readTree(decodedResult);
      resultNode.putPOJO("Name", root.get("metadata").get("name"));
      resultNode.putPOJO("CreationTimeStamp", root.get("metadata").get("creationTimestamp"));
      resultNode.putPOJO("Status", root.get("status").get("phase"));
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

  public JsonNode listNamespaces(String account, String region, String clusterName) throws IOException, InterruptedException
  {
    parentNode = mapper.createObjectNode();

    ArrayNode resultNode = mapper.createArrayNode();
    ObjectNode warningNode = mapper.createObjectNode();
    ObjectNode errorNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(kubeCtlBinaryLocation + " ");
    cmd.append("get" + " ");
    cmd.append("namespace" + " ");
    cmd.append("--context=" + "arn:aws:eks:" + region + ":" + account + ":cluster/" + clusterName + " ");
    cmd.append("-o json" + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config.file.location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();

    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      JsonNode root = mapper.readTree(decodedResult);
      ArrayNode itemsNode = (ArrayNode) root.get("items");
      Iterator<JsonNode> itemsNodeIterator = itemsNode.elements();
      while (itemsNodeIterator.hasNext())
      {
        JsonNode item = itemsNodeIterator.next();
        ObjectNode namespaceNode = mapper.createObjectNode();
        namespaceNode.putPOJO("Name", item.get("metadata").get("name"));
        namespaceNode.putPOJO("CreationTimeStamp", item.get("metadata").get("creationTimestamp"));
        namespaceNode.putPOJO("Status", item.get("status").get("phase"));
        resultNode.add(namespaceNode);
      }
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

  public JsonNode listPods() throws IOException, InterruptedException
  {
    parentNode = mapper.createObjectNode();

    ArrayNode resultNode = mapper.createArrayNode();
    ObjectNode warningNode = mapper.createObjectNode();
    ObjectNode errorNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(kubeCtlBinaryLocation + " ");
    cmd.append("get" + " ");
    cmd.append("pods" + " ");
    cmd.append("-o json" + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config.file.location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();

    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      JsonNode root = mapper.readTree(decodedResult);
      ArrayNode itemsNode = (ArrayNode) root.get("items");
      Iterator<JsonNode> itemsNodeIterator = itemsNode.elements();
      while (itemsNodeIterator.hasNext())
      {
        JsonNode item = itemsNodeIterator.next();
        ObjectNode namespaceNode = mapper.createObjectNode();
        namespaceNode.putPOJO("Name", item.get("metadata").get("name"));
        namespaceNode.putPOJO("CreationTimeStamp", item.get("metadata").get("creationTimestamp"));
        namespaceNode.putPOJO("Status", item.get("status").get("phase"));
        resultNode.add(namespaceNode);
      }
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

  public JsonNode deleteNamespace(String account, String region, String clusterName, String namespace) throws IOException, InterruptedException
  {

    ObjectNode warningNode = mapper.createObjectNode();
    ObjectNode errorNode = mapper.createObjectNode();

    StringBuffer cmd = new StringBuffer();
    cmd.append(kubeCtlBinaryLocation + " ");
    cmd.append("delete" + " ");
    cmd.append("namespace" + " ");
    cmd.append(namespace + " ");
    cmd.append("-o name" + " ");
    cmd.append("--context=" + "arn:aws:eks:" + region + ":" + account + ":cluster/" + clusterName + " ");
    if (useKubeconfig) cmd.append("--kubeconfig" + " " + ApplicationProperties.getProperties("config.file.location") + " ");

    JsonNode outputNode = runner.runExec(cmd);
    String result = outputNode.get("Result").asText();
    String warning = outputNode.get("Warning").asText();
    String error = outputNode.get("Error").asText();

    if (result.length() > 0)
    {
      String decodedResult = new String(Base64.getDecoder().decode(result));
      parentNode.putPOJO("Result", decodedResult);
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
}

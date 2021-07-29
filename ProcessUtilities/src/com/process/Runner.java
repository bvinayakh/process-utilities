package com.process;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.process.utils.ApplicationProperties;

public class Runner
{
  public static final Logger logger = LoggerFactory.getLogger(Runner.class);

  private ObjectMapper mapper = null;

  public Runner()
  {
    mapper = new ObjectMapper();
  }

  public JsonNode runExec(StringBuffer cmd) throws IOException, InterruptedException
  {
    Map<String, String> environmentVars = new HashMap<>();
    String envVars = ApplicationProperties.getProperties("env_path");
    if (envVars != null) environmentVars.put("PATH", envVars);
    else
      environmentVars.put("PATH", System.getenv("PATH"));
    return runExec(cmd, environmentVars);
  }

  public JsonNode runExec(StringBuffer cmd, Map<String, String> environmentVars) throws IOException, InterruptedException
  {
    logger.debug("Execution Command Equivalent: " + cmd);
    StringBuffer outputValid = new StringBuffer();
    StringBuffer outputWarning = new StringBuffer();
    StringBuffer outputError = new StringBuffer();

    ObjectNode outputNode = mapper.createObjectNode();

    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
    DefaultExecutor executor = new DefaultExecutor();
    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
    CommandLine commands = CommandLine.parse(cmd.toString());
    // timeout 1 minute
    Long timeout = Long.valueOf(ApplicationProperties.getProperties("process_timeout"));
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    PumpStreamHandler streamHander = new PumpStreamHandler(stdoutStream, stderrStream);
    executor.setStreamHandler(streamHander);
    executor.setExitValue(1);
    executor.setWatchdog(watchdog);
    // executor.execute(commands, resultHandler);
    logger.debug("Environment Variables included during execution: " + environmentVars);
    executor.execute(commands, environmentVars, resultHandler);
    resultHandler.waitFor();
    int returnValue = resultHandler.getExitValue();
    if (returnValue == 1) logger.info("Execution encountered error, returned {" + returnValue + "}. See JSON response for more details");
    if (returnValue == 0) logger.info("Execution successful, returned {" + returnValue + "}");
    String stdOut = stdoutStream.toString();
    String stdErr = stderrStream.toString();

    if (stdOut.length() > 5)
    {
      outputValid.append(stdOut);
      outputValid.append(System.lineSeparator());
    }

    if (stdErr.length() > 5)
    {
      if (stdErr.toLowerCase().startsWith("warn"))
      {
        outputWarning.append(stdErr);
      }
      else
        outputError.append(stdErr);
    }

    String encodedOutput = Base64.getEncoder().encodeToString(outputValid.toString().getBytes());
    String encodedWarning = Base64.getEncoder().encodeToString(outputWarning.toString().getBytes());
    String encodedError = Base64.getEncoder().encodeToString(outputError.toString().getBytes());
    outputNode.putPOJO("Result", encodedOutput);
    outputNode.putPOJO("Warning", encodedWarning);
    outputNode.putPOJO("Error", encodedError);

    return outputNode;
  }

  public JsonNode runExecWithEnv(StringBuffer cmd, ArrayList<String> envVars) throws IOException, InterruptedException
  {
    Runtime rt = Runtime.getRuntime();
    StringBuffer outputValid = new StringBuffer();
    StringBuffer outputWarning = new StringBuffer();
    StringBuffer outputError = new StringBuffer();

    ObjectNode outputNode = mapper.createObjectNode();

    if (envVars == null) envVars = new ArrayList<String>();
    envVars.add("PATH=" + ApplicationProperties.getProperties("env_path"));
    String[] env = Arrays.stream(envVars.toArray()).toArray(String[]::new);

    Process pr = rt.exec(cmd.toString(), env, null);
    BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

    String line = null;

    while ((line = input.readLine()) != null)
    {
      outputValid.append(line);
      outputValid.append(System.lineSeparator());
    }

    while ((line = error.readLine()) != null)
    {
      if (line.toLowerCase().startsWith("warn"))
      {
        outputWarning.append(line);
        outputWarning.append(System.lineSeparator());
      }
      else
      {
        outputError.append(line);
        outputError.append(System.lineSeparator());
      }
    }

    int exitVal = pr.waitFor();
    System.out.println("Process Result Exit Value: " + exitVal);
    String encodedOutput = Base64.getEncoder().encodeToString(outputValid.toString().getBytes());
    String encodedWarning = Base64.getEncoder().encodeToString(outputWarning.toString().getBytes());
    String encodedError = Base64.getEncoder().encodeToString(outputError.toString().getBytes());
    outputNode.putPOJO("Result", encodedOutput);
    outputNode.putPOJO("Warning", encodedWarning);
    outputNode.putPOJO("Error", encodedError);

    return outputNode;
  }
}

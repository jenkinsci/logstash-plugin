package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Slave;
import jenkins.plugins.logstash.configuration.MemoryIndexer;
import jenkins.plugins.logstash.persistence.MemoryDao;
import net.sf.json.JSONObject;

public class PipelineTest
{

  @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule public JenkinsRule j = new JenkinsRule();

  private MemoryDao memoryDao;

  @Before
  public void setup() throws Exception
  {
      memoryDao = new MemoryDao();
      LogstashConfiguration config = LogstashConfiguration.getInstance();
      MemoryIndexer indexer = new MemoryIndexer(memoryDao);
      config.setLogstashIndexer(indexer);
      config.setEnabled(true);
  }

  @Test
  public void logstash() throws Exception
  {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition("logstash {\n" +
        "currentBuild.result = 'SUCCESS'\n" +
        "echo 'Message'\n" +
    "}", true));
    j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    List<JSONObject> dataLines = memoryDao.getOutput();
    assertThat(dataLines.size(), equalTo(1));
    JSONObject firstLine = dataLines.get(0);
    JSONObject data = firstLine.getJSONObject("data");
    assertThat(data.getString("result"),equalTo("SUCCESS"));
  }

  @Test
  public void logstashStageAndAgent() throws Exception
  {
    Slave slave = j.createOnlineSlave();
    String agentName = slave.getNodeName();
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition("stage('stage1') { " +
      "node('" + agentName + "') {\n" +
      "logstash {\n" +
        "currentBuild.result = 'SUCCESS'\n" +
        "echo 'Message'\n" +
    "}}}", true));
    j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    List<JSONObject> dataLines = memoryDao.getOutput();
    assertThat(dataLines.size(), equalTo(1));
    JSONObject firstLine = dataLines.get(0);
    JSONObject data = firstLine.getJSONObject("data");
    assertThat(data.getString("result"),equalTo("SUCCESS"));
    assertThat(data.getString("stageName"),equalTo("stage1"));
    assertThat(data.getString("agentName"),equalTo(agentName));
  }

  @Test
  public void globalLogstash() throws Exception
  {
    LogstashConfiguration config = LogstashConfiguration.getInstance();
    config.setEnableGlobally(true);
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(
        "currentBuild.result = 'SUCCESS'\n" +
        "echo 'Message'\n", true));
    j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    List<JSONObject> dataLines = memoryDao.getOutput();
    assertThat(dataLines.size(), greaterThan(0));
    JSONObject lastLine = dataLines.get(dataLines.size()-1);
    JSONObject data = lastLine.getJSONObject("data");
    assertThat(data.getString("result"),equalTo("SUCCESS"));
  }

  @Test
  public void logstashSendNotifier() throws Exception
  {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition("node {" +
          "echo 'Message'\n" +
          "currentBuild.result = 'SUCCESS'\n" +
          "step([$class: 'LogstashNotifier',  failBuild: true, maxLines: 5])" +
    "}", true));
    j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    List<JSONObject> dataLines = memoryDao.getOutput();
    assertThat(dataLines.size(), equalTo(1));
    JSONObject firstLine = dataLines.get(0);
    JSONObject data = firstLine.getJSONObject("data");
    assertThat(data.getString("result"),equalTo("SUCCESS"));
  }

  @Test
  public void logstashSend() throws Exception
  {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(
          "echo 'Message'\n" +
          "currentBuild.result = 'SUCCESS'\n" +
          "logstashSend  failBuild: true, maxLines: 5"
    , true));
    j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    List<JSONObject> dataLines = memoryDao.getOutput();
    assertThat(dataLines.size(), equalTo(1));
    JSONObject firstLine = dataLines.get(0);
    JSONObject data = firstLine.getJSONObject("data");
    assertThat(data.getString("result"),equalTo("SUCCESS"));
  }

  @Test
  public void logstashSendWithAdditionalParams() throws Exception
  {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(
          "echo 'Message'\n" +
          "currentBuild.result = 'SUCCESS'\n" +
          "HashMap<String, String> someMap = new HashMap<String,String>()\n" + 
          "someMap.put('param1', 'value1')\n" +
          "someMap.put('param2', 'value2')\n" + 
          "logstashSend failBuild: true, maxLines: 5, additionalParams: someMap" 
    , true));
    j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    List<JSONObject> dataLines = memoryDao.getOutput();
    assertThat(dataLines.size(), equalTo(1));
    JSONObject firstLine = dataLines.get(0);
    JSONObject data = firstLine.getJSONObject("data");
    assertThat(data.getString("result"),equalTo("SUCCESS"));
  }
}

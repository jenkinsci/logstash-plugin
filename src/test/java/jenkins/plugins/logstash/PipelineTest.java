package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

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
    p.setDefinition(new CpsFlowDefinition("node {" +
          "echo 'Message'\n" +
          "currentBuild.result = 'SUCCESS'\n" +
          "logstashSend  failBuild: true, maxLines: 5" +
    "}", true));
    j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    List<JSONObject> dataLines = memoryDao.getOutput();
    assertThat(dataLines.size(), equalTo(1));
    JSONObject firstLine = dataLines.get(0);
    JSONObject data = firstLine.getJSONObject("data");
    assertThat(data.getString("result"),equalTo("SUCCESS"));
  }
}

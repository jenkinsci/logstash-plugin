package jenkins.plugins.logstash;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsConfig;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper.VarPasswordPair;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.ansicolor.AnsiColorBuildWrapper;
import net.sf.json.JSONArray;
import jenkins.plugins.logstash.configuration.MemoryIndexer;
import jenkins.plugins.logstash.persistence.MemoryDao;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class LogstashIntegrationTest
{
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Slave slave;

    private FreeStyleProject project;

    private MemoryDao memoryDao;

    @Before
    public void setup() throws Exception
    {
        memoryDao = new MemoryDao();
        LogstashConfiguration config = LogstashConfiguration.getInstance();
        MemoryIndexer indexer = new MemoryIndexer(memoryDao);
        config.setLogstashIndexer(indexer);
        config.setEnabled(true);

        slave = jenkins.createSlave();
        slave.setLabelString("myLabel");
        project = jenkins.createFreeStyleProject();
    }

    @Test
    public void dataIsSetWhenEnabledViaJobPropertyOnMaster() throws Exception
    {
        project.addProperty(new LogstashJobProperty());
        QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);
        FreeStyleBuild build = f.get();
        assertThat(build.getResult(), equalTo(Result.SUCCESS));
        List<JSONObject> dataLines = memoryDao.getOutput();
        assertThat(dataLines.size(), greaterThan(3));
        JSONObject firstLine = dataLines.get(0);
        JSONObject lastLine = dataLines.get(dataLines.size()-1);
        JSONObject data = firstLine.getJSONObject("data");
        assertThat(data.getString("buildHost"),equalTo("Jenkins"));
        assertThat(data.getString("buildLabel"),equalTo("master"));
        assertThat(lastLine.getJSONArray("message").get(0).toString(),equalTo("Finished: SUCCESS"));
    }

    @Test
    public void dataIsSetWhenEnabledViaJobPropertyOnSlave() throws Exception
    {
        project.addProperty(new LogstashJobProperty());
        project.setAssignedNode(slave);

        QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);
        FreeStyleBuild build = f.get();
        assertThat(build.getResult(), equalTo(Result.SUCCESS));
        List<JSONObject> dataLines = memoryDao.getOutput();
        assertThat(dataLines.size(), greaterThan(3));
        JSONObject firstLine = dataLines.get(0);
        JSONObject lastLine = dataLines.get(dataLines.size()-1);
        JSONObject data = firstLine.getJSONObject("data");
        assertThat(data.getString("buildHost"),equalTo(slave.getDisplayName()));
        assertThat(data.getString("buildLabel"),equalTo(slave.getLabelString()));
        assertThat(lastLine.getJSONArray("message").get(0).toString(),equalTo("Finished: SUCCESS"));
    }

    @Test
    public void dataIsSetForNotifierOnMaster() throws Exception
    {
        project.getPublishersList().add(new LogstashNotifier(10, false));
        QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);
        FreeStyleBuild build = f.get();
        assertThat(build.getResult(), equalTo(Result.SUCCESS));
        List<JSONObject> dataLines = memoryDao.getOutput();
        assertThat(dataLines.size(), is(1));
        JSONObject firstLine = dataLines.get(0);
        JSONObject data = firstLine.getJSONObject("data");
        assertThat(data.getString("buildHost"),equalTo("Jenkins"));
        assertThat(data.getString("buildLabel"),equalTo("master"));
    }

    @Test
    public void dataIsSetForNotifierOnSlave() throws Exception
    {
        project.getPublishersList().add(new LogstashNotifier(10, false));
        project.setAssignedNode(slave);
        QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);
        FreeStyleBuild build = f.get();
        assertThat(build.getResult(), equalTo(Result.SUCCESS));
        List<JSONObject> dataLines = memoryDao.getOutput();
        assertThat(dataLines.size(), is(1));
        JSONObject firstLine = dataLines.get(0);
        JSONObject data = firstLine.getJSONObject("data");
        assertThat(data.getString("buildHost"),equalTo(slave.getDisplayName()));
        assertThat(data.getString("buildLabel"),equalTo(slave.getLabelString()));
    }

    @Test
    public void buildJobPropertyUpdatesResult() throws Exception
    {
      project.addProperty(new LogstashJobProperty());
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);
      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      assertThat(dataLines.size(), greaterThan(3));
      JSONObject firstLine = dataLines.get(0);
      JSONObject lastLine = dataLines.get(dataLines.size()-1);
      JSONObject data = firstLine.getJSONObject("data");
      thrown.expect(JSONException.class);
      thrown.expectMessage("JSONObject[\"result\"] not found.");
      data.getString("result");
      data = lastLine.getJSONObject("data");
      assertThat(data.getString("result"),equalTo("SUCCESS"));
    }

    @Test
    public void passwordsAreMaskedWithMaskpasswordsBuildWrapper() throws Exception
    {
      EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, "PWD=myPassword", null, null, false, null);
      EnvInjectBuildWrapper e = new EnvInjectBuildWrapper(info);

      List<VarPasswordPair> pwdPairs = new ArrayList<>();
      VarPasswordPair pwdPair = new VarPasswordPair("PWD", "myPassword");
      pwdPairs.add(pwdPair);
      MaskPasswordsBuildWrapper maskPwdWrapper = new MaskPasswordsBuildWrapper(pwdPairs);

      project.addProperty(new LogstashJobProperty());
      project.getBuildWrappersList().add(maskPwdWrapper);
      project.getBuildWrappersList().add(e);
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      for (JSONObject line: dataLines)
      {
        JSONArray message = line.getJSONArray("message");
        String logline = (String) message.get(0);
        assertThat(logline,not(containsString("myPassword")));
      }
    }

    @Test
    public void passwordsAreMaskedWithGlobalMaskPasswordsConfiguration() throws Exception
    {
      MaskPasswordsConfig config = MaskPasswordsConfig.getInstance();
      config.setGlobalVarEnabledGlobally(true);
      VarPasswordPair pwdPair = new VarPasswordPair("PWD", "myPassword");
      config.addGlobalVarPasswordPair(pwdPair);
      EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, "PWD=myPassword", null, null, false, null);
      EnvInjectBuildWrapper e = new EnvInjectBuildWrapper(info);

      project.getBuildWrappersList().add(e);
      project.addProperty(new LogstashJobProperty());
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      for (JSONObject line: dataLines)
      {
        JSONArray message = line.getJSONArray("message");
        String logline = (String) message.get(0);
        assertThat(logline,not(containsString("myPassword")));
      }
    }

    @Test
    public void enableGlobally() throws Exception
    {
      LogstashConfiguration.getInstance().setEnableGlobally(true);
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      assertThat(dataLines.size(), greaterThan(3));
      JSONObject firstLine = dataLines.get(0);
      JSONObject lastLine = dataLines.get(dataLines.size()-1);
      JSONObject data = firstLine.getJSONObject("data");
      assertThat(data.getString("buildHost"),equalTo("Jenkins"));
      assertThat(data.getString("buildLabel"),equalTo("master"));
      assertThat(lastLine.getJSONArray("message").get(0).toString(),equalTo("Finished: SUCCESS"));
    }

    @Test
    public void milliSecondTimestamps() throws Exception
    {

      LogstashConfiguration.getInstance().setMilliSecondTimestamps(true);
      project.addProperty(new LogstashJobProperty());
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      for (JSONObject line: dataLines)
      {
        String timestamp = line.getString("@timestamp");
        assertThat(timestamp,matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}$"));
      }
    }

    @Test
    public void secondTimestamps() throws Exception
    {
      LogstashConfiguration.getInstance().setMilliSecondTimestamps(false);
      project.addProperty(new LogstashJobProperty());
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      for (JSONObject line: dataLines)
      {
        String timestamp = line.getString("@timestamp");
        assertThat(timestamp,matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{4}$"));
      }
    }

    @Test
    public void ansiColorAnnotationsAreNotIncluded() throws Exception
    {
      AnsiColorBuildWrapper ansi = new AnsiColorBuildWrapper("vga");
      project.addProperty(new LogstashJobProperty());
      project.getBuildWrappersList().add(ansi);
      Cause cause = new Cause.UserIdCause();
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0, cause);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      JSONObject firstLine = dataLines.get(0);
      assertThat(firstLine.getJSONArray("message").get(0).toString(),not(startsWith("[8mha")));
    }

    @Test
    public void disabledWillNotWrite() throws Exception
    {
      LogstashConfiguration.getInstance().setEnabled(false);
      project.addProperty(new LogstashJobProperty());
      Cause cause = new Cause.UserIdCause();
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0, cause);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      assertThat(dataLines.size(), equalTo(0));
    }
}

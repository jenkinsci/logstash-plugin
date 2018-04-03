package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsConfig;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper.VarPasswordPair;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.queue.QueueTaskFuture;
import net.sf.json.JSONArray;
import jenkins.plugins.logstash.persistence.MemoryDao;
import net.sf.json.JSONObject;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"})
@PrepareForTest(LogstashConfiguration.class)
public class LogstashIntegrationTest
{
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private LogstashConfiguration logstashConfiguration;

    private Slave slave;

    private FreeStyleProject project;

    private MemoryDao memoryDao;

    @Before
    public void setup() throws Exception
    {
        memoryDao = new MemoryDao();
        PowerMockito.mockStatic(LogstashConfiguration.class);
        when(LogstashConfiguration.getInstance()).thenReturn(logstashConfiguration);
        when(logstashConfiguration.getIndexerInstance()).thenReturn(memoryDao);

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
        assertThat(dataLines.size(), is(4));
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
        assertThat(dataLines.size(), is(4));
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
      when(logstashConfiguration.isEnableGlobally()).thenReturn(true);
      QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);

      FreeStyleBuild build = f.get();
      assertThat(build.getResult(), equalTo(Result.SUCCESS));
      List<JSONObject> dataLines = memoryDao.getOutput();
      assertThat(dataLines.size(), is(4));
      JSONObject firstLine = dataLines.get(0);
      JSONObject lastLine = dataLines.get(dataLines.size()-1);
      JSONObject data = firstLine.getJSONObject("data");
      assertThat(data.getString("buildHost"),equalTo("Jenkins"));
      assertThat(data.getString("buildLabel"),equalTo("master"));
      assertThat(lastLine.getJSONArray("message").get(0).toString(),equalTo("Finished: SUCCESS"));
    }

}

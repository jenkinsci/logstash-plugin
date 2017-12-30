package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.queue.QueueTaskFuture;
import jenkins.plugins.logstash.LogstashInstallation.Descriptor;
import jenkins.plugins.logstash.persistence.AbstractLogstashIndexerDao;
import jenkins.plugins.logstash.persistence.IndexerDaoFactory;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.IndexerType;
import net.sf.json.JSONObject;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"})
@PrepareForTest({ IndexerDaoFactory.class, LogstashInstallation.class })
public class LogstashIntegrationTest
{
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private Slave slave;

    private FreeStyleProject project;

    private MemoryDao memoryDao;

    @Mock
    Descriptor descriptor;

    @Before
    public void setup() throws Exception
    {
        PowerMockito.mockStatic(IndexerDaoFactory.class);
        PowerMockito.mockStatic(LogstashInstallation.class);
        when(LogstashInstallation.getLogstashDescriptor()).thenReturn(descriptor);
        when(descriptor.getType()).thenReturn(IndexerType.SYSLOG);
        when(descriptor.getHost()).thenReturn("localhost");
        when(descriptor.getPort()).thenReturn(1);
        when(descriptor.getUsername()).thenReturn("username");
        when(descriptor.getKey()).thenReturn("password");
        when(descriptor.getKey()).thenReturn("key");

        memoryDao = new MemoryDao();
        when(IndexerDaoFactory.getInstance(IndexerType.SYSLOG, descriptor.getHost(), descriptor.getPort(),
            descriptor.getKey(),descriptor.getUsername(), descriptor.getPassword())).thenReturn(memoryDao);
        slave = jenkins.createSlave();
        slave.setLabelString("myLabel");
        project = jenkins.createFreeStyleProject();
    }

    @Test
    public void test_buildWrapperOnMaster() throws Exception
    {
        project.getBuildWrappersList().add(new LogstashBuildWrapper());
        QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);
        FreeStyleBuild build = f.get();
        assertThat(build.getResult(), equalTo(Result.SUCCESS));
        List<JSONObject> dataLines = memoryDao.getOutput();
        assertThat(dataLines.size(), is(3));
        JSONObject firstLine = dataLines.get(0);
        JSONObject lastLine = dataLines.get(dataLines.size()-1);
        JSONObject data = firstLine.getJSONObject("data");
        assertThat(data.getString("buildHost"),equalTo("Jenkins"));
        assertThat(data.getString("buildLabel"),equalTo("master"));
        assertThat(lastLine.getJSONArray("message").get(0).toString(),equalTo("Finished: SUCCESS"));
    }

    @Test
    public void test_buildWrapperOnSlave() throws Exception
    {
        project.getBuildWrappersList().add(new LogstashBuildWrapper());
        project.setAssignedNode(slave);

        QueueTaskFuture<FreeStyleBuild> f = project.scheduleBuild2(0);
        FreeStyleBuild build = f.get();
        assertThat(build.getResult(), equalTo(Result.SUCCESS));
        List<JSONObject> dataLines = memoryDao.getOutput();
        assertThat(dataLines.size(), is(3));
        JSONObject firstLine = dataLines.get(0);
        JSONObject lastLine = dataLines.get(dataLines.size()-1);
        JSONObject data = firstLine.getJSONObject("data");
        assertThat(data.getString("buildHost"),equalTo(slave.getDisplayName()));
        assertThat(data.getString("buildLabel"),equalTo(slave.getLabelString()));
        assertThat(lastLine.getJSONArray("message").get(0).toString(),equalTo("Finished: SUCCESS"));
    }

    @Test
    public void test_buildNotifierOnMaster() throws Exception
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
    public void test_buildNotifierOnSlave() throws Exception
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

    private static class MemoryDao extends AbstractLogstashIndexerDao
    {
        List<JSONObject> output = new ArrayList<>();

        public MemoryDao()
        {
            super("localhost", 1, "key", "username", "password");
        }

        @Override
        public IndexerType getIndexerType()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void push(String data) throws IOException
        {
            JSONObject json = JSONObject.fromObject(data);
            output.add(json);
        }

        public List<JSONObject> getOutput()
        {
            return output;
        }
    }
}

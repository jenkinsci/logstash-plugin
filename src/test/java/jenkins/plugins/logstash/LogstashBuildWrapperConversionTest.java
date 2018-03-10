package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import hudson.model.Project;

public class LogstashBuildWrapperConversionTest
{
  @Rule
  public JenkinsRule j;

  public LogstashBuildWrapperConversionTest() throws Exception
  {
    j = new JenkinsRule().withExistingHome(new File("src/test/resources/home"));
  }

  @Test
  public void existingJobIsConvertedAtStartup()
  {
    Project<?, ?> item = (Project<?, ?>) j.getInstance().getItem("test");

    assertThat(item.getBuildWrappersList().get(LogstashBuildWrapper.class), equalTo(null));
    assertThat(item.getProperty(LogstashJobProperty.class), not(equalTo(null)));
  }

  @Test
  public void buildWrapperIsConvertedToJobPropertyWhenPostingXML() throws IOException
  {
    MockFolder folder = j.createFolder("folder");
    FileInputStream xml = new FileInputStream("src/test/resources/buildWrapperConfig.xml");
    Project<?, ?> item = (Project<?, ?>) folder.createProjectFromXML("converted", xml);
    assertThat(item.getBuildWrappersList().get(LogstashBuildWrapper.class), equalTo(null));
    assertThat(item.getProperty(LogstashJobProperty.class), not(equalTo(null)));
  }
}

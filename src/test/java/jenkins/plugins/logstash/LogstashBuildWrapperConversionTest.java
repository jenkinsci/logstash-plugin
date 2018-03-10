package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;

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
    Project<?, ?> project = (Project<?, ?>)j.getInstance().getItem("test");
    checkNoBuildWrapper(project);
  }

  @Before
  public void setup()
  {
    j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
  }

  @Test
  public void buildWrapperIsConvertedToJobPropertyWhenPostingXMLToNewJob() throws IOException
  {
    j.jenkins.setCrumbIssuer(null);
    String newJobName = "newJob";
    URL apiURL = new URL(MessageFormat.format(
        "{0}createItem?name={1}",
        j.getURL().toString(), newJobName));
    WebRequest request = new WebRequest(apiURL, HttpMethod.POST);
    request.setAdditionalHeader("Content-Type", "application/xml");

    int result = -1;
    try
    {
      request.setRequestBody("<?xml version='1.0' encoding='UTF-8'?>\n<project>\n" +
          "<properties/>" +
          "<buildWrappers>" +
          "<jenkins.plugins.logstash.LogstashBuildWrapper/>" +
          "</buildWrappers>" +
          "</project>");
      result = j.createWebClient().getPage(request).getWebResponse().getStatusCode();
    }
    catch (FailingHttpStatusCodeException e)
    {
      result = e.getResponse().getStatusCode();
    }

    assertEquals("Creating job should succeed.", 200, result);
    Project<?, ?> project = (Project<?, ?>)j.getInstance().getItem(newJobName);
    checkNoBuildWrapper(project);
  }

  @Test
  public void buildWrapperIsConvertedToJobPropertyWhenPostingXMLToExistingJob() throws IOException
  {
    j.jenkins.setCrumbIssuer(null);
    String newJobName = "newJob";
    j.createFreeStyleProject(newJobName);
    URL apiURL = new URL(MessageFormat.format(
        "{0}job/{1}/config.xml",
        j.getURL().toString(), newJobName));
    WebRequest request = new WebRequest(apiURL, HttpMethod.POST);
    request.setAdditionalHeader("Content-Type", "application/xml");

    int result = -1;
    try
    {
      request.setRequestBody("<?xml version='1.0' encoding='UTF-8'?>\n<project>\n" +
          "<properties/>" +
          "<buildWrappers>" +
          "<jenkins.plugins.logstash.LogstashBuildWrapper/>" +
          "</buildWrappers>" +
          "</project>");
      result = j.createWebClient().getPage(request).getWebResponse().getStatusCode();
    }
    catch (FailingHttpStatusCodeException e)
    {
      result = e.getResponse().getStatusCode();
    }

    assertEquals("Updating job should succeed.", 200, result);
    Project<?, ?> project = (Project<?, ?>)j.getInstance().getItem(newJobName);
    checkNoBuildWrapper(project);
  }

  private void checkNoBuildWrapper(Project<?, ?> project)
  {
    assertThat(project.getBuildWrappersList().get(LogstashBuildWrapper.class), equalTo(null));
    assertThat(project.getProperty(LogstashJobProperty.class), not(equalTo(null)));
  }
}

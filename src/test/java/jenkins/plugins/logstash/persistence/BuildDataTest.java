package jenkins.plugins.logstash.persistence;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import hudson.model.Environment;
import hudson.model.EnvironmentList;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Project;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import jenkins.plugins.logstash.persistence.BuildData.TestData;
import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SuppressWarnings("rawtypes")
@RunWith(MockitoJUnitRunner.class)
public class BuildDataTest {

  static final String FULL_STRING = "{\"id\":\"TEST_JOB_123\",\"result\":\"SUCCESS\",\"projectName\":\"PROJECT_NAME\",\"displayName\":\"DISPLAY NAME\",\"fullDisplayName\":\"FULL DISPLAY NAME\",\"description\":\"DESCRIPTION\",\"url\":\"http://localhost:8080/jenkins/jobs/PROJECT_NAME/123\",\"buildHost\":\"http://localhost:8080/jenkins\",\"buildLabel\":\"master\",\"buildNum\":123,\"buildDuration\":100,\"rootProjectName\":\"ROOT PROJECT NAME\",\"rootProjectDisplayName\":\"ROOT PROJECT DISPLAY NAME\",\"rootBuildNum\":456,\"buildVariables\":{},\"sensitiveBuildVariables\":[],\"testResults\":{\"totalCount\":0,\"skipCount\":0,\"failCount\":0,\"failedTests\":[]}}";

  @Mock AbstractBuild mockBuild;
  @Mock AbstractTestResultAction mockTestResultAction;
  @Mock Project mockProject;
  @Mock Node mockNode;
  @Mock Environment mockEnvironment;
  @Mock Date mockDate;
  @Mock GregorianCalendar mockCalendar;

  @Before
  public void before() throws Exception {
    when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
    when(mockBuild.getDisplayName()).thenReturn("BuildDataTest");
    when(mockBuild.getFullDisplayName()).thenReturn("BuildDataTest #123456");
    when(mockBuild.getDescription()).thenReturn("Mock project for testing BuildData");
    when(mockBuild.getProject()).thenReturn(mockProject);
    when(mockBuild.getNumber()).thenReturn(123456);
    when(mockBuild.getDuration()).thenReturn(0L);
    when(mockBuild.getTimestamp()).thenReturn(mockCalendar);
    when(mockBuild.getRootBuild()).thenReturn(mockBuild);
    when(mockBuild.getBuildVariables()).thenReturn(Collections.emptyMap());
    when(mockBuild.getSensitiveBuildVariables()).thenReturn(Collections.emptySet());
    when(mockBuild.getEnvironments()).thenReturn(null);
    when(mockBuild.getLog(3)).thenReturn(Arrays.asList("line 1", "line 2", "line 3"));
    when(mockBuild.getAction(AbstractTestResultAction.class)).thenReturn(mockTestResultAction);

    when(mockTestResultAction.getTotalCount()).thenReturn(0);
    when(mockTestResultAction.getSkipCount()).thenReturn(0);
    when(mockTestResultAction.getFailCount()).thenReturn(0);
    when(mockTestResultAction.getFailedTests()).thenReturn(Collections.emptyList());

    when(mockProject.getName()).thenReturn("BuildDataTest");

    when(mockDate.getTime()).thenReturn(60L);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockBuild);
    verifyNoMoreInteractions(mockTestResultAction);
    verifyNoMoreInteractions(mockProject);
    verifyNoMoreInteractions(mockEnvironment);
    verifyNoMoreInteractions(mockDate);
  }

  @Test
  public void constructorSuccessBuiltOnNull() throws Exception {
    when(mockBuild.getBuiltOn()).thenReturn(null);

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate);

    // build.getDuration() is always 0 in Notifiers
    Assert.assertEquals("Incorrect buildDuration", 60L, buildData.getBuildDuration());

    // Verify the rest of the results
    Assert.assertEquals("Incorrect buildHost", "master", buildData.getBuildHost());
    Assert.assertEquals("Incorrect buildLabel", "master", buildData.getBuildLabel());

    verify(mockBuild).getId();
    verify(mockBuild, times(2)).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild, times(2)).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getBuiltOn();
    verify(mockBuild, times(2)).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(3)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();

    verify(mockTestResultAction).getTotalCount();
    verify(mockTestResultAction).getSkipCount();
    verify(mockTestResultAction).getFailCount();
    verify(mockTestResultAction, times(2)).getFailedTests();

    verify(mockProject, times(2)).getName();

    verify(mockDate).getTime();
  }

  @Test
  public void constructorSuccessBuiltOnMaster() throws Exception {
    when(mockBuild.getBuiltOn()).thenReturn(mockNode);

    when(mockNode.getDisplayName()).thenReturn("Jenkins");
    when(mockNode.getLabelString()).thenReturn("");

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate);

    // build.getDuration() is always 0 in Notifiers
    Assert.assertEquals("Incorrect buildDuration", 60L, buildData.getBuildDuration());

    // Verify the rest of the results
    Assert.assertEquals("Incorrect buildHost", "Jenkins", buildData.getBuildHost());
    Assert.assertEquals("Incorrect buildLabel", "master", buildData.getBuildLabel());

    verify(mockBuild).getId();
    verify(mockBuild, times(2)).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild, times(2)).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getBuiltOn();
    verify(mockBuild, times(2)).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(3)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();

    verify(mockTestResultAction).getTotalCount();
    verify(mockTestResultAction).getSkipCount();
    verify(mockTestResultAction).getFailCount();
    verify(mockTestResultAction, times(2)).getFailedTests();

    verify(mockProject, times(2)).getName();

    verify(mockDate).getTime();
  }

  @Test
  public void constructorSuccessBuiltOnSlave() throws Exception {
    when(mockBuild.getBuiltOn()).thenReturn(mockNode);

    when(mockNode.getDisplayName()).thenReturn("Test Slave 01");
    when(mockNode.getLabelString()).thenReturn("Test Slave");

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate);

    // build.getDuration() is always 0 in Notifiers
    Assert.assertEquals("Incorrect buildDuration", 60L, buildData.getBuildDuration());

    // Verify the rest of the results
    Assert.assertEquals("Incorrect buildHost", "Test Slave 01", buildData.getBuildHost());
    Assert.assertEquals("Incorrect buildLabel", "Test Slave", buildData.getBuildLabel());

    verify(mockBuild).getId();
    verify(mockBuild, times(2)).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild, times(2)).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getBuiltOn();
    verify(mockBuild, times(2)).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(3)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();

    verify(mockTestResultAction).getTotalCount();
    verify(mockTestResultAction).getSkipCount();
    verify(mockTestResultAction).getFailCount();
    verify(mockTestResultAction, times(2)).getFailedTests();

    verify(mockProject, times(2)).getName();

    verify(mockDate).getTime();
  }

  @Test
  public void constructorSuccessTestFailures() {
    TestResult mockTestResult = Mockito.mock(hudson.tasks.test.TestResult.class);
    when(mockTestResult.getSafeName()).thenReturn("Mock Test");

    when(mockTestResultAction.getTotalCount()).thenReturn(123);
    when(mockTestResultAction.getSkipCount()).thenReturn(0);
    when(mockTestResultAction.getFailCount()).thenReturn(1);
    when(mockTestResultAction.getFailedTests()).thenReturn(Arrays.asList(mockTestResult));

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate);

    Assert.assertEquals("Incorrect test results", 123, buildData.testResults.totalCount);
    Assert.assertEquals("Incorrect test results", 0, buildData.testResults.skipCount);
    Assert.assertEquals("Incorrect test results", 1, buildData.testResults.failCount);

    // Verify the rest of the results
    verify(mockBuild).getId();
    verify(mockBuild, times(2)).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild, times(2)).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getBuiltOn();
    verify(mockBuild, times(2)).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(3)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();

    verify(mockTestResultAction).getTotalCount();
    verify(mockTestResultAction).getSkipCount();
    verify(mockTestResultAction).getFailCount();
    verify(mockTestResultAction, times(2)).getFailedTests();

    verify(mockProject, times(2)).getName();

    verify(mockDate).getTime();
  }

  @Test
  public void constructorSuccessNoTests() {
    when(mockBuild.getAction(AbstractTestResultAction.class)).thenReturn(null);

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate);

    Assert.assertEquals("Incorrect test results", null, buildData.testResults);

    // Verify the rest of the results
    verify(mockBuild).getId();
    verify(mockBuild, times(2)).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild, times(2)).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getBuiltOn();
    verify(mockBuild, times(2)).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(3)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();

    verify(mockProject, times(2)).getName();

    verify(mockDate).getTime();
  }

  @Test
  public void constructorSuccessWithEnvVars() throws Exception {
    when(mockBuild.getEnvironments()).thenReturn(new EnvironmentList(Arrays.asList(mockEnvironment)));
    when(mockBuild.getBuildVariables()).thenReturn(new HashMap<String, String>());
    when(mockBuild.getSensitiveBuildVariables()).thenReturn(new HashSet<String>());

    when(mockBuild.getBuiltOn()).thenReturn(mockNode);

    when(mockNode.getDisplayName()).thenReturn("Jenkins");
    when(mockNode.getLabelString()).thenReturn("");

    final String envVarKey = "EnvVarKey";
    final String envVarVal = "EnvVarVal";
    doAnswer(new Answer<Void>() {
      @SuppressWarnings("unchecked")
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Map<String, String> output = (Map<String, String>) invocation.getArguments()[0];
        output.put(envVarKey, envVarVal);
        return null;
      }
    }).when(mockEnvironment).buildEnvVars(Matchers.<Map<String, String>>any());

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate);

    // Verify results
    Assert.assertEquals("Wrong number of environment variables", 1, buildData.getBuildVariables().size());
    Assert.assertEquals("Missing environment variable '" + envVarKey + "'", envVarVal, buildData.getBuildVariables().get(envVarKey));

    verify(mockBuild).getId();
    verify(mockBuild, times(2)).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild, times(2)).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getBuiltOn();
    verify(mockBuild, times(2)).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(3)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();

    verify(mockEnvironment).buildEnvVars(Matchers.<Map<String, String>>any());

    verify(mockTestResultAction).getTotalCount();
    verify(mockTestResultAction).getSkipCount();
    verify(mockTestResultAction).getFailCount();
    verify(mockTestResultAction, times(2)).getFailedTests();

    verify(mockProject, times(2)).getName();

    verify(mockDate).getTime();
  }

  @Test
  public void toJsonSuccess() throws Exception {
    BuildData buildData = makeFullBuildData();

    // Unit under test
    JSONObject result = buildData.toJson();

    // Verify results
    JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(FULL_STRING), result);
  }

  BuildData makeFullBuildData() {
    Map<String, String> buildVariables = Collections.emptyMap();
    Set<String> sensitiveBuildVariables = Collections.emptySet();
    BuildData buildData = new BuildData();

    buildData.setBuildDuration(100);
    buildData.setBuildHost("http://localhost:8080/jenkins");
    buildData.setBuildLabel("master");
    buildData.setBuildNum(123);
    buildData.setBuildVariables(buildVariables);
    buildData.setSensitiveBuildVariables(sensitiveBuildVariables);
    buildData.setDescription("DESCRIPTION");
    buildData.setDisplayName("DISPLAY NAME");
    buildData.setFullDisplayName("FULL DISPLAY NAME");
    buildData.setId("TEST_JOB_123");
    buildData.setProjectName("PROJECT_NAME");
    buildData.setResult(Result.SUCCESS);
    buildData.setRootBuildNum(456);
    buildData.setRootProjectDisplayName("ROOT PROJECT DISPLAY NAME");
    buildData.setRootProjectName("ROOT PROJECT NAME");
    buildData.timestamp = "2000-02-01T00:00:00-0800";
    buildData.setUrl("http://localhost:8080/jenkins/jobs/PROJECT_NAME/123");
    buildData.setTestResults(new TestData());

    return buildData;
  }
}

package jenkins.plugins.logstash.persistence;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Environment;
import hudson.model.EnvironmentList;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.plugins.logstash.LogstashConfiguration;
import jenkins.plugins.logstash.persistence.BuildData.TestData;
import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;

@SuppressWarnings("rawtypes")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"})
@PrepareForTest(LogstashConfiguration.class)
public class BuildDataTest {

  static final String FULL_STRING = "{\"id\":\"TEST_JOB_123\",\"result\":\"SUCCESS\",\"fullProjectName\":\"parent/BuildDataTest\","
      + "\"projectName\":\"BuildDataTest\",\"displayName\":\"BuildData Test\",\"fullDisplayName\":\"BuildData Test #123456\","
      + "\"description\":\"Mock project for testing BuildData\",\"url\":\"http://localhost:8080/jenkins/jobs/PROJECT_NAME/123\","
      + "\"buildHost\":\"master\",\"buildLabel\":\"master\",\"buildNum\":123456,\"buildDuration\":60,"
      + "\"rootProjectName\":\"RootBuildDataTest\",\"rootFullProjectName\":\"parent/RootBuildDataTest\","
      + "\"rootProjectDisplayName\":\"Root BuildData Test\",\"rootBuildNum\":456,\"buildVariables\":{},"
      + "\"sensitiveBuildVariables\":[],\"testResults\":{\"totalCount\":0,\"skipCount\":0,\"failCount\":0, \"passCount\":0,"
      + "\"failedTests\":[], \"failedTestsWithErrorDetail\":[]}}";

  @Mock AbstractBuild mockBuild;
  @Mock AbstractBuild mockRootBuild;
  @Mock AbstractTestResultAction mockTestResultAction;
  @Mock Project mockProject;
  @Mock Project mockRootProject;
  @Mock Node mockNode;
  @Mock Environment mockEnvironment;
  @Mock Date mockDate;
  @Mock TaskListener mockListener;
  @Mock Computer mockComputer;
  @Mock Executor mockExecutor;
  @Mock LogstashConfiguration logstashConfiguration;

  @Before
  public void before() throws Exception {

    PowerMockito.mockStatic(LogstashConfiguration.class);
    when(LogstashConfiguration.getInstance()).thenReturn(logstashConfiguration);
    when(logstashConfiguration.getDateFormatter()).thenCallRealMethod();

    when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
    when(mockBuild.getDisplayName()).thenReturn("BuildData Test");
    when(mockBuild.getFullDisplayName()).thenReturn("BuildData Test #123456");
    when(mockBuild.getDescription()).thenReturn("Mock project for testing BuildData");
    when(mockBuild.getParent()).thenReturn(mockProject);
    when(mockBuild.getNumber()).thenReturn(123456);
    when(mockBuild.getTimestamp()).thenReturn(new GregorianCalendar());
    when(mockBuild.getRootBuild()).thenReturn(mockBuild);
    when(mockBuild.getBuildVariables()).thenReturn(Collections.emptyMap());
    when(mockBuild.getSensitiveBuildVariables()).thenReturn(Collections.emptySet());
    when(mockBuild.getEnvironments()).thenReturn(null);
    when(mockBuild.getAction(AbstractTestResultAction.class)).thenReturn(mockTestResultAction);
    when(mockBuild.getEnvironment(mockListener)).thenReturn(new EnvVars());
    when(mockBuild.getRootBuild()).thenReturn(mockRootBuild);

    when(mockTestResultAction.getTotalCount()).thenReturn(0);
    when(mockTestResultAction.getSkipCount()).thenReturn(0);
    when(mockTestResultAction.getFailCount()).thenReturn(0);
    when(mockTestResultAction.getFailedTests()).thenReturn(Collections.emptyList());

    when(mockProject.getName()).thenReturn("BuildDataTest");
    when(mockProject.getFullName()).thenReturn("parent/BuildDataTest");

    when(mockRootBuild.getProject()).thenReturn(mockRootProject);
    when(mockRootBuild.getNumber()).thenReturn(456);
    when(mockRootBuild.getDisplayName()).thenReturn("Root BuildData Test");

    when(mockRootProject.getName()).thenReturn("RootBuildDataTest");
    when(mockRootProject.getFullName()).thenReturn("parent/RootBuildDataTest");

    when(mockDate.getTime()).thenReturn(60L);
    when(mockBuild.getExecutor()).thenReturn(mockExecutor);
    when(mockExecutor.getOwner()).thenReturn(mockComputer);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockBuild);
    verifyNoMoreInteractions(mockTestResultAction);
    verifyNoMoreInteractions(mockProject);
    verifyNoMoreInteractions(mockEnvironment);
    verifyNoMoreInteractions(mockDate);
    verifyNoMoreInteractions(mockRootBuild);
    verifyNoMoreInteractions(mockRootProject);
  }

  private void verifyMocks() throws Exception
  {
    verify(mockProject).getName();
    verify(mockProject).getFullName();

    verify(mockBuild).getId();
    verify(mockBuild).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getStartTimeInMillis();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getExecutor();
    verify(mockBuild).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(4)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();
    verify(mockBuild).getEnvironment(mockListener);

    verify(mockExecutor).getOwner();

    verify(mockRootProject).getName();
    verify(mockRootProject).getFullName();

    verify(mockRootBuild, times(2)).getProject();
    verify(mockRootBuild).getDisplayName();
    verify(mockRootBuild).getNumber();

    verify(mockDate).getTime();
  }

  private void verifyTestResultActions() {
    verify(mockTestResultAction).getTotalCount();
    verify(mockTestResultAction).getSkipCount();
    verify(mockTestResultAction).getFailCount();
    verify(mockTestResultAction, times(1)).getFailedTests();
  }

  private void verifiyNodeActions(int labelCount) {
      verify(mockComputer).getNode();
      verify(mockNode, times(2)).getDisplayName();
      verify(mockNode, times(labelCount)).getLabelString();
  }

  @Test
  public void constructorSuccessBuiltOnNull() throws Exception {
     when(mockComputer.getNode()).thenReturn(null);

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

    // build.getDuration() is always 0 in Notifiers
    Assert.assertEquals("Incorrect buildDuration", 60L, buildData.getBuildDuration());

    // Verify the rest of the results
    Assert.assertEquals("Incorrect buildHost", "master", buildData.getBuildHost());
    Assert.assertEquals("Incorrect buildLabel", "master", buildData.getBuildLabel());

    verifyMocks();
    verifyTestResultActions();
   }

  @Test
  public void constructorSuccessBuiltOnMaster() throws Exception {
    when(mockComputer.getNode()).thenReturn(mockNode);

    when(mockNode.getDisplayName()).thenReturn("Jenkins");
    when(mockNode.getLabelString()).thenReturn("");

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

    // build.getDuration() is always 0 in Notifiers
    Assert.assertEquals("Incorrect buildDuration", 60L, buildData.getBuildDuration());

    // Verify the rest of the results
    Assert.assertEquals("Incorrect buildHost", "Jenkins", buildData.getBuildHost());
    Assert.assertEquals("Incorrect buildLabel", "master", buildData.getBuildLabel());

    verifyMocks();
    verifyTestResultActions();
    verifiyNodeActions(1);
  }

  @Test
  public void constructorSuccessBuiltOnSlave() throws Exception {
    when(mockComputer.getNode()).thenReturn(mockNode);

    when(mockNode.getDisplayName()).thenReturn("Test Slave 01");
    when(mockNode.getLabelString()).thenReturn("Test Slave");

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

    // build.getDuration() is always 0 in Notifiers
    Assert.assertEquals("Incorrect buildDuration", 60L, buildData.getBuildDuration());

    // Verify the rest of the results
    Assert.assertEquals("Incorrect buildHost", "Test Slave 01", buildData.getBuildHost());
    Assert.assertEquals("Incorrect buildLabel", "Test Slave", buildData.getBuildLabel());

    verifyMocks();
    verifyTestResultActions();
    verifiyNodeActions(2);
  }

  @Test
  public void constructorSuccessTestFailures() throws Exception {
    TestResult mockTestResult = Mockito.mock(hudson.tasks.test.TestResult.class);
    when(mockTestResult.getFullName()).thenReturn("Mock Full Test");
    when(mockTestResult.getErrorDetails()).thenReturn("ErrorDetails Test");

    when(mockTestResultAction.getTotalCount()).thenReturn(123);
    when(mockTestResultAction.getSkipCount()).thenReturn(0);
    when(mockTestResultAction.getFailCount()).thenReturn(1);
    when(mockTestResultAction.getFailedTests()).thenReturn(Arrays.asList(mockTestResult));

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

    TestData testResults = buildData.getTestResults();

    Assert.assertEquals("Incorrect test results", 123, testResults.getTotalCount());
    Assert.assertEquals("Incorrect test results", 0, testResults.getSkipCount());
    Assert.assertEquals("Incorrect test results", 1, testResults.getFailCount());
    Assert.assertEquals("Incorrect test details count", 1, testResults.getFailedTestsWithErrorDetail().size());
    Assert.assertEquals("Incorrect failed test error details", "ErrorDetails Test", testResults.getFailedTestsWithErrorDetail().get(0).getErrorDetails());
    Assert.assertEquals("Incorrect failed test fullName", "Mock Full Test", testResults.getFailedTestsWithErrorDetail().get(0).getFullName());

    verifyMocks();
    verifyTestResultActions();
  }

  @Test
  public void constructorSuccessNoTests() throws Exception {
    when(mockBuild.getAction(AbstractTestResultAction.class)).thenReturn(null);

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);
    TestData testResults = buildData.getTestResults();

    Assert.assertEquals("Incorrect test results", null, testResults);

    verifyMocks();
  }

  @Test
  public void constructorSuccessWithEnvVars() throws Exception {
    when(mockBuild.getEnvironments()).thenReturn(new EnvironmentList(Arrays.asList(mockEnvironment)));
    when(mockBuild.getBuildVariables()).thenReturn(new HashMap<String, String>());

    when(mockComputer.getNode()).thenReturn(mockNode);
    when(mockNode.getDisplayName()).thenReturn("Jenkins");
    when(mockNode.getLabelString()).thenReturn("");

    final String envVarKey = "EnvVarKey";
    final String envVarVal = "EnvVarVal";
    final String buildVarKey = "BuildVarKey";
    final String buildVarVal = "BuildVarVal";
    final String sensitiveVarKey = "SensitiveVarKey";

    doAnswer(new Answer<Void>() {
      @SuppressWarnings("unchecked")
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Map<String, String> output = (Map<String, String>) invocation.getArguments()[0];
        output.put(envVarKey, envVarVal);
        output.put(sensitiveVarKey, "privateKey");
        return null;
      }
    }).when(mockEnvironment).buildEnvVars(Matchers.<Map<String, String>>any());
    when(mockBuild.getEnvironment(mockListener)).thenReturn(new EnvVars(buildVarKey, buildVarVal));
    when(mockBuild.getSensitiveBuildVariables()).thenReturn(new HashSet<>(Arrays.asList(sensitiveVarKey)));

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

    // Verify results
    Assert.assertEquals("Wrong number of environment variables", 2, buildData.getBuildVariables().size());
    Assert.assertEquals("Missing environment variable '" + envVarKey + "'", envVarVal, buildData.getBuildVariables().get(envVarKey));
    Assert.assertEquals("Missing environment variable '" + buildVarKey + "'", buildVarVal, buildData.getBuildVariables().get(buildVarKey));
    Assert.assertNull("Found sensitive environment variable '" + sensitiveVarKey + "'", buildData.getBuildVariables().get(sensitiveVarKey));

    verify(mockEnvironment).buildEnvVars(Matchers.<Map<String, String>>any());

    verifyMocks();
    verifyTestResultActions();
    verifiyNodeActions(1);
  }

  @Test // JENKINS-41324
  public void constructorSuccessWithChangedEnvVars() throws Exception {
    when(mockBuild.getEnvironments()).thenReturn(new EnvironmentList(Arrays.asList(mockEnvironment)));
    when(mockBuild.getBuildVariables()).thenReturn(new HashMap<String, String>());
    when(mockBuild.getSensitiveBuildVariables()).thenReturn(new HashSet<String>());

    when(mockComputer.getNode()).thenReturn(mockNode);
    when(mockNode.getDisplayName()).thenReturn("Jenkins");
    when(mockNode.getLabelString()).thenReturn("");

    final String varKey = "modifiedVarKey";
    final String envVarVal = "initialValue";
    final String buildVarVal = "changedValue";
    doAnswer(new Answer<Void>() {
      @SuppressWarnings("unchecked")
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Map<String, String> output = (Map<String, String>) invocation.getArguments()[0];
        output.put(varKey, envVarVal);
        return null;
      }
    }).when(mockEnvironment).buildEnvVars(Matchers.<Map<String, String>>any());
    when(mockBuild.getEnvironment(mockListener)).thenReturn(new EnvVars(varKey, buildVarVal));

    // Unit under test
    BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

    // Verify results
    Assert.assertEquals("Wrong number of environment variables", 1, buildData.getBuildVariables().size());
    Assert.assertEquals("Missing environment variable '" + varKey + "'", buildVarVal, buildData.getBuildVariables().get(varKey));

    verify(mockEnvironment).buildEnvVars(Matchers.<Map<String, String>>any());

    verifyMocks();
    verifyTestResultActions();
    verifiyNodeActions(1);
  }

  @Test
  public void toJsonSuccess() throws Exception
  {
      when(mockBuild.getId()).thenReturn("TEST_JOB_123");
      when(mockBuild.getUrl()).thenReturn("http://localhost:8080/jenkins/jobs/PROJECT_NAME/123");

      BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

      // Unit under test
      JSONObject result = buildData.toJson();

      // Verify results
      JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(FULL_STRING), result);

      verifyMocks();
      verifyTestResultActions();
  }

  @Test
  public void fullName() throws Exception
  {
      when(mockBuild.getId()).thenReturn("TEST_JOB_123");
      when(mockBuild.getUrl()).thenReturn("http://localhost:8080/jenkins/jobs/PROJECT_NAME/123");

      BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

      Assert.assertEquals(buildData.getFullProjectName(), "parent/BuildDataTest");

      verifyMocks();
      verifyTestResultActions();
  }

  @Test
  public void rootProjectFullName() throws Exception
  {
      when(mockBuild.getId()).thenReturn("TEST_JOB_123");
      when(mockBuild.getUrl()).thenReturn("http://localhost:8080/jenkins/jobs/PROJECT_NAME/123");

      BuildData buildData = new BuildData(mockBuild, mockDate, mockListener);

      Assert.assertEquals(buildData.getRootFullProjectName(), "parent/RootBuildDataTest");

      verifyMocks();
      verifyTestResultActions();
  }
}

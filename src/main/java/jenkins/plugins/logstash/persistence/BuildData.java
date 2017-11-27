/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.logstash.persistence;

import hudson.model.Action;
import hudson.model.Environment;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.Node;
import hudson.model.Executor;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * POJO for mapping build info to JSON.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public class BuildData {
  // ISO 8601 date format
  public transient static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private final static Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  public static class TestData {
    int totalCount, skipCount, failCount, passCount;
    List<FailedTest> failedTestsWithErrorDetail;
    List<String> failedTests;

    public static class FailedTest {
      final String fullName, errorDetails;

		public FailedTest(String fullName, String errorDetails) {
			super();
			this.fullName = fullName;
			this.errorDetails = errorDetails;
		}
    }

    public TestData() {
      this(null);
    }

    public TestData(Action action) {
      AbstractTestResultAction<?> testResultAction = null;
      if (action instanceof AbstractTestResultAction) {
        testResultAction = (AbstractTestResultAction<?>) action;
      }

      if (testResultAction == null) {
        totalCount = skipCount = failCount = 0;
        failedTests = Collections.emptyList();
        failedTestsWithErrorDetail = Collections.emptyList();
        return;
      }

      totalCount = testResultAction.getTotalCount();
      skipCount = testResultAction.getSkipCount();
      failCount = testResultAction.getFailCount();
      passCount = totalCount - skipCount - failCount;

      failedTests = new ArrayList<String>();
      failedTestsWithErrorDetail = new ArrayList<FailedTest>();
      for (TestResult result : testResultAction.getFailedTests()) {
          failedTests.add(result.getFullName());
          failedTestsWithErrorDetail.add(new FailedTest(result.getFullName(),result.getErrorDetails()));
      }
    }
  }

  protected String id;
  protected String result;
  protected String projectName;
  protected String fullProjectName;
  protected String displayName;
  protected String fullDisplayName;
  protected String description;
  protected String url;
  protected String buildHost;
  protected String buildLabel;
  protected int buildNum;
  protected long buildDuration;
  protected transient String timestamp; // This belongs in the root object
  protected String rootProjectName;
  protected String rootFullProjectName;
  protected String rootProjectDisplayName;
  protected int rootBuildNum;
  protected Map<String, String> buildVariables;
  protected Set<String> sensitiveBuildVariables;
  protected TestData testResults = null;

  // Freestyle project build
  public BuildData(AbstractBuild<?, ?> build, Date currentTime, TaskListener listener) {
    initData(build, currentTime);

    Node node = build.getBuiltOn();
    if (node == null) {
      buildHost = "master";
      buildLabel = "master";
    } else {
      buildHost = StringUtils.isBlank(node.getDisplayName()) ? "master" : node.getDisplayName();
      buildLabel = StringUtils.isBlank(node.getLabelString()) ? "master" : node.getLabelString();
    }

    // build.getDuration() is always 0 in Notifiers
    rootProjectName = build.getRootBuild().getProject().getName();
    rootFullProjectName = build.getRootBuild().getProject().getFullName();
    rootProjectDisplayName = build.getRootBuild().getDisplayName();
    rootBuildNum = build.getRootBuild().getNumber();
    buildVariables = build.getBuildVariables();
    sensitiveBuildVariables = build.getSensitiveBuildVariables();

    // Get environment build variables and merge them into the buildVariables map
    Map<String, String> buildEnvVariables = new HashMap<String, String>();
    List<Environment> buildEnvironments = build.getEnvironments();
    if (buildEnvironments != null) {
      for (Environment env : buildEnvironments) {
        if (env == null) {
          continue;
        }

        env.buildEnvVars(buildEnvVariables);
        if (!buildEnvVariables.isEmpty()) {
          buildVariables.putAll(buildEnvVariables);
          buildEnvVariables.clear();
        }
      }
    }
    try {
      buildVariables.putAll(build.getEnvironment(listener));
    } catch (Exception e) {
      // no base build env vars to merge
      LOGGER.log(WARNING,"Unable update logstash buildVariables with EnvVars from " + build.getDisplayName(),e);
    }
    for (String key : sensitiveBuildVariables) {
      buildVariables.remove(key);
    }
  }

  // Pipeline project build
  public BuildData(Run<?, ?> build, Date currentTime, TaskListener listener) {
    initData(build, currentTime);

    Executor executor = build.getExecutor();
    if (executor == null) {
      buildHost = "master";
    } else {
      buildHost = StringUtils.isBlank(executor.getDisplayName()) ? "master" : executor.getDisplayName();
    }

    rootProjectName = projectName;
    rootFullProjectName = fullProjectName;
    rootProjectDisplayName = displayName;
    rootBuildNum = buildNum;

    try {
      // TODO: sensitive variables are not filtered, c.f. https://stackoverflow.com/questions/30916085
      buildVariables = build.getEnvironment(listener);
    } catch (IOException | InterruptedException e) {
      LOGGER.log(WARNING,"Unable to get environment for " + build.getDisplayName(),e);
      buildVariables = new HashMap<String, String>();
    }
  }

  private void initData(Run<?, ?> build, Date currentTime) {
    result = build.getResult() == null ? null : build.getResult().toString();
    id = build.getId();
    projectName = build.getParent().getName();
    fullProjectName = build.getParent().getFullName();
    displayName = build.getDisplayName();
    fullDisplayName = build.getFullDisplayName();
    description = build.getDescription();
    url = build.getUrl();
    buildNum = build.getNumber();

    Action testResultAction = build.getAction(AbstractTestResultAction.class);
    if (testResultAction != null) {
      testResults = new TestData(testResultAction);
    }

    buildDuration = currentTime.getTime() - build.getStartTimeInMillis();
    timestamp = DATE_FORMATTER.format(build.getTimestamp().getTime());
  }

  @Override
  public String toString() {
    Gson gson = new GsonBuilder().create();
    return gson.toJson(this);
  }

  public JSONObject toJson() {
    String data = toString();
    return JSONObject.fromObject(data);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getResult() {
    return result;
  }

  public void setResult(Result result) {
    this.result = result.toString();
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getFullProjectName() {
    return fullProjectName;
  }

  public void setFullProjectName(String fullProjectName) {
    this.fullProjectName = fullProjectName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getFullDisplayName() {
    return fullDisplayName;
  }

  public void setFullDisplayName(String fullDisplayName) {
    this.fullDisplayName = fullDisplayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getBuildHost() {
    return buildHost;
  }

  public void setBuildHost(String buildHost) {
    this.buildHost = buildHost;
  }

  public String getBuildLabel() {
    return buildLabel;
  }

  public void setBuildLabel(String buildLabel) {
    this.buildLabel = buildLabel;
  }

  public int getBuildNum() {
    return buildNum;
  }

  public void setBuildNum(int buildNum) {
    this.buildNum = buildNum;
  }

  public long getBuildDuration() {
    return buildDuration;
  }

  public void setBuildDuration(long buildDuration) {
    this.buildDuration = buildDuration;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Calendar timestamp) {
    this.timestamp = DATE_FORMATTER.format(timestamp.getTime());
  }

  public String getRootProjectName() {
    return rootProjectName;
  }

  public void setRootProjectName(String rootProjectName) {
    this.rootProjectName = rootProjectName;
  }

  public String getRootFullProjectName() {
    return rootFullProjectName;
  }

  public void setRootFullProjectName(String rootFullProjectName) {
    this.rootFullProjectName = rootFullProjectName;
  }

  public String getRootProjectDisplayName() {
    return rootProjectDisplayName;
  }

  public void setRootProjectDisplayName(String rootProjectDisplayName) {
    this.rootProjectDisplayName = rootProjectDisplayName;
  }

  public int getRootBuildNum() {
    return rootBuildNum;
  }

  public void setRootBuildNum(int rootBuildNum) {
    this.rootBuildNum = rootBuildNum;
  }

  public Map<String, String> getBuildVariables() {
    return buildVariables;
  }

  public void setBuildVariables(Map<String, String> buildVariables) {
    this.buildVariables = buildVariables;
  }

  public Set<String> getSensitiveBuildVariables() {
    return sensitiveBuildVariables;
  }

  public void setSensitiveBuildVariables(Set<String> sensitiveBuildVariables) {
    this.sensitiveBuildVariables = sensitiveBuildVariables;
  }

  public TestData getTestResults() {
    return testResults;
  }

  public void setTestResults(TestData testResults) {
    this.testResults = testResults;
  }
}

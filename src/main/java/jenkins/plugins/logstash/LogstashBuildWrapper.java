/*
 * The MIT License
 *
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
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

package jenkins.plugins.logstash;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper.VarPasswordPair;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsConfig;

/**
 * Build wrapper that decorates the build's logger to insert a
 * {@link LogstashNote} on each output line.
 *
 * @author K Jonathan Harker
 */
public class LogstashBuildWrapper extends BuildWrapper {

  /**
   * Create a new {@link LogstashBuildWrapper}.
   */
  @DataBoundConstructor
  public LogstashBuildWrapper() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

    return new Environment() {};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) {
    LogstashWriter logstash = getLogStashWriter(build, logger);

    LogstashOutputStream los = new LogstashOutputStream(logger, logstash);

    if (build.getProject() instanceof BuildableItemWithBuildWrappers) {
      BuildableItemWithBuildWrappers project = (BuildableItemWithBuildWrappers) build.getProject();
      for (BuildWrapper wrapper: project.getBuildWrappersList()) {
        if (wrapper instanceof MaskPasswordsBuildWrapper) {
          List<VarPasswordPair> allPasswordPairs = new ArrayList<VarPasswordPair>();

          MaskPasswordsBuildWrapper maskPasswordsWrapper = (MaskPasswordsBuildWrapper) wrapper;
          List<VarPasswordPair> jobPasswordPairs = maskPasswordsWrapper.getVarPasswordPairs();
          if (jobPasswordPairs != null) {
            allPasswordPairs.addAll(jobPasswordPairs);
          }

          MaskPasswordsConfig config = MaskPasswordsConfig.getInstance();
          List<VarPasswordPair> globalPasswordPairs = config.getGlobalVarPasswordPairs();
          if (globalPasswordPairs != null) {
            allPasswordPairs.addAll(globalPasswordPairs);
          }

          return los.maskPasswords(allPasswordPairs);
        }
      }
    }

    return los;
  }

  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  // Method to encapsulate calls for unit-testing
  LogstashWriter getLogStashWriter(AbstractBuild<?, ?> build, OutputStream errorStream) {
    return new LogstashWriter(build, errorStream, null);
  }

  /**
   * Registers {@link LogstashBuildWrapper} as a {@link BuildWrapper}.
   */
  @Extension
  public static class DescriptorImpl extends BuildWrapperDescriptor {

    public DescriptorImpl() {
      super(LogstashBuildWrapper.class);
      load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
      return true;
    }
  }
}

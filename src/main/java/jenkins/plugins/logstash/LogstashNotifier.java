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

package jenkins.plugins.logstash;

import hudson.Extension;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.jenkinsci.Symbol;

/**
 * Post-build action to push build log to Logstash.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public class LogstashNotifier extends Notifier implements SimpleBuildStep {

  private static final Logger LOGGER = Logger.getLogger(LogstashNotifier.class.getName());

  private final int maxLines;
  private final boolean failBuild;

  @DataBoundConstructor
  public LogstashNotifier(int maxLines, boolean failBuild) {
    this.maxLines = maxLines;
    this.failBuild = failBuild;
  }

  public int getMaxLines()
  {
    return maxLines;
  }

  public boolean isFailBuild()
  {
    return failBuild;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    return perform(build, listener);
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher,
    TaskListener listener) throws InterruptedException, IOException {
    if (!perform(run, listener)) {
      run.setResult(Result.FAILURE);
    }
  }

  private boolean perform(Run<?, ?> run, TaskListener listener) {
    LogstashConfiguration configuration = LogstashConfiguration.getInstance();
    if (!configuration.isEnabled())
    {
      LOGGER.log(Level.FINE, "Logstash is disabled. Logs will not be forwarded.");
      return true;
    }

    PrintStream errorPrintStream = listener.getLogger();
    LogstashWriter logstash = getLogStashWriter(run, errorPrintStream, listener);
    logstash.writeBuildLog(maxLines);
    return !(failBuild && logstash.isConnectionBroken());
  }

  // Method to encapsulate calls for unit-testing
  LogstashWriter getLogStashWriter(Run<?, ?> run, OutputStream errorStream, TaskListener listener) {
    return new LogstashWriter(run, errorStream, listener, run.getCharset());
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    // We don't call Run#getPreviousBuild() so no external synchronization between builds is required
    return BuildStepMonitor.NONE;
  }

  @Override
  public Descriptor getDescriptor() {
    return (Descriptor) super.getDescriptor();
  }

  @Extension @Symbol("logstashSend")
  public static class Descriptor extends BuildStepDescriptor<Publisher> {

    @Override
    public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }

    /*
     * Form validation methods
     */
    public FormValidation doCheckMaxLines(@QueryParameter("value") String value) {
      try {
        Integer.parseInt(value);
      } catch (NumberFormatException e) {
        return FormValidation.error(Messages.ValueIsInt());
      }

      return FormValidation.ok();
    }

  }
}

package jenkins.plugins.logstash.pipeline;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import jenkins.YesNoMaybe;
import jenkins.plugins.logstash.LogstashConsoleLogFilter;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;

/**
 * Pipeline plug-in step for logstash.
 */
public class LogstashStep extends Step {

  @CheckForNull
  private SecureGroovyScript secureGroovyScript;

  /** Constructor. */
  @DataBoundConstructor
  public LogstashStep() {}

  @DataBoundSetter
  public void setSecureGroovyScript(@CheckForNull SecureGroovyScript script){
    this.secureGroovyScript = script != null ? script.configuringWithNonKeyItem() : null;
  }

  @Override public StepExecution start(StepContext context) throws Exception {
    return new ExecutionImpl(context, secureGroovyScript);
  }

  /** Execution for {@link LogstashStep}. */
  public static class ExecutionImpl extends AbstractStepExecutionImpl {

    private static final long serialVersionUID = 1L;

    @CheckForNull
    private SecureGroovyScript secureGroovyScript;

    /** Constructor. */
    public ExecutionImpl(StepContext context, SecureGroovyScript script) {
      super(context);
      this.secureGroovyScript = script;
    }

    /** {@inheritDoc} */
    @Override
    public boolean start() throws Exception {
      StepContext context = getContext();
      context
          .newBodyInvoker()
          .withContext(createConsoleLogFilter(context))
          .withCallback(BodyExecutionCallback.wrap(context))
          .start();
      return false;
    }

    private ConsoleLogFilter createConsoleLogFilter(StepContext context)
        throws IOException, InterruptedException {
      ConsoleLogFilter original = context.get(ConsoleLogFilter.class);
      Run<?, ?> build = context.get(Run.class);
      LogstashStep step = context.get(LogstashStep.class);
      ConsoleLogFilter subsequent = new LogstashConsoleLogFilter(build, secureGroovyScript);
      return BodyInvoker.mergeConsoleLogFilters(original, subsequent);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
      getContext().onFailure(cause);
    }
  }

  /** Descriptor for {@link LogstashStep}. */
  @Extension(dynamicLoadable = YesNoMaybe.YES, optional = true)
  public static class DescriptorImpl extends AbstractStepDescriptorImpl {

    /** Constructor. */
    public DescriptorImpl() {
      super(ExecutionImpl.class);
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
      return "Send individual log lines to Logstash";
    }

    /** {@inheritDoc} */
    @Override
    public String getFunctionName() {
      return "logstash";
    }

    /** {@inheritDoc} */
    @Override
    public boolean takesImplicitBlockArgument() {
      return true;
    }
  }
}

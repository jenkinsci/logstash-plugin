package jenkins.plugins.logstash.pipeline;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import jenkins.YesNoMaybe;
import jenkins.plugins.logstash.LogstashConsoleLogFilter;

/**
 * This is the pipeline counterpart of the LogstashJobProperty.
 * This step will send the logs line by line to an indexer.
 */
public class LogstashStep extends Step {

  /** Constructor. */
  @DataBoundConstructor
  public LogstashStep() {}

  @Override
  public StepExecution start(StepContext context) throws Exception
  {
    return new Execution(context);
  }

  /** Execution for {@link LogstashStep}. */
  public static class Execution extends AbstractStepExecutionImpl  {

    public Execution(StepContext context)
    {
      super(context);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public void onResume()
    {
    }

    /** {@inheritDoc} */
    @Override
    public boolean start() throws Exception {
      StepContext context = getContext();
      context
          .newBodyInvoker()
          .withContext(createConsoleLogFilter(context))
          .withContext(context.get(EnvVars.class))
          .withCallback(BodyExecutionCallback.wrap(context))
          .start();
      return false;
    }

    private ConsoleLogFilter createConsoleLogFilter(StepContext context)
        throws IOException, InterruptedException {
      ConsoleLogFilter original = context.get(ConsoleLogFilter.class);
      Run<?, ?> build = context.get(Run.class);
      EnvVars envVars = context.get(EnvVars.class);
      ConsoleLogFilter subsequent = new LogstashConsoleLogFilter(build, envVars);
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
  public static class DescriptorImpl extends StepDescriptor {

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

    @Override
    public Set<? extends Class<?>> getRequiredContext()
    {
      Set<Class<?>> contexts = new HashSet<>();
      contexts.add(Run.class);
      return contexts;
    }
  }

}

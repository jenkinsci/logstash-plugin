package jenkins.plugins.logstash.pipeline;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStep;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.YesNoMaybe;
import jenkins.plugins.logstash.LogstashConfiguration;

/**
 * This is the pipeline counterpart of the LogstashJobProperty.
 * This step will send the logs line by line to an indexer.
 */
public class LogstashStep extends Step {

  private static final Logger LOGGER = Logger.getLogger(LogstashStep.class.getName());
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
      BodyInvoker invoker = context.newBodyInvoker().withCallback(BodyExecutionCallback.wrap(context));
      if (LogstashConfiguration.getInstance().isEnableGlobally()) {
        context.get(TaskListener.class).getLogger().println("The logstash step is unnecessary when logstash is enabled for all builds.");
      } else {
            invoker.withContext(getMergedDecorator(context));
      }
      invoker.start();
      return false;
    }

    private TaskListenerDecorator getMergedDecorator(StepContext context)
        throws IOException, InterruptedException {
      Run<?, ?> run = context.get(Run.class);
      FlowNode node = context.get(FlowNode.class);
      FlowNode stageNode = getStageNode(node);
      String stageName = null;
      if (stageNode != null) {
        LabelAction labelAction = stageNode.getAction(LabelAction.class);
        if (labelAction != null) {
            stageName = labelAction.getDisplayName();
        }
      }
      String agentName = getAgentName(node);
      return TaskListenerDecorator.merge(context.get(TaskListenerDecorator.class), new GlobalDecorator((WorkflowRun) run, stageName, agentName));
    }

    private String getAgentName(FlowNode node) {
      for (BlockStartNode bsn : node.iterateEnclosingBlocks()) {
          if (bsn instanceof StepNode) {
              StepDescriptor descriptor = ((StepNode) bsn).getDescriptor();
              if (descriptor instanceof ExecutorStep.DescriptorImpl) {
                  WorkspaceAction workspaceAction = bsn.getAction(WorkspaceAction.class);
                  if (workspaceAction != null) {
                      return workspaceAction.getNode();
                  }
              }
          }
      }

      return null;
  }

    private FlowNode getStageNode(FlowNode node) {
      for (BlockStartNode bsn : node.iterateEnclosingBlocks()) {
        if (isStageNode(bsn)) {
          return bsn;
        }
      }
      return null;
    }

    private boolean isStageNode(FlowNode node) {
        if (node instanceof StepNode) {
            StepDescriptor descriptor = ((StepNode) node).getDescriptor();
            if (descriptor instanceof StageStep.DescriptorImpl) {
                LabelAction labelAction = node.getAction(LabelAction.class);
                if (labelAction != null) {
                    return true;
                }
            }
        }

        return false;
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
    public Set<? extends Class<?>> getRequiredContext() {
      Set<Class<?>> context = new HashSet<>();
      Collections.addAll(context, Run.class, FlowNode.class);
      return Collections.unmodifiableSet(context);
    }
  }

}

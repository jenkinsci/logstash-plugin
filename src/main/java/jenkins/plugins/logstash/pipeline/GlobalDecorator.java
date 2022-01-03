package jenkins.plugins.logstash.pipeline;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.plugins.logstash.LogstashConfiguration;
import jenkins.plugins.logstash.LogstashOutputStream;
import jenkins.plugins.logstash.LogstashWriter;

@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class GlobalDecorator extends TaskListenerDecorator {
  private static final Logger LOGGER = Logger.getLogger(GlobalDecorator.class.getName());

  private transient Run<?, ?> run;
  private String stageName;
  private String agentName;
  private LogstashWriter writer;

  public GlobalDecorator(WorkflowRun run) {
    this(run, null, null);
  }
  public GlobalDecorator(WorkflowRun run, String stageName, String agentName) {
    LOGGER.log(Level.INFO, "Creating decorator for {0}", run.toString());
    this.run = run;
    this.stageName = stageName;
    this.agentName = agentName;

  }

  @Override
  public OutputStream decorate(OutputStream logger) throws IOException, InterruptedException {
    writer = new LogstashWriter(run, logger, null, StandardCharsets.UTF_8, stageName, agentName);
    // this.writer = new LogstashWriter(run, logger, null, StandardCharsets.UTF_8, stageName, agentName);

    // writer.setErrorStream(logger);
    LogstashOutputStream out = new LogstashOutputStream(logger, writer);
    return out;
  }

  @Extension
  public static final class Factory implements TaskListenerDecorator.Factory {

    @Override
    public TaskListenerDecorator of(FlowExecutionOwner owner) {
      if (!LogstashConfiguration.getInstance().isEnableGlobally()) {
        return null;
      }
      try {
        Queue.Executable executable = owner.getExecutable();
        if (executable instanceof WorkflowRun) {
          return new GlobalDecorator((WorkflowRun) executable);
        }
      } catch (IOException x) {
        LOGGER.log(Level.WARNING, null, x);
      }
      return null;
    }
  }
}

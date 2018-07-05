package jenkins.plugins.logstash;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;

@Extension(ordinal = 1000)
public class LogstashConsoleLogFilter extends ConsoleLogFilter implements Serializable
{

  private static Logger LOGGER = Logger.getLogger(LogstashConsoleLogFilter.class.getName());

  private transient Run<?, ?> run;
  public LogstashConsoleLogFilter() {};

  public LogstashConsoleLogFilter(Run<?, ?> run)
  {
    this.run = run;
  }
  private static final long serialVersionUID = 1L;

  @Override
  public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException
  {
    LogstashConfiguration configuration = LogstashConfiguration.getInstance();
    if (!configuration.isEnabled())
    {
      LOGGER.log(Level.FINE, "Logstash is disabled. Logs will not be forwarded.");
      return logger;
    }

    if (build != null && build instanceof AbstractBuild<?, ?>)
    {
      if (isLogstashEnabled(build))
      {
        LogstashWriter logstash = getLogStashWriter(build, logger);
        return new LogstashOutputStream(logger, logstash);
      }
      else
      {
        return logger;
      }
    }
    if (run != null)
    {
      LogstashWriter logstash = getLogStashWriter(run, logger);
      return new LogstashOutputStream(logger, logstash);
    }
    else
    {
      return logger;
    }
  }

  LogstashWriter getLogStashWriter(Run<?, ?> build, OutputStream errorStream)
  {
    return new LogstashWriter(build, errorStream, null, build.getCharset());
  }

  private boolean isLogstashEnabled(Run<?, ?> build)
  {
    LogstashConfiguration configuration = LogstashConfiguration.getInstance();
    if (configuration.isEnableGlobally())
    {
      return true;
    }

    if (build.getParent() instanceof AbstractProject)
    {
      AbstractProject<?, ?> project = (AbstractProject<?, ?>)build.getParent();
      if (project.getProperty(LogstashJobProperty.class) != null)
      {
        return true;
      }
    }
    return false;
  }

}

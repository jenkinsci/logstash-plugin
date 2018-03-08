package jenkins.plugins.logstash;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;

@Extension(ordinal = 1000)
public class LogstashConsoleLogFilter extends ConsoleLogFilter implements Serializable
{

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

    if (build.getParent() instanceof BuildableItemWithBuildWrappers)
    {
      BuildableItemWithBuildWrappers project = (BuildableItemWithBuildWrappers)build.getParent();
      for (BuildWrapper wrapper : project.getBuildWrappersList())
      {
        if (wrapper instanceof LogstashBuildWrapper)
        {
          return true;
        }
      }
    }
    return false;
  }

}

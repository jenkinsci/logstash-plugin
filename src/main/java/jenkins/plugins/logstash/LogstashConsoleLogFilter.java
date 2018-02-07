package jenkins.plugins.logstash;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;

@Extension(ordinal = 1000)
public class LogstashConsoleLogFilter extends ConsoleLogFilter
{

  @Override
  public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException
  {
    if (isLogstashEnabled(build))
    {
      LogstashWriter logstash = getLogStashWriter(build, logger);
      return new LogstashOutputStream(logger, logstash);
    }
    return logger;
  }

  LogstashWriter getLogStashWriter(AbstractBuild<?, ?> build, OutputStream errorStream)
  {
    return new LogstashWriter(build, errorStream, null, build.getCharset());
  }

  private boolean isLogstashEnabled(AbstractBuild<?, ?> build)
  {
    if (build.getProject() instanceof BuildableItemWithBuildWrappers)
    {
      BuildableItemWithBuildWrappers project = (BuildableItemWithBuildWrappers)build.getProject();
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

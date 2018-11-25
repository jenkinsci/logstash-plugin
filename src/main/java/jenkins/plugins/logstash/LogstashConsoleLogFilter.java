package jenkins.plugins.logstash;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.Publisher;

@Extension(ordinal = 1000)
public class LogstashConsoleLogFilter extends ConsoleLogFilter implements Serializable
{

  private static final Logger LOGGER = Logger.getLogger(LogstashConsoleLogFilter.class.getName());

  private transient Run<?, ?> run;

  public LogstashConsoleLogFilter() {}

  public LogstashConsoleLogFilter(Run<?, ?> run)
  {
    this.run = run;
  }

  private static final long serialVersionUID = 1L;

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException
  {

    LogstashConfiguration configuration = LogstashConfiguration.getInstance();
    if (!configuration.isEnabled())
    {
      LOGGER.log(Level.FINE, "Logstash is disabled. Logs will not be forwarded.");
      return logger;
    }


    // A pipeline step uses the constructor which sets run.
    if (run != null)
    {
      if (run.getAction(LogstashMarkerAction.class) != null)
      {
        LOGGER.log(Level.FINEST, "Logstash is enabled globally. No need to decorate the logger another time for {0}",
            run.toString());
        return logger;
      }
      return getLogstashOutputStream(run, logger);
    }

    LogstashMarkerAction markerAction = new LogstashMarkerAction();
    build.addAction(markerAction);

    // Not pipeline step so @{code build} should be set.
    if (isLogstashEnabled(build))
    {
      markerAction.setLineModeEnabled(true);
      return getLogstashOutputStream(build, logger);
    }

    return logger;
  }

  private LogstashOutputStream getLogstashOutputStream(Run<?, ?> run, OutputStream logger)
  {
    LogstashWriter logstash = getLogStashWriter(run, logger);
    return new LogstashOutputStream(logger, logstash);
  }

  LogstashWriter getLogStashWriter(Run<?, ?> build, OutputStream errorStream)
  {
    return new LogstashWriter(build, errorStream, null, build.getCharset());
  }

  private boolean isLogstashEnabledGlobally()
  {
    LogstashConfiguration configuration = LogstashConfiguration.getInstance();
    if (configuration.getGlobalMode() == GloballyEnabledMode.LINEMODE)
    {
      LOGGER.log(Level.FINEST, "Line mode is enabled globally.");
      return true;
    }
    return false;
  }

  private boolean isLogstashEnabled(Run<?, ?> build)
  {
    if (build == null)
    {
      return false;
    }

    if (build.getParent() instanceof AbstractProject)
    {
      AbstractProject<?, ?> project = (AbstractProject<?, ?>)build.getParent();
      LogstashJobProperty property = project.getProperty(LogstashJobProperty.class);
      if (property != null)
      {
        LOGGER.log(Level.FINEST, "Property is set and disableGlobal is: " + property.isDisableGlobal());
        return !property.isDisableGlobal();
      }
      else
      {
        if (PluginImpl.getLogstashNotifier(project) != null)
        {
          return false;
        }
        return isLogstashEnabledGlobally();
      }
    }
    return false;
  }
}

package jenkins.plugins.logstash;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.ui.AbstractProcessingFilter;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.Publisher;

@Extension
public class LogstashRunListener extends RunListener<Run<?,?>>
{

  private static final Logger LOGGER = Logger.getLogger(LogstashRunListener.class.getName());

  @Override
  public void onFinalized(Run<?, ?> run)
  {
    LogstashConfiguration config = LogstashConfiguration.getInstance();
    if (!config.isEnabled())
    {
      return;
    }
    LogstashMarkerAction markerAction = run.getAction(LogstashMarkerAction.class);
    if ((markerAction != null && markerAction.isRunNotifierAtEnd()) || config.getGlobalMode() == GloballyEnabledMode.NOTIFIERMODE)
    {
      int maxLines = config.getMaxLines();
      if (run.getParent() instanceof AbstractProject)
      {
        AbstractProject<?, ?> project = (AbstractProject<?, ?>) run.getParent();
        //don't run if project has a the wrapper explicitly enabled or disabled global settings
        if (project.getProperty(LogstashJobProperty.class) != null)
        {
          LOGGER.log(Level.FINEST, "Job Property is set. Disabling global notifier mode.");
          return;
        }
        //don't run if project has the notifier explicitly enabled
        if (PluginImpl.getLogstashNotifier(project) != null)
        {
          if (markerAction == null || !markerAction.isRunNotifierAtEnd())
          {
            LOGGER.log(Level.FINEST, "Job has explicit Notifier not running at end.");
            return;
          }
        }
      }
      if (markerAction != null && markerAction.isRunNotifierAtEnd())
      {
        maxLines = markerAction.getMaxLines();
      }
      LogstashWriter logstash = new LogstashWriter(run, null, null, run.getCharset());
      logstash.writeBuildLog(maxLines);
    }
  }
}

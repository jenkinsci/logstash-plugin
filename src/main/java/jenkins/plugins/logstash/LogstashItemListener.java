package jenkins.plugins.logstash;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.BulkChange;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;

@Extension
public class LogstashItemListener extends ItemListener
{

  private static Logger LOGGER = Logger.getLogger(LogstashItemListener.class.getName());

  @Override
  public void onCreated(Item item)
  {
    if (item instanceof BuildableItemWithBuildWrappers)
    {
      convertBuildWrapperToJobProperty((BuildableItemWithBuildWrappers)item);
    }
  }

  @Override
  public void onLoaded()
  {
    for (BuildableItemWithBuildWrappers item : Jenkins.getInstance().getAllItems(BuildableItemWithBuildWrappers.class))
    {
      convertBuildWrapperToJobProperty(item);
    }
  }

  static void convertBuildWrapperToJobProperty(BuildableItemWithBuildWrappers item)
  {
    DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappers = item.getBuildWrappersList();
    LogstashBuildWrapper logstashBuildWrapper = wrappers.get(LogstashBuildWrapper.class);
    if (logstashBuildWrapper != null && item instanceof AbstractProject<?, ?>)
    {
      AbstractProject<?, ?> project = (AbstractProject<?, ?>)item;
      BulkChange bc = new BulkChange(project);
      try
      {
        project.addProperty(new LogstashJobProperty());
        wrappers.remove(logstashBuildWrapper);
        bc.commit();
      }
      catch (IOException e)
      {
        LOGGER.log(Level.SEVERE,
            "Failed to convert LogstashBuildWrapper to LogstashJobProperty for project " + project.getFullName(), e);
      }
      finally
      {
        bc.abort();
      }
    }
  }

}

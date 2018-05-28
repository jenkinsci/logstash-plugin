package jenkins.plugins.logstash;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

@Extension
public class LogstashSaveableListener extends SaveableListener
{

  @Override
  public void onChange(Saveable o, XmlFile file)
  {
    if (o instanceof BuildableItemWithBuildWrappers)
    {
      LogstashItemListener.convertBuildWrapperToJobProperty((BuildableItemWithBuildWrappers)o);
    }
  }

}

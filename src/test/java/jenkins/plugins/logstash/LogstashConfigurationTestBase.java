package jenkins.plugins.logstash;

import java.io.File;

import hudson.XmlFile;

public class LogstashConfigurationTestBase
{
  protected static File configFile;

  public static class LogstashConfigurationForTest extends LogstashConfiguration
  {

    @Override
    public synchronized void save()
    {
    }

    @Override
    protected XmlFile getConfigFile()
    {
      return new XmlFile(configFile);
    }
  }

}

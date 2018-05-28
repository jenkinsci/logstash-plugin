package jenkins.plugins.logstash.configuration;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import jenkins.plugins.logstash.persistence.LogstashDao;

public class Logstash extends HostBasedLogstashIndexer<LogstashDao>
{

  @DataBoundConstructor
  public Logstash()
  {
  }

  @Override
  protected LogstashDao createIndexerInstance()
  {
    return new LogstashDao(getHost(), getPort());
  }

  @Extension
  public static class Descriptor extends LogstashIndexerDescriptor
  {

    @Override
    public String getDisplayName()
    {
      return "Logstash TCP";
    }

    @Override
    public int getDefaultPort()
    {
      return 9000;
    }

  }
}

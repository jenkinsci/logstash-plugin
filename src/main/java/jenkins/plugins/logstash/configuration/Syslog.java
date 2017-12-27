package jenkins.plugins.logstash.configuration;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.syslog.MessageFormat;

import hudson.Extension;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.SyslogProtocol;
import jenkins.plugins.logstash.persistence.SyslogDao;

public class Syslog extends LogstashIndexer<SyslogDao>
{
  private MessageFormat messageFormat;
  private SyslogProtocol syslogProtocol;

  @DataBoundConstructor
  public Syslog()
  {
  }

  public MessageFormat getMessageFormat()
  {
    return messageFormat;
  }

  @DataBoundSetter
  public void setMessageFormat(MessageFormat messageFormat)
  {
    this.messageFormat = messageFormat;
  }

  public SyslogProtocol getSyslogProtocol()
  {
    return syslogProtocol;
  }

  @DataBoundSetter()
  public void setSyslogProtocol(SyslogProtocol syslogProtocol)
  {
    this.syslogProtocol = syslogProtocol;
  }

  @Override
  protected boolean shouldRefreshInstance()
  {
    return super.shouldRefreshInstance() ||
        !instance.getMessageFormat().equals(messageFormat);
  }

  @Override
  public SyslogDao createIndexerInstance()
  {
    SyslogDao syslogDao = new SyslogDao(host, port);
    syslogDao.setMessageFormat(messageFormat);
    return syslogDao;
  }

  @Extension
  public static class SyslogDescriptor extends LogstashIndexerDescriptor
  {

    @Override
    public String getDisplayName()
    {
      return "Syslog";
    }

    @Override
    public int getDefaultPort()
    {
      return 519;
    }
  }
}

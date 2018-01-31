package jenkins.plugins.logstash.configuration;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.syslog.MessageFormat;

import hudson.Extension;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.SyslogProtocol;
import jenkins.plugins.logstash.persistence.SyslogDao;

public class Syslog extends HostBasedLogstashIndexer<SyslogDao>
{
  private MessageFormat messageFormat;
  private SyslogProtocol syslogProtocol;

  @DataBoundConstructor
  public Syslog()
  {}

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
  public int hashCode()
  {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((messageFormat == null) ? 0 : messageFormat.hashCode());
    result = prime * result + ((syslogProtocol == null) ? 0 : syslogProtocol.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Syslog other = (Syslog)obj;
    if (messageFormat != other.messageFormat)
      return false;
    if (syslogProtocol != other.syslogProtocol)
      return false;
    return true;
  }

  @Override
  public SyslogDao createIndexerInstance()
  {
    SyslogDao syslogDao = new SyslogDao(getHost(), getPort());
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

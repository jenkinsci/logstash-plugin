package jenkins.plugins.logstash.configuration;

import java.nio.charset.Charset;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.plugins.logstash.Messages;
import jenkins.plugins.logstash.persistence.RabbitMqDao;

public class RabbitMq extends HostBasedLogstashIndexer<RabbitMqDao>
{

  private String queue;
  private String username;
  private Secret password;
  private String charset;
  private String virtualHost;

  @DataBoundConstructor
  public RabbitMq(String charset)
  {
    if (charset == null || charset.isEmpty())
    {
      this.charset = Charset.defaultCharset().toString();
    }
    else
    {
      this.charset = Charset.forName(charset).toString();
    }
  }

  protected Object readResolve()
  {
    if (charset == null)
    {
      charset = Charset.defaultCharset().toString();
    }
    if (virtualHost == null)
    {
      virtualHost = "/";
    }
    return this;
  }

  public String getCharset()
  {
    return charset;
  }

  // package visibility for testing only
  @Restricted(NoExternalUse.class)
  Charset getEffectiveCharset()
  {
    try
    {
      return Charset.forName(charset);

    }
    catch (IllegalArgumentException e)
    {
      return Charset.defaultCharset();
    }
  }

  public String getVirtualHost()
  {
    return virtualHost;
  }

  @DataBoundSetter
  public void setVirtualHost(String virtualHost)
  {
    this.virtualHost = virtualHost;
  }

  public String getQueue()
  {
    return queue;
  }

  @DataBoundSetter
  public void setQueue(String queue)
  {
    this.queue = queue;
  }

  public String getUsername()
  {
    return username;
  }

  @DataBoundSetter
  public void setUsername(String username)
  {
    this.username = username;
  }

  public String getPassword()
  {
    return Secret.toString(password);
  }

  @DataBoundSetter
  public void setPassword(String password)
  {
    this.password = Secret.fromString(password);
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
    RabbitMq other = (RabbitMq) obj;
    if (!Secret.toString(password).equals(other.getPassword()))
    {
      return false;
    }
    if (!StringUtils.equals(queue, other.queue))
    {
        return false;
    }
    if (!StringUtils.equals(username, other.username))
    {
        return false;
    }
    if (!StringUtils.equals(virtualHost, other.virtualHost))
    {
        return false;
    }
    if (charset == null)
    {
      if (other.charset != null)
      {
        return false;
      }
    } else if (!charset.equals(other.charset))
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((queue == null) ? 0 : queue.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    result = prime * result + ((charset == null) ? 0 : charset.hashCode());
    result = prime * result + ((virtualHost == null) ? 0 : virtualHost.hashCode());
    result = prime * result + Secret.toString(password).hashCode();
    return result;
  }

  @Override
  public RabbitMqDao createIndexerInstance()
  {
    return new RabbitMqDao(getHost(), getPort(), queue, username, Secret.toString(password), getEffectiveCharset(), getVirtualHost());
  }

  @Extension
  public static class RabbitMqDescriptor extends LogstashIndexerDescriptor
  {
    @Override
    public String getDisplayName()
    {
      return "RabbitMQ";
    }

    @Override
    public int getDefaultPort()
    {
      return 5672;
    }

    public FormValidation doCheckQueue(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.error(Messages.ValueIsRequired());
      }

      return FormValidation.ok();
    }

  }
}

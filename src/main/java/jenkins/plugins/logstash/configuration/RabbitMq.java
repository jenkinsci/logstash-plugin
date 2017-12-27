package jenkins.plugins.logstash.configuration;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.plugins.logstash.Messages;
import jenkins.plugins.logstash.persistence.RabbitMqDao;

public class RabbitMq extends LogstashIndexer<RabbitMqDao>
{

  protected String queue;
  protected String username;
  protected Secret password;

  @DataBoundConstructor
  public RabbitMq()
  {
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
  protected boolean shouldRefreshInstance()
  {
    return super.shouldRefreshInstance() ||
        !instance.getPassword().equals(Secret.toString(password)) ||
        !StringUtils.equals(instance.getUsername(), username) ||
        !StringUtils.equals(instance.getQueue(), queue);
  }

  @Override
  public RabbitMqDao createIndexerInstance()
  {
    return new RabbitMqDao(host, port, queue, username, Secret.toString(password));
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

package jenkins.plugins.logstash.configuration;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.plugins.logstash.Messages;
import jenkins.plugins.logstash.persistence.RedisDao;

public class Redis extends HostBasedLogstashIndexer<RedisDao>
{

  protected String key;
  protected Secret password;

  @DataBoundConstructor
  public Redis()
  {
  }

  public String getKey()
  {
    return key;
  }

  @DataBoundSetter
  public void setKey(String key)
  {
    this.key = key;
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
    Redis other = (Redis) obj;
    if (!Secret.toString(password).equals(other.getPassword()))
    {
      return false;
    }
    if (key == null)
    {
      if (other.key != null)
        return false;
    }
    else if (!key.equals(other.key))
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
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + Secret.toString(password).hashCode();
    return result;
  }


  @Override
  public RedisDao createIndexerInstance()
  {
    return new RedisDao(getHost(), getPort(), key, Secret.toString(password));
  }

  @Extension
  public static class RedisDescriptor extends LogstashIndexerDescriptor
  {

    @Override
    public String getDisplayName()
    {
      return "Redis";
    }

    @Override
    public int getDefaultPort()
    {
      return 6379;
    }

    public FormValidation doCheckKey(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.error(Messages.ValueIsRequired());
      }

      return FormValidation.ok();
    }

  }
}

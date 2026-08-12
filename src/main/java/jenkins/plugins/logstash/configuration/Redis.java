package jenkins.plugins.logstash.configuration;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
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
  protected int dbindex;

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

  public Secret getPassword()
  {
    return password;
  }

  @DataBoundSetter
  public void setPassword(Secret password)
  {
    this.password = password;
  }

  public int getDbindex()
  {
    return dbindex;
  }

  @DataBoundSetter
  public void setDbindex(int dbindex)
  {
    this.dbindex = dbindex;
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
    if (!Secret.toString(password).equals(Secret.toString(other.getPassword())))
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
    return new RedisDao(getHost(), getPort(), key, Secret.toString(password), getDbindex());
  }

  @Extension
  @Symbol("redis")
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

    public int getDefaultDbindex()
    {
      return 0;
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

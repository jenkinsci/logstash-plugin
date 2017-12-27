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

public class Redis extends LogstashIndexer<RedisDao>
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
  protected boolean shouldRefreshInstance()
  {
    return super.shouldRefreshInstance() ||
        !instance.getPassword().equals(Secret.toString(password))||
        !StringUtils.equals(instance.getKey(), key);
  }


  @Override
  public RedisDao createIndexerInstance()
  {
    return new RedisDao(host, port, key, Secret.toString(password));
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

package jenkins.plugins.logstash.configuration;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.plugins.logstash.Messages;
import jenkins.plugins.logstash.persistence.ElasticSearchDao;

public class ElasticSearch extends LogstashIndexer<ElasticSearchDao>
{
  protected String key;
  protected String username;
  protected Secret password;

  @DataBoundConstructor
  public ElasticSearch()
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
        !StringUtils.equals(instance.getKey(), key);
  }

  @Override
  public ElasticSearchDao createIndexerInstance()
  {
    return new ElasticSearchDao(host, port, key, username, Secret.toString(password));
  }

  @Extension
  public static class ElasticSearchDescriptor extends LogstashIndexerDescriptor
  {
    @Override
    public String getDisplayName()
    {
      return "Elastic Search";
    }

    @Override
    public int getDefaultPort()
    {
      return 9300;
    }

    @Override
    public FormValidation doCheckHost(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.warning(Messages.PleaseProvideHost());
      }
      try
      {
        new URL(value);
      }
      catch (MalformedURLException e)
      {
        return FormValidation.error(e.getMessage());
      }
      return FormValidation.ok();
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

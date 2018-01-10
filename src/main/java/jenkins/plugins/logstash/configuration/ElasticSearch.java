package jenkins.plugins.logstash.configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
  private String username;
  private Secret password;
  private URI uri;

  @DataBoundConstructor
  public ElasticSearch()
  {
  }

  public URI getUri()
  {
    return uri;
  }


  /*
   * We use URL for the setter as stapler can autoconvert a string to a URL but not to a URI
   */
  @DataBoundSetter
  public void setUri(URL url) throws URISyntaxException
  {
    this.uri = url.toURI();
  }

  public void setUri(URI uri) throws URISyntaxException
  {
    this.uri = uri;
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
    if (obj == null)
      return false;
    if (this == obj)
      return true;
    if (getClass() != obj.getClass())
      return false;
    ElasticSearch other = (ElasticSearch) obj;
    if (!Secret.toString(password).equals(other.getPassword()))
    {
      return false;
    }
    if (uri == null)
    {
      if (other.uri != null)
        return false;
    }
    else if (!uri.equals(other.uri))
    {
      return false;
    }
    if (username == null)
    {
      if (other.username != null)
        return false;
    }
    else if (!username.equals(other.username))
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
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    result = prime * result + Secret.toString(password).hashCode();
    return result;
  }

  @Override
  public ElasticSearchDao createIndexerInstance()
  {
    return new ElasticSearchDao(getUri(), username, Secret.toString(password));
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
      return 0;
    }

    public FormValidation doCheckUrl(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.warning(Messages.PleaseProvideHost());
      }
      try
      {
        URL url = new URL(value);

        if (url.getUserInfo() != null)
        {
          return FormValidation.error("Please specify user and password not as part of the url.");
        }

        if(StringUtils.isBlank(url.getPath()) || url.getPath().trim().matches("^\\/+$")) {
          return FormValidation.warning("Elastic Search requires a key to be able to index the logs.");
        }

        url.toURI();
      }
      catch (MalformedURLException | URISyntaxException e)
      {
        return FormValidation.error(e.getMessage());
      }
      return FormValidation.ok();
    }
  }
}

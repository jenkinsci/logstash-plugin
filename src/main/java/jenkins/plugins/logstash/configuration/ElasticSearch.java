package jenkins.plugins.logstash.configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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
  public ElasticSearch(String uri) throws URISyntaxException
  {
    this.uri = new URI(uri);
    validateUri(this.uri);
  }

  public URI getUri()
  {
    return uri;
  }

  public void setUri(String value) throws URISyntaxException
  {
    URI uri = new URI(value);
    validateUri(uri);
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
    return new ElasticSearchDao(uri, username, Secret.toString(password));
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

    public FormValidation doCheckUri(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.warning(Messages.PleaseProvideHost());
      }
      try
      {
        URI uri = new URI(value);
        validateUri(uri);
      }
      catch (URISyntaxException | IllegalArgumentException e)
      {
        return FormValidation.error(e.getMessage());
      }
      return FormValidation.ok();
    }
  }

  /**
   * Validates that the given uri has a scheme, a port and a path which is not empty or just "/"
   *
   * @param uri
   * @throws IllegalArgumentException when one of scheme, port or path
   */
  public static void validateUri(URI uri) throws IllegalArgumentException
  {
      try
      {
        uri.toURL();
      }
      catch (MalformedURLException e)
      {
        throw new IllegalArgumentException(e.getMessage());
      }

    if(uri.getPort() == -1) {
      throw new IllegalArgumentException("Please specify a port.");
    }

    if(StringUtils.isBlank(uri.getPath()) || uri.getPath().trim().matches("^\\/+$")) {
      throw new IllegalArgumentException("Please specify an elastic search key.");
    }
  }
}

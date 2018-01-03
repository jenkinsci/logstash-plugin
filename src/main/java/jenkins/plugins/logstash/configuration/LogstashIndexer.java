package jenkins.plugins.logstash.configuration;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.ReconfigurableDescribable;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.plugins.logstash.Messages;
import jenkins.plugins.logstash.persistence.AbstractLogstashIndexerDao;
import net.sf.json.JSONObject;

/**
 * Extension point for logstash indexers.
 * This extension point provides the configuration for the indexer. You also have to implement the actual
 * indexer in a separate class extending {@link AbstractLogstashIndexerDao}.
 *
 * @param <T> The class implementing the push to the indexer
 */
public abstract class LogstashIndexer<T extends AbstractLogstashIndexerDao>
    extends AbstractDescribableImpl<LogstashIndexer<?>>
    implements ExtensionPoint, ReconfigurableDescribable<LogstashIndexer<?>>
{
  protected String host;
  protected int port;

  protected transient T instance;

  /**
   * Returns the host for connecting to the indexer.
   *
   * @return Host of the indexer
   */
  public String getHost()
  {
    return host;
  }

  /**
   * Sets the host for connecting to the indexer.
   *
   * @param host
   *          host to connect to.
   */
  @DataBoundSetter
  public void setHost(String host)
  {
    this.host = host;
  }

  /**
   * Returns the port for connecting to the indexer.
   *
   * @return Port of the indexer
   */
  public int getPort()
  {
    return port;
  }

  /**
   * Sets the port used for connecting to the indexer
   *
   * @param port
   *          The port of the indexer
   */
  @DataBoundSetter
  public void setPort(int port)
  {
    this.port = port;
  }

  /**
   * Gets the instance of the actual {@link AbstractLogstashIndexerDao} that is represented by this
   * configuration.
   *
   * @return {@link AbstractLogstashIndexerDao} instance
   */
  @Nonnull
  public final T getInstance()
  {
    if (instance == null)
    {
      instance = createIndexerInstance();
    }
    return instance;
  }


  /**
   * Creates a new {@link AbstractLogstashIndexerDao} instance corresponding to this configuration.
   *
   * @return {@link AbstractLogstashIndexerDao} instance
   */
  protected abstract T createIndexerInstance();

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    LogstashIndexer<?> other = (LogstashIndexer<?>) obj;
    if (host == null) {
      if (other.host != null)
        return false;
    } else if (!host.equals(other.host))
      return false;
    if (port != other.port)
      return false;
    return true;
  }


  @SuppressWarnings("unchecked")
  public static DescriptorExtensionList<LogstashIndexer<?>, Descriptor<LogstashIndexer<?>>> all()
  {
    return (DescriptorExtensionList) Jenkins.getInstance().getDescriptorList(LogstashIndexer.class);
  }

  public static abstract class LogstashIndexerDescriptor extends Descriptor<LogstashIndexer<?>>
  {
    /*
     * Form validation methods
     */
    public FormValidation doCheckPort(@QueryParameter("value") String value)
    {
      try
      {
        Integer.parseInt(value);
      }
      catch (NumberFormatException e)
      {
        return FormValidation.error(Messages.ValueIsInt());
      }

      return FormValidation.ok();
    }

    public FormValidation doCheckHost(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.warning(Messages.PleaseProvideHost());
      }

      return FormValidation.ok();
    }

    public abstract int getDefaultPort();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogstashIndexer<T> reconfigure(StaplerRequest req, JSONObject form) throws FormException
  {
    req.bindJSON(this, form);
    return this;
  }
}

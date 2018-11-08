package jenkins.plugins.logstash.configuration;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
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
  protected transient T instance;

  /**
   * Gets the instance of the actual {@link AbstractLogstashIndexerDao} that is represented by this
   * configuration.
   *
   * @return {@link AbstractLogstashIndexerDao} instance
   */
  @Nonnull
  public synchronized T getInstance()
  {
    if (instance == null)
    {
      instance = createIndexerInstance();
    }
    return instance;
  }

  /**
   * Purpose of this method is to validate the inputs (if required) and if found
   * erroneous throw an exception so that it will be bubbled up to the UI.
   *
   * @throws Exception on erroneous input
   */
  public void validate() throws Exception {
  }



  /**
   * Creates a new {@link AbstractLogstashIndexerDao} instance corresponding to this configuration.
   *
   * @return {@link AbstractLogstashIndexerDao} instance
   */
  protected abstract T createIndexerInstance();


  @SuppressWarnings("unchecked")
  public static DescriptorExtensionList<LogstashIndexer<?>, Descriptor<LogstashIndexer<?>>> all()
  {
    return (DescriptorExtensionList<LogstashIndexer<?>, Descriptor<LogstashIndexer<?>>>) Jenkins.getInstance().getDescriptorList(LogstashIndexer.class);
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

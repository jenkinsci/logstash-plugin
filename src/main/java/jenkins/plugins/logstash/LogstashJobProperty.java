package jenkins.plugins.logstash;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import jenkins.model.OptionalJobProperty;

/**
 * This JobProperty is a marker to decide if logs should be sent to an indexer.
 *
 */
public class LogstashJobProperty extends OptionalJobProperty<Job<?, ?>>
{

  private boolean disableGlobal;

  @DataBoundConstructor
  public LogstashJobProperty()
  {}

  public boolean isDisableGlobal()
  {
    return disableGlobal;
  }

  @DataBoundSetter
  public void setDisableGlobal(boolean disableGlobal)
  {
    this.disableGlobal = disableGlobal;
  }

  @Extension
  public static class DescriptorImpl extends OptionalJobPropertyDescriptor
  {

    @Override
    public String getDisplayName()
    {
      return Messages.DisplayName();
    }

    @Override
    public boolean isApplicable(Class<? extends Job> jobType)
    {
      return AbstractProject.class.isAssignableFrom(jobType);
    }
  }
}

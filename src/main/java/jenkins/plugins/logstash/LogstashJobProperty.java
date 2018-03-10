package jenkins.plugins.logstash;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;

/**
 * This JobProperty is a marker to decide if logs should be sent to an indexer.
 *
 */
public class LogstashJobProperty extends JobProperty<Job<?, ?>>
{

  @DataBoundConstructor
  public LogstashJobProperty()
  {}

  @Extension
  public static class DescriptorImpl extends JobPropertyDescriptor
  {
    @Override
    public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException
    {
      if (formData.containsKey("enable"))
      {
        return new LogstashJobProperty();
      }
      return null;
    }

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

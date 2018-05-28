/*
 * The MIT License
 *
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.logstash;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

/**
 *
 * This BuildWrapper is not used anymore.
 * We just keep it to be able to convert projects that have the BuildWrapper configured at startup or when posting the xml via the rest api
 * to the JobProperty.
 *
 * @author K Jonathan Harker
 */
@Deprecated
public class LogstashBuildWrapper extends BuildWrapper
{

  /**
   * Create a new {@link LogstashBuildWrapper}.
   */
  @DataBoundConstructor
  public LogstashBuildWrapper()
  {}

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException
  {
    return new Environment()
    {
    };
  }

  @Override
  public DescriptorImpl getDescriptor()
  {
    return (DescriptorImpl)super.getDescriptor();
  }

  /**
   * Registers {@link LogstashBuildWrapper} as a {@link BuildWrapper}.
   */
  @Extension
  public static class DescriptorImpl extends BuildWrapperDescriptor
  {

    public DescriptorImpl()
    {
      super(LogstashBuildWrapper.class);
      load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName()
    {
      return Messages.DisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(AbstractProject<?, ?> item)
    {
      return false;
    }
  }
}

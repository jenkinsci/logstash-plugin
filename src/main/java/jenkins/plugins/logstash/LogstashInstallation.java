/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard
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

import hudson.Extension;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolProperty;
import hudson.tools.ToolInstallation;

import java.util.List;

import jenkins.model.Jenkins;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.IndexerType;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.SyslogFormat;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.SyslogProtocol;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * POJO for storing global configurations shared between components.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public class LogstashInstallation extends ToolInstallation {
  private static final long serialVersionUID = -5730780734005293851L;

  @DataBoundConstructor
  public LogstashInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
    super(name, home, properties);
  }

  public static Descriptor getLogstashDescriptor() {
    return (Descriptor) Jenkins.getInstance().getDescriptor(LogstashInstallation.class);
  }

  @Extension
  public static final class Descriptor extends ToolDescriptor<LogstashInstallation> {
    private transient IndexerType type;
    private transient SyslogFormat syslogFormat;
    private transient SyslogProtocol syslogProtocol;
    private transient String host;
    private transient Integer port = -1;
    private transient String username;
    private transient String password;
    private transient String key;

    public Descriptor() {
      super();
      load();
    }


    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }


    public IndexerType getType()
    {
      return type;
    }


    public SyslogFormat getSyslogFormat()
    {
      return syslogFormat;
    }


    public SyslogProtocol getSyslogProtocol()
    {
      return syslogProtocol;
    }


    public String getHost()
    {
      return host;
    }


    public Integer getPort()
    {
      return port;
    }


    public String getUsername()
    {
      return username;
    }


    public String getPassword()
    {
      return password;
    }


    public String getKey()
    {
      return key;
    }

  }
}

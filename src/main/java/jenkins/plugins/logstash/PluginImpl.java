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

import hudson.DescriptorExtensionList;
import hudson.Plugin;
import hudson.model.Descriptor;
import jenkins.plugins.logstash.configuration.LogstashIndexer;

import java.util.logging.Logger;

public class PluginImpl extends Plugin {
  private final static Logger LOG = Logger.getLogger(PluginImpl.class.getName());

  /*
   * TODO: do we really need this method?
   *       All it does is printing a message at startup of Jenkins.
   */
  @Override
  public void start() throws Exception {
    LOG.info("Logstash: a logstash agent to send jenkins logs to a logstash indexer.");
  }

  public DescriptorExtensionList<LogstashIndexer<?>, Descriptor<LogstashIndexer<?>>> getAllIndexers()
  {
    return LogstashIndexer.all();
  }
}


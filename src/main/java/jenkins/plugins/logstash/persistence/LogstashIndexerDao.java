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

package jenkins.plugins.logstash.persistence;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import net.sf.json.JSONObject;

/**
 * Interface describing data access objects for Logstash indexers.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public interface LogstashIndexerDao {
  @Deprecated
  static enum IndexerType {
    REDIS,
    RABBIT_MQ,
    ELASTICSEARCH,
    SYSLOG
  }

  @Deprecated
  static enum SyslogFormat {
	RFC5424,
	RFC3164
  }

  static enum SyslogProtocol {
	UDP
  }

  public String getDescription();

  /**
   * Sends the log data to the Logstash indexer.
   *
   * @param data
   *          The serialized data, not null
   * @throws java.io.IOException
   *          The data is not written to the server
   */
  public void push(String data) throws IOException;

  /**
   * Builds a JSON payload compatible with the Logstash schema.
   *
   * @param buildData
   *          Metadata about the current build, not null
   * @param jenkinsUrl
   *          The host name of the Jenkins instance, not null
   * @param logLines
   *          The log data to transmit, not null
   * @return The formatted JSON object, never null
   */
  public JSONObject buildPayload(BuildData buildData, String jenkinsUrl, List<String> logLines);
}

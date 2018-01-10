/*
 * The MIT License
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

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONObject;

/**
 * Abstract data access object for Logstash indexers.
 *
 * @since 2.0.0
 */
public abstract class HostBasedLogstashIndexerDao extends AbstractLogstashIndexerDao {
  private final String host;
  private final int port;
  private Charset charset;

  public HostBasedLogstashIndexerDao(String host, int port) {
    this.host = host;
    this.port = port;
    if (StringUtils.isBlank(host)) {
      throw new IllegalArgumentException("host name is required");
    }
  }

  /**
   * Sets the charset used to push data to the indexer
   *
   *@param charset The charset to push data
   */
  @Override
  public void setCharset(Charset charset)
  {
    this.charset = charset;
  }

  /**
   * Gets the configured charset used to push data to the indexer
   *
   * @return charste to push data
   */
  @Override
  public Charset getCharset()
  {
    return charset;
  }

  @Override
  public JSONObject buildPayload(BuildData buildData, String jenkinsUrl, List<String> logLines) {
    JSONObject payload = new JSONObject();
    payload.put("data", buildData.toJson());
    payload.put("message", logLines);
    payload.put("source", "jenkins");
    payload.put("source_host", jenkinsUrl);
    payload.put("@buildTimestamp", buildData.getTimestamp());
    payload.put("@timestamp", BuildData.getDateFormatter().format(Calendar.getInstance().getTime()));
    payload.put("@version", 1);

    return payload;
  }

  public String getHost()
  {
    return host;
  }

  public int getPort()
  {
    return port;
  }

  @Override
  public String getDescription() {
    return this.host + ":" + this.port;
  }
}

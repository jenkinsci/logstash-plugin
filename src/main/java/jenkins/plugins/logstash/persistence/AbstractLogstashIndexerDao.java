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

import hudson.model.Run;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONObject;

/**
 * Abstract data access object for Logstash indexers.
 *
 * TODO: a charset is only required for RabbitMq currently (ES as well but there it is currently configured via the ContentType),
 *   so better move this to the corresponding classes.
 * @author Rusty Gerard
 * @since 1.0.0
 */
public abstract class AbstractLogstashIndexerDao implements LogstashIndexerDao {
  private static volatile String localhost;

  private Charset charset;

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
    if (localhost == null) {
      try {
        localhost = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException x) {
        localhost = "?";
      }
    }
    payload.put("sender", localhost);
    payload.put("@buildTimestamp", buildData.getTimestamp());
    payload.put("@timestamp", BuildData.getDateFormatter().format(Calendar.getInstance().getTime()));
    payload.put("@version", 1);

    return payload;
  }

   @Override
   public Collection<String> pullLogs(Run run, long sinceMs, long toMs) throws IOException {
     throw new IOException("Destination does not support data read: " + this.getClass());
   }
}

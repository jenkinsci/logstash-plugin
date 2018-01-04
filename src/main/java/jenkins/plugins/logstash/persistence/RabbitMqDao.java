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

import org.apache.commons.lang.StringUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * RabbitMQ Data Access Object.
 *
 * TODO: make the charset configurable via the UI with UTF-8 being the default
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public class RabbitMqDao extends HostBasedLogstashIndexerDao {
  private final ConnectionFactory pool;

  private String queue;
  private String username;
  private String password;

  //primary constructor used by indexer factory
  public RabbitMqDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  /*
   * TODO: this constructor is only for testing so one can inject a mocked ConnectionFactory.
   *       With Powermock we can intercept the creation of the ConnectionFactory and replace with a mock
   *       making this constructor obsolete
   */
  RabbitMqDao(ConnectionFactory factory, String host, int port, String queue, String username, String password) {
    super(host, port);

    this.queue = queue;
    this.username = username;
    this.password = password;

    if (StringUtils.isBlank(queue)) {
      throw new IllegalArgumentException("rabbit queue name is required");
    }

    // The ConnectionFactory must be a singleton
    // We assume this is used as a singleton as well
    // Calling this method means the configuration has changed and the pool must be re-initialized
    pool = factory == null ? new ConnectionFactory() : factory;
    pool.setHost(host);
    pool.setPort(port);

    if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
      pool.setPassword(password);
      pool.setUsername(username);
    }
  }

  public String getQueue()
  {
    return queue;
  }

  public String getUsername()
  {
    return username;
  }

  public String getPassword()
  {
      return password;
  }


  @Override
  public void push(String data) throws IOException {
    Connection connection = null;
    Channel channel = null;
    try {
      connection = pool.newConnection();
      channel = connection.createChannel();

      // Ensure the queue exists
      try {
        channel.queueDeclarePassive(queue);
      } catch (IOException e) {
        // The queue does not exist and the channel has been closed
        finalizeChannel(channel);

        // Create the queue
        channel = connection.createChannel();
        channel.queueDeclare(queue, true, false, false, null);
      }

      channel.basicPublish("", queue, null, data.getBytes(getCharset()));
    } finally {
      finalizeChannel(channel);
      finalizeConnection(connection);
    }
  }

  private void finalizeConnection(Connection connection) {
    if (connection != null && connection.isOpen()) {
      try {
        connection.close();
      } catch (IOException e) {
        // This shouldn't happen but if it does there's nothing we can do
        e.printStackTrace();
      }
    }
  }

  private void finalizeChannel(Channel channel) {
    if (channel != null && channel.isOpen()) {
      try {
        channel.close();
      } catch (IOException e) {
        // This shouldn't happen but if it does there's nothing we can do
        e.printStackTrace();
      }
    }
  }
}

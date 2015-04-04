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
import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * RabbitMQ Data Access Object.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public class RabbitMqDao extends AbstractLogstashIndexerDao {
  final ConnectionFactory pool;

  //primary constructor used by indexer factory
  public RabbitMqDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  // Factored for unit testing
  RabbitMqDao(ConnectionFactory factory, String host, int port, String key, String username, String password) {
    super(host, port, key, username, password);

    if (StringUtils.isBlank(key)) {
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

  @Override
  public long push(String data, PrintStream logger) {
    Connection connection = null;
    Channel channel = null;
    try {
      connection = pool.newConnection();
      channel = connection.createChannel();

      channel.queueDeclare(key, true, false, false, null);
      channel.basicPublish("", key, null, data.getBytes());

      channel.close();
      connection.close();

      return 1;
    } catch (IOException e) {
      logger.println(ExceptionUtils.getStackTrace(e));
    }

    return -1;
  }

  @Override
  public IndexerType getIndexerType() {
    return IndexerType.RABBIT_MQ;
  }
}

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

import java.io.PrintStream;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * ActiveMq Data Access Object.
 *
 * @author Rusty Gerard
 * @author dbs
 * @since 1.0.4
 */
public class ActiveMqDao extends AbstractLogstashIndexerDao {
  //ConnectionFactory pool;
  ActiveMQConnectionFactory connectionFactory = null;

  ActiveMqDao() { /* Required by IndexerDaoFactory */ }

  // Constructor for unit testing
  ActiveMqDao(String host, int port, String key, String username, String password) {
    init(host, port, key, username, password);
  }

  final void init(String host, int port, String key, String username, String password) {
    super.init(host, port, key, username, password);

    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("JMS queue name is required");
    }

      // The ConnectionFactory must be a singleton
      // We assume this is used as a singleton as well
      // Calling this method means the configuration has changed and the pool must be re-initialized
      connectionFactory = new ActiveMQConnectionFactory(String.format("tcp://%s:%d", host, port));
      
      if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
        connectionFactory.setUserName(username);
        connectionFactory.setPassword(password);
      }

  }

  @Override
  public long push(String data, PrintStream logger) {
    try {
      // Create a Connection
      Connection connection = connectionFactory.createConnection();
      connection.start();
      // Create a Session
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      
      // Create the destination (Topic or Queue)
      Destination destination = session.createQueue(key);
      
      // Create a MessageProducer from the Session to the Topic or Queue
      MessageProducer producer = session.createProducer(destination);
      producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      
      // Create a messages
      TextMessage message = session.createTextMessage(data);

      // Tell the producer to send the message
      producer.send(message);

      // Clean up
      session.close();
      connection.close();
      return 1;
    } catch (JMSException e) {
      logger.println(ExceptionUtils.getStackTrace(e));
    }
    return -1;
  }

  @Override
  public IndexerType getIndexerType() {
    return IndexerType.JMS;
  }
}

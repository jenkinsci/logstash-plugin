package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.net.SocketException;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ActiveMqDaoTest {
  ActiveMqDao dao;
  @Mock ActiveMQConnectionFactory mockConnectionFactory;
  @Mock Connection mockConnection;
  @Mock Session mockSession;
  @Mock Queue mockDestination;
  @Mock MessageProducer mockProducer;
  @Mock TextMessage mockMessage;
  @Mock PrintStream mockLogger;

  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);
    dao = new ActiveMqDao("localhost", port, "logstash", "username", "password");

    // Note that we can't run these tests in parallel
    dao.connectionFactory = mockConnectionFactory;

    when(mockConnectionFactory.createConnection()).thenReturn(mockConnection);

    when(mockConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);
    
    when(mockSession.createQueue("logstash")).thenReturn(mockDestination);
    
    when(mockSession.createProducer(mockDestination)).thenReturn(mockProducer);
    
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockConnectionFactory);
    verifyNoMoreInteractions(mockConnection);
    verifyNoMoreInteractions(mockSession);
    verifyNoMoreInteractions(mockDestination);
    verifyNoMoreInteractions(mockProducer);
    verifyNoMoreInteractions(mockLogger);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullHost() throws Exception {
    try {
      new ActiveMqDao(null, 5672, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyHost() throws Exception {
    try {
      new ActiveMqDao(" ", 5672, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullKey() throws Exception {
    try {
      new ActiveMqDao("localhost", 5672, null, "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "jms queue name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyKey() throws Exception {
    try {
      new ActiveMqDao("localhost", 5672, " ", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "jms queue name is required", e.getMessage());
      throw e;
    }
  }

  @Test
  public void constructorSuccess() throws Exception {
    // Unit under test
    dao = new ActiveMqDao("localhost", 5672, "logstash", "username", "password");

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.host);
    assertEquals("Wrong port", 5672, dao.port);
    assertEquals("Wrong key", "logstash", dao.key);
    assertEquals("Wrong password", "password", dao.password);
  }

  //@Test
  public void pushFailUnauthorized() throws Exception {
    // Initialize mocks
    when(mockConnectionFactory.createConnection()).thenThrow(new JMSException("Not authorized"));

    // Unit under test
    long result = dao.push("", mockLogger);

    // Verify results
    assertEquals("Return code should be an error", -1L, result);

    verify(mockConnectionFactory).createConnection();
    verify(mockLogger).println(Matchers.startsWith("javax.jms.JMSException: Not authorized"));
  }

  //@Test
  public void pushFailCantConnect() throws Exception {
    // Initialize mocks
    when(mockConnectionFactory.createConnection()).thenThrow(new SocketException("Connection refused"));

    // Unit under test
    long result = dao.push("", mockLogger);

    // Verify results
    assertEquals("Return code should be an error", -1L, result);

    verify(mockConnectionFactory).createConnection();
    verify(mockLogger).println(Matchers.startsWith("java.net.SocketException: Connection refused"));
  }

  //@Test
  public void pushFailCantWrite() throws Exception {
    // Initialize mocks
    doThrow(new SocketException("Queue length limit exceeded")).when(mockProducer).send(mockSession.createTextMessage("{}"));

    // Unit under test
    long result = dao.push("{}", mockLogger);

    // Verify results
    assertEquals("Return code should be an error", -1L, result);

    verify(mockConnectionFactory).createConnection();
    verify(mockConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    verify(mockSession).createQueue("logstash");
    verify(mockSession).createProducer(mockDestination);
    mockProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    verify(mockSession).createTextMessage("{}");
    verify(mockProducer).send(mockMessage);
    verify(mockLogger).println(Matchers.startsWith("java.net.SocketException: Queue length limit exceeded"));
  }

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Unit under test
    long result = dao.push(json, mockLogger);

    // Verify results
    assertEquals("Unexpected return code", 1L, result);

    verify(mockConnectionFactory).createConnection();
    verify(mockConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    verify(mockSession).createQueue("logstash");
    verify(mockSession).createProducer(mockDestination);
    mockProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    verify(mockSession).createTextMessage(json);
    verify(mockProducer).send(mockMessage);
    verify(mockSession).close();
    verify(mockConnection).close();
  }

  @Test
  public void pushSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = new ActiveMqDao("localhost", 5672, "logstash", null, null);
    dao.connectionFactory = mockConnectionFactory;

    // Unit under test
    long result = dao.push(json, mockLogger);

    // Verify results
    assertEquals("Unexpected return code", 1L, result);

    verify(mockConnectionFactory).createConnection();
    verify(mockConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    verify(mockSession).createQueue("logstash");
    verify(mockSession).createProducer(mockDestination);
    mockProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    verify(mockSession).createTextMessage(json);
    verify(mockProducer).send(mockMessage);
    verify(mockSession).close();
    verify(mockConnection).close();
  }
}

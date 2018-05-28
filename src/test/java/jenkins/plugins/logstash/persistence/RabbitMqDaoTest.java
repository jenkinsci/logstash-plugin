package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.rabbitmq.client.AuthenticationFailureException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@RunWith(MockitoJUnitRunner.class)
public class RabbitMqDaoTest {
  RabbitMqDao dao;
  @Mock ConnectionFactory mockPool;
  @Mock Connection mockConnection;
  @Mock Channel mockChannel;

  RabbitMqDao createDao(String host, int port, String key, String username, String password, String vhost) {
    RabbitMqDao factory = new RabbitMqDao(mockPool, host, port, key, username, password, StandardCharsets.UTF_8, vhost);
    verify(mockPool, atLeastOnce()).setHost(host);
    verify(mockPool, atLeastOnce()).setPort(port);
    verify(mockPool, atLeastOnce()).setVirtualHost(vhost);

    if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
      verify(mockPool, atLeastOnce()).setUsername(username);
      verify(mockPool, atLeastOnce()).setPassword(password);
    }

    return factory;
  }

  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);
    // Note that we can't run these tests in parallel
    dao = createDao("localhost", port, "logstash", "username", "password", "/");

    when(mockPool.newConnection()).thenReturn(mockConnection);

    when(mockConnection.createChannel()).thenReturn(mockChannel);
    when(mockConnection.isOpen()).thenReturn(true);

    when(mockChannel.isOpen()).thenReturn(true);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockPool);
    verifyNoMoreInteractions(mockConnection);
    verifyNoMoreInteractions(mockChannel);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullHost() throws Exception {
    try {
      createDao(null, 5672, "logstash", "username", "password", "/");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyHost() throws Exception {
    try {
      createDao(" ", 5672, "logstash", "username", "password", "/");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullKey() throws Exception {
    try {
      createDao("localhost", 5672, null, "username", "password", "/");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "rabbit queue name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyKey() throws Exception {
    try {
      createDao("localhost", 5672, " ", "username", "password", "/");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "rabbit queue name is required", e.getMessage());
      throw e;
    }
  }

  @Test
  public void constructorSuccess() throws Exception {
    // Unit under test
    dao = createDao("localhost", 5672, "logstash", "username", "password", "/");

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.getHost());
    assertEquals("Wrong port", 5672, dao.getPort());
    assertEquals("Wrong key", "logstash", dao.getQueue());
    assertEquals("Wrong name", "username", dao.getUsername());
    assertEquals("Wrong password", "password", dao.getPassword());
    assertEquals("Wrong vhost", "/", dao.getVirtualHost());
  }

  @Test(expected = IOException.class)
  public void pushFailUnauthorized() throws Exception {
    // Initialize mocks
    when(mockPool.newConnection()).thenThrow(new AuthenticationFailureException("Not authorized"));

    // Unit under test
    try {
      dao.push("");
    } catch (IOException e) {
      // Verify results
      verify(mockPool).newConnection();
      assertEquals("wrong error message",
        "AuthenticationFailureException: Not authorized", ExceptionUtils.getMessage(e));
      throw e;
    }

  }

  @Test(expected = IOException.class)
  public void pushFailCannotConnect() throws Exception {
    // Initialize mocks
    when(mockPool.newConnection()).thenThrow(new SocketException("Connection refused"));

    // Unit under test
    try {
      dao.push("");
    } catch (IOException e) {
      // Verify results
      verify(mockPool).newConnection();
      assertEquals("wrong error message",
        "SocketException: Connection refused", ExceptionUtils.getMessage(e));
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void pushFailCantWrite() throws Exception {
    // Initialize mocks
    doThrow(new SocketException("Queue length limit exceeded")).when(mockChannel).basicPublish("", "logstash", null, "{}".getBytes());

    // Unit under test
    try {
      dao.push("{}");
    } catch (IOException e) {
      // Verify results
      verify(mockPool).newConnection();
      verify(mockConnection).createChannel();
      verify(mockConnection).isOpen();
      verify(mockConnection).close();
      verify(mockChannel).queueDeclarePassive("logstash");
      verify(mockChannel).basicPublish("", "logstash", null, "{}".getBytes());
      verify(mockChannel).isOpen();
      verify(mockChannel).close();
      assertEquals("wrong error message",
        "SocketException: Queue length limit exceeded", ExceptionUtils.getMessage(e));
      throw e;
    }
  }

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Unit under test
    dao.push(json);

    // Verify results
    verify(mockPool).newConnection();
    verify(mockConnection).createChannel();
    verify(mockConnection).isOpen();
    verify(mockConnection).close();
    verify(mockChannel).queueDeclarePassive("logstash");
    verify(mockChannel).basicPublish("", "logstash", null, json.getBytes());
    verify(mockChannel).isOpen();
    verify(mockChannel).close();
  }

  @Test
  public void pushSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("localhost", 5672, "logstash", null, null, "/");

    // Unit under test
    dao.push(json);

    // Verify results
    verify(mockPool).newConnection();
    verify(mockConnection).createChannel();
    verify(mockConnection).isOpen();
    verify(mockConnection).close();
    verify(mockChannel).queueDeclarePassive("logstash");
    verify(mockChannel).basicPublish("", "logstash", null, json.getBytes());
    verify(mockChannel).isOpen();
    verify(mockChannel).close();
  }

  @Test
  public void pushSuccesBuildQueue() throws Exception {
    String json = "{ 'foo': 'bar' }";

    when(mockChannel.queueDeclarePassive("logstash")).thenThrow(new IOException("Queue does not exist"));

    // Unit under test
    dao.push(json);

    // Verify results
    verify(mockPool).newConnection();
    verify(mockConnection, times(2)).createChannel();
    verify(mockConnection).isOpen();
    verify(mockConnection).close();
    verify(mockChannel).queueDeclarePassive("logstash");
    verify(mockChannel).queueDeclare("logstash", true, false, false, null);
    verify(mockChannel).basicPublish("", "logstash", null, json.getBytes());
    verify(mockChannel, times(2)).isOpen();
    verify(mockChannel, times(2)).close();
  }
}

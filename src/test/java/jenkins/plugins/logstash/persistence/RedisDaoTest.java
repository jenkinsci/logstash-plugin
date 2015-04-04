package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

@RunWith(MockitoJUnitRunner.class)
public class RedisDaoTest {
  RedisDao dao;
  @Mock JedisPool mockPool;
  @Mock Jedis mockJedis;
  @Mock PrintStream mockLogger;

  RedisDao createDao(String host, int port, String key, String username, String password) {
    return new RedisDao(mockPool, host, port, key, username, password);
  }

  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);
    dao = createDao("localhost", port, "logstash", "username", "password");

    when(mockPool.getResource()).thenReturn(mockJedis);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockPool);
    verifyNoMoreInteractions(mockJedis);
    verifyNoMoreInteractions(mockLogger);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullHost() throws Exception {
    try {
      createDao(null, 6379, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyHost() throws Exception {
    try {
      createDao(" ", 6379, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullKey() throws Exception {
    try {
      createDao("localhost", 6379, null, "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "redis key is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyKey() throws Exception {
    try {
      createDao("localhost", 6379, " ", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "redis key is required", e.getMessage());
      throw e;
    }
  }

  @Test
  public void constructorSuccess() throws Exception {
    // Unit under test
    dao = createDao("localhost", 6379, "logstash", "username", "password");

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.host);
    assertEquals("Wrong port", 6379, dao.port);
    assertEquals("Wrong key", "logstash", dao.key);
    assertEquals("Wrong name", "username", dao.username);
    assertEquals("Wrong password", "password", dao.password);
  }

  @Test
  public void pushFailUnauthorized() throws Exception {
    // Initialize mocks
    when(mockJedis.auth("password")).thenThrow(new JedisConnectionException("Unauthorized"));

    // Unit under test
    long result = dao.push("", mockLogger);

    // Verify results
    assertEquals("Return code should be an error", -1L, result);

    verify(mockPool).getResource();
    verify(mockPool).returnBrokenResource(mockJedis);
    verify(mockJedis).auth("password");
    verify(mockLogger).println(Matchers.startsWith("redis.clients.jedis.exceptions.JedisConnectionException: Unauthorized"));
  }

  @Test
  public void pushFailCantConnect() throws Exception {
    // Initialize mocks
    doThrow(new JedisConnectionException("Connection refused")).when(mockJedis).connect();

    // Unit under test
    long result = dao.push("", mockLogger);

    // Verify results
    assertEquals("Return code should be an error", -1L, result);

    verify(mockPool).getResource();
    verify(mockPool).returnBrokenResource(mockJedis);
    verify(mockJedis).auth("password");
    verify(mockLogger).println(Matchers.startsWith("redis.clients.jedis.exceptions.JedisConnectionException: Connection refused"));
    verify(mockJedis).connect();
  }

  @Test
  public void pushFailCantWrite() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Initialize mocks
    when(mockJedis.rpush("logstash", json)).thenThrow(new JedisConnectionException("Push failed"));

    // Unit under test
    long result = dao.push(json, mockLogger);

    // Verify results
    assertEquals("Return code should be an error", -1L, result);

    verify(mockPool).getResource();
    verify(mockPool).returnBrokenResource(mockJedis);
    verify(mockJedis).auth("password");
    verify(mockLogger).println(Matchers.startsWith("redis.clients.jedis.exceptions.JedisConnectionException: Push failed"));
    verify(mockJedis).connect();
    verify(mockJedis).rpush("logstash", json);
  }

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Initialize mocks
    when(mockJedis.rpush("logstash", json)).thenReturn(1L);

    // Unit under test
    long result = dao.push(json, mockLogger);

    // Verify results
    assertEquals("Unexpected return code", 1L, result);

    verify(mockPool).getResource();
    verify(mockPool).returnResource(mockJedis);
    verify(mockJedis).auth("password");
    verify(mockJedis).connect();
    verify(mockJedis).rpush("logstash", json);
    verify(mockJedis).disconnect();
  }

  @Test
  public void pushSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Initialize mocks
    dao = createDao("localhost", 6379, "logstash", null, null);
    when(mockJedis.rpush("logstash", json)).thenReturn(1L);

    // Unit under test
    long result = dao.push(json, mockLogger);

    // Verify results
    assertEquals("Unexpected return code", 1L, result);

    verify(mockPool).getResource();
    verify(mockPool).returnResource(mockJedis);
    verify(mockJedis).connect();
    verify(mockJedis).rpush("logstash", json);
    verify(mockJedis).disconnect();
  }
}

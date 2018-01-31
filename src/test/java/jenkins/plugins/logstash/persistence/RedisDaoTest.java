package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class RedisDaoTest {
  RedisDao dao;
  @Mock JedisPool mockPool;
  @Mock Jedis mockJedis;

  RedisDao createDao(String host, int port, String key, String username, String password) {
    return new RedisDao(mockPool, host, port, key, password);
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
    assertEquals("Wrong host name", "localhost", dao.getHost());
    assertEquals("Wrong port", 6379, dao.getPort());
    assertEquals("Wrong key", "logstash", dao.getKey());
    assertEquals("Wrong password", "password", dao.getPassword());
  }

  @Test(expected = IOException.class)
  public void pushFailUnauthorized() throws Exception {
    // Initialize mocks
    when(mockJedis.auth("password")).thenThrow(new JedisConnectionException("Unauthorized"));

    // Unit under test
    try {
      dao.push("");
    } catch (IOException e) {
      // Verify results
      verify(mockPool).getResource();
      verify(mockPool).returnBrokenResource(mockJedis);
      verify(mockJedis).auth("password");
      assertEquals("wrong error message",
        "IOException: redis.clients.jedis.exceptions.JedisConnectionException: Unauthorized", ExceptionUtils.getMessage(e));
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void pushFailCantConnect() throws Exception {
    // Initialize mocks
    doThrow(new JedisConnectionException("Connection refused")).when(mockJedis).connect();

    // Unit under test
    try {
      dao.push("");
    } catch (IOException e) {
      // Verify results
      verify(mockPool).getResource();
      verify(mockPool).returnBrokenResource(mockJedis);
      verify(mockJedis).auth("password");
      verify(mockJedis).connect();
      assertEquals("wrong error message",
        "IOException: redis.clients.jedis.exceptions.JedisConnectionException: Connection refused", ExceptionUtils.getMessage(e));
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void pushFailCantWrite() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Initialize mocks
    when(mockJedis.rpush("logstash", json)).thenThrow(new JedisConnectionException("Push failed"));

    try {
      // Unit under test
      dao.push(json);
    } catch (IOException e) {
      // Verify results
      verify(mockPool).getResource();
      verify(mockPool).returnBrokenResource(mockJedis);
      verify(mockJedis).auth("password");
      verify(mockJedis).connect();
      verify(mockJedis).rpush("logstash", json);
      assertEquals("wrong error message",
        "IOException: redis.clients.jedis.exceptions.JedisConnectionException: Push failed", ExceptionUtils.getMessage(e));
      throw e;
    }
  }

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Initialize mocks
    when(mockJedis.rpush("logstash", json)).thenReturn(1L);

    // Unit under test
    dao.push(json);

    // Verify results
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
    dao.push(json);

    // Verify results
    verify(mockPool).getResource();
    verify(mockPool).returnResource(mockJedis);
    verify(mockJedis).connect();
    verify(mockJedis).rpush("logstash", json);
    verify(mockJedis).disconnect();
  }
}

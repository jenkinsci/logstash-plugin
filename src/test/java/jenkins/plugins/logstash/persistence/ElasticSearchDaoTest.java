package jenkins.plugins.logstash.persistence;

import org.apache.commons.lang.CharEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchDaoTest {
  ElasticSearchDao dao;
  @Mock HttpClientBuilder mockClientBuilder;
  @Mock CloseableHttpClient mockHttpClient;
  @Mock StatusLine mockStatusLine;
  @Mock CloseableHttpResponse mockResponse;
  @Mock HttpEntity mockEntity;
  @Mock PrintStream mockLogger;

  ElasticSearchDao createDao(String host, int port, String key, String username, String password) {
    return new ElasticSearchDao(mockClientBuilder, host, port, key, username, password);
  }

  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);
    dao = createDao("http://localhost", port, "/jenkins/logstash", "username", "password");

    when(mockClientBuilder.build()).thenReturn(mockHttpClient);
    when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockClientBuilder);
    verifyNoMoreInteractions(mockHttpClient);
    verifyNoMoreInteractions(mockLogger);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullHost() throws Exception {
    try {
      createDao(null, 8200, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyHost() throws Exception {
    try {
      createDao(" ", 8200, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailMissingScheme() throws Exception {
    try {
      createDao("localhost", 8200, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host field must specify scheme, such as 'http://'", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullKey() throws Exception {
    try {
      createDao("http://localhost", 8200, null, "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "elastic index name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyKey() throws Exception {
    try {
      createDao("http://localhost", 8200, " ", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "elastic index name is required", e.getMessage());
      throw e;
    }
  }

  @Test
  public void constructorSuccess1() throws Exception {
    // Unit under test
    dao = createDao("https://localhost", 8200, "logstash", "username", "password");

    // Verify results
    assertEquals("Wrong host name", "https://localhost", dao.host);
    assertEquals("Wrong port", 8200, dao.port);
    assertEquals("Wrong key", "logstash", dao.key);
    assertEquals("Wrong name", "username", dao.username);
    assertEquals("Wrong password", "password", dao.password);
    assertEquals("Wrong auth", "dXNlcm5hbWU6cGFzc3dvcmQ=", dao.auth);
    assertEquals("Wrong uri", new URI("https://localhost:8200/logstash"), dao.uri);
  }

  @Test
  public void constructorSuccess2() throws Exception {
    // Unit under test
    dao = createDao("http://localhost", 8200, "jenkins/logstash", "", "password");

    // Verify results
    assertEquals("Wrong host name", "http://localhost", dao.host);
    assertEquals("Wrong port", 8200, dao.port);
    assertEquals("Wrong key", "jenkins/logstash", dao.key);
    assertEquals("Wrong name", "", dao.username);
    assertEquals("Wrong password", "password", dao.password);
    assertEquals("Wrong auth", null, dao.auth);
    assertEquals("Wrong uri", new URI("http://localhost:8200/jenkins/logstash"), dao.uri);
  }

  @Test
  public void constructorSuccess3() throws Exception {
    // Unit under test
    dao = createDao("http://localhost", 8200, "/jenkins//logstash/", "userlongername", null);

    // Verify results
    assertEquals("Wrong host name", "http://localhost", dao.host);
    assertEquals("Wrong port", 8200, dao.port);
    assertEquals("Wrong key", "/jenkins//logstash/", dao.key);
    assertEquals("Wrong name", "userlongername", dao.username);
    assertEquals("Wrong password", null, dao.password);
    assertEquals("Wrong auth", "dXNlcmxvbmdlcm5hbWU6", dao.auth);
    assertEquals("Wrong uri", new URI("http://localhost:8200/jenkins//logstash/"), dao.uri);
  }

  @Test
  public void getPostSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("http://localhost", 8200, "/jenkins/logstash", "", "");

    // Unit under test
    HttpPost post = dao.getHttpPost(json);
    HttpEntity entity = post.getEntity();

    assertEquals("Wrong uri", new URI("http://localhost:8200/jenkins/logstash") , post.getURI());
    assertEquals("Wrong auth", 0, post.getHeaders("Authorization").length);
    assertEquals("Wrong content type", entity.getContentType().getValue(), ContentType.APPLICATION_JSON.toString());
    assertTrue("Wrong content class", entity instanceof StringEntity);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    entity.writeTo(stream);
    assertEquals("Wrong content", stream.toString(CharEncoding.UTF_8), "{ 'foo': 'bar' }");
  }

  @Test
  public void getPostSuccessAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("https://localhost", 8200, "/jenkins/logstash", "username", "password");

    // Unit under test
    HttpPost post = dao.getHttpPost(json);
    HttpEntity entity = post.getEntity();

    assertEquals("Wrong uri", new URI("https://localhost:8200/jenkins/logstash") , post.getURI());
    assertEquals("Wrong auth", 1, post.getHeaders("Authorization").length);
    assertEquals("Wrong auth value", "Basic dXNlcm5hbWU6cGFzc3dvcmQ=", post.getHeaders("Authorization")[0].getValue());


    assertEquals("Wrong content type", entity.getContentType().getValue(), ContentType.APPLICATION_JSON.toString());
    assertTrue("Wrong content class", entity instanceof StringEntity);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    entity.writeTo(stream);
    assertEquals("Wrong content", stream.toString(CharEncoding.UTF_8), "{ 'foo': 'bar' }");
  }

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("http://localhost", 8200, "/jenkins/logstash", "", "");

    when(mockStatusLine.getStatusCode()).thenReturn(201);

    // Unit under test
    long result = dao.push(json, mockLogger);

    // Verify results
    assertEquals("Unexpected return code", 1L, result);

    verify(mockClientBuilder).build();
    verify(mockHttpClient).execute(any(HttpPost.class));
    verify(mockStatusLine, atLeastOnce()).getStatusCode();
    verify(mockResponse).close();
    verify(mockHttpClient).close();
  }

  @Test
  public void pushFailStatusCode() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("http://localhost", 8200, "/jenkins/logstash", "username", "password");

    when(mockStatusLine.getStatusCode()).thenReturn(500);
    when(mockResponse.getEntity()).thenReturn(new StringEntity("Something bad happened.", ContentType.TEXT_PLAIN));

    // Unit under test
    long result = dao.push(json, mockLogger);

    // Verify results
    assertEquals("Unexpected return code", -1L, result);

    verify(mockClientBuilder).build();
    verify(mockHttpClient).execute(any(HttpPost.class));
    verify(mockStatusLine, atLeastOnce()).getStatusCode();
    verify(mockLogger).println(AdditionalMatchers.and(
      startsWith("java.io.IOException: HTTP error code: 500"),
      contains("Something bad happened.")));
    verify(mockResponse).close();
    verify(mockHttpClient).close();
  }
}

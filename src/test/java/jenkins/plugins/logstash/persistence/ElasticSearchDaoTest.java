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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchDaoTest {
  ElasticSearchDao dao;
  @Mock HttpClientBuilder mockClientBuilder;
  @Mock CloseableHttpClient mockHttpClient;
  @Mock StatusLine mockStatusLine;
  @Mock CloseableHttpResponse mockResponse;
  @Mock HttpEntity mockEntity;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  ElasticSearchDao createDao(String url, String username, String password) throws URISyntaxException {
    URI uri = new URI(url);
    return new ElasticSearchDao(mockClientBuilder, uri, username, password);
  }

  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);
    dao = createDao("http://localhost:8200/logstash", "username", "password");
    
    when(mockClientBuilder.build()).thenReturn(mockHttpClient);
    when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockClientBuilder);
    verifyNoMoreInteractions(mockHttpClient);
  }

  @Test
  public void constructorFailInvalidUrl() throws Exception {
    URI uri = new URI("localhost:8000/logstash");
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("java.net.MalformedURLException: unknown protocol: localhost");
    new ElasticSearchDao(mockClientBuilder, uri,  "username", "password");
  }

  @Test
  public void constructorSuccess1() throws Exception {
    // Unit under test
    dao = createDao("https://localhost:8200/logstash", "username", "password");

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.getHost());
    assertEquals("Wrong port", 8200, dao.getPort());
    assertEquals("Wrong key", "/logstash", dao.getKey());
    assertEquals("Wrong name", "username", dao.getUsername());
    assertEquals("Wrong password", "password", dao.getPassword());
    assertEquals("Wrong auth", "dXNlcm5hbWU6cGFzc3dvcmQ=", dao.getAuth());
    assertEquals("Wrong uri", new URI("https://localhost:8200/logstash"), dao.getUri());
  }

  @Test
  public void constructorSuccess2() throws Exception {
    // Unit under test
    dao = createDao("http://localhost:8200/jenkins/logstash", "", "password");

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.getHost());
    assertEquals("Wrong port", 8200, dao.getPort());
    assertEquals("Wrong scheme", "http", dao.getScheme());
    assertEquals("Wrong key", "/jenkins/logstash", dao.getKey());
    assertEquals("Wrong name", "", dao.getUsername());
    assertEquals("Wrong password", "password", dao.getPassword());
    assertEquals("Wrong auth", null, dao.getAuth());
    assertEquals("Wrong uri", new URI("http://localhost:8200/jenkins/logstash"), dao.getUri());
  }

  @Test
  public void constructorSuccess3() throws Exception {
    // Unit under test
    dao = createDao("http://localhost:8200/jenkins//logstash/", "userlongername", null);

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.getHost());
    assertEquals("Wrong port", 8200, dao.getPort());
    assertEquals("Wrong scheme", "http", dao.getScheme());
    assertEquals("Wrong key", "/jenkins//logstash/", dao.getKey());
    assertEquals("Wrong name", "userlongername", dao.getUsername());
    assertEquals("Wrong password", null, dao.getPassword());
    assertEquals("Wrong auth", "dXNlcmxvbmdlcm5hbWU6", dao.getAuth());
    assertEquals("Wrong uri", new URI("http://localhost:8200/jenkins//logstash/"), dao.getUri());
  }

  @Test
  public void getPostSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("http://localhost:8200/jenkins/logstash", "", "");

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
    dao = createDao("https://localhost:8200/jenkins/logstash", "username", "password");

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
    dao = createDao("http://localhost:8200/jenkins/logstash", "", "");

    when(mockStatusLine.getStatusCode()).thenReturn(201);

    // Unit under test
    dao.push(json);

    verify(mockClientBuilder).build();
    verify(mockHttpClient).execute(any(HttpPost.class));
    verify(mockStatusLine, atLeastOnce()).getStatusCode();
    verify(mockResponse).close();
    verify(mockHttpClient).close();
  }

  @Test(expected = IOException.class)
  public void pushFailStatusCode() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("http://localhost:8200/jenkins/logstash", "username", "password");

    when(mockStatusLine.getStatusCode()).thenReturn(500);
    when(mockResponse.getEntity()).thenReturn(new StringEntity("Something bad happened.", ContentType.TEXT_PLAIN));

    // Unit under test
    try {
      dao.push(json);
    } catch (IOException e) {
      // Verify results
      verify(mockClientBuilder).build();
      verify(mockHttpClient).execute(any(HttpPost.class));
      verify(mockStatusLine, atLeastOnce()).getStatusCode();
      verify(mockResponse).close();
      verify(mockHttpClient).close();
      assertTrue("wrong error message",
        e.getMessage().contains("Something bad happened.") && e.getMessage().contains("HTTP error code: 500"));
        throw e;
    }
    
  }
  @Test
  public void getHttpPostSuccessWithUserInput() throws Exception {
    String json = "{ 'foo': 'bar' }";
    String mimeType = "application/json";
    dao = createDao("http://localhost:8200/jenkins/logstash", "username", "password");
    dao.setMimeType(mimeType);
    HttpPost post = dao.getHttpPost(json);
    HttpEntity entity = post.getEntity();
    assertEquals("Content type do not match", mimeType, entity.getContentType().getValue());
  }
  @Test
  public void getHttpPostWithFallbackInput() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("http://localhost:8200/jenkins/logstash", "username", "password");
    HttpPost post = dao.getHttpPost(json);
    HttpEntity entity = post.getEntity();
    assertEquals("Content type do not match", ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());
  }
}

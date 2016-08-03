package jenkins.plugins.logstash.persistence;

import org.apache.commons.lang.CharEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ValoDaoTest {
  ValoDao dao;
  @Mock HttpClientBuilder mockClientBuilder;
  @Mock CloseableHttpClient mockHttpClient;
  @Mock StatusLine mockStatusLine;
  @Mock CloseableHttpResponse mockResponse;
  @Mock HttpEntity mockEntity;

  ValoDao createDao(String host, int port, String key, String username, String password) {
    return new ValoDao(mockClientBuilder, host, port, key, username, password);
  }

  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);
    dao = createDao("http://localhost", port, "BUILD/ci/build", "", "");

    when(mockClientBuilder.build()).thenReturn(mockHttpClient);
    when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockClientBuilder);
    verifyNoMoreInteractions(mockHttpClient);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullHost() throws Exception {
    try {
      createDao(null, 8888, "BUILD/ci/build", "", "");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyHost() throws Exception {
    try {
      createDao(" ", 8888, "BUILD/ci/build", "", "");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailMissingScheme() throws Exception {
    try {
      createDao("localhost", 8888, "BUILD/ci/build", "", "");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host field must specify scheme, such as 'http://'", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullKey() throws Exception {
    try {
      createDao("http://localhost", 8888, null, "", "");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "tenant + stream uri is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyKey() throws Exception {
    try {
      createDao("http://localhost", 8888, " ", "", "");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "tenant + stream uri is required", e.getMessage());
      throw e;
    }
  }

  @Test
  public void constructorSuccess1() throws Exception {
    // Unit under test
    dao = createDao("https://localhost", 8888, "BUILD/ci/build", "", "");

    // Verify results
    assertEquals("Wrong host name", "https://localhost", dao.host);
    assertEquals("Wrong port", 8888, dao.port);
    assertEquals("Wrong key", "BUILD/ci/build", dao.key);
    assertEquals("Wrong name", "", dao.username);
    assertEquals("Wrong password", "", dao.password);
    assertEquals("Wrong auth", null, dao.auth); // Valo Doesnt Support auth right now
    assertEquals("Wrong uri", new URI("https://localhost:8888/streams/BUILD/ci/build"), dao.uri);
  }

  @Test
  public void getPostSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("http://localhost", 8888, "BUILD/ci/build", "", "");

    // Unit under test
    HttpPost post = dao.getHttpPost(json);
    HttpEntity entity = post.getEntity();

    assertEquals("Wrong uri", new URI("http://localhost:8888/streams/BUILD/ci/build") , post.getURI());
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
    dao = createDao("https://localhost", 8888, "BUILD/ci/build", "username", "password");

    // Unit under test
    HttpPost post = dao.getHttpPost(json);
    HttpEntity entity = post.getEntity();

    assertEquals("Wrong uri", new URI("https://localhost:8888/streams/BUILD/ci/build") , post.getURI());
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
    dao = createDao("http://localhost", 8888, "BUILD/ci/build", "", "");

    when(mockStatusLine.getStatusCode()).thenReturn(200);

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
    dao = createDao("http://localhost", 8888, "BUILD/ci/build", "username", "password");

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
}
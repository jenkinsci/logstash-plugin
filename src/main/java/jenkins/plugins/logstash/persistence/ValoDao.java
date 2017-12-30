/*
 * The MIT License
 *
 * Copyright 2014 Barnes and Noble College
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Valo Data Access Object.
 *
 * @author Paulino Padial
 * @since 1.0.4
 */
public class ValoDao extends AbstractLogstashIndexerDao {
  final HttpClientBuilder clientBuilder;
  final URI uri;
  final String auth;

  //primary constructor used by indexer factory
  public ValoDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  // Factored for unit testing
  ValoDao(HttpClientBuilder factory, String host, int port, String key, String username, String password) {
    super(host, port, key, username, password);

    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("tenant + stream uri is required");
    }

    try {
      uri = new URIBuilder(host)
        .setPort(port)
        // Normalizer will remove extra starting slashes, but missing slash will cause annoying failures
        .setPath("/streams/" + key)
        .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Could not create uri", e);
    }

    if(StringUtils.isBlank(uri.getScheme())) {
      throw new IllegalArgumentException("host field must specify scheme, such as 'http://'");
    }

    if (StringUtils.isNotBlank(username)) {
      auth = Base64.encodeBase64String((username + ":" + StringUtils.defaultString(password)).getBytes());
    } else {
      auth = null;
    }

    clientBuilder = factory == null ? HttpClientBuilder.create() : factory;
  }

  HttpPost getHttpPost(String data) {
    HttpPost postRequest;
    postRequest = new HttpPost(uri);
    StringEntity input = new StringEntity(data, ContentType.APPLICATION_JSON);
    postRequest.setEntity(input);
    if (auth != null) {
      postRequest.addHeader("Authorization", "Basic " + auth);
    }
    return postRequest;
  }
  HttpPut getHttpPut(String data) {
    HttpPut putRequest;
    putRequest = new HttpPut(uri);
    StringEntity input = new StringEntity(data, ContentType.APPLICATION_JSON);
    putRequest.setEntity(input);
    if (auth != null) {
      putRequest.addHeader("Authorization", "Basic " + auth);
    }
    return putRequest;
  }

  @Override
  public void push(String data) throws IOException {
    CloseableHttpClient httpClient = null;
    CloseableHttpResponse response = null;
    HttpPost post = getHttpPost(data);

    try {
      httpClient = clientBuilder.build();
      response = httpClient.execute(post);

      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException(this.getErrorMessage(response));
      }

    } finally {
      if (response != null) {
        response.close();
      }
      if (httpClient != null) {
        httpClient.close();
      }
    }
  }

  private String getErrorMessage(CloseableHttpResponse response) {
    ByteArrayOutputStream byteStream = null;
    PrintStream stream = null;
    try {
      byteStream = new ByteArrayOutputStream();
      stream = new PrintStream(byteStream);

      try {
        stream.print("HTTP error code: ");
        stream.println(response.getStatusLine().getStatusCode());
        stream.print("URI: ");
        stream.println(uri.toString());
        stream.println("RESPONSE: " + response.toString());
        response.getEntity().writeTo(stream);
      } catch (IOException e) {
        stream.println(ExceptionUtils.getStackTrace(e));
      }
      stream.flush();
      return byteStream.toString();
    } finally {
      if (stream != null) {
        stream.close();
      }
    }
  }

  @Override
  public IndexerType getIndexerType() { return IndexerType.VALO; }
}

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
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jenkins.plugins.logstash.utils.SSLHelper;
import org.apache.tools.ant.filters.StringInputStream;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;


/**
 * Elastic Search Data Access Object.
 *
 * @author Liam Newman
 * @since 1.0.4
 */
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class ElasticSearchDao extends AbstractLogstashIndexerDao {

  private transient HttpClientBuilder clientBuilder;
  private final URI uri;
  private final String auth;

  private String username;
  private String password;
  private String mimeType;
  private byte[] keystoreBytes;
  private String keyStorePassword;

  private final Boolean awsEnabled;
  private final String awsKeyId;
  private final String awsSecret;
  private final String awsRegion;
  private final String awsSessionToken;

  //primary constructor used by indexer factory
  public ElasticSearchDao(URI uri, String username, String password) {
    this(null, uri, username, password);
  }

  public ElasticSearchDao(URI uri, String username, String password,
                          boolean awsEnabled, String awsRegion, String awsKeyId, String awsSecret, String awsSessionToken) {
    this(null, uri, username, password,
        awsEnabled, awsRegion, awsKeyId, awsSecret, awsSessionToken);
  }

  // Factored for unit testing
  ElasticSearchDao(HttpClientBuilder factory, URI uri, String username, String password) {
    this(factory, uri, username, password,
        false, null, null, null, null);
  }


  ElasticSearchDao(HttpClientBuilder factory, URI uri, String username, String password,
                   boolean awsEnabled, String awsRegion, String awsKeyId, String awsSecret, String awsSessionToken) {
    if (uri == null)
    {
      throw new IllegalArgumentException("uri field must not be empty");
    }

    this.uri = uri;
    this.username = username;
    this.password = password;

    this.awsEnabled = awsEnabled;
    this.awsRegion = awsRegion;
    this.awsKeyId = awsKeyId;
    this.awsSecret = awsSecret;
    this.awsSessionToken = awsSessionToken;

    try
    {
      uri.toURL();
    }
    catch (MalformedURLException e)
    {
      throw new IllegalArgumentException(e);
    }

    if (StringUtils.isNotBlank(username)) {
      auth = Base64.encodeBase64String((username + ":" + StringUtils.defaultString(password)).getBytes(StandardCharsets.UTF_8));
    } else {
      auth = null;
    }

    clientBuilder = factory;
  }

  private byte[] getKeystoreBytes() {
    return keystoreBytes;
  }

  private String getKeyStorePassword() {
    return keyStorePassword;
  }

  private synchronized HttpClientBuilder getClientBuilder() throws IOException {
    if (clientBuilder == null) {
      clientBuilder = HttpClientBuilder.create();
      if (getKeystoreBytes() != null) {
        KeyStore trustStore;
        try {
          trustStore = KeyStore.getInstance("PKCS12");
          String pwd = getKeyStorePassword();
          if (pwd == null) {
            pwd = "";
          }
          trustStore.load(new ByteArrayInputStream(getKeystoreBytes()), pwd.toCharArray());
          SSLHelper.setClientBuilderSSLContext(clientBuilder, trustStore);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | KeyManagementException e) {
          throw new IOException(e);
        }
      }
    }
    return clientBuilder;
  }


  public URI getUri()
  {
    return uri;
  }
  public String getHost()
  {
    return uri.getHost();
  }

  public String getScheme()
  {
    return uri.getScheme();
  }

  public int getPort()
  {
    return uri.getPort();
  }

  public String getUsername()
  {
    return username;
  }

  public String getPassword()
  {
      return password;
  }

  public String getKey()
  {
    return uri.getPath();
  }

  public String getMimeType() {
    return this.mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  String getAuth()
  {
    return auth;
  }

  public void setCustomKeyStore(KeyStore customKeyStore, String keyStorePassword) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    if (customKeyStore != null) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      customKeyStore.store(bos, keyStorePassword.toCharArray());
      keystoreBytes = bos.toByteArray();
      this.keyStorePassword = keyStorePassword;
    }
  }

  HttpPost getHttpPost(String data) {
    HttpPost postRequest = new HttpPost(uri);
    String mimeType = this.getMimeType();
    // char encoding is set to UTF_8 since this request posts a JSON string
    StringEntity input = new StringEntity(data, StandardCharsets.UTF_8);
    mimeType = (mimeType != null) ? mimeType : ContentType.APPLICATION_JSON.toString();
    input.setContentType(mimeType);
    postRequest.setEntity(input);
    if (auth != null) {
      postRequest.addHeader("Authorization", "Basic " + auth);
    }
    if (awsEnabled){
      postRequest = awsSign(data, postRequest);
    }
    return postRequest;
  }

  private HttpPost awsSign(String data, HttpPost postRequest)
  {
    Aws4Signer singer = Aws4Signer.create();
    AwsCredentials awsCredentials;
    if (StringUtils.isNotEmpty(awsKeyId) && StringUtils.isNotEmpty( awsSecret) && StringUtils.isEmpty(awsSessionToken)){
      awsCredentials= AwsBasicCredentials.create(awsKeyId,awsSecret);
    } else if (StringUtils.isNotEmpty(awsKeyId) && StringUtils.isNotEmpty( awsSecret) && StringUtils.isNotEmpty(awsSessionToken)){
      awsCredentials= AwsSessionCredentials.create(awsKeyId,awsSecret,awsSessionToken);
    }else{
      awsCredentials = DefaultCredentialsProvider.builder().build().resolveCredentials();
    }
    Aws4SignerParams params = Aws4SignerParams.builder()
        .awsCredentials(awsCredentials)
        .signingName("es")
        .signingRegion(Region.of(awsRegion))
        .build();
    SdkHttpFullRequest sdkHttpFullRequest = SdkHttpFullRequest.builder()
        .uri(uri)
        .headers(StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                postRequest.headerIterator(),
                Spliterator.ORDERED),
            false)
            .map(Header.class::cast)
            .collect(Collectors.toMap(
                Header::getName,
                header -> Collections.singletonList(header.getValue())))
        )
        .contentStreamProvider(() -> new StringInputStream(data))
        .method(SdkHttpMethod.fromValue(postRequest.getMethod()))
        .build();
    SdkHttpFullRequest singedRequest = singer.sign(sdkHttpFullRequest, params);
    singedRequest.headers()
        .entrySet()
        .stream()
        .filter(headEntry -> !"Host".equals(headEntry.getKey()))
        .forEach(headEntry-> postRequest.setHeader(headEntry.getKey(), headEntry.getValue().get(0)));
    return postRequest;
  }

  @Override
  public void push(String data) throws IOException {
    HttpPost post = getHttpPost(data);

    try (CloseableHttpClient httpClient = getClientBuilder().build(); CloseableHttpResponse response = httpClient.execute(post)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode >= 300) {
        throw new IOException(this.getErrorMessage(response));
      }
    }
  }

  private String getErrorMessage(CloseableHttpResponse response) {
    ByteArrayOutputStream byteStream = null;
    PrintStream stream = null;
    try {
      byteStream = new ByteArrayOutputStream();
      stream = new PrintStream(byteStream, true, StandardCharsets.UTF_8.name());

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
      return byteStream.toString(StandardCharsets.UTF_8.name());
    }
    catch (UnsupportedEncodingException e)
    {
      return ExceptionUtils.getStackTrace(e);
    } finally {
      if (stream != null) {
        stream.close();
      }
    }
  }


  @Override
  public String getDescription()
  {
    return uri.toString();
  }
}

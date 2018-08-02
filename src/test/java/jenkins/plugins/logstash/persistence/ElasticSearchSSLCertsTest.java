package jenkins.plugins.logstash.persistence;

import jenkins.plugins.logstash.configuration.ElasticSearch;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ElasticSearchSSLCertsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final KeyStore NO_CLIENT_KEYSTORE = null;
    private static final SSLContext NO_SSL_CONTEXT = null;
    private static final TrustManager[] NO_SERVER_TRUST_MANAGER = null;

    private static final char[] KEYPASS_AND_STOREPASS_VALUE = "aaaaaa".toCharArray();
    private static final String JAVA_KEYSTORE = "jks";
    private static final String CLIENT_KEYSTORE = "elasticsearch-sslcerts/keystore.ks";
    private static final String CLIENT_TRUSTSTORE = "elasticsearch-sslcerts/truststore.ks";

    @Test
    public void NoSSLPost_NoSSLServer_Returns200OK() throws Exception {
        final HttpServer server = createLocalTestServer(NO_SSL_CONTEXT);
        server.start();

        String baseUrl = getBaseUrl(server);
        ElasticSearchDao dao = new ElasticSearchDao(null, new URI("http://" + baseUrl), "", "");

        try {
            dao.push("");
        } finally {
            server.stop();
        }
    }

    @Test
    public void SSLPost_NoSSLServer_NoTrustStore_ThrowsSSLException() throws Exception {
        SSLContext sslContext = createServerSSLContext(CLIENT_KEYSTORE, KEYPASS_AND_STOREPASS_VALUE);
        final HttpServer server = createLocalTestServer(sslContext);
        server.start();

        String baseUrl = getBaseUrl(server);
        ElasticSearchDao dao = new ElasticSearchDao(null, new URI("https://" + baseUrl), "", "");

        /*
            The server's cert does not exist in the default trust store. When connecting to a server
            that presents a certificate for validation during the SSL handshake, ElasticSearchDao cannot
            validate it and throws an SSLHandshakeException
        */
        try {
            thrown.expect(IsInstanceOf.instanceOf(SSLHandshakeException.class));
            thrown.expectMessage("unable to find valid certification path to requested target");

            dao.push("");
        } finally {
            server.stop();
        }
    }

    @Test
    public void SSLPost_SSLServer_UpdatedTrustStore_Returns200OK() throws Exception {
        SSLContext serverSSLContext = createServerSSLContext(CLIENT_KEYSTORE, KEYPASS_AND_STOREPASS_VALUE);
        final HttpServer server = createLocalTestServer(serverSSLContext);
        server.start();

        String baseUrl = getBaseUrl(server);

        ElasticSearchDao dao = new ElasticSearchDao(new URI("https://" + baseUrl), "", "");
        KeyStore keyStore = getStore(CLIENT_KEYSTORE, KEYPASS_AND_STOREPASS_VALUE);
        dao.setCustomKeyStore(keyStore);

        try {
            dao.push("");
        } finally {
            server.stop();
        }
    }

    @Test
    public void SSLPost_NoSSLServer_ThrowsSSLException() throws Exception {
        final HttpServer server = createLocalTestServer(NO_SSL_CONTEXT);
        server.start();

        String baseUrl = getBaseUrl(server);

        ElasticSearchDao dao = new ElasticSearchDao(new URI("https://" + baseUrl), "", "");
        KeyStore keyStore = getStore(CLIENT_KEYSTORE, KEYPASS_AND_STOREPASS_VALUE);
        dao.setCustomKeyStore(keyStore);

        try {
            thrown.expect(IsInstanceOf.instanceOf(SSLException.class));
            thrown.expectMessage("Unrecognized SSL message, plaintext connection?");

            dao.push("");
        } finally {
            server.stop();
        }
    }

    protected HttpServer createLocalTestServer(SSLContext sslContext)
            throws UnknownHostException {
            final HttpServer server = ServerBootstrap.bootstrap()
                .setLocalAddress(Inet4Address.getByName("localhost"))
                .setSslContext(sslContext)
                .registerHandler("*", new HttpRequestHandler(){
                        @Override
                        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
                            throws HttpException, IOException {
                            response.setStatusCode(HttpStatus.SC_OK);
                        }
                    })
                .create();

            return server;
    }

    protected String getBaseUrl(HttpServer server) {
                return server.getInetAddress().getHostName() + ":" + server.getLocalPort();
    }

    protected SSLContext createServerSSLContext(final String keyStoreFileName,
            final char[] password) throws CertificateException,
              NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException,
              KeyManagementException {
                  KeyStore serverKeyStore = getStore(keyStoreFileName, password);
                  KeyManager[] serverKeyManagers = getKeyManagers(serverKeyStore, password);

                  SSLContext sslContext = SSLContexts.custom().useProtocol("TLS").build();
                  // We don't install any trust managers in the server, hence null
                  sslContext.init(serverKeyManagers, null, new SecureRandom());

                  return sslContext;
    }

    protected KeyStore getStore(final String storeFileName, final char[] password) throws
        KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
            final KeyStore store = KeyStore.getInstance(JAVA_KEYSTORE);
            URL url = getClass().getClassLoader().getResource(storeFileName);
            InputStream inputStream = url.openStream();
            try {
                store.load(inputStream, password);
            } finally {
                inputStream.close();
            }

            return store;
        }

    protected KeyManager[] getKeyManagers(KeyStore store, final char[] password) throws
        NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(store, password);

            return keyManagerFactory.getKeyManagers();
        }

    protected TrustManager[] getTrustManagers(KeyStore store) throws NoSuchAlgorithmException,
              KeyStoreException {
                  TrustManagerFactory trustManagerFactory =
                      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                  trustManagerFactory.init(store);

                  return trustManagerFactory.getTrustManagers();
    }
}

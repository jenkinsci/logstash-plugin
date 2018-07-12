
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

package jenkins.plugins.logstash.utils;

import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class SSLHelper {

  public static void setClientBuilderSSLContext(HttpClientBuilder clientBuilder, KeyStore customKeyStore)
      throws CertificateException, NoSuchAlgorithmException,
          IOException, KeyStoreException, KeyManagementException
  {
    if (customKeyStore == null)
      return;
    String alias = customKeyStore.aliases().nextElement();
    X509Certificate certificate = (X509Certificate) customKeyStore.getCertificate(alias);
    if (certificate != null)
      clientBuilder.setSslcontext(SSLHelper.createSSLContext(alias, certificate));
  }

  public static SSLContext createSSLContext(String alias, X509Certificate certificate)
          throws CertificateException, NoSuchAlgorithmException,
                 IOException, KeyStoreException, KeyManagementException
  {
    // Step 1: Get all defaults
    TrustManagerFactory tmf = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
    // Using null here initialises the TMF with the default trust store.
    tmf.init((KeyStore) null);

    // Get hold of the default trust manager
    X509TrustManager defaultTM = null;
    for (TrustManager tm : tmf.getTrustManagers()) {
      if (tm instanceof X509TrustManager) {
        defaultTM = (X509TrustManager) tm;
        break;
      }
    }

    // Step 2: Add custom cert to keystore
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);
    ks.setEntry(alias, new KeyStore.TrustedCertificateEntry(certificate), null);

    // Create TMF with our custom cert's KS
    tmf = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);

    // Get hold of the custom trust manager
    X509TrustManager customTM = null;
    for (TrustManager tm : tmf.getTrustManagers()) {
      if (tm instanceof X509TrustManager) {
        customTM = (X509TrustManager) tm;
        break;
      }
    }

    // Step 3: Wrap it in our own class.
    final X509TrustManager finalDefaultTM = defaultTM;
    final X509TrustManager finalCustomTM = customTM;
    X509TrustManager combinedTM = new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return finalDefaultTM.getAcceptedIssuers();
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain,
                       String authType) throws CertificateException {
        try {
          finalCustomTM.checkServerTrusted(chain, authType);
        } catch (CertificateException e) {
          // This will throw another CertificateException if this fails too.
          finalDefaultTM.checkServerTrusted(chain, authType);
        }
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain,
                       String authType) throws CertificateException {
        finalDefaultTM.checkClientTrusted(chain, authType);
      }
    };

    // Step 4: Finally, create SSLContext based off of this combined TM
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[]{combinedTM}, null);

    return sslContext;
  }
}

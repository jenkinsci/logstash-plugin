package jenkins.plugins.logstash.configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.logstash.Messages;
import jenkins.plugins.logstash.persistence.ElasticSearchDao;

public class ElasticSearch extends LogstashIndexer<ElasticSearchDao>
{
  private String username;
  private Secret password;
  private URI uri;
  private String mimeType;
  private String customServerCertificateId;

  @DataBoundConstructor
  public ElasticSearch()
  {
  }

  public URI getUri()
  {
    return uri;
  }

  @Override
 public void validate() throws MimeTypeParseException {
    new MimeType(this.mimeType);
  }

  /*
   * We use URL for the setter as stapler can autoconvert a string to a URL but not to a URI
   */
  @DataBoundSetter
  public void setUri(URL url) throws URISyntaxException
  {
    this.uri = url.toURI();
  }

  public void setUri(URI uri) throws URISyntaxException
  {
    this.uri = uri;
  }

  public String getUsername()
  {
    return username;
  }

  @DataBoundSetter
  public void setUsername(String username)
  {
    this.username = username;
  }

  public String getPassword()
  {
    return Secret.toString(password);
  }

  @DataBoundSetter
  public void setPassword(String password)
  {
    this.password = Secret.fromString(password);
  }

  @DataBoundSetter
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }

  @DataBoundSetter
  public void setCustomServerCertificateId(String customServerCertificateId)
  {
    this.customServerCertificateId = customServerCertificateId;
  }

  public String getCustomServerCertificateId()
  {
    return customServerCertificateId;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
      return false;
    if (this == obj)
      return true;
    if (getClass() != obj.getClass())
      return false;
    ElasticSearch other = (ElasticSearch) obj;
    if (!Secret.toString(password).equals(other.getPassword()))
    {
      return false;
    }
    if (uri == null)
    {
      if (other.uri != null)
        return false;
    }
    else if (!uri.equals(other.uri))
    {
      return false;
    }
    if (username == null)
    {
      if (other.username != null)
        return false;
    }
    else if (!username.equals(other.username))
    {
      return false;
    }
    else if (!mimeType.equals(other.mimeType))
    {
      return false;
    }

    if (this.customServerCertificateId == null)
    {
      if (other.customServerCertificateId != null)
        return false;
    }
    else if (!this.customServerCertificateId.equals(other.customServerCertificateId))
    {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    result = prime * result + Secret.toString(password).hashCode();
    return result;
  }

  @Override
  public ElasticSearchDao createIndexerInstance()
  {
    ElasticSearchDao esDao = null;
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

    // Install custom certificate (if present) on a best-effort basis
    if (!StringUtils.isBlank(customServerCertificateId)) {
      StandardCertificateCredentials certificateCredentials = getCredentials(customServerCertificateId);
      if (certificateCredentials instanceof StandardCertificateCredentials) {
        try {
          // Fetch our custom certificate
          KeyStore keyStore = certificateCredentials.getKeyStore();
          String alias = keyStore.aliases().nextElement();
          X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

          SSLContext sslContext = ElasticSearch.createSSLContext(alias, certificate);
          if (sslContext != null)
            httpClientBuilder = httpClientBuilder.setSslcontext(sslContext);
        } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException
            | UnrecoverableKeyException | CertificateException | IOException ex) {
          //TODO: Report exception in logger
        }
      }
    }

    esDao = new ElasticSearchDao(httpClientBuilder, getUri(), username, Secret.toString(password));

    esDao.setMimeType(getMimeType());
    return esDao;
  }

  public static SSLContext createSSLContext(String alias, X509Certificate certificate)
      throws KeyStoreException, KeyManagementException, NoSuchAlgorithmException, IOException,
      UnrecoverableKeyException, CertificateException
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

  private StandardCertificateCredentials getCredentials(String credentials)
  {
    return (StandardCertificateCredentials) CredentialsMatchers.firstOrNull(
        CredentialsProvider.lookupCredentials(StandardCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
        CredentialsMatchers.withId(credentials)
    );
  }

  @Extension
  public static class ElasticSearchDescriptor extends LogstashIndexerDescriptor
  {
    @Override
    public String getDisplayName()
    {
      return "Elastic Search";
    }

    @Override
    public int getDefaultPort()
    {
      return 0;
    }

    public ListBoxModel doFillCustomServerCertificateIdItems(
        @AncestorInPath Item item,
        @QueryParameter String customServerCertificateId)
    {
      return new StandardListBoxModel().withEmptySelection()
          .withMatching( //
              CredentialsMatchers.anyOf(
                  CredentialsMatchers.instanceOf(StandardCertificateCredentials.class)),
              CredentialsProvider.lookupCredentials(StandardCredentials.class,
                  Jenkins.getInstance(),
                  ACL.SYSTEM,
                  Collections.EMPTY_LIST
              ));
    }

    public FormValidation doCheckUrl(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.warning(Messages.PleaseProvideHost());
      }
      try
      {
        URL url = new URL(value);

        if (url.getUserInfo() != null)
        {
          return FormValidation.error("Please specify user and password not as part of the url.");
        }

        if(StringUtils.isBlank(url.getPath()) || url.getPath().trim().matches("^\\/+$")) {
          return FormValidation.warning("Elastic Search requires a key to be able to index the logs.");
        }

        url.toURI();
      }
      catch (MalformedURLException | URISyntaxException e)
      {
        return FormValidation.error(e.getMessage());
      }
      return FormValidation.ok();
    }
    public FormValidation doCheckMimeType(@QueryParameter("value") String value) {
      if (StringUtils.isBlank(value)) {
        return FormValidation.error(Messages.ValueIsRequired());
      }
      try {
        //This is simply to check validity of the given mimeType
        new MimeType(value);
      } catch (MimeTypeParseException e) {
        return FormValidation.error(Messages.ProvideValidMimeType());
      }
      return FormValidation.ok();
    }
  }
}

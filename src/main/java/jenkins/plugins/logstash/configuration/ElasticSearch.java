package jenkins.plugins.logstash.configuration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.security.cert.CertificateException;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  private static final Logger LOGGER = Logger.getLogger(ElasticSearch.class.getName());

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
    ElasticSearchDao esDao = new ElasticSearchDao(getUri(), username, Secret.toString(password));

    esDao.setMimeType(getMimeType());
    try {
        esDao.setCustomKeyStore(getCustomKeyStore());
    } catch (KeyStoreException | CertificateException |
             NoSuchAlgorithmException | KeyManagementException | IOException e) {
          LOGGER.log(Level.WARNING, e.getMessage(), e);
    }
    return esDao;
  }

  private KeyStore getCustomKeyStore() {
    KeyStore customKeyStore = null;

    // Fetch custom alias+certificate as a keystore (if present)
    if (!StringUtils.isBlank(customServerCertificateId)) {
      StandardCertificateCredentials certificateCredentials = getCredentials(customServerCertificateId);
      if (certificateCredentials != null) {
        // Fetch keystore containing custom certificate
        customKeyStore = certificateCredentials.getKeyStore();
      }
    }

    return customKeyStore;
  }

  private StandardCertificateCredentials getCredentials(String credentials)
  {
    return (StandardCertificateCredentials) CredentialsMatchers.firstOrNull(
        CredentialsProvider.lookupCredentials(StandardCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
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
                  CredentialsMatchers.instanceOf(StandardCertificateCredentials.class)
              ),
              CredentialsProvider.lookupCredentials(StandardCredentials.class,
                  Jenkins.getInstance(),
                  ACL.SYSTEM,
                  Collections.emptyList()
              )
          );
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

package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.plugins.logstash.Messages;
import jenkins.plugins.logstash.persistence.ElasticSearchDao;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ElasticSearchTest
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private ElasticSearch indexer;
  private ElasticSearch indexer2;

  @Before
  public void setup() throws MalformedURLException, URISyntaxException
  {
    URL url = new URL("http://localhost:4567/key");
    indexer = new ElasticSearch();
    indexer.setUri(url);
    indexer.setPassword(Secret.fromString("password"));
    indexer.setUsername("user");
    indexer.setMimeType("application/json");

    indexer2 = new ElasticSearch();
    indexer2.setUri(url);
    indexer2.setPassword(Secret.fromString("password"));
    indexer2.setUsername("user");
    indexer2.setMimeType("application/json");
}

  @Test
  public void sameSettingsAreEqual()
  {
    assertThat(indexer.equals(indexer2), is(true));
  }

  @Test
  public void passwordChangeIsNotEqual()
  {
    indexer.setPassword(Secret.fromString("newPassword"));
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void urlChangeIsNotEqual() throws MalformedURLException, URISyntaxException
  {
    indexer.setUri(new URL("https://localhost:4567/key"));
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void usernameChangeIsNotEqual()
  {
    indexer.setUsername("newUser");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void connectTimeoutChangeIsNotEqual()
  {
    indexer.setConnectTimeout(30);
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void socketTimeoutChangeIsNotEqual()
  {
    indexer.setSocketTimeout(120);
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void connectTimeoutAffectsHashCode()
  {
    indexer.setConnectTimeout(30);
    assertThat(indexer.hashCode() == indexer2.hashCode(), is(false));
  }

  @Test
  public void socketTimeoutAffectsHashCode()
  {
    indexer.setSocketTimeout(120);
    assertThat(indexer.hashCode() == indexer2.hashCode(), is(false));
  }

  @Test
  public void createIndexerInstanceUsesTimeouts()
  {
    indexer.setConnectTimeout(30);
    indexer.setSocketTimeout(120);
    ElasticSearchDao dao = indexer.createIndexerInstance();
    assertThat(dao.getConnectTimeout(), is(30));
    assertThat(dao.getSocketTimeout(), is(120));
  }

  @Test
  public void doCheckConnectTimeoutRejectsNonPositive()
  {
    ElasticSearch.ElasticSearchDescriptor d = new ElasticSearch.ElasticSearchDescriptor();
    FormValidation validation = d.doCheckConnectTimeout(null, 0);
    assertThat(validation.kind, is(FormValidation.Kind.ERROR));
    assertThat(validation.getMessage(), is(Messages.ValueMustBePositive()));
  }

  @Test
  public void doCheckConnectTimeoutAcceptsPositive()
  {
    ElasticSearch.ElasticSearchDescriptor d = new ElasticSearch.ElasticSearchDescriptor();
    assertThat(d.doCheckConnectTimeout(null, 10).kind, is(FormValidation.Kind.OK));
  }

  @Test
  public void doCheckSocketTimeoutRejectsNonPositive()
  {
    ElasticSearch.ElasticSearchDescriptor d = new ElasticSearch.ElasticSearchDescriptor();
    FormValidation validation = d.doCheckSocketTimeout(null, 0);
    assertThat(validation.kind, is(FormValidation.Kind.ERROR));
    assertThat(validation.getMessage(), is(Messages.ValueMustBePositive()));
  }

  @Test
  public void doCheckSocketTimeoutAcceptsPositive()
  {
    ElasticSearch.ElasticSearchDescriptor d = new ElasticSearch.ElasticSearchDescriptor();
    assertThat(d.doCheckSocketTimeout(null, 60).kind, is(FormValidation.Kind.OK));
  }

}

package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

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
    indexer.setPassword("password");
    indexer.setUsername("user");
    indexer.setMimeType("application/json");

    indexer2 = new ElasticSearch();
    indexer2.setUri(url);
    indexer2.setPassword("password");
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
    indexer.setPassword("newPassword");
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

}

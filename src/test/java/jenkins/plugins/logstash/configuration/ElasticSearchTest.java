package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
  public void setup()
  {
    indexer = new ElasticSearch();
    indexer.setHost("http://localhost");
    indexer.setPort(4567);
    indexer.setPassword("password");
    indexer.setUsername("user");
    indexer.setKey("key");

    indexer2 = new ElasticSearch();
    indexer2.setHost("http://localhost");
    indexer2.setPort(4567);
    indexer2.setPassword("password");
    indexer2.setUsername("user");
    indexer2.setKey("key");
}

  @Test
  public void test_sameSettingsAreEqual()
  {
    assertThat(indexer.equals(indexer2), is(true));
  }

  @Test
  public void test_passwordChangeIsNotEqual()
  {
    indexer.setPassword("newPassword");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void test_usernameChangeIsNotEqual()
  {
    indexer.setUsername("newUser");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void test_KeyChangeIsNotEqual()
  {
    indexer.setKey("newKey");
    assertThat(indexer.equals(indexer2), is(false));
  }

}

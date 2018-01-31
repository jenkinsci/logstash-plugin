package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class RedisTest
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private Redis indexer;
  private Redis indexer2;

  @Before
  public void setup()
  {
    indexer = new Redis();
    indexer.setHost("localhost");
    indexer.setPort(4567);
    indexer.setKey("key");
    indexer.setPassword("password");

    indexer2 = new Redis();
    indexer2.setHost("localhost");
    indexer2.setPort(4567);
    indexer2.setKey("key");
    indexer2.setPassword("password");
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
  public void keyChangeIsNotEqual()
  {
    indexer.setKey("newKey");
    assertThat(indexer.equals(indexer2), is(false));
  }
}

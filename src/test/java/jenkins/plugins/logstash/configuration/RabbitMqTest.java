package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class RabbitMqTest
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private RabbitMq indexer;
  private RabbitMq indexer2;

  @Before
  public void setup()
  {
    indexer = new RabbitMq();
    indexer.setHost("localhost");
    indexer.setPort(4567);
    indexer.setPassword("password");
    indexer.setUsername("user");
    indexer.setQueue("queue");

    indexer2 = new RabbitMq();
    indexer2.setHost("localhost");
    indexer2.setPort(4567);
    indexer2.setPassword("password");
    indexer2.setUsername("user");
    indexer2.setQueue("queue");
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
  public void usernameChangeIsNotEqual()
  {
    indexer.setUsername("newUser");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void queueChangeIsNotEqual()
  {
    indexer.setQueue("newQueue");
    assertThat(indexer.equals(indexer2), is(false));
  }

}

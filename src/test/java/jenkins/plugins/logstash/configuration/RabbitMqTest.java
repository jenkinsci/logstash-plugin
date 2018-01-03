package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import jenkins.plugins.logstash.persistence.RabbitMqDao;

public class RabbitMqTest
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private RabbitMq indexer;
  private RabbitMqDao dao;

  @Before
  public void setup()
  {
    indexer = new RabbitMq();
    indexer.setHost("localhost");
    indexer.setPort(4567);
    indexer.setPassword("password");
    indexer.setUsername("user");
    indexer.setQueue("queue");
    dao = indexer.getInstance();
  }

  @Test
  public void noChangeReturnsSameInstance()
  {
    assertThat(indexer.shouldRefreshInstance(), is(false));
    assertThat(indexer.getInstance(),is(dao));
  }

  @Test
  public void passwordChangeLeadsToNewInstance()
  {
    indexer.setPassword("newPassword");
    assertThat(indexer.shouldRefreshInstance(), is(true));
  }

  @Test
  public void usernameChangeLeadsToNewInstance()
  {
    indexer.setUsername("newUser");
    assertThat(indexer.shouldRefreshInstance(), is(true));
  }

  @Test
  public void queueChangeLeadsToNewInstance()
  {
    indexer.setQueue("newQueue");
    assertThat(indexer.shouldRefreshInstance(), is(true));
  }

}

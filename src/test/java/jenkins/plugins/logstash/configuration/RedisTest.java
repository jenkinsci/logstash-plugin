package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import jenkins.plugins.logstash.persistence.RedisDao;

public class RedisTest
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private Redis indexer;
  private RedisDao dao;

  @Before
  public void setup()
  {
    indexer = new Redis();
    indexer.setHost("localhost");
    indexer.setPort(4567);
    indexer.setKey("key");
    indexer.setPassword("password");
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
  public void keyChangeLeadsToNewInstance()
  {
    indexer.setKey("newKey");
    assertThat(indexer.shouldRefreshInstance(), is(true));
  }


}

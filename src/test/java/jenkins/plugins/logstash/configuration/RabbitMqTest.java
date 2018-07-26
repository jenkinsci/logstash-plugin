package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.charset.Charset;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import jenkins.plugins.logstash.LogstashConfiguration;
import jenkins.plugins.logstash.LogstashConfigurationTestBase;
import jenkins.plugins.logstash.LogstashConfigurationTestBase.LogstashConfigurationForTest;
import jenkins.plugins.logstash.persistence.RabbitMqDao;

public class RabbitMqTest extends LogstashConfigurationTestBase
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private RabbitMq indexer;
  private RabbitMq indexer2;
  private RabbitMq indexer3;

  @Before
  public void setup()
  {
    indexer = new RabbitMq("UTF-8");
    indexer.setHost("localhost");
    indexer.setPort(4567);
    indexer.setPassword("password");
    indexer.setUsername("user");
    indexer.setQueue("queue");
    indexer.setVirtualHost("vhost");

    indexer2 = new RabbitMq("UTF-8");
    indexer2.setHost("localhost");
    indexer2.setPort(4567);
    indexer2.setPassword("password");
    indexer2.setUsername("user");
    indexer2.setQueue("queue");
    indexer2.setVirtualHost("vhost");

    indexer3 = new RabbitMq("UTF-16");
    indexer3.setHost("localhost");
    indexer3.setPort(4567);
    indexer3.setPassword("password");
    indexer3.setUsername("user");
    indexer3.setQueue("queue");
    indexer3.setQueue("vhost");
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

  @Test
  public void charsetChangeIsNotEqual()
  {
    assertThat(indexer.equals(indexer3), is(false));
  }

  public void vhostChangeIsNotEqual()
  {
    indexer.setVirtualHost("newVhost");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void rabbitMqBrokenCharset_returns_default_charset()
  {
    LogstashConfigurationTestBase.configFile = new File("src/test/resources/rabbitmq_brokenCharset.xml");
    LogstashConfiguration configuration = new LogstashConfigurationForTest();
    assertThat(configuration.getIndexerInstance(), IsInstanceOf.instanceOf(RabbitMqDao.class));
    assertThat(configuration.isEnabled(), equalTo(true));
    assertThat(configuration.getLogstashIndexer(),IsInstanceOf.instanceOf(RabbitMq.class));
    assertThat(((RabbitMq)configuration.getLogstashIndexer()).getEffectiveCharset(),equalTo(Charset.defaultCharset()));
  }

}

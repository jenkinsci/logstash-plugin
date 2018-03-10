package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;

import jenkins.plugins.logstash.persistence.ElasticSearchDao;
import jenkins.plugins.logstash.persistence.RabbitMqDao;
import jenkins.plugins.logstash.persistence.RedisDao;
import jenkins.plugins.logstash.persistence.SyslogDao;

public class LogstashConfigurationTest extends LogstashConfigurationTestBase
{

  @Test
  public void unconfiguredWillReturnNull()
  {
    LogstashConfigurationTestBase.configFile = new File("src/test/resources/notExisting.xml");
    LogstashConfiguration configuration = new LogstashConfigurationForTest();
    assertThat(configuration.getIndexerInstance(), equalTo(null));
  }

  @Test
  public void elasticSearchIsProperlyConfigured()
  {
    LogstashConfigurationTestBase.configFile = new File("src/test/resources/elasticSearch.xml");
    LogstashConfiguration configuration = new LogstashConfigurationForTest();
    assertThat(configuration.getIndexerInstance(), IsInstanceOf.instanceOf(ElasticSearchDao.class));
  }

  @Test
  public void rabbitMqIsProperlyConfigured()
  {
    LogstashConfigurationTestBase.configFile = new File("src/test/resources/rabbitmq.xml");
    LogstashConfiguration configuration = new LogstashConfigurationForTest();
    assertThat(configuration.getIndexerInstance(), IsInstanceOf.instanceOf(RabbitMqDao.class));
  }

  @Test
  public void redisIsProperlyConfigured()
  {
    LogstashConfigurationTestBase.configFile = new File("src/test/resources/redis.xml");
    LogstashConfiguration configuration = new LogstashConfigurationForTest();
    assertThat(configuration.getIndexerInstance(), IsInstanceOf.instanceOf(RedisDao.class));
  }

  @Test
  public void syslogIsProperlyConfigured()
  {
    LogstashConfigurationTestBase.configFile = new File("src/test/resources/syslog.xml");
    LogstashConfiguration configuration = new LogstashConfigurationForTest();
    assertThat(configuration.getIndexerInstance(), IsInstanceOf.instanceOf(SyslogDao.class));
  }

  @Test
  public void millSecondsConfigured()
  {
    LogstashConfigurationTestBase.configFile = new File("src/test/resources/rabbitmq.xml");
    LogstashConfiguration configuration = new LogstashConfigurationForTest();
    assertThat(configuration.isMilliSecondTimestamps(),equalTo(true));
    FastDateFormat formatter = configuration.getDateFormatter();
    assertThat(formatter.format(new Date(118,02,10,22,22)), matchesPattern("2018-03-10T22:22:00.000[+-]\\d{4}"));
  }
}

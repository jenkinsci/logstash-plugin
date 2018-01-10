package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloudbees.syslog.MessageFormat;

import jenkins.plugins.logstash.LogstashInstallation.Descriptor;
import jenkins.plugins.logstash.configuration.ElasticSearch;
import jenkins.plugins.logstash.configuration.LogstashIndexer;
import jenkins.plugins.logstash.configuration.RabbitMq;
import jenkins.plugins.logstash.configuration.Redis;
import jenkins.plugins.logstash.configuration.Syslog;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.IndexerType;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.SyslogFormat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LogstashInstallation.class, Descriptor.class })
@PowerMockIgnore({"javax.crypto.*"})
public class LogstashConfigurationMigrationTest extends LogstashConfigurationTestBase
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Mock
  Descriptor descriptor;

  LogstashConfiguration configuration;

  @Before
  public void setup()
  {
    mockStatic(LogstashInstallation.class);
    configFile = new File("notExisting.xml");
    when(LogstashInstallation.getLogstashDescriptor()).thenReturn(descriptor);
    when(descriptor.getHost()).thenReturn("localhost");
    when(descriptor.getPort()).thenReturn(4567);
    when(descriptor.getKey()).thenReturn("logstash");
    when(descriptor.getUsername()).thenReturn("user");
    when(descriptor.getPassword()).thenReturn("pwd");
    configuration = new LogstashConfigurationForTest();
  }

  @Test
  public void redisMigration()
  {
    when(descriptor.getType()).thenReturn(IndexerType.REDIS);
    configuration.migrateData();
    LogstashIndexer<?> indexer = configuration.getLogstashIndexer();
    assertThat(indexer, IsInstanceOf.instanceOf(Redis.class));
    Redis redis = (Redis) indexer;
    assertThat(redis.getHost(),equalTo("localhost"));
    assertThat(redis.getPort(),is(4567));
    assertThat(redis.getKey(), equalTo("logstash"));
    assertThat(redis.getPassword(), equalTo("pwd"));
  }

  @Test
  public void syslogMigrationRFC3164()
  {
    when(descriptor.getType()).thenReturn(IndexerType.SYSLOG);
    when(descriptor.getSyslogFormat()).thenReturn(SyslogFormat.RFC3164);
    configuration.migrateData();
    LogstashIndexer<?> indexer = configuration.getLogstashIndexer();
    assertThat(indexer, IsInstanceOf.instanceOf(Syslog.class));
    Syslog syslog = (Syslog) indexer;
    assertThat(syslog.getHost(),equalTo("localhost"));
    assertThat(syslog.getPort(),is(4567));
    assertThat(syslog.getMessageFormat(), equalTo(MessageFormat.RFC_3164));
  }

  @Test
  public void syslogMigrationRFC5424()
  {
    when(descriptor.getType()).thenReturn(IndexerType.SYSLOG);
    when(descriptor.getSyslogFormat()).thenReturn(SyslogFormat.RFC5424);
    configuration.migrateData();
    LogstashIndexer<?> indexer = configuration.getLogstashIndexer();
    assertThat(indexer, IsInstanceOf.instanceOf(Syslog.class));
    Syslog syslog = (Syslog) indexer;
    assertThat(syslog.getHost(),equalTo("localhost"));
    assertThat(syslog.getPort(),is(4567));
    assertThat(syslog.getMessageFormat(), equalTo(MessageFormat.RFC_5424));
  }

  @Test
  public void elasticSearchMigration() throws URISyntaxException, MalformedURLException
  {
    when(descriptor.getType()).thenReturn(IndexerType.ELASTICSEARCH);
    when(descriptor.getHost()).thenReturn("http://localhost");
    configuration.migrateData();
    LogstashIndexer<?> indexer = configuration.getLogstashIndexer();
    assertThat(indexer, IsInstanceOf.instanceOf(ElasticSearch.class));
    ElasticSearch es = (ElasticSearch) indexer;
    URI uri = new URI("http://localhost:4567/logstash");
    assertThat(es.getUri(),equalTo(uri));
    assertThat(es.getPassword(), equalTo("pwd"));
    assertThat(es.getUsername(), equalTo("user"));
  }

  @Test
  public void rabbitMqMigration()
  {
    when(descriptor.getType()).thenReturn(IndexerType.RABBIT_MQ);
    configuration.migrateData();
    LogstashIndexer<?> indexer = configuration.getLogstashIndexer();
    assertThat(indexer, IsInstanceOf.instanceOf(RabbitMq.class));
    RabbitMq es = (RabbitMq) indexer;
    assertThat(es.getHost(),equalTo("localhost"));
    assertThat(es.getPort(),is(4567));
    assertThat(es.getQueue(), equalTo("logstash"));
    assertThat(es.getPassword(), equalTo("pwd"));
    assertThat(es.getUsername(), equalTo("user"));
  }

}

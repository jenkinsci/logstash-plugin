package jenkins.plugins.logstash;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import jenkins.plugins.logstash.configuration.ElasticSearch;
import jenkins.plugins.logstash.configuration.Logstash;
import jenkins.plugins.logstash.configuration.RabbitMq;
import jenkins.plugins.logstash.configuration.Redis;

public class ConfigAsCodeTest {

  @Rule public JenkinsRule r = new JenkinsRule();

  @Test
  public void elasticSearch() throws ConfiguratorException {
    ConfigurationAsCode.get().configure(ConfigAsCodeTest.class.getResource("/jcasc/elasticSearch.yaml").toString());
    LogstashConfiguration c = LogstashConfiguration.getInstance();
    assertThat(c.isEnabled(), is(true));
    assertThat(c.isEnableGlobally(), is(true));
    assertThat(c.isMilliSecondTimestamps(), is(true));
    assertThat(c.getLogstashIndexer(), is(instanceOf(ElasticSearch.class)));
    ElasticSearch es = (ElasticSearch) c.getLogstashIndexer();
    assertThat(es.getUri().toString(), is("http://localhost:9200/jenkins/test"));
    assertThat(es.getMimeType(), is("application/json"));
    assertThat(es.getUsername(), is("es"));
  }

  @Test
  public void logstash() throws ConfiguratorException {
    ConfigurationAsCode.get().configure(ConfigAsCodeTest.class.getResource("/jcasc/logstash.yaml").toString());
    LogstashConfiguration c = LogstashConfiguration.getInstance();
    assertThat(c.isEnabled(), is(true));
    assertThat(c.isEnableGlobally(), is(true));
    assertThat(c.isMilliSecondTimestamps(), is(true));
    assertThat(c.getLogstashIndexer(), is(instanceOf(Logstash.class)));
    Logstash logstash = (Logstash) c.getLogstashIndexer();
    assertThat(logstash.getHost(), is("localhost"));
    assertThat(logstash.getPort(), is(9200));
  }

  @Test
  public void rabbitMq() throws ConfiguratorException {
    ConfigurationAsCode.get().configure(ConfigAsCodeTest.class.getResource("/jcasc/rabbitmq.yaml").toString());
    LogstashConfiguration c = LogstashConfiguration.getInstance();
    assertThat(c.isEnabled(), is(true));
    assertThat(c.isEnableGlobally(), is(true));
    assertThat(c.isMilliSecondTimestamps(), is(true));
    assertThat(c.getLogstashIndexer(), is(instanceOf(RabbitMq.class)));
    RabbitMq rabbitMq = (RabbitMq) c.getLogstashIndexer();
    assertThat(rabbitMq.getHost(), is("localhost"));
    assertThat(rabbitMq.getPort(), is(9200));
    assertThat(rabbitMq.getVirtualHost(), is("/vhost"));
    assertThat(rabbitMq.getUsername(), is("rabbit"));
  }

  @Test
  public void redis() throws ConfiguratorException {
    ConfigurationAsCode.get().configure(ConfigAsCodeTest.class.getResource("/jcasc/redis.yaml").toString());
    LogstashConfiguration c = LogstashConfiguration.getInstance();
    assertThat(c.isEnabled(), is(true));
    assertThat(c.isEnableGlobally(), is(true));
    assertThat(c.isMilliSecondTimestamps(), is(true));
    assertThat(c.getLogstashIndexer(), is(instanceOf(Redis.class)));
    Redis redis = (Redis) c.getLogstashIndexer();
    assertThat(redis.getHost(), is("localhost"));
    assertThat(redis.getPort(), is(9200));
    assertThat(redis.getKey(), is("redis"));
  }
}

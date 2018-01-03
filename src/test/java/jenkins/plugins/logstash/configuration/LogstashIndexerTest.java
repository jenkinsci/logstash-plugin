package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.cloudbees.syslog.MessageFormat;

import jenkins.plugins.logstash.persistence.MemoryDao;

public class LogstashIndexerTest
{

  private LogstashIndexerForTest indexer;

  @Before
  public void setup()
  {
    indexer = new LogstashIndexerForTest("localhost", 4567);
    indexer.getInstance();
  }

  @Test
  public void hostChangeLeadsToNewInstance()
  {
    indexer.setHost("remoteHost");
    assertThat(indexer.shouldRefreshInstance(), is(true));
  }

  @Test
  public void portChangeLeadsToNewInstance()
  {
    indexer.setPort(7654);
    assertThat(indexer.shouldRefreshInstance(), is(true));
  }

  public static class LogstashIndexerForTest extends LogstashIndexer<MemoryDao>
  {

    public LogstashIndexerForTest(String host, int port)
    {
      setHost(host);
      setPort(port);
    }

    @Override
    public MemoryDao createIndexerInstance()
    {
      return new MemoryDao(getHost(), getPort());
    }
  }
}

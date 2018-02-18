package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import jenkins.plugins.logstash.persistence.MemoryDao;

public class HostBasedLogstashIndexerTest
{

  private LogstashIndexerForTest indexer;
  private LogstashIndexerForTest indexer2;

  @Before
  public void setup()
  {
    indexer = new LogstashIndexerForTest("localhost", 4567);
    indexer2 = new LogstashIndexerForTest("localhost", 4567);
  }

  @Test
  public void sameSettingsAreEqual()
  {
    assertThat(indexer.equals(indexer2), is(true));
  }

  @Test
  public void hostChangeIsNotEqual()
  {
    indexer.setHost("remoteHost");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void portChangeIsNotEqual()
  {
    indexer.setPort(7654);
    assertThat(indexer.equals(indexer2), is(false));
  }

  public static class LogstashIndexerForTest extends HostBasedLogstashIndexer<MemoryDao>
  {

    public LogstashIndexerForTest(String host, int port)
    {
      setHost(host);
      setPort(port);
    }

    @Override
    public MemoryDao createIndexerInstance()
    {
      return new MemoryDao();
    }
  }
}

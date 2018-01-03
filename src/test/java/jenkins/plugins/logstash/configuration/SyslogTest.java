package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.cloudbees.syslog.MessageFormat;

import jenkins.plugins.logstash.persistence.SyslogDao;

public class SyslogTest
{

  private Syslog indexer;
  private SyslogDao dao;

  @Before
  public void setup()
  {
    indexer = new Syslog();
    indexer.setHost("localhost");
    indexer.setPort(4567);
    indexer.setMessageFormat(MessageFormat.RFC_3164);
    dao = indexer.getInstance();
  }

  @Test
  public void noChangeReturnsSameInstance()
  {
    assertThat(indexer.shouldRefreshInstance(), is(false));
    assertThat(indexer.getInstance(),is(dao));
  }

  @Test
  public void messageFormatChangeLeadsToNewInstance()
  {
    indexer.setMessageFormat(MessageFormat.RFC_5424);
    assertThat(indexer.shouldRefreshInstance(), is(true));
  }

}

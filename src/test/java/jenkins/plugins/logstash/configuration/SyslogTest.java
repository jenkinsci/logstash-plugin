package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.cloudbees.syslog.MessageFormat;

public class SyslogTest
{

  private Syslog indexer;
  private Syslog indexer2;

  @Before
  public void setup()
  {
    indexer = new Syslog();
    indexer.setHost("localhost");
    indexer.setPort(4567);
    indexer.setMessageFormat(MessageFormat.RFC_3164);

    indexer2 = new Syslog();
    indexer2.setHost("localhost");
    indexer2.setPort(4567);
    indexer2.setMessageFormat(MessageFormat.RFC_3164);
}

  @Test
  public void sameSettingsAreEqual()
  {
    assertThat(indexer.equals(indexer2), is(true));
  }

  @Test
  public void messageFormatChangeIsNotEqual()
  {
    indexer.setMessageFormat(MessageFormat.RFC_5424);
    assertThat(indexer.equals(indexer2), is(false));
  }

}

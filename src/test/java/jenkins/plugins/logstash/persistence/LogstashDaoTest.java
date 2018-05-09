package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.Charset;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.rabbitmq.client.AuthenticationFailureException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@RunWith(MockitoJUnitRunner.class)
public class LogstashDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  LogstashDao dao;

  LogstashDao createDao(String host, int port) {
    LogstashDao factory = new LogstashDao(host, port);

    factory.setCharset(Charset.defaultCharset());

    return factory;
  }

  @Test
  public void constructorFailNullHost() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("host name is required");
    createDao(null, 9000);
  }

  @Test
  public void constructorFailEmptyHost() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("host name is required");
    createDao(" ", 9000);
  }

  @Test
  public void constructorSuccess() throws Exception {
    // Unit under test
    dao = createDao("localhost", 5672);

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.getHost());
    assertEquals("Wrong port", 5672, dao.getPort());
  }
}

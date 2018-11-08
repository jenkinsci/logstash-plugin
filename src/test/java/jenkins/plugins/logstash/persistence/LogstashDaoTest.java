package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LogstashDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  LogstashDao dao;

  LogstashDao createDao(String host, int port) {
    return new LogstashDao(host, port);
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

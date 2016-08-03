package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.IndexerType;

import org.junit.Test;

public class IndexerDaoFactoryTest {

  @Test
  public void getAllInstances() throws Exception {
    for (IndexerType type : IndexerType.values()) {
      String host = (type == IndexerType.ELASTICSEARCH || type == IndexerType.VALO)  ? "http://localhost" : "localhost";
      LogstashIndexerDao dao = IndexerDaoFactory.getInstance(type, host, 1234, "key", "username", "password");

      assertNotNull("Result was null", dao);
      assertEquals("Result implements wrong IndexerType", type, dao.getIndexerType());
    }
  }

  @Test
  public void successNulls() throws Exception {
    for (IndexerType type : IndexerType.values()) {
      String host = (type == IndexerType.ELASTICSEARCH || type == IndexerType.VALO) ? "http://localhost" : "localhost";
      LogstashIndexerDao dao = IndexerDaoFactory.getInstance(type, host, null, "key", null, null);

      assertNotNull("Result was null", dao);
      assertEquals("Result implements wrong IndexerType", type, dao.getIndexerType());
    }
  }

  @Test(expected = InstantiationException.class)
  public void failureNullType() throws Exception {
    try {
      IndexerDaoFactory.getInstance(null, "localhost", 1234, "key", "username", "password");
    } catch (InstantiationException e) {
      String msg = "[logstash-plugin]: Unknown IndexerType 'null'. Did you forget to configure the plugin?";
      assertEquals("Wrong message", msg, e.getMessage());
      throw e;
    }
  }
}

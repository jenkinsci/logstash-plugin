package jenkins.plugins.logstash.configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ElasticSearchTest
{

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private ElasticSearch indexer;
  private ElasticSearch indexer2;

  @Before
  public void setup() throws URISyntaxException
  {
    String uri = "http://localhost:4567/key";
    indexer = new ElasticSearch(uri);
    indexer.setPassword("password");
    indexer.setUsername("user");

    indexer2 = new ElasticSearch(uri);
    indexer2.setPassword("password");
    indexer2.setUsername("user");
}

  @Test
  public void sameSettingsAreEqual()
  {
    assertThat(indexer.equals(indexer2), is(true));
  }

  @Test
  public void passwordChangeIsNotEqual()
  {
    indexer.setPassword("newPassword");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void uriChangeIsNotEqual() throws URISyntaxException
  {
    indexer.setUri("https://localhost:4567/key");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void usernameChangeIsNotEqual()
  {
    indexer.setUsername("newUser");
    assertThat(indexer.equals(indexer2), is(false));
  }

  @Test
  public void missingPathThrowsException() throws URISyntaxException
  {
    try
    {
      indexer.setUri("http://localhost:8000/");
      fail("Expected an IllegalArgumentException");
    }
    catch (IllegalArgumentException e)
    {
      assertThat("Please specify an elastic search key.",equalTo(e.getMessage()));
    }
  }

  @Test
  public void missingPortThrowsException() throws URISyntaxException
  {
    try
    {
      indexer.setUri("http://localhost/logstash");
      fail("Expected an IllegalArgumentException");
    }
    catch (IllegalArgumentException e)
    {
      assertThat("Please specify a port.",equalTo(e.getMessage()));
    }
  }

  @Test
  public void missingSchemeThrowsException() throws URISyntaxException
  {
    try
    {
      indexer.setUri("localhost:8000/logstash");
      fail("Expected an IllegalArgumentException");
    }
    catch (IllegalArgumentException e)
    {
      assertThat("unknown protocol: localhost",equalTo(e.getMessage()));
    }
  }
}

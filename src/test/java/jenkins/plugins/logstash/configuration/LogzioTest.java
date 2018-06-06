package jenkins.plugins.logstash.configuration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LogzioTest{
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private Logzio indexer;
    private Logzio indexer2;

    @Before
    public void setup(){
        indexer = new Logzio();
        indexer.setHost("https://listener.logz.io:8071");
        indexer.setKey("key");

        indexer2 = new Logzio();
        indexer2.setHost("https://listener.logz.io:8071");
        indexer2.setKey("key");
    }

    @Test
    public void sameSettingsAreEqual(){ assertThat(indexer.equals(indexer2), is(true)); }

    @Test
    public void keyChangeIsNotEqual() {
        indexer.setKey("newPassword");
        assertThat(indexer.equals(indexer2), is(false));
    }

    @Test
    public void hostChangeIsNotEqual() {
        indexer.setHost("https://logz.io");
        assertThat(indexer.equals(indexer2), is(false));
    }

}

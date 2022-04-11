package jenkins.plugins.logstash.configuration;

import com.gargoylesoftware.htmlunit.html.*;
import hudson.model.Descriptor;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.plugins.logstash.LogstashConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.html.HTMLInputElement;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ElasticSearchAwsTest
{

  @Rule
  public JenkinsRule j = new JenkinsRule();



  @Test
  public void setAws() throws IOException, SAXException
  {
    LogstashConfiguration logstashConfiguration = (LogstashConfiguration) j.jenkins.getDescriptor(LogstashConfiguration.class);
    logstashConfiguration.setEnabled(true);
    ElasticSearch elasticSearch=new ElasticSearch();
    elasticSearch.setAwsEnabled(true);
    elasticSearch.setAwsRegion("region1");
    elasticSearch.setAwsKeyId("keyid1");
    Secret awsSecret = Secret.fromString("secret1");
    elasticSearch.setAwsSecret(awsSecret);
    Secret awsSessionToken = Secret.fromString("sessionToken1");
    elasticSearch.setAwsSessionToken(awsSessionToken);
    logstashConfiguration.setLogstashIndexer(elasticSearch);
    logstashConfiguration.save();

    HtmlPage configurePage = j.createWebClient().goTo("configure");

    HtmlCheckBoxInput awsEnabled =(HtmlCheckBoxInput) configurePage.getElementsByName("awsEnabled").get(0);
    String checkedAttribute = awsEnabled.getCheckedAttribute();
    assertEquals( "true",checkedAttribute);
    HtmlTextInput region =(HtmlTextInput) configurePage.getElementsByName("_.awsRegion").get(0);
    assertEquals( "region1", region.getValueAttribute());
    HtmlTextInput keyId =(HtmlTextInput) configurePage.getElementsByName("_.awsKeyId").get(0);
    assertEquals( "keyid1", keyId.getValueAttribute());
    HtmlHiddenInput secret =(HtmlHiddenInput) configurePage.getElementsByName("_.awsSecret").get(0);
    assertEquals(awsSecret.getEncryptedValue(), secret.getValueAttribute());
    HtmlHiddenInput sessionToken =(HtmlHiddenInput) configurePage.getElementsByName("_.awsSessionToken").get(0);
    assertEquals(awsSessionToken.getEncryptedValue(), sessionToken.getValueAttribute());

  }



}

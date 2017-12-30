package jenkins.plugins.logstash.persistence;

import com.cloudbees.syslog.sender.UdpSyslogMessageSender;
import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import jenkins.plugins.logstash.LogstashInstallation;
import jenkins.plugins.logstash.LogstashInstallation.Descriptor;

public class SyslogDao extends AbstractLogstashIndexerDao {

  private String syslogFormat = null;
  private final static Logger LOG = Logger.getLogger(SyslogDao.class.getName());
  private final UdpSyslogMessageSender messageSender;

  public SyslogDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  public SyslogDao(UdpSyslogMessageSender udpSyslogMessageSender, String host, int port, String key, String username, String password) {
    super(host, port, key, username, password);
    messageSender = udpSyslogMessageSender == null ? new UdpSyslogMessageSender() : udpSyslogMessageSender;
  }

  public void setSyslogFormat(String format) {
	syslogFormat = format;
  }

  @Override
  public void push(String data) throws IOException {

    try {
	  Descriptor logstashPluginConfig = (Descriptor) Jenkins.getInstance().getDescriptor(LogstashInstallation.class);
	  syslogFormat = logstashPluginConfig.getSyslogFormat().toString();
	} catch (NullPointerException e){
	  LOG.log(Level.WARNING, "Unable to read syslogFormat in the jenkins logstash plugin configuration");
	}

    // Making the JSON document compliant to Common Event Expression (CEE)
    // Ref: http://www.rsyslog.com/json-elasticsearch/
    data = " @cee: "  + data;
    // SYSLOG Configuration
    messageSender.setDefaultMessageHostname(host);
    messageSender.setDefaultAppName("jenkins:");
    messageSender.setDefaultFacility(Facility.USER);
    messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
    messageSender.setSyslogServerHostname(host);
    messageSender.setSyslogServerPort(port);
    // The Logstash syslog input module support only the RFC_3164 format
    // Ref: https://www.elastic.co/guide/en/logstash/current/plugins-inputs-syslog.html
    if (syslogFormat == "RFC3164" ) {
      messageSender.setMessageFormat(MessageFormat.RFC_3164);
    }
    else {
      messageSender.setMessageFormat(MessageFormat.RFC_5424);
    }
    // Sending the message
    messageSender.sendMessage(data);
  }

  @Override
  public IndexerType getIndexerType() { return IndexerType.SYSLOG; }
}

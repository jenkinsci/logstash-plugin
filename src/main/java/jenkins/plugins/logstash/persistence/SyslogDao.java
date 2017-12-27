package jenkins.plugins.logstash.persistence;

import java.io.IOException;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

public class SyslogDao extends AbstractLogstashIndexerDao {

  private MessageFormat messageFormat = MessageFormat.RFC_3164;
  private final UdpSyslogMessageSender messageSender;

  public SyslogDao(String host, int port) {
    this(null, host, port);
  }

  public SyslogDao(UdpSyslogMessageSender udpSyslogMessageSender, String host, int port) {
    super(host, port);
    messageSender = udpSyslogMessageSender == null ? new UdpSyslogMessageSender() : udpSyslogMessageSender;
  }

  public void setMessageFormat(MessageFormat format) {
    messageFormat = format;
  }

  public MessageFormat getMessageFormat() {
    return messageFormat;
  }

  @Override
  public void push(String data) throws IOException {

    // Making the JSON document compliant to Common Event Expression (CEE)
    // Ref: http://www.rsyslog.com/json-elasticsearch/
    data = " @cee: "  + data;
    // SYSLOG Configuration
    messageSender.setDefaultMessageHostname(getHost());
    messageSender.setDefaultAppName("jenkins:");
    messageSender.setDefaultFacility(Facility.USER);
    messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
    messageSender.setSyslogServerHostname(getHost());
    messageSender.setSyslogServerPort(getPort());
    // The Logstash syslog input module support only the RFC_3164 format
    // Ref: https://www.elastic.co/guide/en/logstash/current/plugins-inputs-syslog.html
    messageSender.setMessageFormat(messageFormat);
    // Sending the message
    messageSender.sendMessage(data);
  }
}

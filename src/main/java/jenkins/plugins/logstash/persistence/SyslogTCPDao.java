package jenkins.plugins.logstash.persistence;

import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import java.io.IOException;


public class SyslogTCPDao extends AbstractLogstashIndexerDao {
  final TcpSyslogMessageSender messageSender;
  
  public SyslogTCPDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  public SyslogTCPDao(TcpSyslogMessageSender tcpSyslogMessageSender, String host, int port, String key, String username, String password) {
    super(host, port, key, username, password);
    messageSender = tcpSyslogMessageSender == null ? new TcpSyslogMessageSender() : tcpSyslogMessageSender;
  }

  @Override
  public void push(String data) throws IOException {
    // Making the JSON document compliant to Common Event Expression (CEE)
    // http://www.rsyslog.com/json-elasticsearch/
    data = " @cee: "  + data;
    // SYSLOGTCP Configuration
    messageSender.setDefaultMessageHostname(host);
    messageSender.setDefaultAppName("jenkins:");
    messageSender.setDefaultFacility(Facility.USER);
    messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
    messageSender.setSyslogServerHostname(host);
    messageSender.setSyslogServerPort(port);
    messageSender.setMessageFormat(MessageFormat.RFC_5424);
    messageSender.setSsl(false);
    // Sending the message
    messageSender.sendMessage(data);
  }

  @Override
  public IndexerType getIndexerType() { return IndexerType.SYSLOGTCP; }
}

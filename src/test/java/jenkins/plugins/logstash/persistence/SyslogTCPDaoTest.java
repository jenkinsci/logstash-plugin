package jenkins.plugins.logstash.persistence;

import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;


@RunWith(MockitoJUnitRunner.class)
public class SyslogTCPDaoTest {
  SyslogTCPDao dao;
  String data = "{ 'junit': 'SyslogTCPDaoTest' }";
  String host = "localhost";
  String appname = "jenkins:";
  int port = 514;
  TcpSyslogMessageSender testSyslogSend = new TcpSyslogMessageSender();
  @Mock TcpSyslogMessageSender mockTcpSyslogMessageSender;
 	  
  @Before
  public void before() throws Exception {    
    dao = createDao(host, port, null, null, null);
    dao.push(data); 
  }

  // Test the Message format.
  @Test
  public void ceeMessageFormat() throws Exception {
    verify(mockTcpSyslogMessageSender, times(1)).sendMessage(" @cee: " + data);
  }

  // Test the MessageSender configuration. 
  @Test
  public void syslogConfig() throws Exception {
    verify(mockTcpSyslogMessageSender, times(1)).setDefaultMessageHostname(host);
    verify(mockTcpSyslogMessageSender, times(1)).setDefaultAppName(appname);
    verify(mockTcpSyslogMessageSender, times(1)).setSyslogServerHostname(host);
    verify(mockTcpSyslogMessageSender, times(1)).setSyslogServerPort(port);
    verify(mockTcpSyslogMessageSender, times(1)).setDefaultFacility(Facility.USER);
    verify(mockTcpSyslogMessageSender, times(1)).setDefaultSeverity(Severity.INFORMATIONAL);
    verify(mockTcpSyslogMessageSender, times(1)).setMessageFormat(MessageFormat.RFC_5424);
    verify(mockTcpSyslogMessageSender, times(1)).setSsl(false);
  }

  // Send a real Syslog message.
  @Test
  public void syslogSend() throws Exception {
    testSyslogSend.setDefaultMessageHostname(host);
    testSyslogSend.setDefaultAppName(appname);
    testSyslogSend.setSyslogServerHostname(host);
    testSyslogSend.setSyslogServerPort(port);
    testSyslogSend.setDefaultFacility(Facility.USER);
    testSyslogSend.setDefaultSeverity(Severity.INFORMATIONAL);
    testSyslogSend.setMessageFormat(MessageFormat.RFC_5424);  
    testSyslogSend.setSsl(false);
    testSyslogSend.sendMessage(" @cee: " + data);
  }
  
  SyslogTCPDao createDao(String host, int port, String key, String username, String password) {
    return new SyslogTCPDao(mockTcpSyslogMessageSender, host, port, key, username, password);
  }
}

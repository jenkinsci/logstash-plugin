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
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

@RunWith(MockitoJUnitRunner.class)
public class SyslogDaoTest {
  SyslogDao dao;
  String data = "{ 'junit': 'SyslogDaoTest' }";
  String host = "localhost";
  String appname = "jenkins:";
  int port = 514;
  UdpSyslogMessageSender udpSyslogMessageSender = new UdpSyslogMessageSender();
  @Mock UdpSyslogMessageSender mockUdpSyslogMessageSender;
 	  
  @Before
  public void before() throws Exception {  
    dao = createDao(host, port, null, null, null);
    dao.push(data);
  }

  // Test the Message content.
  @Test
  public void ceeMessageFormat() throws Exception {
    verify(mockUdpSyslogMessageSender, times(1)).sendMessage(" @cee: " + data);
  }

  // Test the MessageSender configuration. 
  @Test
  public void syslogConfig() throws Exception {
    verify(mockUdpSyslogMessageSender, times(1)).setDefaultMessageHostname(host);
    verify(mockUdpSyslogMessageSender, times(1)).setDefaultAppName(appname);
    verify(mockUdpSyslogMessageSender, times(1)).setSyslogServerHostname(host);
    verify(mockUdpSyslogMessageSender, times(1)).setSyslogServerPort(port);
    verify(mockUdpSyslogMessageSender, times(1)).setDefaultFacility(Facility.USER);
    verify(mockUdpSyslogMessageSender, times(1)).setDefaultSeverity(Severity.INFORMATIONAL);
    verify(mockUdpSyslogMessageSender, times(1)).setMessageFormat(MessageFormat.RFC_5424);
  }
  
  SyslogDao createDao(String host, int port, String key, String username, String password) {
    return new SyslogDao(mockUdpSyslogMessageSender, host, port, key, username, password);
  }
  
}

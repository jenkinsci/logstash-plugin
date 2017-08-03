package jenkins.plugins.logstash.persistence;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.Socket;


public class LogstashDao extends AbstractLogstashIndexerDao {

  final String logstashHost;
  final int logstashPort;
  Socket logstashClientSocket;

  public LogstashDao(String logstashHostString, int logstashPortInt, String indexKey, String username, String password) {
    this(null, logstashHostString, logstashPortInt, indexKey, username, password);
  }

  public LogstashDao(HttpClientBuilder factory, String logstashHostString, int logstashPortInt, String indexKey, String username, String password) {
    super(logstashHostString, logstashPortInt, indexKey, username, password);
    this.logstashHost = logstashHostString;
    this.logstashPort = logstashPortInt;
  }

  @Override
  public void push(String data) throws IOException {

    try
    {
      logstashClientSocket = new Socket(logstashHost, logstashPort);
      DataOutputStream outToServer = new DataOutputStream(logstashClientSocket.getOutputStream());
      BufferedReader inFromServer = new BufferedReader(new InputStreamReader(logstashClientSocket.getInputStream()));
      outToServer.writeBytes(data);
      logstashClientSocket.setSoTimeout(10000);
      logstashClientSocket.close();
      outToServer.close();
      inFromServer.close();
    }
    catch (Exception exc)
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      exc.printStackTrace(pw);
      throw new IOException(sw.toString());
    }

  }

  @Override
  public IndexerType getIndexerType() { return IndexerType.LOGSTASH; }
}

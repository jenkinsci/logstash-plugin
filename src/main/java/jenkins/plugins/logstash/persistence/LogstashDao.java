package jenkins.plugins.logstash.persistence;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class LogstashDao extends HostBasedLogstashIndexerDao {

  public LogstashDao(String logstashHostString, int logstashPortInt) {
    super(logstashHostString, logstashPortInt);
  }

  @Override
  public void push(String data) throws IOException {

    try (Socket logstashClientSocket = new Socket(getHost(), getPort()))
    {
      OutputStream out = logstashClientSocket.getOutputStream();
      out.write(data.getBytes(StandardCharsets.UTF_8));
      out.write(10);
      out.flush();
      out.close();
    }
  }
}
package jenkins.plugins.logstash.configuration;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class HostConfiguration
{

  protected String host;
  protected int port;

  @DataBoundConstructor
  public HostConfiguration()
  {
  }

  public String getHost()
  {
    return host;
  }

  @DataBoundSetter
  public void setHost(String host)
  {
    this.host = host;
  }

  public int getPort()
  {
    return port;
  }

  @DataBoundSetter
  public void setPort(int port)
  {
    this.port = port;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    HostConfiguration other = (HostConfiguration) obj;
    if (host == null) {
      if (other.host != null)
        return false;
    } else if (!host.equals(other.host))
      return false;
    if (port != other.port)
      return false;
    return true;
  }
}

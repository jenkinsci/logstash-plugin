package jenkins.plugins.logstash.configuration;

import org.kohsuke.stapler.DataBoundSetter;

import jenkins.plugins.logstash.persistence.AbstractLogstashIndexerDao;

public abstract class HostBasedLogstashIndexer<T extends AbstractLogstashIndexerDao> extends LogstashIndexer<T>
{
  private String host;
  private int port;

  /**
   * Returns the host for connecting to the indexer.
   *
   * @return Host of the indexer
   */
  public String getHost()
  {
    return host;
  }

  /**
   * Sets the host for connecting to the indexer.
   *
   * @param host
   *          host to connect to.
   */
  @DataBoundSetter
  public void setHost(String host)
  {
    this.host = host;
  }

  /**
   * Returns the port for connecting to the indexer.
   *
   * @return Port of the indexer
   */
  public int getPort()
  {
    return port;
  }

  /**
   * Sets the port used for connecting to the indexer
   *
   * @param port
   *          The port of the indexer
   */
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
    HostBasedLogstashIndexer<?> other = (HostBasedLogstashIndexer<?>) obj;
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

package jenkins.plugins.logstash.configuration;

import jenkins.plugins.logstash.persistence.MemoryDao;

public class MemoryIndexer extends LogstashIndexer<MemoryDao>
{
  MemoryDao dao;

  public MemoryIndexer(MemoryDao dao)
  {
    this.dao = dao;
  }

  @Override
  protected MemoryDao createIndexerInstance()
  {
    return dao;
  }

}

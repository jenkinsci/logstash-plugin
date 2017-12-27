package jenkins.plugins.logstash;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.integration.jul.SyslogMessageFormatter;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.GlobalConfiguration;
import jenkins.plugins.logstash.LogstashInstallation.Descriptor;
import jenkins.plugins.logstash.configuration.ElasticSearch;
import jenkins.plugins.logstash.configuration.LogstashIndexer;
import jenkins.plugins.logstash.configuration.LogstashIndexer.LogstashIndexerDescriptor;
import jenkins.plugins.logstash.configuration.RabbitMq;
import jenkins.plugins.logstash.configuration.Redis;
import jenkins.plugins.logstash.configuration.Syslog;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.IndexerType;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao.SyslogFormat;
import net.sf.json.JSONObject;

@Extension
public class LogstashConfiguration extends GlobalConfiguration
{
  private static final Logger LOGGER = Logger.getLogger(LogstashConfiguration.class.getName());
  private LogstashIndexer<?> logstashIndexer;
  private boolean dataMigrated = false;

  public LogstashConfiguration()
  {
    load();
  }

  public LogstashIndexer<?> getLogstashIndexer()
  {
    return logstashIndexer;
  }

  public void setLogstashIndexer(LogstashIndexer<?> logstashIndexer)
  {
    this.logstashIndexer = logstashIndexer;
  }

  @CheckForNull
  public LogstashIndexerDao getIndexerInstance()
  {
    if (logstashIndexer != null)
    {
      return logstashIndexer.getInstance();
    }
    return null;
  }

  public List<?> getIndexerTypes()
  {
    return LogstashIndexer.all();
  }

  @Initializer(after = InitMilestone.JOB_LOADED)
  public void migrateData()
  {
    if (!dataMigrated)
    {
      Descriptor descriptor = LogstashInstallation.getLogstashDescriptor();
      if (descriptor.getType() != null)
      {
        IndexerType type = descriptor.getType();
        switch (type)
        {
          case REDIS:
            LOGGER.log(Level.INFO, "Migrating logstash configuration for Redis");
            Redis redis = new Redis();
            redis.setHost(descriptor.getHost());
            redis.setPort(descriptor.getPort());
            redis.setKey(descriptor.getKey());
            redis.setPassword(descriptor.getPassword());
            logstashIndexer = redis;
            break;
          case ELASTICSEARCH:
            LOGGER.log(Level.INFO, "Migrating logstash configuration for Elastic Search");
            ElasticSearch es = new ElasticSearch();
            es.setHost(descriptor.getHost());
            es.setPort(descriptor.getPort());
            es.setKey(descriptor.getKey());
            es.setUsername(descriptor.getUsername());
            es.setPassword(descriptor.getPassword());
            logstashIndexer = es;
            break;
          case RABBIT_MQ:
            LOGGER.log(Level.INFO, "Migrating logstash configuration for  RabbitMQ");
            RabbitMq rabbitMq = new RabbitMq();
            rabbitMq.setHost(descriptor.getHost());
            rabbitMq.setPort(descriptor.getPort());
            rabbitMq.setQueue(descriptor.getKey());
            rabbitMq.setUsername(descriptor.getUsername());
            rabbitMq.setPassword(descriptor.getPassword());
            logstashIndexer = rabbitMq;
            break;
          case SYSLOG:
            LOGGER.log(Level.INFO, "Migrating logstash configuration for  SYSLOG");
            Syslog syslog = new Syslog();
            syslog.setHost(descriptor.getHost());
            syslog.setPort(descriptor.getPort());
            syslog.setSyslogProtocol(descriptor.getSyslogProtocol());
            switch (descriptor.getSyslogFormat())
            {
              case RFC3164:
                syslog.setMessageFormat(MessageFormat.RFC_3164);
                break;
              case RFC5424:
                syslog.setMessageFormat(MessageFormat.RFC_5424);
                break;
              default:
                syslog.setMessageFormat(MessageFormat.RFC_3164);
                break;
            }
            logstashIndexer = syslog;
            break;
          default:
            LOGGER.log(Level.INFO, "unknown logstash Indexer type: " + type);
            break;
        }
      }
      dataMigrated = true;
      save();
    }
  }

  @Override
  public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException
  {
    JSONObject j = json.getJSONObject("logstashIndexer");
    String clazz = j.getString("stapler-class");
    if (logstashIndexer == null || !logstashIndexer.getClass().getName().equals(clazz))
    {
      staplerRequest.bindJSON(this, json);
    }
    else
    {
      logstashIndexer.reconfigure(staplerRequest, j);
    }
    save();
    return true;
  }

  public static LogstashConfiguration getInstance()
  {
    return GlobalConfiguration.all().get(LogstashConfiguration.class);
  }

}

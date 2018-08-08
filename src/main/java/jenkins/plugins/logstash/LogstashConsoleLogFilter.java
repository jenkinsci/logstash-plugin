package jenkins.plugins.logstash;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;

import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;

@Extension(ordinal = 1000)
public class LogstashConsoleLogFilter extends ConsoleLogFilter implements Serializable
{

  private static Logger LOGGER = Logger.getLogger(LogstashConsoleLogFilter.class.getName());

  @CheckForNull
  private SecureGroovyScript secureGroovyScript = null;
  private transient Run<?, ?> run;
  public LogstashConsoleLogFilter() {};

  public LogstashConsoleLogFilter(Run<?, ?> run)
  {
    this(run, null);
  }

  public LogstashConsoleLogFilter(Run<?, ?> run, SecureGroovyScript script)
  {
    this.run = run;
    this.secureGroovyScript = script;
  }
  private static final long serialVersionUID = 1L;

  @Override
  public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException
  {
    LogstashConfiguration configuration = LogstashConfiguration.getInstance();
    if (!configuration.isEnabled())
    {
      LOGGER.log(Level.FINE, "Logstash is disabled. Logs will not be forwarded.");
      return logger;
    }

    if (build != null && build instanceof AbstractBuild<?, ?>)
    {
      if (isLogstashEnabled(build))
      {
        LogstashWriter logstash = getLogStashWriter(build, logger, secureGroovyScript);
        return new LogstashOutputStream(logger, logstash);
      }
      else
      {
        return logger;
      }
    }
    if (run != null)
    {
      LogstashWriter logstash = getLogStashWriter(run, logger, secureGroovyScript);
      return new LogstashOutputStream(logger, logstash);
    }
    else
    {
      return logger;
    }
  }

  LogstashWriter getLogStashWriter(Run<?, ?> build, OutputStream errorStream, SecureGroovyScript script)
  {
    LogstashScriptProcessor processor = null;
    if (secureGroovyScript != null) {
      processor = new LogstashScriptProcessor(secureGroovyScript, new OutputStreamWriter(errorStream, build.getCharset()));
    }
    return new LogstashWriter(build, errorStream, null, build.getCharset(), processor);
  }

  private boolean isLogstashEnabled(Run<?, ?> build)
  {
    LogstashConfiguration configuration = LogstashConfiguration.getInstance();
    if (configuration.isEnableGlobally())
    {
      return true;
    }

    if (build.getParent() instanceof AbstractProject)
    {
      AbstractProject<?, ?> project = (AbstractProject<?, ?>)build.getParent();
      LogstashJobProperty jobProperty = project.getProperty(LogstashJobProperty.class);
      if (jobProperty != null)
      {
        this.secureGroovyScript = jobProperty.getSecureGroovyScript();
        return true;
      }
    }
    return false;
  }

}

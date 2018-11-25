package jenkins.plugins.logstash;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.Extension;
import hudson.model.InvisibleAction;

/*
 * A marker for Runs containing information about Logstash configuration of the job
 * When enabled per JobProperty it doesn't make sense to send the data another time in the notifier
 * And when the notifier decides to send at the end, we need to know the number of lines to send.
 * Note: When wrapped in a conditional build step or flexible publish, the standard detection will not catch it
 */
@Extension
@Restricted(NoExternalUse.class)
public class LogstashMarkerAction extends InvisibleAction
{
  private boolean lineModeEnabled;
  private boolean runNotifierAtEnd;
  private int maxLines;

  public boolean isRunNotifierAtEnd()
  {
    return runNotifierAtEnd;
  }

  public void setRunNotifierAtEnd(boolean runNotifierAtEnd)
  {
    this.runNotifierAtEnd = runNotifierAtEnd;
  }

  public int getMaxLines()
  {
    return maxLines;
  }

  public void setMaxLines(int maxLines)
  {
    this.maxLines = maxLines;
  }

  public boolean isLineModeEnabled()
  {
    return lineModeEnabled;
  }

  public void setLineModeEnabled(boolean lineModeEnabled)
  {
    this.lineModeEnabled = lineModeEnabled;
  }
}
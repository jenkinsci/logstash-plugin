package jenkins.plugins.logstash;

public enum GloballyEnabledMode
{

  // TODO: Introduce a DATAONLY mode, that allows to send a start event and an end event only without sending the log itself.
  OFF("Do not send logs globally."),
  LINEMODE("Send log line by line."),
  NOTIFIERMODE("Send log at the end of the build.");

  private String description;

  private GloballyEnabledMode(String description)
  {
    this.description = description;
  }

  public String getDescrition()
  {
    return description;
  }
}

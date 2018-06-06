package jenkins.plugins.logstash.configuration;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;

import jenkins.plugins.logstash.persistence.LogzioDao;
import jenkins.plugins.logstash.Messages;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

public class Logzio extends LogstashIndexer<LogzioDao>
{
    private Secret key;
    private String host;

    @DataBoundConstructor
    public Logzio(){}

    /*
     * We use URL for the setter as stapler can autoconvert a string to a URL but not to a URI
     */
    public String getHost(){ return this.host; }

    @DataBoundSetter
    public void setHost(String host){ this.host = host; }

    public String getKey()
    {
        return Secret.toString(key);
    }

    @DataBoundSetter
    public void setKey(String key)
    {
        this.key = Secret.fromString(key);
    }


    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        Logzio other = (Logzio) obj;
        if (!Secret.toString(key).equals(other.getKey()))
        {
            return false;
        }
        if (host == null)
        {
            return other.host == null;
        }
        else return host.equals(other.host);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + Secret.toString(key).hashCode();
        return result;
    }

    @Override
    public LogzioDao createIndexerInstance() { return new LogzioDao(host, Secret.toString(key)); }

    @Extension
    public static class LogzioDescriptor extends LogstashIndexerDescriptor
    {
        private static String EU_HOST = "https://listener-eu.logz.io:8071";
        private static String NONEU_HOST = "https://listener.logz.io:8071";

        @Nonnull
        @Override
        public String getDisplayName()
        {
            return "Logz.io";
        }

        @Override
        public int getDefaultPort()
        {
            return 0;
        }

        public FormValidation doCheckKey(@QueryParameter("value") String value)
        {
            if (StringUtils.isBlank(value))
            {
                return FormValidation.error(Messages.ValueIsRequired());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckHost(@QueryParameter("value") String value)
        {
            if (StringUtils.isBlank(value))
            {
                return FormValidation.error(Messages.ValueIsRequired());
            }
            else if (!(value.equals(EU_HOST) || value.equals(NONEU_HOST))){
                return FormValidation.error("Please verify your logz.io host is one of the two possible urls - "
                + EU_HOST + " or " + NONEU_HOST);
            }
            return FormValidation.ok();
        }
    }
}

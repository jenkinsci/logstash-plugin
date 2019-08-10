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
import java.util.Objects;

public class Logzio extends LogstashIndexer<LogzioDao>
{
    private Secret token;
    private String host;

    @DataBoundConstructor
    public Logzio(){}

    public String getHost(){ return this.host; }

    @DataBoundSetter
    public void setHost(String host){ this.host = host; }

    public String getToken()
    {
        return Secret.toString(token);
    }

    @DataBoundSetter
    public void setToken(String token)
    {
        this.token = Secret.fromString(token);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        Logzio other = (Logzio) obj;
        return Secret.toString(token).equals(other.getToken()) && Objects.equals(host, other.host);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + Secret.toString(token).hashCode();
        return result;
    }

    @Override
    public LogzioDao createIndexerInstance() { return new LogzioDao(host, Secret.toString(token)); }

    @Extension
    public static class LogzioDescriptor extends LogstashIndexerDescriptor
    {
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

        public FormValidation doCheckToken(@QueryParameter("value") String value)
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
            return FormValidation.ok();
        }
    }
}

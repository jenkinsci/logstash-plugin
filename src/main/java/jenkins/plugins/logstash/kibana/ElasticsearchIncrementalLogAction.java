/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jenkins.plugins.logstash.kibana;

import com.jcraft.jzlib.GZIPInputStream;
import com.trilead.ssh2.crypto.Base64;
import hudson.console.AnnotatedLargeText;
import hudson.console.ConsoleAnnotationOutputStream;
import hudson.console.ConsoleAnnotator;
import hudson.model.Action;
import hudson.model.Run;
import hudson.remoting.ObjectInputStreamEx;
import hudson.util.TimeUnit2;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.io.Writer;
import static java.lang.Math.abs;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import jenkins.model.Jenkins;
import jenkins.plugins.logstash.LogstashInstallation;
import jenkins.plugins.logstash.persistence.IndexerDaoFactory;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao;
import jenkins.plugins.logstash.util.UniqueIdHelper;
import jenkins.security.CryptoConfidentialKey;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jelly.XMLOutput;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.framework.io.ByteBuffer;
import org.kohsuke.stapler.framework.io.WriterOutputStream;

/**
 * Displays Embedded log.
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public class ElasticsearchIncrementalLogAction implements Action {
    
    private final String jobId;
    private final Run run;
    
    public ElasticsearchIncrementalLogAction(Run run) {
        this.run = run;
        this.jobId =  UniqueIdHelper.getOrCreateId(run.getParent());
    }
    
    @Override
    public String getDisplayName() {
        return "External log (Elasticsearch)";
    }

    @Override
    public String getIconFileName() {
        return "terminal.png";
    }

    @Override
    public String getUrlName() {
        return "externalLog";
    } 

    public Run getRun() {
        return run;
    }

    public String getJobId() {
        return jobId;
    }
    
    /**
     * Used from <tt>index.jelly</tt> to write annotated log to the given
     * output.
     */
    public void writeLogTo(long offset, @Nonnull XMLOutput out) throws IOException {
        try {
            StringWriter wr = new StringWriter();
            //getLogText().writeHtmlTo(offset, wr);
            getLogText().writeLogTo(offset, wr);
            String res = wr.toString();
            
            getLogText().writeHtmlTo(offset, out.asWriter());
        } catch (IOException e) {
            // try to fall back to the old getLogInputStream()
            // mainly to support .gz compressed files
            // In this case, console annotation handling will be turned off.
            InputStream input = readLogToBuffer(0).newInputStream();
            try {
                IOUtils.copy(input, out.asWriter());
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
    }
    
    /**
     * Used to URL-bind {@link AnnotatedLargeText}.
     * @return A {@link Run} log with annotations
     */   
    public @Nonnull AnnotatedLargeText getLogText() {
        ByteBuffer buf;
        try {
            buf = readLogToBuffer(0);
        } catch (IOException ex) {
            buf = new ByteBuffer();
        }
        return new UncompressedAnnotatedLargeText(buf, StandardCharsets.UTF_8, !isLogUpdated(), this);
    }
    
    public boolean isLogUpdated() {
        return run.isLogUpdated();
    }
    
    /**
     * Returns an input stream that reads from the log file.
     *
     * @throws IOException Operation error
     * @since 1.349
     */
    public @Nonnull ByteBuffer readLogToBuffer(int initialOffset) throws IOException {
        LogstashInstallation.Descriptor descriptor = LogstashInstallation.getLogstashDescriptor();
        IndexerDaoFactory.Info info = new IndexerDaoFactory.Info(descriptor.type, descriptor.host, descriptor.port, descriptor.key, descriptor.username, descriptor.password);
        final LogstashIndexerDao dao;
        try {
            dao = IndexerDaoFactory.getInstance(info);
        } catch(InstantiationException ex) {
            throw new IOException("Cannot retrieve Logstash destination Dao", ex);
        }
        
        ByteBuffer buffer = new ByteBuffer();
        Collection<String> pulledLogs = dao.pullLogs(run, 0, Long.MAX_VALUE);
        for (String logEntry : pulledLogs) {
            byte[] bytes = logEntry.getBytes();
            buffer.write(bytes, 0, bytes.length);
        }
        return buffer;
    }
    
    public static class UncompressedAnnotatedLargeText<T> extends AnnotatedLargeText<T> {

        private T context;
        private ByteBuffer memory;
        
        public UncompressedAnnotatedLargeText(ByteBuffer memory, Charset charset, boolean completed, T context) {
            super(memory, charset, completed, context);
            this.context = context;
            this.memory = memory;
        }
        
        public long writeHtmlTo(long start, Writer w) throws IOException {
           // ConsoleAnnotationOutputStream caw = new ConsoleAnnotationOutputStream(
           //         w, createAnnotator(Stapler.getCurrentRequest()), context, charset);
           // long r = super.writeLogTo(start,caw);    
           long initial = memory.length();
           memory.writeTo(new WriterOutputStream(w));
           return initial - memory.length();
        }
        
        /**
        * Used for sending the state of ConsoleAnnotator to the client, because we are deserializing this object later.
        */
        private static final CryptoConfidentialKey PASSING_ANNOTATOR = new CryptoConfidentialKey(AnnotatedLargeText.class,"consoleAnnotator");

        
        private ConsoleAnnotator createAnnotator(StaplerRequest req) throws IOException {
        try {
            String base64 = req!=null ? req.getHeader("X-ConsoleAnnotator") : null;
            if (base64!=null) {
                Cipher sym = PASSING_ANNOTATOR.decrypt();

                ObjectInputStream ois = new ObjectInputStreamEx(new GZIPInputStream(
                        new CipherInputStream(new ByteArrayInputStream(Base64.decode(base64.toCharArray())),sym)),
                        Jenkins.getInstance().pluginManager.uberClassLoader);
                try {
                    long timestamp = ois.readLong();
                    if (TimeUnit2.HOURS.toMillis(1) > abs(System.currentTimeMillis()-timestamp))
                        // don't deserialize something too old to prevent a replay attack
                        return (ConsoleAnnotator)ois.readObject();
                } finally {
                    ois.close();
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        // start from scratch
        return ConsoleAnnotator.initial(context==null ? null : context.getClass());
    }
    }
}

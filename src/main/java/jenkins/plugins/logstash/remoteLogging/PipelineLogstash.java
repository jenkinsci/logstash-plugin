/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.logstash.remoteLogging;

import com.google.common.primitives.Ints;
import hudson.Extension;
import hudson.console.LineTransformationOutputStream;
import hudson.model.BuildListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import jenkins.model.Jenkins;
import jenkins.plugins.logstash.LogstashInstallation;
import jenkins.plugins.logstash.persistence.IndexerDaoFactory;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.job.console.PipelineLogFile;
import org.jenkinsci.plugins.workflow.support.actions.LessAbstractTaskListener;

/**
 * Integrates remote logging with Pipeline builds using an experimental API.
 */
@Extension public class PipelineLogstash extends PipelineLogFile {

    @Override protected BuildListener listenerFor(WorkflowRun b) throws IOException, InterruptedException {
        return new PipelineListener(b);
    }

    @Override protected InputStream logFor(WorkflowRun b, long start) throws IOException {
        LogstashInstallation.Descriptor descriptor = LogstashInstallation.getLogstashDescriptor();
        IndexerDaoFactory.Info info = new IndexerDaoFactory.Info(descriptor.type, descriptor.host, descriptor.port, descriptor.key, descriptor.username, descriptor.password);
        final LogstashIndexerDao dao;
        try {
            dao = IndexerDaoFactory.getInstance(info);
        } catch (InstantiationException ex) {
            throw new IOException("Cannot retrieve Logstash destination Dao", ex);
        }
        // TODO very inefficient
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> pulledLogs = dao.pullLogs(b, 0, Long.MAX_VALUE);
        for (String logEntry : pulledLogs) {
            // cf. BuildData.DATE_FORMATTER
            byte[] bytes = logEntry.replaceFirst("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.]\\d{3}[+-]\\d{4} > ", "").getBytes(StandardCharsets.UTF_8);
            baos.write(bytes, 0, bytes.length);
            baos.write('\n');
        }
        int _start = Ints.checkedCast(start); // this implementation is not streaming anyway
        return new ByteArrayInputStream(baos.toByteArray(), _start, baos.size() - _start);
    }

    private static class PipelineListener extends LessAbstractTaskListener implements BuildListener {

        private static final long serialVersionUID = 1;

        private final RemoteLogstashWriter writer;
        private transient PrintStream logger;

        PipelineListener(WorkflowRun b) {
            writer = new RemoteLogstashWriter(b, Jenkins.getInstance());
        }

        @Override public PrintStream getLogger() {
            if (logger == null) {
                try {
                    logger = new PrintStream(new LineTransformationOutputStream() {
                        @Override protected void eol(byte[] b, int len) throws IOException {
                            int eol = len;
                            while (eol > 0) {
                                byte c = b[eol - 1];
                                if (c == '\n' || c == '\r') {
                                    eol--;
                                } else {
                                    break;
                                }
                            }
                            writer.write(new String(b, 0, eol, StandardCharsets.UTF_8));
                        }
                    }, true, "UTF-8");
                } catch (UnsupportedEncodingException x) {
                    throw new AssertionError(x);
                }
            }
            return logger;
        }

    }

}

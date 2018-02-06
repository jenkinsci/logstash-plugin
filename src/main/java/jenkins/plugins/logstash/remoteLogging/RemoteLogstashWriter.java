/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard and Liam Newman
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

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.logstash.persistence.BuildData;
import jenkins.plugins.logstash.persistence.LogstashIndexerDao;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import java.util.logging.Logger;

/**
 * A writer that wraps all Logstash DAOs. Handles error reporting and per build
 * connection state. Each call to write (one line or multiple lines) sends a
 * Logstash payload to the DAO. If any write fails, writer will not attempt to
 * send any further messages to logstash during this build.
 *
 * @author Rusty Gerard
 * @author Liam Newman
 * @since 1.0.5
 */
public class RemoteLogstashWriter implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RemoteLogstashWriter.class.getName());

    final BuildData buildData;
    final String jenkinsUrl;
    LogstashIndexerDao dao;
    private boolean connectionBroken;

    public RemoteLogstashWriter(Run run, Jenkins jenkins, TaskListener listener, LogstashIndexerDao dao) {
        this.jenkinsUrl = jenkins.getRootUrl();
        this.buildData = new BuildData(run, new Date(), listener);
        this.dao = dao;
    }

    /**
     * Sends a logstash payload for a single line to the indexer. Call will be
     * ignored if the line is empty or if the connection to the indexer is
     * broken. If write fails, errors will logged to errorStream and
     * connectionBroken will be set to true.
     *
     * @param line Message, not null
     */
    public void write(String line) {
        if (!isConnectionBroken() && StringUtils.isNotEmpty(line)) {
            this.write(Arrays.asList(line));
        }
    }

    /**
     * @return True if errors have occurred during initialization or write.
     */
    public boolean isConnectionBroken() {
        return connectionBroken || dao == null || buildData == null;
    }

    /**
     * Write a list of lines to the indexer as one Logstash payload.
     */
    private void write(List<String> lines) {
        JSONObject payload = dao.buildPayload(buildData, jenkinsUrl, lines);
        try {
            dao.push(payload.toString());
        } catch (IOException e) {
            String msg = "[logstash-plugin]: Failed to send log data to " + dao.getDescription() + ".\n"
                    + "[logstash-plugin]: No Further logs will be sent to " + dao.getDescription() + ".\n"
                    + ExceptionUtils.getStackTrace(e);
            logErrorMessage(msg);
        }
    }

    /**
     * Write error message to errorStream and set connectionBroken to true.
     */
    private void logErrorMessage(String msg) {
        connectionBroken = true;
        LOGGER.log(Level.WARNING, msg);
    }
}

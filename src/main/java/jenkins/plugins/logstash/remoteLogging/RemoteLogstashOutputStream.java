/*
 * The MIT License
 *
 * Copyright 2014 K Jonathan Harker & Rusty Gerard
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

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;

import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

/**
 * Output stream that writes each line to the provided delegate output stream
 * and also sends it to an indexer for logstash to consume.
 *
 * @author K Jonathan Harker
 * @author Rusty Gerard
 */
public class RemoteLogstashOutputStream extends LineTransformationOutputStream {

    final RemoteLogstashWriter logstash;
    final String prefix;

    private static final Logger LOGGER = Logger.getLogger(RemoteLogstashOutputStream.class.getName());

    public RemoteLogstashOutputStream(RemoteLogstashWriter logstash, String prefix) {
        super();
        this.logstash = logstash;
        this.prefix = prefix;
    }

    
    public MaskPasswordsOutputStream maskPasswords(List<String> passwordStrings) {
      return new MaskPasswordsOutputStream(this, passwordStrings);
    }
    
    @Override
    protected void eol(byte[] b, int len) throws IOException {
        try {
            this.flush();
            if (!logstash.isConnectionBroken()) {
                String line = new String(b, 0, len).trim();
                line = ConsoleNote.removeNotes(line);
                logstash.write(prefix + line);
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "BOOM", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        super.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        super.close();
    }
}

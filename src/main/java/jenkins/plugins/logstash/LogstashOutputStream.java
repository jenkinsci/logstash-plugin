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

package jenkins.plugins.logstash;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Output stream that writes each line to the provided delegate output stream
 * and also sends it to an indexer for logstash to consume.
 *
 * @author K Jonathan Harker
 * @author Rusty Gerard
 */
public class LogstashOutputStream extends LineTransformationOutputStream {
  private static final Logger LOGGER = Logger.getLogger(LogstashOutputStream.class.getName());

  private final OutputStream delegate;
  private final LogstashWriter logstash;
  private final AtomicBoolean isBuildConnectionBroken;
  private final String run;

  public LogstashOutputStream(OutputStream delegate, LogstashWriter logstash) {
    this(delegate, logstash, new AtomicBoolean(false), "");
  }

  public LogstashOutputStream(OutputStream delegate, LogstashWriter logstash, AtomicBoolean isBuildConnectionBroken, String run) {
    super();
    this.delegate = delegate;
    this.logstash = logstash;
    this.isBuildConnectionBroken = isBuildConnectionBroken;
    this.run = run;

  }

  public AtomicBoolean getIsBuildConnectionBroken() {
    return isBuildConnectionBroken;
  }

  // for testing purposes
  LogstashWriter getLogstashWriter()
  {
    return logstash;
  }

  @Override
  protected void eol(byte[] b, int len) throws IOException {
    delegate.write(b, 0, len);
    this.flush();

    if (!getIsBuildConnectionBroken().get()) {
      if (!logstash.isConnectionBroken()) {
        String line = new String(b, 0, len, logstash.getCharset());
        line = ConsoleNote.removeNotes(line).trim();
        logstash.write(line);
      }
      // Once it gets connection broken, set the build connection flag to true.
      if (logstash.isConnectionBroken()) {
        getIsBuildConnectionBroken().set(true);
        LOGGER.log(Level.WARNING, "Mark logstash connection broken for build: {0}.", run);
      }
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException {
    delegate.flush();
    super.flush();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    delegate.close();
    super.close();
  }
}

/*
 * The MIT License
 *
 * Copyright 2017 Red Hat inc. and individual contributors
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;

import javax.annotation.Nonnull;

import groovy.lang.Binding;

import net.sf.json.JSONObject;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

/**
 * This class is handling custom groovy script processing of JSON payload.
 * Each call to process executes the script provided in job configuration.
 * Script is executed under the same binding each time so that it has ability
 * to persist data during build execution if desired by script author.
 * When build is finished, script will receive null as the payload and can
 * return any cached but non-sent data back for persisting.
 * The return value of script is the payload to be persisted unless null.
 *
 * @author Aleksandar Kostadinov
 * @since 1.4.0
 */
public class LogstashScriptProcessor implements LogstashPayloadProcessor{
  @Nonnull
  private final SecureGroovyScript script;

  @Nonnull
  private final OutputStreamWriter consoleWriter;

  /** Groovy binding for script execution */
  @Nonnull
  private final Binding binding;

  /** Classloader for script execution */
  @Nonnull
  private final ClassLoader classLoader;

  public LogstashScriptProcessor(SecureGroovyScript script, OutputStreamWriter consoleWriter) {
    this.script = script;
    this.consoleWriter = consoleWriter;

    // TODO: should we put variables in the binding like manager, job, etc.?
    binding = new Binding();
    binding.setVariable("console", new BuildConsoleWrapper());

    // if users need access to types defined by any plugin we may decide to
    // switch to Jenkins.getInstance().getPluginManager().uberClassLoader
    classLoader = LogstashScriptProcessor.class.getClassLoader();
  }

  /**
   * Helper method to allow logging to build console.
   */
  private void buildLogPrintln(Object o) throws IOException {
    consoleWriter.write(o.toString() + "\n");
    consoleWriter.flush();
  }

  /*
   * good examples in:
   *  https://github.com/jenkinsci/envinject-plugin/blob/master/src/main/java/org/jenkinsci/plugins/envinject/service/EnvInjectEnvVars.java
   *  https://github.com/jenkinsci/groovy-postbuild-plugin/pull/11/files
   */
  @Override
  public JSONObject process(JSONObject payload) throws Exception {
    binding.setVariable("payload", payload);
    script.evaluate(classLoader, binding);
    return (JSONObject) binding.getVariable("payload");
  }

  @Override
  public JSONObject finish() throws Exception {
    buildLogPrintln("Tearing down Script Log Processor..");
    return process(new JSONObject());
  }

  /**
   * Helper to allow access from sandboxed script to output messages to console.
   */
  private class BuildConsoleWrapper {
    @Whitelisted
    public void println(Object o) throws IOException {
      buildLogPrintln(o);
    }
  }
}

package jenkins.plugins.logstash;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Mockito.verify;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.Run;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jenkins.plugins.logstash.persistence.BuildData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"})
@PrepareForTest(LogstashConfiguration.class)
public class LogstashConsoloLogFilterTest {

  @Mock
  private LogstashConfiguration logstashConfiguration;

  // Extension of the unit under test that avoids making calls to statics or constructors
  static class MockLogstashConsoloeLogFilter extends LogstashConsoleLogFilter {
    LogstashWriter writer;

    MockLogstashConsoloeLogFilter(LogstashWriter writer) {
      super();
      this.writer = writer;
    }

    @Override
    LogstashWriter getLogStashWriter(Run<?, ?> build, OutputStream errorStream) {
      // Simulate bad Writer
      if(writer.isConnectionBroken()) {
        try {
          errorStream.write("Mocked Constructor failure".getBytes());
        } catch (IOException e) {
        }
      }
      return writer;
    }
  }

  ByteArrayOutputStream buffer;

  @Mock AbstractBuild mockBuild;
  @Mock Project<?, ?> mockProject;
  @Mock BuildData mockBuildData;
  @Mock LogstashWriter mockWriter;

  @Before
  public void before() throws Exception {
    PowerMockito.mockStatic(LogstashConfiguration.class);
    when(LogstashConfiguration.getInstance()).thenReturn(logstashConfiguration);
    when(logstashConfiguration.isEnableGlobally()).thenReturn(false);

    when(mockWriter.isConnectionBroken()).thenReturn(false);
    when(mockBuild.getParent()).thenReturn(mockProject);
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(new LogstashJobProperty());

    buffer = new ByteArrayOutputStream();
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockWriter);
    verifyNoMoreInteractions(mockBuildData);
    buffer.close();
  }

  @Test
  public void decorateLoggerSuccess() throws Exception {
    MockLogstashConsoloeLogFilter consoleLogFilter = new MockLogstashConsoloeLogFilter(mockWriter);

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result instanceof LogstashOutputStream);
    assertSame("Result has wrong writer", mockWriter, ((LogstashOutputStream) result).getLogstashWriter());
    assertEquals("Results don't match", "", buffer.toString());
    verify(mockWriter).isConnectionBroken();
  }

  @Test
  public void decorateLoggerSuccessLogstashNotEnabled() throws Exception {
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(null);

    MockLogstashConsoloeLogFilter consoleLogFilter = new MockLogstashConsoloeLogFilter(mockWriter);

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result == buffer);
    assertEquals("Results don't match", "", buffer.toString());
  }

  @Test
  public void decorateLoggerSuccessBadWriter() throws Exception {
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    MockLogstashConsoloeLogFilter consoleLogFilter = new MockLogstashConsoloeLogFilter(mockWriter);

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result instanceof LogstashOutputStream);
    assertSame("Result has wrong writer", mockWriter, ((LogstashOutputStream) result).getLogstashWriter());
    assertEquals("Error was not written", "Mocked Constructor failure", buffer.toString());
    verify(mockWriter).isConnectionBroken();
  }

  @Test
  public void decorateLoggerSuccessEnabledGlobally() throws IOException, InterruptedException
  {
    when(logstashConfiguration.isEnableGlobally()).thenReturn(true);
    MockLogstashConsoloeLogFilter buildWrapper = new MockLogstashConsoloeLogFilter(mockWriter);

    // Unit under test
    OutputStream result = buildWrapper.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result instanceof LogstashOutputStream);
    assertSame("Result has wrong writer", mockWriter, ((LogstashOutputStream) result).getLogstashWriter());
    assertEquals("Results don't match", "", buffer.toString());
    verify(mockWriter).isConnectionBroken();
  }
}

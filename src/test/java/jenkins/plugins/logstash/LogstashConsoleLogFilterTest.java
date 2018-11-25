package jenkins.plugins.logstash;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Mockito.verify;

import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Run;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

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
public class LogstashConsoleLogFilterTest {

  @Mock
  private LogstashConfiguration logstashConfiguration;

  // Extension of the unit under test that avoids making calls to statics or constructors
  static class MockLogstashConsoleLogFilter extends LogstashConsoleLogFilter {
    LogstashWriter writer;

    MockLogstashConsoleLogFilter(LogstashWriter writer) {
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
  @Mock LogstashJobProperty mockProperty;
  private DescribableList<Publisher, Descriptor<Publisher>> publishers;
  private MockLogstashConsoleLogFilter consoleLogFilter;

  @Before
  public void before() throws Exception {
    publishers = new DescribableList<>(mockProject);
    buffer = new ByteArrayOutputStream();
    consoleLogFilter = new MockLogstashConsoleLogFilter(mockWriter);

    PowerMockito.mockStatic(LogstashConfiguration.class);
    when(LogstashConfiguration.getInstance()).thenReturn(logstashConfiguration);
    when(logstashConfiguration.getGlobalMode()).thenReturn(GloballyEnabledMode.OFF);
    when(logstashConfiguration.isEnabled()).thenReturn(true);

    when(mockWriter.isConnectionBroken()).thenReturn(false);
    when(mockBuild.getParent()).thenReturn(mockProject);
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(null);
    when(mockProject.getPublishersList()).thenReturn(publishers);

  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockWriter);
    verifyNoMoreInteractions(mockBuildData);
    buffer.close();
  }

  private void assertIsOriginalOutputStream(OutputStream result)
  {
    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result == buffer);
    assertEquals("Results don't match", "", buffer.toString());
  }

  private void assertIsLogstashOutputStream(OutputStream result)
  {
    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result instanceof LogstashOutputStream);
    assertSame("Result has wrong writer", mockWriter, ((LogstashOutputStream) result).getLogstashWriter());
    assertEquals("Results don't match", "", buffer.toString());
  }

  @Test
  public void successBadWriter() throws Exception {
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(mockProperty);
    when(mockWriter.isConnectionBroken()).thenReturn(true);

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
  public void successJobPropertyEnabled() throws Exception {
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(mockProperty);

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsLogstashOutputStream(result);
    verify(mockWriter).isConnectionBroken();
  }

  @Test
  public void successJobPropertySetDisable() throws Exception {
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(mockProperty);
    when(mockProperty.isDisableGlobal()).thenReturn(true);

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsOriginalOutputStream(result);
  }

  @Test
  public void successGlobalOffJobPropertyNull() throws Exception {
    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsOriginalOutputStream(result);
  }

  @Test
  public void successGlobalLineMode() throws IOException, InterruptedException
  {
    when(logstashConfiguration.getGlobalMode()).thenReturn(GloballyEnabledMode.LINEMODE);

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsLogstashOutputStream(result);
    verify(mockWriter).isConnectionBroken();
  }

  @Test
  public void successGlobalLineModeWithNotifier() throws Exception {
    when(logstashConfiguration.getGlobalMode()).thenReturn(GloballyEnabledMode.LINEMODE);
    publishers.add(new LogstashNotifier(10, false));

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsOriginalOutputStream(result);
  }

  @Test
  public void successGlobalLineModeWithJobPropertyDisabled() throws Exception {
    when(mockProperty.isDisableGlobal()).thenReturn(true);
    when(logstashConfiguration.getGlobalMode()).thenReturn(GloballyEnabledMode.LINEMODE);
    publishers.add(new LogstashNotifier(10, false));

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsOriginalOutputStream(result);
  }

  @Test
  public void successGlobalNotifierModeWithJobProperty() throws IOException, InterruptedException
  {
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(mockProperty);
    when(logstashConfiguration.getGlobalMode()).thenReturn(GloballyEnabledMode.NOTIFIERMODE);

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsLogstashOutputStream(result);
    verify(mockWriter).isConnectionBroken();
  }

  @Test
  public void successGlobalNotifierMode() throws Exception {
    when(mockProject.getProperty(LogstashJobProperty.class)).thenReturn(null);
    when(logstashConfiguration.getGlobalMode()).thenReturn(GloballyEnabledMode.NOTIFIERMODE);
    publishers.add(new LogstashNotifier(10, false));

    // Unit under test
    OutputStream result = consoleLogFilter.decorateLogger(mockBuild, buffer);

    assertIsOriginalOutputStream(result);
  }
}

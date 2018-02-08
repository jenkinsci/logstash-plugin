package jenkins.plugins.logstash;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jenkins.plugins.logstash.persistence.BuildData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LogstashConsoloLogFilterTest {
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
  @Mock Project mockProject;
  @Mock BuildData mockBuildData;
  @Mock LogstashWriter mockWriter;

  private volatile DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappers;


  @Before
  public void before() throws Exception {
    buildWrappers = new DescribableList<BuildWrapper,Descriptor<BuildWrapper>>(mockProject);
    when(mockWriter.isConnectionBroken()).thenReturn(false);
    when(mockBuild.getParent()).thenReturn(mockProject);
    when(mockProject.getBuildWrappersList()).thenReturn(buildWrappers);

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
    buildWrappers.add(new LogstashBuildWrapper());
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

  @Test
  public void decorateLoggerSuccessNoLogstashBuildWrapper() throws Exception {
    MockLogstashConsoloeLogFilter buildWrapper = new MockLogstashConsoloeLogFilter(mockWriter);

    // Unit under test
    OutputStream result = buildWrapper.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result == buffer);
    assertEquals("Results don't match", "", buffer.toString());
  }

  @Test
  public void decorateLoggerSuccessBadWriter() throws Exception {
    buildWrappers.add(new LogstashBuildWrapper());
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    MockLogstashConsoloeLogFilter buildWrapper = new MockLogstashConsoloeLogFilter(mockWriter);

    // Unit under test
    OutputStream result = buildWrapper.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result instanceof LogstashOutputStream);
    assertSame("Result has wrong writer", mockWriter, ((LogstashOutputStream) result).getLogstashWriter());
    assertEquals("Error was not written", "Mocked Constructor failure", buffer.toString());
    verify(mockWriter).isConnectionBroken();
  }
}

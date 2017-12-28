package jenkins.plugins.logstash;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SuppressWarnings("rawtypes")
@RunWith(MockitoJUnitRunner.class)
public class LogstashNotifierTest {
  // Extension of the unit under test that avoids making calls to Jenkins.getInstance() to get the DAO singleton
  static class MockLogstashNotifier extends LogstashNotifier {
    LogstashWriter writer;

    MockLogstashNotifier(int maxLines, boolean failBuild, LogstashWriter writer) {
      super(maxLines, failBuild);
      this.writer = writer;
    }

    @Override
    LogstashWriter getLogStashWriter(Run<?, ?> run, OutputStream errorStream, TaskListener listener) {
      // Simulate bad Writer
      if(writer.isConnectionBroken()) {
        try {
          errorStream.write("Mocked Constructor failure".getBytes());
        } catch (IOException e) {
        }
      }
      return this.writer;
    }
  }

  static class MockRun<P extends AbstractProject<P,R>, R extends AbstractBuild<P,R>> extends Run<P,R> {
    Result result;

    MockRun(P job) throws IOException {
      super(job);
    }

    @Override
    public void setResult(Result r) {
      result = r;
    }

    @Override
    public Result getResult() {
      return result;
    }
  }

  @Mock AbstractBuild<?, ?> mockBuild;
  @Mock LogstashWriter mockWriter;
  @Mock Launcher mockLauncher;
  @Mock BuildListener mockListener;
  @Mock AbstractProject mockProject;

  ByteArrayOutputStream errorBuffer;
  PrintStream errorStream;
  LogstashNotifier notifier;
  MockRun mockRun;


  @Before
  public void before() throws Exception {
    errorBuffer = new ByteArrayOutputStream();
    errorStream = new PrintStream(errorBuffer, true);

    when(mockProject.assignBuildNumber()).thenReturn(1);
    mockRun = new MockRun(mockProject);

    when(mockListener.getLogger()).thenReturn(errorStream);

    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(false);
    Mockito.doNothing().when(mockWriter).writeBuildLog(anyInt());

    notifier = new MockLogstashNotifier(3, false, mockWriter);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockBuild);
    verifyNoMoreInteractions(mockLauncher);
    verifyNoMoreInteractions(mockListener);
    verifyNoMoreInteractions(mockWriter);
    errorStream.close();
  }

  @Test
  public void performSuccess() throws Exception {
    // Unit under test
    boolean result = notifier.perform(mockBuild, mockLauncher, mockListener);

    // Verify results
    assertTrue("Build should not be marked as failure", result);

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(3);
    verify(mockWriter).isConnectionBroken();

    assertEquals("Errors were written", "", errorBuffer.toString());

  }

  @Test
  public void performStepSuccess() throws Exception {
    // Unit under test
    notifier.perform(mockRun, null, mockLauncher, mockListener);

    // Verify results
    assertEquals("Result not null", null, mockRun.getResult());

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(3);
    verify(mockWriter).isConnectionBroken();

    assertEquals("Errors were written", "", errorBuffer.toString());
  }

  @Test
  public void performBadWriterDoNotFailBuild() throws Exception {
    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    notifier = new MockLogstashNotifier(3, false, mockWriter);

    // Unit under test
    boolean result = notifier.perform(mockBuild, mockLauncher, mockListener);

    // Verify results
    assertTrue("Build should not be marked as failure", result);

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(3);
    verify(mockWriter).isConnectionBroken();

    assertEquals("Error was not written", "Mocked Constructor failure", errorBuffer.toString());
  }

  @Test
  public void performStepBadWriterDoNotFailBuild() throws Exception {
    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    notifier = new MockLogstashNotifier(3, false, mockWriter);

    // Unit under test
    notifier.perform(mockRun, null, mockLauncher, mockListener);

    // Verify results
    assertEquals("Result not null", null, mockRun.getResult());

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(3);
    verify(mockWriter).isConnectionBroken();

    assertEquals("Error was not written", "Mocked Constructor failure", errorBuffer.toString());
  }

  @Test
  public void performBadWriterDoFailBuild() throws Exception {
    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    notifier = new MockLogstashNotifier(3, true, mockWriter);

    // Unit under test
    boolean result = notifier.perform(mockBuild, mockLauncher, mockListener);


    // Verify results
    assertFalse("Build should be marked as failure", result);

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(3);
    verify(mockWriter, times(2)).isConnectionBroken();
    assertEquals("Error was not written", "Mocked Constructor failure", errorBuffer.toString());
  }

  @Test
  public void performStepBadWriterDoFailBuild() throws Exception {
    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    notifier = new MockLogstashNotifier(3, true, mockWriter);

    // Unit under test
    notifier.perform(mockRun, null, mockLauncher, mockListener);

    // Verify results
    assertEquals("Result is not FAILURE", Result.FAILURE, mockRun.getResult());

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(3);
    verify(mockWriter, times(2)).isConnectionBroken();
    assertEquals("Error was not written", "Mocked Constructor failure", errorBuffer.toString());
  }

  @Test
  public void performWriteFailDoFailBuild() throws Exception {
    final String errorMsg = "[logstash-plugin]: Unable to serialize log data.\n" +
      "java.io.IOException: Unable to read log file\n";

    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(false).thenReturn(false).thenReturn(true);
    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        if(!mockWriter.isConnectionBroken()) {
          errorBuffer.write(errorMsg.getBytes());
        }
        return null;
      }
    }).when(mockWriter).writeBuildLog(anyInt());

    notifier = new MockLogstashNotifier(3, true, mockWriter);
    assertEquals("Errors were written", "", errorBuffer.toString());

    // Unit under test
    boolean result = notifier.perform(mockBuild, mockLauncher, mockListener);

    // Verify results
    assertFalse("Build should be marked as failure", result);

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(3);
    verify(mockWriter, times(3)).isConnectionBroken();

    assertThat("Wrong error message", errorBuffer.toString(), containsString(errorMsg));
  }

  @Test
  public void performAllLines() throws Exception {
    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(false);

    Mockito.doNothing().when(mockWriter).writeBuildLog(anyInt());

    notifier = new MockLogstashNotifier(-1, true, mockWriter);

    // Unit under test
    boolean result = notifier.perform(mockBuild, mockLauncher, mockListener);

    // Verify results
    assertTrue("Build should not be marked as failure", result);

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(-1);
    verify(mockWriter, times(2)).isConnectionBroken();

    assertEquals("Errors were written", "", errorBuffer.toString());
  }

  @Test
  public void performZeroLines() throws Exception {
    // Initialize mocks
    when(mockWriter.isConnectionBroken()).thenReturn(false);

    Mockito.doNothing().when(mockWriter).writeBuildLog(anyInt());

    notifier = new MockLogstashNotifier(0, true, mockWriter);

    // Unit under test
    boolean result = notifier.perform(mockBuild, mockLauncher, mockListener);

    // Verify results
    assertTrue("Build should not be marked as failure", result);

    verify(mockListener).getLogger();
    verify(mockWriter).writeBuildLog(0);
    verify(mockWriter, times(2)).isConnectionBroken();

    assertEquals("Errors were written", "", errorBuffer.toString());
  }

  @Test
  public void getRequiredMonitorService() throws Exception {
    Notifier notifier = new LogstashNotifier(1, false);

    // Unit under test
    BuildStepMonitor synchronizationMonitor = notifier.getRequiredMonitorService();

    // Verify results
    assertEquals("External synchronization between builds is not required", BuildStepMonitor.NONE, synchronizationMonitor);
  }
}

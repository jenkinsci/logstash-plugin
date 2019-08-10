package jenkins.plugins.logstash.persistence;

import io.logz.sender.FormattedLogMessage;
import io.logz.sender.exceptions.LogzioServerErrorException;
import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LogzioDaoTest {

    private static final String data = "{\"a\":{\"b\":1,\"c\":2,\"d\":[false, true]},\"e\":\"f\",\"g\":2.3}";
    private static final String flat_data = "\"a_b\":1,\"a_c\":2,\"a_d[0]\":false,\"a_d[1]\":true,\"e\":\"f\",\"g\":2.3";
    private static final String EMPTY_STRING_WITH_DATA = "{\"@buildTimestamp\":\"2000-01-01\"," + flat_data + ",\"message\":[],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
    private static final String ONE_LINE_STRING_WITH_DATA = "{\"@buildTimestamp\":\"2000-01-01\"," + flat_data + ",\"message\":[\"LINE 1\"],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
    private static final String TWO_LINE_STRING_WITH_DATA = "{\"@buildTimestamp\":\"2000-01-01\"," + flat_data + ",\"message\":[\"LINE 1\", \"LINE 2\"],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
    private static final String EMPTY_STRING_NO_DATA = "{\"@buildTimestamp\":\"2000-01-01\",\"message\":[],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
    private static final String ONE_LINE_STRING_NO_DATA = "{\"@buildTimestamp\":\"2000-01-01\",\"message\":[\"LINE 1\"],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
    private static final String TWO_LINE_STRING_NO_DATA = "{\"@buildTimestamp\":\"2000-01-01\",\"message\":[\"LINE 1\", \"LINE 2\"],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
    private LogzioDao dao;

    @Captor private ArgumentCaptor<FormattedLogMessage> sendArgument = ArgumentCaptor.forClass(FormattedLogMessage.class);

    @Mock private LogzioHttpsClient logzioSender;
    @Mock private BuildData mockBuildData;

    private LogzioDao createDao(String host, String key) throws IllegalArgumentException {
        return new LogzioDao(logzioSender, host, key);
    }

    @Before
    public void before() throws IllegalArgumentException, LogzioServerErrorException {
        when(mockBuildData.getTimestamp()).thenReturn("2000-01-01");

        doNothing().when(logzioSender).send(any(FormattedLogMessage.class));
        doNothing().when(logzioSender).flush();

        dao = createDao("http://localhost:8200/", "123456789");

    }

    @Test
    public void constructorSuccess() throws IllegalArgumentException {
        // Unit under test
        dao = createDao("https://localhost:8201/", "123");

        // Verify results
        assertEquals("Wrong host name", "https://localhost:8201/", dao.getHost());
        assertEquals("Wrong key", "123", dao.getKey());
    }

    @Test
    public void buildPayloadSuccessEmpty(){
        when(mockBuildData.toString()).thenReturn("{}");
        // Unit under test
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", new ArrayList<String>());
        result.remove("@timestamp");

        // Verify results
        JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(EMPTY_STRING_NO_DATA), result);
    }

    @Test
    public void buildPayloadSuccessOneLine(){
        when(mockBuildData.toString()).thenReturn("{}");
        // Unit under test
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", Collections.singletonList("LINE 1"));
        result.remove("@timestamp");

        // Verify results
        JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(ONE_LINE_STRING_NO_DATA), result);
    }

    @Test
    public void buildPayloadSuccessTwoLines(){
        when(mockBuildData.toString()).thenReturn("{}");
        // Unit under test
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", Arrays.asList("LINE 1", "LINE 2"));
        result.remove("@timestamp");

        // Verify results
        JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(TWO_LINE_STRING_NO_DATA), result);
    }

    @Test
    public void buildPayloadWithDataSuccessEmpty(){
        when(mockBuildData.toString()).thenReturn(data);
        // Unit under test
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", new ArrayList<String>());
        result.remove("@timestamp");

        // Verify results
        JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(EMPTY_STRING_WITH_DATA), result);
    }

    @Test
    public void buildPayloadWithDataSuccessOneLine(){
        when(mockBuildData.toString()).thenReturn(data);
        // Unit under test
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", Collections.singletonList("LINE 1"));
        result.remove("@timestamp");

        // Verify results
        JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(ONE_LINE_STRING_WITH_DATA), result);
    }

    @Test
    public void buildPayloadWithDataSuccessTwoLines(){
        when(mockBuildData.toString()).thenReturn(data);
        // Unit under test
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", Arrays.asList("LINE 1", "LINE 2"));
        result.remove("@timestamp");

        // Verify results
        JSONAssert.assertEquals("Results don't match", JSONObject.fromObject(TWO_LINE_STRING_WITH_DATA), result);
    }

    @Test
    public void pushNoMessage() throws IOException, LogzioServerErrorException {
        // Unit under test
        dao.push(EMPTY_STRING_WITH_DATA);
        verify(logzioSender, never()).send(sendArgument.capture());
        verify(logzioSender, never()).flush();
    }

    @Test
    public void pushOneMessage() throws IOException, LogzioServerErrorException {
        // Unit under test
        dao.push(ONE_LINE_STRING_WITH_DATA);
        // Verify results
        verify(logzioSender, times(1)).send(sendArgument.capture());
        verify(logzioSender, times(1)).flush();
    }

    @Test
    public void pushMultiMessages() throws IOException, LogzioServerErrorException {
        // Unit under test
        dao.push(TWO_LINE_STRING_WITH_DATA);
        // Verify results
        verify(logzioSender, times(2)).send(sendArgument.capture());
        verify(logzioSender, times(1)).flush();
    }
}

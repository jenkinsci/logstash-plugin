package jenkins.plugins.logstash.persistence;

import io.logz.sender.LogzioSender;
import io.logz.sender.com.google.gson.JsonObject;

import jenkins.plugins.logstash.LogstashConfiguration;

import java.util.*;

import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"})
@PrepareForTest(LogstashConfiguration.class)
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

    @Captor private ArgumentCaptor<JsonObject> sendArgument = ArgumentCaptor.forClass(JsonObject.class);

    @Mock private LogzioSender logzioSender;
    @Mock private BuildData mockBuildData;
    @Mock private LogstashConfiguration logstashConfiguration;

    private LogzioDao createDao(String host, String key) throws IllegalArgumentException {
        return new LogzioDao(logzioSender, host, key);
    }

    @Before
    public void before() throws IllegalArgumentException {
        PowerMockito.mockStatic(LogstashConfiguration.class);
        when(LogstashConfiguration.getInstance()).thenReturn(logstashConfiguration);
        when(logstashConfiguration.getDateFormatter()).thenCallRealMethod();
        when(mockBuildData.getTimestamp()).thenReturn("2000-01-01");

        doNothing().when(logzioSender).start();
        doNothing().when(logzioSender).send(any(JsonObject.class));
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
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", new ArrayList<>());
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
        JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", new ArrayList<>());
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
    public void pushNoMessage(){
        // Unit under test
        dao.push(EMPTY_STRING_WITH_DATA);
        verify(logzioSender, never()).send(sendArgument.capture());
    }

    @Test
    public void pushOneMessage(){
        // Unit under test
        dao.push(ONE_LINE_STRING_WITH_DATA);
        // Verify results
        verify(logzioSender, times(1)).send(sendArgument.capture());

        JsonObject sentJson = sendArgument.getValue();
        JsonObject expectedJson = dao.
                createLogLine(JSONObject.fromObject(ONE_LINE_STRING_WITH_DATA), sentJson.get("message").getAsString());

        sentJson.remove("@timestamp");
        expectedJson.remove("@timestamp");

        assertTrue(expectedJson.equals(sentJson));
    }

    @Test
    public void pushMultiMessages(){
        // Unit under test
        dao.push(TWO_LINE_STRING_WITH_DATA);
        // Verify results
        verify(logzioSender, times(2)).send(sendArgument.capture());

        JsonObject sentJson1 = sendArgument.getAllValues().get(0);
        JsonObject expectedJson1 = dao.
                createLogLine(JSONObject.fromObject(TWO_LINE_STRING_WITH_DATA),sentJson1.get("message").getAsString());
        JsonObject sentJson2 = sendArgument.getAllValues().get(1);
        JsonObject expectedJson2 = dao.
                createLogLine(JSONObject.fromObject(TWO_LINE_STRING_WITH_DATA),sentJson2.get("message").getAsString());

        sentJson1.remove("@timestamp");
        sentJson2.remove("@timestamp");
        expectedJson1.remove("@timestamp");
        expectedJson2.remove("@timestamp");

        assertTrue(expectedJson1.equals(sentJson1));
        assertTrue(expectedJson2.equals(sentJson2));
    }
}

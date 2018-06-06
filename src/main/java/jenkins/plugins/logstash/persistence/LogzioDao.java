package jenkins.plugins.logstash.persistence;

import com.github.wnameless.json.flattener.JsonFlattener;
import com.google.common.io.Files;
import hudson.model.Executor;

import io.logz.sender.LogzioSender;
import io.logz.sender.SenderStatusReporter;
import io.logz.sender.com.google.gson.JsonObject;
import io.logz.sender.exceptions.LogzioParameterErrorException;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;

import jenkins.plugins.logstash.LogstashConfiguration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Logz.io Data Access Object.
 *
 * @author Ido Halevi
 */

public class LogzioDao extends AbstractLogstashIndexerDao {
    private static final int CONNECT_TIMEOUT = 10*1000;
    private static final int CORE_POOL_SIZE = 2;
    private static final int DRAIN_TIMEOUT = 2;
    private static final int FS_PERCENT_THRESHOLD = 98;
    private static final int GC_PERSISTED_QUEUE_FILE_INTERVAL_SECOND = 30;
    private static final int SOCKET_TIMEOUT = 10*1000;
    private static final String TYPE = "jenkins_plugin";
    private final LogzioSender logzioSender;

    private String key;
    private String host;

    //primary constructor used by indexer factory
    public LogzioDao(String host, String key){
        this(null, host, key);
    }

    // Factored for unit testing
    LogzioDao(LogzioSender factory, String host, String key){
        this.host = host;
        this.key = key;

        // create file for sender queue
        File fp;
        // for tests
        if (Executor.currentExecutor() != null){
            hudson.FilePath workspace = Executor.currentExecutor().getCurrentWorkspace();
            fp = new File(workspace + "/logzio_jenkins");
        }else{
            fp = Files.createTempDir();
        }
        try{
            this.logzioSender = factory == null ? LogzioSender.getOrCreateSenderByType(key, TYPE, DRAIN_TIMEOUT,
                    FS_PERCENT_THRESHOLD, fp, host, SOCKET_TIMEOUT,
                    CONNECT_TIMEOUT,false, new LogzioDaoLogger(),
                    Executors.newScheduledThreadPool(CORE_POOL_SIZE),GC_PERSISTED_QUEUE_FILE_INTERVAL_SECOND) : factory;
            this.logzioSender.start();
        }catch (LogzioParameterErrorException e){
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public void push(String data) throws IOException{
        JSONObject jsonData = JSONObject.fromObject(data);
        JSONArray logMessages = jsonData.getJSONArray("message");
        for (Object logMsg : logMessages) {
            JsonObject logLine = createLogLine(jsonData, logMsg.toString());
            this.logzioSender.send(logLine);
        }
    }

    protected JsonObject createLogLine(JSONObject jsonData, String logMsg) {
        JsonObject logLine = new JsonObject();
        logLine.addProperty("message", logMsg);
        logLine.addProperty("@timestamp", LogstashConfiguration.getInstance().getDateFormatter().format(Calendar.getInstance().getTime()));
        jsonData.keySet().stream()
        .filter(key -> !(key.equals("message")))
        .forEach(key -> logLine.addProperty(key.toString(), jsonData.getString(key.toString())));
        return logLine;
    }

    @Override
    public JSONObject buildPayload(BuildData buildData, String jenkinsUrl, List<String> logLines) {
        JSONObject payload = new JSONObject();
        payload.put("message", logLines);
        payload.put("source", "jenkins");
        payload.put("source_host", jenkinsUrl);
        payload.put("@buildTimestamp", buildData.getTimestamp());
        payload.put("@version", 1);
        // flatten build data
        Map<String, Object> flattenJson = JsonFlattener.flattenAsMap(buildData.toString());
        for (Map.Entry<String, Object> entry : flattenJson.entrySet()) {
            String key = entry.getKey().replace('.','_');
            Object value = entry.getValue();
            payload.put(key, value);
        }

        return payload;
    }

    @Override
    public String getDescription(){ return host; }

    public String getHost(){ return host; }

    public String getKey(){ return key; }

    public String getType(){ return TYPE; }

    public class LogzioDaoLogger implements SenderStatusReporter {

        private final OutputStream logStream = System.out;

        private void pringLogMessage(String msg) {
            try {
                msg = msg + "\n";
                logStream.write(msg.getBytes());
                logStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void error(String msg) { pringLogMessage("[LogzioSender]ERROR: " + msg); }

        @Override
        public void error(String msg, Throwable e) { pringLogMessage("[LogzioSender]ERROR: " + msg + "\n" +e); }

        @Override
        public void warning(String msg) {pringLogMessage("[LogzioSender]WARNING: " + msg);}

        @Override
        public void warning(String msg, Throwable e) {pringLogMessage("[LogzioSender]WARNING: " + msg + "\n" + e);}

        @Override
        public void info(String msg) {pringLogMessage("[LogzioSender]INFO: " + msg);}

        @Override
        public void info(String msg, Throwable e) {pringLogMessage("[LogzioSender]INFO: " + msg + "\n" + e);}
    }
}


package jenkins.plugins.logstash.persistence;

import io.logz.sender.FormattedLogMessage;
import io.logz.sender.HttpsRequestConfiguration;
import io.logz.sender.HttpsSyncSender;
import io.logz.sender.SenderStatusReporter;
import io.logz.sender.exceptions.LogzioParameterErrorException;
import io.logz.sender.exceptions.LogzioServerErrorException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogzioHttpsClient{
    private static final int MAX_SIZE_IN_BYTES = 8 * 1024 * 1024;  // 8 MB
    private static final int CONNECT_TIMEOUT = 10 * 1000;
    private static final int SOCKET_TIMEOUT = 10 * 1000;
    private final HttpsSyncSender logzioClient;
    private List<FormattedLogMessage> messages;
    private int size;

    LogzioHttpsClient(String token, String listener, String type) throws LogzioParameterErrorException {
        HttpsRequestConfiguration gzipHttpsRequestConfiguration = HttpsRequestConfiguration
                .builder()
                .setLogzioToken(token)
                .setLogzioType(type)
                .setLogzioListenerUrl(listener)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setCompressRequests(true)
                .build();

        logzioClient = new HttpsSyncSender(gzipHttpsRequestConfiguration, new Reporter());
        messages = Collections.synchronizedList(new ArrayList<FormattedLogMessage>());
        size = 0;
    }

    public void send(FormattedLogMessage log) throws LogzioServerErrorException {
        if (size + log.getSize() > MAX_SIZE_IN_BYTES) {
            sendAndReset();
        }
        messages.add(log);
        size += log.getSize();
    }

    private void reset(){
        size = 0;
        messages.clear();
    }

    void flush() throws LogzioServerErrorException {
        if(messages.size() > 0) {
            sendAndReset();
        }
    }

    private void sendAndReset() throws LogzioServerErrorException {
        logzioClient.sendToLogzio(messages);
        reset();
    }

    private static class Reporter implements SenderStatusReporter{
        private static final Logger LOGGER = Logger.getLogger(LogzioDao.class.getName());

        private void pringLogMessage(Level level, String msg) {
            LOGGER.log(level, msg);
        }

        @Override
        public void error(String msg) {
            pringLogMessage(Level.SEVERE, "[LogzioSender]ERROR: " + msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            pringLogMessage(Level.SEVERE, "[LogzioSender]ERROR: " + msg + "\n" +e);
        }

        @Override
        public void warning(String msg) {
            pringLogMessage(Level.WARNING, "[LogzioSender]WARNING: " + msg);
        }

        @Override
        public void warning(String msg, Throwable e) {
            pringLogMessage(Level.WARNING, "[LogzioSender]WARNING: " + msg + "\n" + e);
        }

        @Override
        public void info(String msg) {
            pringLogMessage(Level.INFO, "[LogzioSender]INFO: " + msg);
        }

        @Override
        public void info(String msg, Throwable e) {
            pringLogMessage(Level.INFO, "[LogzioSender]INFO: " + msg + "\n" + e);
        }
    }
}

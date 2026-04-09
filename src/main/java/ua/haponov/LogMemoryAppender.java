package ua.haponov;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LogMemoryAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_LOGS = 50;
    private static final List<AppLogEntry> logs = new CopyOnWriteArrayList<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (logs.size() >= MAX_LOGS) {
            logs.remove(0);
        }
        logs.add(new AppLogEntry(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(eventObject.getTimeStamp()), ZoneId.systemDefault()),
                eventObject.getLevel().toString(),
                eventObject.getFormattedMessage()
        ));
    }

    public List<AppLogEntry> getRecentLogs() {
        List<AppLogEntry> result = new ArrayList<>(logs);
        Collections.reverse(result);
        return result;
    }

    public record AppLogEntry(LocalDateTime timestamp, String level, String message) {}
}

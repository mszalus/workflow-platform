package com.wfp.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Logstash-Logback custom provider that adds GCP Cloud Logging-compatible fields
 * to every structured log entry:
 * <ul>
 *   <li>{@code severity} — logback level mapped to GCP severity strings
 *       (WARN becomes WARNING so GCP does not demote it to DEFAULT)</li>
 *   <li>{@code logging.googleapis.com/trace} — populated when the
 *       {@code GOOGLE_CLOUD_PROJECT} environment variable is present; links the
 *       log entry to Cloud Trace in the GCP console</li>
 *   <li>{@code logging.googleapis.com/spanId} and
 *       {@code logging.googleapis.com/traceSampled} — required for trace
 *       correlation in Cloud Logging</li>
 * </ul>
 *
 * When running locally (no {@code GOOGLE_CLOUD_PROJECT}), only {@code severity}
 * is written; the trace correlation fields are omitted to keep local JSON concise.
 * {@code traceId} and {@code spanId} are still present via the standard MDC
 * provider and can be used to correlate with a local Tempo instance.
 */
public class GcpLoggingJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    private static final String GOOGLE_CLOUD_PROJECT = System.getenv("GOOGLE_CLOUD_PROJECT");

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeStringField("severity", toGcpSeverity(event.getLevel()));

        if (GOOGLE_CLOUD_PROJECT != null) {
            String traceId = MDC.get("traceId");
            String spanId  = MDC.get("spanId");
            if (traceId != null && !traceId.isBlank()) {
                generator.writeStringField(
                        "logging.googleapis.com/trace",
                        "projects/" + GOOGLE_CLOUD_PROJECT + "/traces/" + traceId);
            }
            if (spanId != null && !spanId.isBlank()) {
                generator.writeStringField("logging.googleapis.com/spanId", spanId);
                generator.writeBooleanField("logging.googleapis.com/traceSampled", true);
            }
        }
    }

    private static String toGcpSeverity(Level level) {
        if (level == Level.ERROR) {
            return "ERROR";
        }
        if (level == Level.WARN) {
            return "WARNING";
        }
        if (level == Level.INFO) {
            return "INFO";
        }
        return "DEBUG"; // TRACE and DEBUG both map to GCP DEBUG
    }
}

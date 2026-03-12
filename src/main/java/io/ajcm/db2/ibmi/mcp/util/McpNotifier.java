package io.ajcm.db2.ibmi.mcp.util;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for sending MCP notifications and logs.
 */
public final class McpNotifier {

    private static final Logger log = LoggerFactory.getLogger(McpNotifier.class);

    private McpNotifier() {
    }

    /**
     * Resolves the progress token from a tool request.
     * 
     * @param req the tool call request
     * @return progress token or null
     */
    public static String resolveProgressToken(McpSchema.CallToolRequest req) {
        if (req.meta() != null && req.meta().containsKey("progressToken")) {
            return req.meta().get("progressToken").toString();
        }
        return null;
    }

    /**
     * Sends a logging notification to the MCP client.
     * 
     * @param exchange the MCP exchange context
     * @param loggingLevel the log severity level
     * @param logger the logger name
     * @param message the log message
     */
    public static void sendLog(McpSyncServerExchange exchange, McpSchema.LoggingLevel loggingLevel, String logger,
            String message) {
        try {
            // Silently discarded if client's minimum log level is higher than 'level'
            exchange.loggingNotification(
                    new McpSchema.LoggingMessageNotification(loggingLevel, logger, message));
        } catch (Exception ex) {
            log.debug("Could not send MCP log notification: {}", ex.getMessage());
        }
    }

    /**
     * Sends a progress notification to the MCP client.
     * 
     * @param exchange the MCP exchange context
     * @param progress the current progress value
     * @param total the total progress expected
     * @param message the progress message
     * @param progressToken the progress token
     * @param loggingLevel the log severity level
     * @param logger the logger name
     */
    public static void sendNotification(McpSyncServerExchange exchange, double progress, double total,
            String message, String progressToken, McpSchema.LoggingLevel loggingLevel, String logger) {
        try {
            if (progressToken != null) {
                exchange.progressNotification(
                        new McpSchema.ProgressNotification(progressToken, progress, total, message));
            }
            sendLog(exchange, loggingLevel, logger,
                    String.format("[%.0f%%] %s", (progress / total) * 100, message));
        } catch (Exception ex) {
            log.debug("Could not send MCP progress notification: {}", ex.getMessage());
        }
    }

    /**
     * Starts a background daemon thread that periodically sends progress
     * notifications to the MCP client.
     * 
     * @param exchange      The MCP exchange context to send notifications through
     * @param progressToken The token identifying the client's original request
     * @param loggerName    The name of the logger to attach to the log messages
     * @param startProgress The initial progress percentage to start from
     * @param maxProgress   The maximum asymptotic progress boundary
     * @param intervalMs    The delay between each background notification ping
     * @return AutoCloseable that halts the periodic notifications upon being closed
     */
    public static AutoCloseable startProgressPinger(McpSyncServerExchange exchange, String progressToken,
            String loggerName, int startProgress, int maxProgress, long intervalMs) {
        if (progressToken == null) {
            return () -> {
            }; // No-op
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mcp-progress-pinger");
            t.setDaemon(true);
            return t;
        });

        AtomicInteger currentProgress = new AtomicInteger(startProgress);
        AtomicInteger secondsElapsed = new AtomicInteger(0);

        scheduler.scheduleAtFixedRate(() -> {
            int current = currentProgress.get();
            if (current < maxProgress) {
                // Increment slowly towards maxProgress
                currentProgress.set(current + Math.max(1, (maxProgress - current) / 4));
            }
            int elapsed = secondsElapsed.addAndGet((int) (intervalMs / 1000));
            sendNotification(exchange, currentProgress.get(), 100,
                    "Still processing query... (" + elapsed + "s elapsed)",
                    progressToken, McpSchema.LoggingLevel.INFO, loggerName);
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        return () -> {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
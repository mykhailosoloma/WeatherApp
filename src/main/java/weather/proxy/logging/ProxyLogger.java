package weather.proxy.logging;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyLogger {
    private static final Logger LOGGER = Logger.getLogger(ProxyLogger.class.getName());

    private final Level requestLevel;
    private final Level responseLevel;
    private final Level errorLevel;
    private final boolean logHeaders;

    public ProxyLogger(Level requestLevel, Level responseLevel, Level errorLevel, boolean logHeaders) {
        this.requestLevel = requestLevel;
        this.responseLevel = responseLevel;
        this.errorLevel = errorLevel;
        this.logHeaders = logHeaders;
    }

    public void logRequest(HttpRequest request) {
        if (!LOGGER.isLoggable(requestLevel)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Request: ").append(request.method()).append(" ").append(request.uri());

        if (logHeaders && !request.headers().map().isEmpty()) {
            sb.append("\nHeaders:");
            request.headers().map().forEach((key, values) -> {
                // Don't log sensitive headers like Authorization
                if (!key.equalsIgnoreCase("Authorization")) {
                    sb.append("\n  ").append(key).append(": ").append(String.join(", ", values));
                }
            });
        }

        LOGGER.log(requestLevel, sb.toString());
    }

    public void logResponse(HttpResponse<?> response, long startTime) {
        if (!LOGGER.isLoggable(responseLevel)) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;

        StringBuilder sb = new StringBuilder();
        sb.append("Response: ").append(response.statusCode())
          .append(" (").append(duration).append("ms)");

        if (logHeaders && !response.headers().map().isEmpty()) {
            sb.append("\nHeaders:");
            response.headers().map().forEach((key, values) -> {
                sb.append("\n  ").append(key).append(": ").append(String.join(", ", values));
            });
        }

        LOGGER.log(responseLevel, sb.toString());
    }

    public void logError(String message, Throwable throwable) {
        LOGGER.log(errorLevel, message, throwable);
    }

}

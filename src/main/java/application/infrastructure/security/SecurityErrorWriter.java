package application.infrastructure.security;

import application.domain.ports.out.ClockPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final int MAX_REQUEST_ID_LENGTH = 100;

    private final ClockPort clockPort;

    public SecurityErrorWriter(ClockPort clockPort) {
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort cannot be null");
    }

    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      int status,
                      String code,
                      String message) throws IOException {
        String requestId = resolveRequestId(request);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String body = "{"
                + "\"timestamp\":\"" + escape(clockPort.now().toString()) + "\","
                + "\"status\":" + status + ","
                + "\"code\":\"" + escape(code) + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"path\":\"" + escape(request.getRequestURI()) + "\","
                + "\"requestId\":\"" + escape(requestId) + "\""
                + "}";
        response.getWriter().write(body);
        response.getWriter().flush();
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String value && !value.isBlank()
                && value.length() <= MAX_REQUEST_ID_LENGTH) {
            return value;
        }
        return UUID.randomUUID().toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}

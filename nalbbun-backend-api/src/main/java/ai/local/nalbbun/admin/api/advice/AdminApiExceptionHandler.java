package ai.local.nalbbun.admin.api.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "ai.local.nalbbun.admin.api")
public class AdminApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body("BAD_REQUEST", e.getMessage(), request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(IllegalStateException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body("SERVICE_UNAVAILABLE", e.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body("INTERNAL_SERVER_ERROR", e.getMessage(), request));
    }

    private Map<String, Object> body(String code, String message, HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("errorCode", code);
        result.put("message", message);
        result.put("path", request != null ? request.getRequestURI() : null);
        result.put("timestamp", OffsetDateTime.now().toString());
        return result;
    }
}

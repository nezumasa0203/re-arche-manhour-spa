package com.example.czConsv.config;

import com.example.czConsv.exception.CzBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for CZ system REST API.
 *
 * <p>Maps CZ error codes to HTTP status codes and returns
 * a consistent JSON error response format:
 * <pre>
 * {
 *   "error": {
 *     "code": "CZ-126",
 *     "message": "{0}は必須入力です",
 *     "field": "workDate",
 *     "params": ["作業日"],
 *     "recordId": 12345
 *   }
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle CzBusinessException.
     * Maps error code range to HTTP status.
     */
    @ExceptionHandler(CzBusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(CzBusinessException ex) {
        int httpStatus = resolveHttpStatus(ex.getCode());

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", ex.getCode());
        errorBody.put("message", ex.getMessage());
        if (ex.getField() != null) {
            errorBody.put("field", ex.getField());
        }
        if (ex.getParams() != null) {
            errorBody.put("params", ex.getParams());
        }
        if (ex.getRecordId() != null) {
            errorBody.put("recordId", ex.getRecordId());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", errorBody);

        log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());

        return ResponseEntity.status(httpStatus).body(body);
    }

    /**
     * Handle Jakarta Validation errors (MethodArgumentNotValidException).
     * Maps to CZ-126 format.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String field = null;
        String message = "入力値が不正です";

        if (ex.getBindingResult().getFieldError() != null) {
            field = ex.getBindingResult().getFieldError().getField();
            String defaultMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
            if (defaultMessage != null) {
                message = defaultMessage;
            }
        }

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", "CZ-126");
        errorBody.put("message", message);
        if (field != null) {
            errorBody.put("field", field);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", errorBody);

        log.warn("Validation error: field={}, message={}", field, message);

        return ResponseEntity.status(400).body(body);
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", "CZ-126");
        errorBody.put("message", ex.getParameterName() + "は必須入力です");
        errorBody.put("field", ex.getParameterName());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", errorBody);

        log.warn("Missing parameter: {}", ex.getParameterName());

        return ResponseEntity.status(400).body(body);
    }

    /**
     * Handle all other exceptions.
     * Maps to CZ-300 (system error) with 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", "CZ-300");
        errorBody.put("message", "システムエラーが発生しました");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", errorBody);

        log.error("Unexpected error", ex);

        return ResponseEntity.status(500).body(body);
    }

    /**
     * Resolve HTTP status code from CZ error code.
     *
     * <ul>
     *   <li>CZ-101 -> 409 Conflict (optimistic lock)</li>
     *   <li>CZ-102 -> 403 Forbidden (time restriction)</li>
     *   <li>CZ-106~CZ-110 -> 403 Forbidden (permission denied)</li>
     *   <li>CZ-100~CZ-299 -> 400 Bad Request (validation/warning)</li>
     *   <li>CZ-300~CZ-499 -> 500 Internal Server Error (system error)</li>
     *   <li>Default -> 500</li>
     * </ul>
     *
     * @param code CZ error code (e.g., "CZ-126")
     * @return HTTP status code
     */
    int resolveHttpStatus(String code) {
        if (code == null || !code.startsWith("CZ-") || code.length() < 4) {
            return 500;
        }

        int num;
        try {
            num = Integer.parseInt(code.substring(3));
        } catch (NumberFormatException e) {
            return 500;
        }

        // CZ-101: Optimistic lock conflict
        if (num == 101) {
            return 409;
        }

        // CZ-102: Time restriction
        if (num == 102) {
            return 403;
        }

        // CZ-106~CZ-110: Permission denied
        if (num >= 106 && num <= 110) {
            return 403;
        }

        // CZ-100~CZ-299: Validation / business rule
        if (num >= 100 && num <= 299) {
            return 400;
        }

        // CZ-300~CZ-499: System error
        if (num >= 300 && num <= 499) {
            return 500;
        }

        return 500;
    }
}

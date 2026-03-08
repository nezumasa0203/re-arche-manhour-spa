package com.example.czConsv.exception;

import java.util.List;

/**
 * CZ system business exception. Carries a CZ error code (CZ-000~999).
 * The GlobalExceptionHandler maps the error code range to HTTP status.
 *
 * <p>Error code ranges:
 * <ul>
 *   <li>CZ-000~099: Success messages (not thrown as exceptions)</li>
 *   <li>CZ-100~199: Validation errors -> 400 Bad Request</li>
 *   <li>CZ-200~299: Warning/business rule violations -> 400 Bad Request</li>
 *   <li>CZ-300~499: System errors -> 500 Internal Server Error</li>
 *   <li>CZ-101 specifically: Optimistic lock conflict -> 409 Conflict</li>
 *   <li>CZ-102 specifically: Time restriction -> 403 Forbidden</li>
 *   <li>CZ-106~110: Permission denied -> 403 Forbidden</li>
 * </ul>
 */
public class CzBusinessException extends RuntimeException {

    private final String code;
    private final String field;
    private final List<String> params;
    private final Long recordId;

    /**
     * Full constructor.
     *
     * @param code     CZ error code (e.g., "CZ-126")
     * @param message  error message
     * @param field    target field name (nullable)
     * @param params   message parameters (nullable)
     * @param recordId record ID for batch operations (nullable)
     */
    public CzBusinessException(String code, String message, String field,
                               List<String> params, Long recordId) {
        super(message);
        this.code = code;
        this.field = field;
        this.params = params;
        this.recordId = recordId;
    }

    /**
     * Constructor with code and message only.
     *
     * @param code    CZ error code
     * @param message error message
     */
    public CzBusinessException(String code, String message) {
        this(code, message, null, null, null);
    }

    /**
     * Constructor with code, message, and field.
     *
     * @param code    CZ error code
     * @param message error message
     * @param field   target field name
     */
    public CzBusinessException(String code, String message, String field) {
        this(code, message, field, null, null);
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public List<String> getParams() {
        return params;
    }

    public Long getRecordId() {
        return recordId;
    }
}

package com.example.czConsv.config;

import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.exception.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests the handler directly (not via MockMvc).
 */
@SuppressWarnings("unchecked")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ─── CzBusinessException tests ───────────────────────────────────

    @Test
    void validationError_CZ126_returns400() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-126", "作業日は必須入力です", "workDate");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(400, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-126", error.get("code"));
        assertEquals("作業日は必須入力です", error.get("message"));
        assertEquals("workDate", error.get("field"));
    }

    @Test
    void optimisticLock_CZ101_returns409() {
        OptimisticLockException ex = new OptimisticLockException();

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(409, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-101", error.get("code"));
    }

    @Test
    void timeRestriction_CZ102_returns403() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-102", "時間制限エラー");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(403, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-102", error.get("code"));
    }

    @Test
    void permissionDenied_CZ106_returns403() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-106", "権限がありません");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(403, response.getStatusCode().value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CZ-107", "CZ-108", "CZ-109", "CZ-110"})
    void permissionDenied_CZ107to110_returns403(String code) {
        CzBusinessException ex = new CzBusinessException(code, "権限エラー");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(403, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals(code, error.get("code"));
    }

    @Test
    void systemError_CZ300_returns500() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-300", "システムエラー");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(500, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-300", error.get("code"));
    }

    @Test
    void businessException_withParams_includesParamsInResponse() {
        List<String> params = List.of("作業日", "2024-01-01");
        CzBusinessException ex = new CzBusinessException(
                "CZ-126", "{0}は{1}以降を入力してください",
                "workDate", params, null);

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        Map<String, Object> error = extractError(response);
        assertEquals(params, error.get("params"));
    }

    @Test
    void businessException_withRecordId_includesRecordIdInResponse() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-200", "バッチエラー", null, null, 12345L);

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        Map<String, Object> error = extractError(response);
        assertEquals(12345L, error.get("recordId"));
    }

    @Test
    void businessException_withField_includesFieldInResponse() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-126", "必須エラー", "systemCode");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        Map<String, Object> error = extractError(response);
        assertEquals("systemCode", error.get("field"));
    }

    @Test
    void businessException_nullFieldAndParams_omittedFromResponse() {
        CzBusinessException ex = new CzBusinessException("CZ-200", "警告メッセージ");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        Map<String, Object> error = extractError(response);
        assertFalse(error.containsKey("field"),
                "field should be omitted when null");
        assertFalse(error.containsKey("params"),
                "params should be omitted when null");
        assertFalse(error.containsKey("recordId"),
                "recordId should be omitted when null");
    }

    // ─── MethodArgumentNotValidException tests ───────────────────────

    @Test
    void validationException_returns400_withCZ126() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request", "workDate", "作業日は必須です"));

        MethodParameter methodParameter = new MethodParameter(
                this.getClass().getDeclaredMethod("dummyMethod", String.class), 0);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-126", error.get("code"));
        assertEquals("作業日は必須です", error.get("message"));
        assertEquals("workDate", error.get("field"));
    }

    // ─── MissingServletRequestParameterException tests ───────────────

    @Test
    void missingParam_returns400_withCZ126() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("yearMonth", "String");

        ResponseEntity<Map<String, Object>> response = handler.handleMissingParam(ex);

        assertEquals(400, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-126", error.get("code"));
        assertEquals("yearMonthは必須入力です", error.get("message"));
        assertEquals("yearMonth", error.get("field"));
    }

    // ─── General Exception tests ─────────────────────────────────────

    @Test
    void generalException_returns500_withCZ300() {
        Exception ex = new RuntimeException("Unexpected NPE");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(500, response.getStatusCode().value());

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-300", error.get("code"));
        assertEquals("システムエラーが発生しました", error.get("message"));
    }

    // ─── resolveHttpStatus edge cases ────────────────────────────────

    @Test
    void resolveHttpStatus_warningRange_returns400() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-250", "業務ルール違反");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void resolveHttpStatus_systemErrorRange_returns500() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-499", "システムエラー上限");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(500, response.getStatusCode().value());
    }

    // ─── Response format verification ────────────────────────────────

    @Test
    void responseFormat_hasErrorWrapper() {
        CzBusinessException ex = new CzBusinessException("CZ-126", "テスト");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"),
                "Response body must have 'error' wrapper key");

        Map<String, Object> error = extractError(response);
        assertNotNull(error.get("code"));
        assertNotNull(error.get("message"));
    }

    @Test
    void fullResponse_containsAllFields() {
        List<String> params = List.of("作業日");
        CzBusinessException ex = new CzBusinessException(
                "CZ-126", "{0}は必須入力です", "workDate", params, 99L);

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        Map<String, Object> error = extractError(response);
        assertEquals("CZ-126", error.get("code"));
        assertEquals("{0}は必須入力です", error.get("message"));
        assertEquals("workDate", error.get("field"));
        assertEquals(params, error.get("params"));
        assertEquals(99L, error.get("recordId"));
    }

    // ─── Helper methods ──────────────────────────────────────────────

    private Map<String, Object> extractError(ResponseEntity<Map<String, Object>> response) {
        assertNotNull(response.getBody());
        Object errorObj = response.getBody().get("error");
        assertNotNull(errorObj, "Response must contain 'error' key");
        return (Map<String, Object>) errorObj;
    }

    /**
     * Dummy method used to create MethodParameter for MethodArgumentNotValidException.
     */
    @SuppressWarnings("unused")
    void dummyMethod(String param) {
        // no-op
    }
}

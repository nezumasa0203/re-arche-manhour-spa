package com.example.czConsv.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for CzBusinessException and OptimisticLockException.
 */
class CzBusinessExceptionTest {

    @Test
    void fullConstructor_setsAllFields() {
        List<String> params = List.of("作業日");
        CzBusinessException ex = new CzBusinessException(
                "CZ-126", "{0}は必須入力です", "workDate", params, 12345L);

        assertEquals("CZ-126", ex.getCode());
        assertEquals("{0}は必須入力です", ex.getMessage());
        assertEquals("workDate", ex.getField());
        assertEquals(params, ex.getParams());
        assertEquals(12345L, ex.getRecordId());
    }

    @Test
    void codeAndMessageConstructor_setsNullsForOptionalFields() {
        CzBusinessException ex = new CzBusinessException("CZ-300", "システムエラー");

        assertEquals("CZ-300", ex.getCode());
        assertEquals("システムエラー", ex.getMessage());
        assertNull(ex.getField());
        assertNull(ex.getParams());
        assertNull(ex.getRecordId());
    }

    @Test
    void codeMessageFieldConstructor_setsNullsForParamsAndRecordId() {
        CzBusinessException ex = new CzBusinessException(
                "CZ-126", "入力エラー", "workDate");

        assertEquals("CZ-126", ex.getCode());
        assertEquals("入力エラー", ex.getMessage());
        assertEquals("workDate", ex.getField());
        assertNull(ex.getParams());
        assertNull(ex.getRecordId());
    }

    @Test
    void isRuntimeException() {
        CzBusinessException ex = new CzBusinessException("CZ-100", "test");
        assertEquals(true, ex instanceof RuntimeException);
    }

    @Test
    void optimisticLockException_hasDefaultCodeAndMessage() {
        OptimisticLockException ex = new OptimisticLockException();

        assertEquals("CZ-101", ex.getCode());
        assertEquals("他のユーザーがデータを更新しました。画面を再読込してください。",
                ex.getMessage());
        assertNull(ex.getField());
        assertNull(ex.getParams());
        assertNull(ex.getRecordId());
    }

    @Test
    void optimisticLockException_isCzBusinessException() {
        OptimisticLockException ex = new OptimisticLockException();
        assertEquals(true, ex instanceof CzBusinessException);
    }
}

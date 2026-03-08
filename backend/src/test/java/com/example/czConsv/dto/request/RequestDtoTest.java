package com.example.czConsv.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * リクエスト DTO 単体テスト。
 *
 * <p>全 11 リクエスト DTO のレコード生成とバリデーション制約を検証する。
 */
@DisplayName("リクエスト DTO バリデーション")
class RequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ================================================================
    // 1. WorkHoursCreateRequest
    // ================================================================
    @Nested
    @DisplayName("WorkHoursCreateRequest")
    class WorkHoursCreateRequestTest {

        @Test
        @DisplayName("全フィールド指定で違反なし")
        void validFullRequest() {
            var req = new WorkHoursCreateRequest(
                    "2025-04", "2025-04-15", "SUB001", "SUB002",
                    "CAT01", "テスト件名", "08:30",
                    "TMR-001", "WR-001", "山田太郎");
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "全フィールド指定時は違反なし: " + violations);
        }

        @Test
        @DisplayName("下書きモード（yearMonth のみ）で違反なし")
        void validDraftRequest() {
            var req = new WorkHoursCreateRequest(
                    "2025-04", null, null, null,
                    null, null, null, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "下書きモードは違反なし: " + violations);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        @DisplayName("yearMonth が空/null で違反")
        void yearMonthBlank(String yearMonth) {
            var req = new WorkHoursCreateRequest(
                    yearMonth, null, null, null,
                    null, null, null, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "yearMonth 空は違反");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025", "2025-1", "2025-13", "2025/04", "202504", "abcd-01"})
        @DisplayName("yearMonth が不正パターンで違反")
        void yearMonthInvalidPattern(String yearMonth) {
            var req = new WorkHoursCreateRequest(
                    yearMonth, null, null, null,
                    null, null, null, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "不正パターン: " + yearMonth);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025-01", "2025-06", "2025-12", "2030-09"})
        @DisplayName("yearMonth の有効パターン")
        void yearMonthValidPattern(String yearMonth) {
            var req = new WorkHoursCreateRequest(
                    yearMonth, null, null, null,
                    null, null, null, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "有効パターン: " + yearMonth);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025-04-1", "2025-4-15", "20250415", "2025/04/15"})
        @DisplayName("workDate が不正パターンで違反")
        void workDateInvalidPattern(String workDate) {
            var req = new WorkHoursCreateRequest(
                    "2025-04", workDate, null, null,
                    null, null, null, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "workDate 不正: " + workDate);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025-04-01", "2025-04-15", "2025-04-30", "2025-12-31"})
        @DisplayName("workDate の有効パターン")
        void workDateValidPattern(String workDate) {
            var req = new WorkHoursCreateRequest(
                    "2025-04", workDate, null, null,
                    null, null, null, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "workDate 有効: " + workDate);
        }

        @ParameterizedTest
        @ValueSource(strings = {"8:30", "25:00", "08:60", "0830", "abc"})
        @DisplayName("hours が不正パターンで違反")
        void hoursInvalidPattern(String hours) {
            var req = new WorkHoursCreateRequest(
                    "2025-04", null, null, null,
                    null, null, hours, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "hours 不正: " + hours);
        }

        @ParameterizedTest
        @ValueSource(strings = {"00:00", "06:00", "08:30", "12:45", "23:30", "23:59"})
        @DisplayName("hours の有効パターン")
        void hoursValidPattern(String hours) {
            var req = new WorkHoursCreateRequest(
                    "2025-04", null, null, null,
                    null, null, hours, null, null, null);
            Set<ConstraintViolation<WorkHoursCreateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "hours 有効: " + hours);
        }

        @Test
        @DisplayName("レコードのアクセサメソッド")
        void accessors() {
            var req = new WorkHoursCreateRequest(
                    "2025-04", "2025-04-15", "SUB001", "SUB002",
                    "CAT01", "件名", "08:30",
                    "TMR-001", "WR-001", "山田");
            assertEquals("2025-04", req.yearMonth());
            assertEquals("2025-04-15", req.workDate());
            assertEquals("SUB001", req.targetSubsystemNo());
            assertEquals("SUB002", req.causeSubsystemNo());
            assertEquals("CAT01", req.categoryCode());
            assertEquals("件名", req.subject());
            assertEquals("08:30", req.hours());
            assertEquals("TMR-001", req.tmrNo());
            assertEquals("WR-001", req.workRequestNo());
            assertEquals("山田", req.workRequesterName());
        }
    }

    // ================================================================
    // 2. WorkHoursUpdateRequest
    // ================================================================
    @Nested
    @DisplayName("WorkHoursUpdateRequest")
    class WorkHoursUpdateRequestTest {

        private static final LocalDateTime NOW = LocalDateTime.of(2025, 4, 15, 10, 30, 0);

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new WorkHoursUpdateRequest("subject", "新しい件名", NOW);
            Set<ConstraintViolation<WorkHoursUpdateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "有効リクエスト: " + violations);
        }

        @Test
        @DisplayName("field が空で違反")
        void fieldBlank() {
            var req = new WorkHoursUpdateRequest("", "値", NOW);
            Set<ConstraintViolation<WorkHoursUpdateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("field が null で違反")
        void fieldNull() {
            var req = new WorkHoursUpdateRequest(null, "値", NOW);
            Set<ConstraintViolation<WorkHoursUpdateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("value が空で違反")
        void valueBlank() {
            var req = new WorkHoursUpdateRequest("subject", "", NOW);
            Set<ConstraintViolation<WorkHoursUpdateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("updatedAt が null で違反")
        void updatedAtNull() {
            var req = new WorkHoursUpdateRequest("subject", "値", null);
            Set<ConstraintViolation<WorkHoursUpdateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var req = new WorkHoursUpdateRequest("subject", "新しい件名", NOW);
            assertEquals("subject", req.field());
            assertEquals("新しい件名", req.value());
            assertEquals(NOW, req.updatedAt());
        }
    }

    // ================================================================
    // 3. WorkHoursCopyRequest
    // ================================================================
    @Nested
    @DisplayName("WorkHoursCopyRequest")
    class WorkHoursCopyRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new WorkHoursCopyRequest(List.of(1L, 2L, 3L));
            Set<ConstraintViolation<WorkHoursCopyRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("ids が空リストで違反")
        void idsEmpty() {
            var req = new WorkHoursCopyRequest(Collections.emptyList());
            Set<ConstraintViolation<WorkHoursCopyRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("ids が null で違反")
        void idsNull() {
            var req = new WorkHoursCopyRequest(null);
            Set<ConstraintViolation<WorkHoursCopyRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var ids = List.of(10L, 20L);
            var req = new WorkHoursCopyRequest(ids);
            assertEquals(ids, req.ids());
        }
    }

    // ================================================================
    // 4. WorkHoursTransferRequest
    // ================================================================
    @Nested
    @DisplayName("WorkHoursTransferRequest")
    class WorkHoursTransferRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new WorkHoursTransferRequest(
                    List.of(1L, 2L), List.of("2025-05", "2025-06"));
            Set<ConstraintViolation<WorkHoursTransferRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "有効リクエスト: " + violations);
        }

        @Test
        @DisplayName("ids が空で違反")
        void idsEmpty() {
            var req = new WorkHoursTransferRequest(
                    Collections.emptyList(), List.of("2025-05"));
            Set<ConstraintViolation<WorkHoursTransferRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("targetMonths が空で違反")
        void targetMonthsEmpty() {
            var req = new WorkHoursTransferRequest(
                    List.of(1L), Collections.emptyList());
            Set<ConstraintViolation<WorkHoursTransferRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("targetMonths に不正パターンで違反")
        void targetMonthsInvalidPattern() {
            var req = new WorkHoursTransferRequest(
                    List.of(1L), List.of("2025-13"));
            Set<ConstraintViolation<WorkHoursTransferRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var ids = List.of(1L, 2L);
            var months = List.of("2025-05");
            var req = new WorkHoursTransferRequest(ids, months);
            assertEquals(ids, req.ids());
            assertEquals(months, req.targetMonths());
        }
    }

    // ================================================================
    // 5. BatchConfirmRequest
    // ================================================================
    @Nested
    @DisplayName("BatchConfirmRequest")
    class BatchConfirmRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new BatchConfirmRequest("2025-04");
            Set<ConstraintViolation<BatchConfirmRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("yearMonth が空/null で違反")
        void yearMonthBlank(String yearMonth) {
            var req = new BatchConfirmRequest(yearMonth);
            Set<ConstraintViolation<BatchConfirmRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025-13", "2025/04", "202504"})
        @DisplayName("yearMonth が不正パターンで違反")
        void yearMonthInvalid(String yearMonth) {
            var req = new BatchConfirmRequest(yearMonth);
            Set<ConstraintViolation<BatchConfirmRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var req = new BatchConfirmRequest("2025-04");
            assertEquals("2025-04", req.yearMonth());
        }
    }

    // ================================================================
    // 6. BatchRevertRequest
    // ================================================================
    @Nested
    @DisplayName("BatchRevertRequest")
    class BatchRevertRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new BatchRevertRequest("2025-04");
            Set<ConstraintViolation<BatchRevertRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("yearMonth が空/null で違反")
        void yearMonthBlank(String yearMonth) {
            var req = new BatchRevertRequest(yearMonth);
            Set<ConstraintViolation<BatchRevertRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025-00", "2025-13"})
        @DisplayName("yearMonth が不正パターンで違反")
        void yearMonthInvalid(String yearMonth) {
            var req = new BatchRevertRequest(yearMonth);
            Set<ConstraintViolation<BatchRevertRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var req = new BatchRevertRequest("2025-04");
            assertEquals("2025-04", req.yearMonth());
        }
    }

    // ================================================================
    // 7. ApproveRequest
    // ================================================================
    @Nested
    @DisplayName("ApproveRequest")
    class ApproveRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new ApproveRequest(List.of(1L, 2L));
            Set<ConstraintViolation<ApproveRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("ids が空で違反")
        void idsEmpty() {
            var req = new ApproveRequest(Collections.emptyList());
            Set<ConstraintViolation<ApproveRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("ids が null で違反")
        void idsNull() {
            var req = new ApproveRequest(null);
            Set<ConstraintViolation<ApproveRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var ids = List.of(5L, 10L, 15L);
            var req = new ApproveRequest(ids);
            assertEquals(ids, req.ids());
        }
    }

    // ================================================================
    // 8. RevertRequest
    // ================================================================
    @Nested
    @DisplayName("RevertRequest")
    class RevertRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new RevertRequest(List.of(1L));
            Set<ConstraintViolation<RevertRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("ids が空で違反")
        void idsEmpty() {
            var req = new RevertRequest(Collections.emptyList());
            Set<ConstraintViolation<RevertRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("ids が null で違反")
        void idsNull() {
            var req = new RevertRequest(null);
            Set<ConstraintViolation<RevertRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }
    }

    // ================================================================
    // 9. MonthlyControlRequest
    // ================================================================
    @Nested
    @DisplayName("MonthlyControlRequest")
    class MonthlyControlRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new MonthlyControlRequest("2025-04", "ORG001");
            Set<ConstraintViolation<MonthlyControlRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("yearMonth が空/null で違反")
        void yearMonthBlank(String yearMonth) {
            var req = new MonthlyControlRequest(yearMonth, "ORG001");
            Set<ConstraintViolation<MonthlyControlRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"2025-00", "2025-13", "abcd-01"})
        @DisplayName("yearMonth が不正パターンで違反")
        void yearMonthInvalid(String yearMonth) {
            var req = new MonthlyControlRequest(yearMonth, "ORG001");
            Set<ConstraintViolation<MonthlyControlRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("organizationCode が空/null で違反")
        void orgCodeBlank(String orgCode) {
            var req = new MonthlyControlRequest("2025-04", orgCode);
            Set<ConstraintViolation<MonthlyControlRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var req = new MonthlyControlRequest("2025-04", "ORG001");
            assertEquals("2025-04", req.yearMonth());
            assertEquals("ORG001", req.organizationCode());
        }
    }

    // ================================================================
    // 10. DelegationSwitchRequest
    // ================================================================
    @Nested
    @DisplayName("DelegationSwitchRequest")
    class DelegationSwitchRequestTest {

        @Test
        @DisplayName("代行指定（targetStaffId あり）")
        void withTarget() {
            var req = new DelegationSwitchRequest("STAFF001");
            // バリデーション制約なし
            Set<ConstraintViolation<DelegationSwitchRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
            assertEquals("STAFF001", req.targetStaffId());
        }

        @Test
        @DisplayName("代行解除（targetStaffId が null）")
        void cancelDelegation() {
            var req = new DelegationSwitchRequest(null);
            Set<ConstraintViolation<DelegationSwitchRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
            assertNull(req.targetStaffId());
        }
    }

    // ================================================================
    // 11. MySystemCreateRequest
    // ================================================================
    @Nested
    @DisplayName("MySystemCreateRequest")
    class MySystemCreateRequestTest {

        @Test
        @DisplayName("有効なリクエスト")
        void validRequest() {
            var req = new MySystemCreateRequest("SYS001");
            Set<ConstraintViolation<MySystemCreateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("systemNo が空/null で違反")
        void systemNoBlank(String systemNo) {
            var req = new MySystemCreateRequest(systemNo);
            Set<ConstraintViolation<MySystemCreateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("レコードのアクセサ")
        void accessors() {
            var req = new MySystemCreateRequest("SYS001");
            assertEquals("SYS001", req.systemNo());
        }
    }

    // ================================================================
    // レコード等値性テスト
    // ================================================================
    @Nested
    @DisplayName("レコード等値性")
    class RecordEqualityTest {

        @Test
        @DisplayName("同値の WorkHoursCreateRequest は等しい")
        void workHoursCreateEquality() {
            var req1 = new WorkHoursCreateRequest(
                    "2025-04", "2025-04-15", "SUB001", "SUB002",
                    "CAT01", "件名", "08:30", null, null, null);
            var req2 = new WorkHoursCreateRequest(
                    "2025-04", "2025-04-15", "SUB001", "SUB002",
                    "CAT01", "件名", "08:30", null, null, null);
            assertEquals(req1, req2);
            assertEquals(req1.hashCode(), req2.hashCode());
        }

        @Test
        @DisplayName("異なる WorkHoursUpdateRequest は等しくない")
        void workHoursUpdateInequality() {
            var now = LocalDateTime.of(2025, 4, 15, 10, 0, 0);
            var req1 = new WorkHoursUpdateRequest("subject", "値A", now);
            var req2 = new WorkHoursUpdateRequest("subject", "値B", now);
            assertNotEquals(req1, req2);
        }

        @Test
        @DisplayName("BatchConfirmRequest の toString にフィールド値が含まれる")
        void toStringContainsValue() {
            var req = new BatchConfirmRequest("2025-04");
            assertTrue(req.toString().contains("2025-04"));
        }
    }
}

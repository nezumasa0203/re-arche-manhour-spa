package com.example.czConsv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationService の単体テスト。
 *
 * <p>03_user_stories.md に定義された VR-001〜VR-015 の全バリデーションルールを検証する。
 * Spring コンテキスト不要の純粋な単体テスト。
 */
@DisplayName("ValidationService: 工数入力バリデーション VR-001〜VR-015")
class ValidationServiceTest {

    private ValidationService service;

    @BeforeEach
    void setUp() {
        service = new ValidationService();
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * 全フィールドが有効な状態で validateWorkHoursInput を呼び出す。
     * テスト対象のフィールドだけを変更したい場合に使用する。
     */
    private List<ValidationError> validateWithDefaults(
            String yearMonth, String workDate, String targetSubsystemNo,
            String causeSubsystemNo, String categoryCode, String hsSyubetu,
            String subject, String hours, String tmrNo,
            String workRequestNo, String workRequesterName,
            int existingDailyMinutes) {
        return service.validateWorkHoursInput(
                yearMonth, workDate, targetSubsystemNo,
                causeSubsystemNo, categoryCode, hsSyubetu,
                subject, hours, tmrNo,
                workRequestNo, workRequesterName,
                existingDailyMinutes);
    }

    /**
     * 全フィールド有効なデフォルト値でバリデーションを実行する。
     */
    private List<ValidationError> validateAllValid() {
        return validateWithDefaults(
                "2025-02", "2025-02-15", "SUB001",
                "SUB002", "01", "1",
                "通常の作業", "01:00", null,
                null, null, 0);
    }

    /**
     * 指定フィールドの ValidationError を抽出する。
     */
    private Optional<ValidationError> findErrorByField(
            List<ValidationError> errors, String field) {
        return errors.stream()
                .filter(e -> field.equals(e.field()))
                .findFirst();
    }

    /**
     * 指定コードの ValidationError を抽出する。
     */
    private Optional<ValidationError> findErrorByCode(
            List<ValidationError> errors, String code) {
        return errors.stream()
                .filter(e -> code.equals(e.code()))
                .findFirst();
    }

    // ========================================================================
    // VR-001: workDate 必須 + YYYY-MM-DD 形式
    // ========================================================================

    @Nested
    @DisplayName("VR-001: workDate 必須・YYYY-MM-DD 形式")
    class VR001_WorkDateRequired {

        @ParameterizedTest(name = "workDate={0} -> CZ-126 エラー")
        @NullAndEmptySource
        @DisplayName("null/空文字 -> CZ-126 エラー")
        void nullOrEmpty_error(String workDate) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", workDate, "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "workDate".equals(e.field())),
                    "workDate が null/空の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("有効な日付 '2025-02-25' -> エラーなし")
        void validDate_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-25", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "workDate".equals(e.field())),
                    "有効な日付の場合 workDate エラーがないこと");
        }

        @Test
        @DisplayName("不正な形式 '25-02-2025' -> CZ-126 エラー")
        void invalidFormat_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "25-02-2025", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "workDate".equals(e.field())),
                    "不正な日付形式の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("不正な月 '2025-13-01' -> CZ-126 エラー")
        void invalidMonth_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-13-01", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "workDate".equals(e.field())),
                    "不正な月の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("不正な日 '2025-02-30' -> CZ-126 エラー")
        void invalidDay_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-30", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "workDate".equals(e.field())),
                    "存在しない日付の場合 CZ-126 エラーが返ること");
        }
    }

    // ========================================================================
    // VR-002: workDate が対象 yearMonth 内
    // ========================================================================

    @Nested
    @DisplayName("VR-002: workDate が対象 yearMonth 内")
    class VR002_WorkDateWithinYearMonth {

        @Test
        @DisplayName("yearMonth='2025-02', workDate='2025-02-15' -> エラーなし")
        void withinMonth_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-144".equals(e.code())),
                    "対象月内の日付の場合 CZ-144 エラーがないこと");
        }

        @Test
        @DisplayName("yearMonth='2025-02', workDate='2025-03-01' -> CZ-144 エラー")
        void afterMonth_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-03-01", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-144".equals(e.code()) && "workDate".equals(e.field())),
                    "翌月の日付の場合 CZ-144 エラーが返ること");
        }

        @Test
        @DisplayName("yearMonth='2025-02', workDate='2025-01-31' -> CZ-144 エラー")
        void beforeMonth_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-01-31", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-144".equals(e.code()) && "workDate".equals(e.field())),
                    "前月の日付の場合 CZ-144 エラーが返ること");
        }
    }

    // ========================================================================
    // VR-003: targetSubsystemNo 必須
    // ========================================================================

    @Nested
    @DisplayName("VR-003: targetSubsystemNo 必須")
    class VR003_TargetSubsystemRequired {

        @ParameterizedTest(name = "targetSubsystemNo={0} -> CZ-126 エラー")
        @NullAndEmptySource
        @DisplayName("null/空文字 -> CZ-126 エラー")
        void nullOrEmpty_error(String value) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", value,
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code())
                            && "targetSubsystemNo".equals(e.field())),
                    "targetSubsystemNo が null/空の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("'SUB001' -> エラーなし")
        void validValue_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "targetSubsystemNo".equals(e.field())),
                    "有効な値の場合 targetSubsystemNo エラーがないこと");
        }
    }

    // ========================================================================
    // VR-004: causeSubsystemNo 必須
    // ========================================================================

    @Nested
    @DisplayName("VR-004: causeSubsystemNo 必須")
    class VR004_CauseSubsystemRequired {

        @ParameterizedTest(name = "causeSubsystemNo={0} -> CZ-126 エラー")
        @NullAndEmptySource
        @DisplayName("null/空文字 -> CZ-126 エラー")
        void nullOrEmpty_error(String value) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    value, "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code())
                            && "causeSubsystemNo".equals(e.field())),
                    "causeSubsystemNo が null/空の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("'SUB002' -> エラーなし")
        void validValue_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "causeSubsystemNo".equals(e.field())),
                    "有効な値の場合 causeSubsystemNo エラーがないこと");
        }
    }

    // ========================================================================
    // VR-005: categoryCode 必須
    // ========================================================================

    @Nested
    @DisplayName("VR-005: categoryCode 必須")
    class VR005_CategoryCodeRequired {

        @ParameterizedTest(name = "categoryCode={0} -> CZ-126 エラー")
        @NullAndEmptySource
        @DisplayName("null/空文字 -> CZ-126 エラー")
        void nullOrEmpty_error(String value) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", value, "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code())
                            && "categoryCode".equals(e.field())),
                    "categoryCode が null/空の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("'01' -> エラーなし")
        void validValue_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "categoryCode".equals(e.field())),
                    "有効な値の場合 categoryCode エラーがないこと");
        }
    }

    // ========================================================================
    // VR-006: subject 必須 + 128バイト以下
    // ========================================================================

    @Nested
    @DisplayName("VR-006: subject 必須・128バイト以下")
    class VR006_SubjectRequired {

        @ParameterizedTest(name = "subject={0} -> CZ-126 エラー")
        @NullAndEmptySource
        @DisplayName("null/空文字 -> CZ-126 エラー")
        void nullOrEmpty_error(String value) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    value, "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "subject".equals(e.field())),
                    "subject が null/空の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("'テスト' (6バイト) -> エラーなし")
        void shortSubject_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "テスト", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "subject".equals(e.field())),
                    "'テスト' (6バイト) はエラーなし");
        }

        @Test
        @DisplayName("64全角文字 = 128バイト -> エラーなし (境界値)")
        void exactly128Bytes_fullWidth_noError() {
            // 64 full-width characters = 128 bytes
            String subject = "あ".repeat(64);
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    subject, "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "subject".equals(e.field())),
                    "64全角文字 (128バイト) はエラーなし");
        }

        @Test
        @DisplayName("65全角文字 = 130バイト -> CZ-126 エラー (境界値超過)")
        void exceeds128Bytes_fullWidth_error() {
            // 65 full-width characters = 130 bytes
            String subject = "あ".repeat(65);
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    subject, "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "subject".equals(e.field())),
                    "65全角文字 (130バイト) は CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("128半角文字 = 128バイト -> エラーなし (境界値)")
        void exactly128Bytes_halfWidth_noError() {
            String subject = "a".repeat(128);
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    subject, "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "subject".equals(e.field())),
                    "128半角文字 (128バイト) はエラーなし");
        }

        @Test
        @DisplayName("129半角文字 = 129バイト -> CZ-126 エラー")
        void exceeds128Bytes_halfWidth_error() {
            String subject = "a".repeat(129);
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    subject, "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "subject".equals(e.field())),
                    "129半角文字 (129バイト) は CZ-126 エラーが返ること");
        }
    }

    // ========================================================================
    // VR-007: hsSyubetu="0" 時の禁止語句チェック
    // ========================================================================

    @Nested
    @DisplayName("VR-007: hsSyubetu='0' 時の禁止語句チェック")
    class VR007_ProhibitedWords {

        @Test
        @DisplayName("通常の件名 + hsSyubetu='0' -> エラーなし")
        void normalSubject_hosyuType0_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "0",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-141".equals(e.code())),
                    "通常の件名は CZ-141 エラーが返らないこと");
        }

        @Test
        @DisplayName("'カ層の修正' + hsSyubetu='0' -> CZ-141 エラー")
        void prohibitedWord_kasou_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "0",
                    "カ層の修正", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-141".equals(e.code()) && "subject".equals(e.field())),
                    "'カ層' を含む件名は CZ-141 エラーが返ること");
        }

        @Test
        @DisplayName("'取得処理' + hsSyubetu='0' -> CZ-141 エラー")
        void prohibitedWord_shutoku_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "0",
                    "取得処理", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-141".equals(e.code()) && "subject".equals(e.field())),
                    "'取得' を含む件名は CZ-141 エラーが返ること");
        }

        @Test
        @DisplayName("'カ層の修正' + hsSyubetu='1' -> エラーなし (hsSyubetu='0' 以外は対象外)")
        void prohibitedWord_hosyuType1_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "カ層の修正", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-141".equals(e.code())),
                    "hsSyubetu='1' の場合は禁止語句チェック対象外");
        }

        @ParameterizedTest(name = "禁止語句 ''{0}'' を含む件名 -> CZ-141")
        @ValueSource(strings = {
                "カ層", "@機能別1", "連絡器", "（相談", "（課程", "・限定",
                "・共存作成", "取得", "賃金", "導入", "経理", "実績演算"
        })
        @DisplayName("12個の禁止語句すべてが検出されること")
        void allProhibitedWords_detected(String word) {
            String subject = "テスト" + word + "テスト";
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "0",
                    subject, "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-141".equals(e.code()) && "subject".equals(e.field())),
                    "禁止語句 '" + word + "' を含む件名は CZ-141 エラーが返ること");
        }
    }

    // ========================================================================
    // VR-008: hours 必須・0 より大きい
    // ========================================================================

    @Nested
    @DisplayName("VR-008: hours 必須・0 より大きい")
    class VR008_HoursRequired {

        @ParameterizedTest(name = "hours={0} -> CZ-126 エラー")
        @NullAndEmptySource
        @DisplayName("null/空文字 -> CZ-126 エラー")
        void nullOrEmpty_error(String value) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", value, null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "hours".equals(e.field())),
                    "hours が null/空の場合 CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("'00:00' -> CZ-126 エラー (0 は不可)")
        void zeroHours_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "00:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "hours".equals(e.field())),
                    "'00:00' は CZ-126 エラーが返ること");
        }

        @Test
        @DisplayName("'01:00' -> エラーなし")
        void validHours_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "hours".equals(e.field())),
                    "'01:00' はエラーなし");
        }
    }

    // ========================================================================
    // VR-009: HH:mm 形式 + 15分刻み
    // ========================================================================

    @Nested
    @DisplayName("VR-009: HH:mm 形式・15分刻み")
    class VR009_HoursFormat {

        @ParameterizedTest(name = "hours=''{0}'' -> エラーなし (15分刻み)")
        @ValueSource(strings = {"03:30", "03:15", "03:00", "03:45"})
        @DisplayName("有効な15分刻みの時間 -> エラーなし")
        void valid15MinIncrements_noError(String hours) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", hours, null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "hours".equals(e.field())),
                    "'" + hours + "' はエラーなし");
        }

        @Test
        @DisplayName("'03:10' -> CZ-147 エラー (15分刻みでない)")
        void nonFifteenMinIncrement_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "03:10", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-147".equals(e.code()) && "hours".equals(e.field())),
                    "'03:10' は CZ-147 エラーが返ること");
        }

        @Test
        @DisplayName("'3:30' -> エラーなし (1桁時間も許可)")
        void singleDigitHour_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "3:30", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "hours".equals(e.field())),
                    "'3:30' はエラーなし");
        }

        @Test
        @DisplayName("'25:00' -> エラーなし (個別レコードは24h超可、VR-010 で日次合計チェック)")
        void over24Hours_individual_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "25:00", null,
                    null, null, 0);

            // VR-010 for daily total check is separate; individual record >24h is format-valid
            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-125".equals(e.code()) && "hours".equals(e.field())),
                    "'25:00' は形式エラーなし");
        }

        @Test
        @DisplayName("'abc' -> CZ-125 エラー (不正な形式)")
        void invalidFormat_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "abc", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-125".equals(e.code()) && "hours".equals(e.field())),
                    "'abc' は CZ-125 エラーが返ること");
        }

        @Test
        @DisplayName("'3' -> HH形式として解釈 -> 180分 (3:00相当) -> エラーなし")
        void singleDigitOnly_treatedAsHours() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "3", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "hours".equals(e.field())),
                    "'3' は HH 形式として有効");
        }
    }

    // ========================================================================
    // VR-010: 日次合計24時間以下
    // ========================================================================

    @Nested
    @DisplayName("VR-010: 日次合計24時間 (1440分) 以下")
    class VR010_DailyTotal {

        @Test
        @DisplayName("既存1200分 + 03:30 (210分) = 1410分 -> エラーなし")
        void withinLimit_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "03:30", null,
                    null, null, 1200);

            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-146".equals(e.code())),
                    "合計1410分は CZ-146 エラーなし");
        }

        @Test
        @DisplayName("既存1200分 + 04:15 (255分) = 1455分 -> CZ-146 エラー")
        void exceedsLimit_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "04:15", null,
                    null, null, 1200);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-146".equals(e.code()) && "hours".equals(e.field())),
                    "合計1455分は CZ-146 エラーが返ること");
        }

        @Test
        @DisplayName("既存0分 + 24:00 (1440分) = 1440分 -> エラーなし (ちょうど24h)")
        void exactly24Hours_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "24:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-146".equals(e.code())),
                    "ちょうど1440分は CZ-146 エラーなし");
        }

        @Test
        @DisplayName("既存0分 + 24:15 (1455分) = 1455分 -> CZ-146 エラー")
        void justOver24Hours_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "24:15", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-146".equals(e.code()) && "hours".equals(e.field())),
                    "合計1455分は CZ-146 エラーが返ること");
        }
    }

    // ========================================================================
    // VR-011: tmrNo 最大5文字・半角英数字のみ
    // ========================================================================

    @Nested
    @DisplayName("VR-011: tmrNo 最大5文字・半角英数字のみ")
    class VR011_TmrNo {

        @Test
        @DisplayName("null -> エラーなし (任意項目)")
        void nullValue_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "tmrNo".equals(e.field())),
                    "null はエラーなし");
        }

        @Test
        @DisplayName("空文字 -> エラーなし")
        void emptyString_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", "",
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "tmrNo".equals(e.field())),
                    "空文字はエラーなし");
        }

        @Test
        @DisplayName("'12345' -> エラーなし (5文字)")
        void fiveChars_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", "12345",
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "tmrNo".equals(e.field())),
                    "'12345' はエラーなし");
        }

        @Test
        @DisplayName("'123456' -> エラー (6文字、上限超過)")
        void sixChars_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", "123456",
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "tmrNo".equals(e.field())),
                    "'123456' はエラーが返ること");
        }

        @Test
        @DisplayName("'AB123' -> エラーなし (半角英数字)")
        void alphanumeric_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", "AB123",
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "tmrNo".equals(e.field())),
                    "'AB123' はエラーなし");
        }

        @Test
        @DisplayName("'あいう' -> エラー (半角英数字でない)")
        void fullWidthChars_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", "あいう",
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "tmrNo".equals(e.field())),
                    "'あいう' はエラーが返ること");
        }
    }

    // ========================================================================
    // VR-012: workRequestNo 空 OR 7文字
    // ========================================================================

    @Nested
    @DisplayName("VR-012: workRequestNo 空 OR 7文字")
    class VR012_WorkRequestNo {

        @Test
        @DisplayName("null -> エラーなし (任意)")
        void nullValue_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "workRequestNo".equals(e.field())),
                    "null はエラーなし");
        }

        @Test
        @DisplayName("空文字 -> エラーなし")
        void emptyString_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    "", null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "workRequestNo".equals(e.field())),
                    "空文字はエラーなし");
        }

        @Test
        @DisplayName("'1234567' -> エラーなし (ちょうど7文字)")
        void sevenChars_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    "1234567", null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "workRequestNo".equals(e.field())),
                    "'1234567' はエラーなし");
        }

        @Test
        @DisplayName("'123456' -> CZ-137 エラー (6文字)")
        void sixChars_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    "123456", null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-137".equals(e.code()) && "workRequestNo".equals(e.field())),
                    "'123456' は CZ-137 エラーが返ること");
        }

        @Test
        @DisplayName("'12345678' -> CZ-137 エラー (8文字)")
        void eightChars_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    "12345678", null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-137".equals(e.code()) && "workRequestNo".equals(e.field())),
                    "'12345678' は CZ-137 エラーが返ること");
        }
    }

    // ========================================================================
    // VR-013: workRequesterName 最大40文字
    // ========================================================================

    @Nested
    @DisplayName("VR-013: workRequesterName 最大40文字")
    class VR013_WorkRequesterName {

        @Test
        @DisplayName("null -> エラーなし (任意)")
        void nullValue_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "workRequesterName".equals(e.field())),
                    "null はエラーなし");
        }

        @Test
        @DisplayName("'山田太郎' (4文字) -> エラーなし")
        void shortName_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, "山田太郎", 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "workRequesterName".equals(e.field())),
                    "'山田太郎' はエラーなし");
        }

        @Test
        @DisplayName("40文字 -> エラーなし (境界値)")
        void fortyChars_noError() {
            String name = "あ".repeat(40);
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, name, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "workRequesterName".equals(e.field())),
                    "40文字はエラーなし");
        }

        @Test
        @DisplayName("41文字 -> エラー (上限超過)")
        void fortyOneChars_error() {
            String name = "あ".repeat(41);
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, name, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "workRequesterName".equals(e.field())),
                    "41文字はエラーが返ること");
        }
    }

    // ========================================================================
    // VR-014: カテゴリ依存の必須チェック
    // ========================================================================

    @Nested
    @DisplayName("VR-014: 特定カテゴリでの workRequestNo/workRequesterName 必須")
    class VR014_ConditionalRequired {

        @Test
        @DisplayName("categoryCode='03', workRequestNo=null -> CZ-142 エラー")
        void category03_noRequestNo_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "03", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-142".equals(e.code())
                            && "workRequestNo".equals(e.field())),
                    "カテゴリ 03 で workRequestNo が null の場合 CZ-142 エラーが返ること");
        }

        @Test
        @DisplayName("categoryCode='03', workRequestNo='1234567', workRequesterName=null -> CZ-142 エラー")
        void category03_noRequesterName_error() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "03", "1",
                    "通常の作業", "01:00", null,
                    "1234567", null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-142".equals(e.code())
                            && "workRequesterName".equals(e.field())),
                    "カテゴリ 03 で workRequesterName が null の場合 CZ-142 エラーが返ること");
        }

        @Test
        @DisplayName("categoryCode='03', 両方あり -> エラーなし")
        void category03_bothPresent_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "03", "1",
                    "通常の作業", "01:00", null,
                    "1234567", "山田", 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-142".equals(e.code())),
                    "カテゴリ 03 で両方指定の場合 CZ-142 エラーなし");
        }

        @Test
        @DisplayName("categoryCode='01', workRequestNo=null -> エラーなし (対象外カテゴリ)")
        void category01_noRequestNo_noError() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", "01", "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().noneMatch(
                    e -> "CZ-142".equals(e.code())),
                    "カテゴリ 01 は対象外のため CZ-142 エラーなし");
        }

        @ParameterizedTest(name = "categoryCode=''{0}'' は workRequestNo 必須カテゴリ")
        @ValueSource(strings = {"03", "04", "05"})
        @DisplayName("カテゴリ 03/04/05 で workRequestNo 必須")
        void requiringCategories_noRequestNo_error(String categoryCode) {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", "2025-02-15", "SUB001",
                    "SUB002", categoryCode, "1",
                    "通常の作業", "01:00", null,
                    null, null, 0);

            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-142".equals(e.code())),
                    "カテゴリ " + categoryCode + " で必須項目なしの場合 CZ-142 エラーが返ること");
        }
    }

    // ========================================================================
    // VR-015: カテゴリ一意性チェック
    // ========================================================================

    @Nested
    @DisplayName("VR-015: 同一サブシステム＋同一件名で異なるカテゴリは不可")
    class VR015_CategoryUniqueness {

        @Test
        @DisplayName("同一サブシステム＋同一件名＋同一カテゴリ -> エラーなし")
        void sameCategory_noError() {
            List<String[]> existingRecords = List.<String[]>of(
                    new String[]{"SUB001", "テスト作業", "01"}
            );
            Optional<ValidationError> error = service.validateCategoryUniqueness(
                    "SUB001", "テスト作業", "01", existingRecords);

            assertTrue(error.isEmpty(),
                    "同一カテゴリの場合はエラーなし");
        }

        @Test
        @DisplayName("同一サブシステム＋同一件名＋異なるカテゴリ -> CZ-132 エラー")
        void differentCategory_error() {
            List<String[]> existingRecords = List.<String[]>of(
                    new String[]{"SUB001", "テスト作業", "01"}
            );
            Optional<ValidationError> error = service.validateCategoryUniqueness(
                    "SUB001", "テスト作業", "02", existingRecords);

            assertTrue(error.isPresent(), "異なるカテゴリの場合はエラーあり");
            assertEquals("CZ-132", error.get().code());
            assertEquals("categoryCode", error.get().field());
        }

        @Test
        @DisplayName("異なるサブシステム＋同一件名＋異なるカテゴリ -> エラーなし")
        void differentSubsystem_noError() {
            List<String[]> existingRecords = List.<String[]>of(
                    new String[]{"SUB001", "テスト作業", "01"}
            );
            Optional<ValidationError> error = service.validateCategoryUniqueness(
                    "SUB002", "テスト作業", "02", existingRecords);

            assertTrue(error.isEmpty(),
                    "異なるサブシステムの場合はエラーなし");
        }

        @Test
        @DisplayName("既存レコードなし -> エラーなし")
        void noExistingRecords_noError() {
            List<String[]> existingRecords = List.of();
            Optional<ValidationError> error = service.validateCategoryUniqueness(
                    "SUB001", "テスト作業", "01", existingRecords);

            assertTrue(error.isEmpty(),
                    "既存レコードなしの場合はエラーなし");
        }
    }

    // ========================================================================
    // calculateByteLength のテスト
    // ========================================================================

    @Nested
    @DisplayName("calculateByteLength: CZ バイト長計算")
    class CalculateByteLengthTest {

        @Test
        @DisplayName("'abc' -> 3バイト (半角)")
        void halfWidthOnly() {
            assertEquals(3, service.calculateByteLength("abc"));
        }

        @Test
        @DisplayName("'あいう' -> 6バイト (全角)")
        void fullWidthOnly() {
            assertEquals(6, service.calculateByteLength("あいう"));
        }

        @Test
        @DisplayName("'ｱｲｳ' -> 6バイト (半角カタカナ = 2バイト/文字)")
        void halfWidthKatakana() {
            assertEquals(6, service.calculateByteLength("ｱｲｳ"));
        }

        @Test
        @DisplayName("'aあｱ' -> 5バイト (混在: 1+2+2)")
        void mixed() {
            assertEquals(5, service.calculateByteLength("aあｱ"));
        }

        @Test
        @DisplayName("空文字 -> 0バイト")
        void emptyString() {
            assertEquals(0, service.calculateByteLength(""));
        }

        @Test
        @DisplayName("null -> 0バイト")
        void nullString() {
            assertEquals(0, service.calculateByteLength(null));
        }

        @Test
        @DisplayName("半角数字 '123' -> 3バイト")
        void halfWidthNumbers() {
            assertEquals(3, service.calculateByteLength("123"));
        }

        @Test
        @DisplayName("全角数字 '１２３' -> 6バイト")
        void fullWidthNumbers() {
            assertEquals(6, service.calculateByteLength("１２３"));
        }

        @Test
        @DisplayName("半角カタカナ濁点 'ｶﾞ' -> 4バイト (2文字 x 2バイト)")
        void halfWidthKatakanaDakuten() {
            // 'ｶ' (U+FF76) + 'ﾞ' (U+FF9E) -> each is half-width katakana range -> 2+2=4
            assertEquals(4, service.calculateByteLength("ｶﾞ"));
        }
    }

    // ========================================================================
    // containsProhibitedWord のテスト
    // ========================================================================

    @Nested
    @DisplayName("containsProhibitedWord: 禁止語句チェック")
    class ContainsProhibitedWordTest {

        @Test
        @DisplayName("通常テキスト -> false")
        void normalText_false() {
            assertFalse(service.containsProhibitedWord("通常の作業"));
        }

        @ParameterizedTest(name = "禁止語句 ''{0}'' -> true")
        @ValueSource(strings = {
                "カ層", "@機能別1", "連絡器", "（相談", "（課程", "・限定",
                "・共存作成", "取得", "賃金", "導入", "経理", "実績演算"
        })
        @DisplayName("12個の禁止語句すべてが検出されること")
        void allProhibitedWords_true(String word) {
            assertTrue(service.containsProhibitedWord(word),
                    "'" + word + "' は禁止語句として検出されること");
        }

        @Test
        @DisplayName("禁止語句が文中に含まれる場合 -> true")
        void embeddedWord_true() {
            assertTrue(service.containsProhibitedWord("データ取得処理"));
        }

        @Test
        @DisplayName("null -> false")
        void nullText_false() {
            assertFalse(service.containsProhibitedWord(null));
        }

        @Test
        @DisplayName("空文字 -> false")
        void emptyText_false() {
            assertFalse(service.containsProhibitedWord(""));
        }
    }

    // ========================================================================
    // parseHoursToMinutes のテスト
    // ========================================================================

    @Nested
    @DisplayName("parseHoursToMinutes: 時間文字列のパース")
    class ParseHoursToMinutesTest {

        @ParameterizedTest(name = "hours=''{0}'' -> {1}分")
        @CsvSource({
                "03:30, 210",
                "00:15, 15",
                "24:00, 1440",
                "1:00, 60",
                "0:45, 45",
                "10:30, 630"
        })
        @DisplayName("HH:mm 形式のパース")
        void validHHmm_parsed(String hours, int expectedMinutes) {
            assertEquals(expectedMinutes, service.parseHoursToMinutes(hours));
        }

        @Test
        @DisplayName("'abc' -> -1 (無効)")
        void invalidFormat_negative1() {
            assertEquals(-1, service.parseHoursToMinutes("abc"));
        }

        @Test
        @DisplayName("null -> -1 (無効)")
        void nullValue_negative1() {
            assertEquals(-1, service.parseHoursToMinutes(null));
        }

        @Test
        @DisplayName("空文字 -> -1 (無効)")
        void emptyString_negative1() {
            assertEquals(-1, service.parseHoursToMinutes(""));
        }

        @Test
        @DisplayName("'3' -> 180 (HH形式として3時間)")
        void singleDigitHH_parsed() {
            assertEquals(180, service.parseHoursToMinutes("3"));
        }

        @Test
        @DisplayName("'12' -> 720 (HH形式として12時間)")
        void twoDigitHH_parsed() {
            assertEquals(720, service.parseHoursToMinutes("12"));
        }
    }

    // ========================================================================
    // validateFieldUpdate のテスト
    // ========================================================================

    @Nested
    @DisplayName("validateFieldUpdate: 単一フィールド更新バリデーション")
    class ValidateFieldUpdateTest {

        @Test
        @DisplayName("subject フィールド更新 - 有効な値 -> エラーなし")
        void validSubject_noError() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "subject", "テスト作業", 0, "1");
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("subject フィールド更新 - null -> CZ-126 エラー")
        void nullSubject_error() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "subject", null, 0, "1");
            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-126".equals(e.code()) && "subject".equals(e.field())));
        }

        @Test
        @DisplayName("subject フィールド更新 - 禁止語句あり + hsSyubetu='0' -> CZ-141 エラー")
        void prohibitedSubject_error() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "subject", "カ層の修正", 0, "0");
            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-141".equals(e.code()) && "subject".equals(e.field())));
        }

        @Test
        @DisplayName("hours フィールド更新 - 有効な値 -> エラーなし")
        void validHours_noError() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "hours", "02:30", 0, "1");
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("hours フィールド更新 - 不正な形式 -> CZ-125 エラー")
        void invalidHoursFormat_error() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "hours", "abc", 0, "1");
            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-125".equals(e.code()) && "hours".equals(e.field())));
        }

        @Test
        @DisplayName("hours フィールド更新 - 日次合計超過 -> CZ-146 エラー")
        void dailyTotalExceeded_error() {
            // 04:15 = 255分 + 既存1200分 = 1455分 > 1440分
            List<ValidationError> errors = service.validateFieldUpdate(
                    "hours", "04:15", 1200, "1");
            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-146".equals(e.code()) && "hours".equals(e.field())));
        }

        @Test
        @DisplayName("tmrNo フィールド更新 - 有効 -> エラーなし")
        void validTmrNo_noError() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "tmrNo", "AB12", 0, "1");
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("tmrNo フィールド更新 - 長すぎる -> エラー")
        void longTmrNo_error() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "tmrNo", "123456", 0, "1");
            assertTrue(errors.stream().anyMatch(
                    e -> "tmrNo".equals(e.field())));
        }

        @Test
        @DisplayName("workRequestNo フィールド更新 - 6文字 -> CZ-137 エラー")
        void invalidWorkRequestNo_error() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "workRequestNo", "123456", 0, "1");
            assertTrue(errors.stream().anyMatch(
                    e -> "CZ-137".equals(e.code()) && "workRequestNo".equals(e.field())));
        }

        @Test
        @DisplayName("workRequesterName フィールド更新 - 41文字 -> エラー")
        void longWorkRequesterName_error() {
            String name = "あ".repeat(41);
            List<ValidationError> errors = service.validateFieldUpdate(
                    "workRequesterName", name, 0, "1");
            assertTrue(errors.stream().anyMatch(
                    e -> "workRequesterName".equals(e.field())));
        }

        @Test
        @DisplayName("未知のフィールド -> エラーなし")
        void unknownField_noError() {
            List<ValidationError> errors = service.validateFieldUpdate(
                    "unknownField", "value", 0, "1");
            assertTrue(errors.isEmpty());
        }
    }

    // ========================================================================
    // 全フィールド有効時のテスト
    // ========================================================================

    @Nested
    @DisplayName("全フィールド有効時")
    class AllValidTest {

        @Test
        @DisplayName("全フィールド有効 -> エラーなし")
        void allValid_noErrors() {
            List<ValidationError> errors = validateAllValid();
            assertTrue(errors.isEmpty(),
                    "全フィールド有効の場合はエラーなし: " + errors);
        }
    }

    // ========================================================================
    // 複数エラー同時発生のテスト
    // ========================================================================

    @Nested
    @DisplayName("複数エラーの同時検出")
    class MultipleErrorsTest {

        @Test
        @DisplayName("複数フィールドが無効 -> 複数のエラーが返ること")
        void multipleInvalidFields_multipleErrors() {
            List<ValidationError> errors = validateWithDefaults(
                    "2025-02", null, null,
                    null, null, "1",
                    null, null, null,
                    null, null, 0);

            // workDate, targetSubsystemNo, causeSubsystemNo,
            // categoryCode, subject, hours の6フィールドがエラー
            assertTrue(errors.size() >= 6,
                    "6つ以上のエラーが返ること: 実際は " + errors.size() + " 件");
        }
    }
}

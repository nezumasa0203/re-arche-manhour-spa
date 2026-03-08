package com.example.czConsv.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工数入力バリデーションサービス。
 *
 * <p>03_user_stories.md に定義された VR-001〜VR-015 の全バリデーションルールを実装する。
 * 現行 CZ システムのバリデーションロジックを忠実に再現し、
 * CZ エラーコード体系（CZ-000〜CZ-999）に準拠したエラーを返す。
 *
 * <p>主な検証内容:
 * <ul>
 *   <li>必須項目チェック（workDate, targetSubsystemNo, causeSubsystemNo, categoryCode, subject, hours）</li>
 *   <li>日付形式・範囲チェック（YYYY-MM-DD、対象年月内）</li>
 *   <li>件名バイト長チェック（128バイト以下、CZ独自バイト計算）</li>
 *   <li>禁止語句チェック（hsSyubetu="0" 時、12語）</li>
 *   <li>時間形式チェック（HH:mm、15分刻み）</li>
 *   <li>日次合計チェック（24時間 = 1440分以下）</li>
 *   <li>任意項目の形式チェック（tmrNo, workRequestNo, workRequesterName）</li>
 *   <li>カテゴリ依存の必須チェック（カテゴリ 03/04/05）</li>
 *   <li>カテゴリ一意性チェック（同一サブシステム＋件名で同一カテゴリ）</li>
 * </ul>
 */
@Service
public class ValidationService {

    /** 禁止語句リスト（VR-007）。hsSyubetu="0" 時に件名に含めてはならない12語。 */
    private static final List<String> PROHIBITED_WORDS = List.of(
            "カ層", "@機能別1", "連絡器", "（相談", "（課程", "・限定",
            "・共存作成", "取得", "賃金", "導入", "経理", "実績演算"
    );

    /** 依頼情報が必須となるカテゴリコード（VR-014）。 */
    private static final Set<String> CATEGORIES_REQUIRING_REQUEST_INFO =
            Set.of("03", "04", "05");

    /** 日付パーサ（YYYY-MM-DD 厳密解決）。 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT);

    /** 年月パーサ（YYYY-MM 厳密解決）。 */
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM")
                    .withResolverStyle(ResolverStyle.STRICT);

    /** 時間パターン: HH:mm 形式 */
    private static final Pattern HOURS_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

    /** 時間パターン: HH のみ形式 */
    private static final Pattern HOURS_ONLY_PATTERN = Pattern.compile("^(\\d{1,2})$");

    /** TMR番号パターン: 半角英数字のみ */
    private static final Pattern TMR_NO_PATTERN = Pattern.compile("^[A-Za-z0-9]*$");

    /** 日次最大分数（24時間 = 1440分）。 */
    private static final int MAX_DAILY_MINUTES = 1440;

    /** 件名最大バイト長。 */
    private static final int MAX_SUBJECT_BYTES = 128;

    /** TMR番号最大文字数。 */
    private static final int MAX_TMR_NO_LENGTH = 5;

    /** 作業依頼番号の必要文字数。 */
    private static final int WORK_REQUEST_NO_LENGTH = 7;

    /** 作業依頼者名最大文字数。 */
    private static final int MAX_WORK_REQUESTER_NAME_LENGTH = 40;

    /** 半角カタカナ Unicode 範囲の下限。 */
    private static final char HALF_WIDTH_KATAKANA_START = '\uFF61';

    /** 半角カタカナ Unicode 範囲の上限。 */
    private static final char HALF_WIDTH_KATAKANA_END = '\uFF9F';

    /**
     * 単一の工数レコードを入力チェックする（一括確定時の isInputCheck 相当）。
     *
     * <p>VR-001〜VR-014 のバリデーションルールを適用し、
     * 違反がある場合は対応する {@link ValidationError} のリストを返す。
     * すべて有効な場合は空リストを返す。
     *
     * @param yearMonth            対象年月（YYYY-MM 形式）
     * @param workDate             作業日（YYYY-MM-DD 形式）
     * @param targetSubsystemNo    対象サブシステム番号
     * @param causeSubsystemNo     原因サブシステム番号
     * @param categoryCode         カテゴリコード
     * @param hsSyubetu            保守種別
     * @param subject              件名
     * @param hours                時間（HH:mm 形式）
     * @param tmrNo                TMR番号（任意）
     * @param workRequestNo        作業依頼番号（任意、ただしカテゴリ依存で必須）
     * @param workRequesterName    作業依頼者名（任意、ただしカテゴリ依存で必須）
     * @param existingDailyMinutes 同日の既存レコード合計分数（このレコードを除く）
     * @return バリデーションエラーのリスト（有効な場合は空リスト）
     */
    public List<ValidationError> validateWorkHoursInput(
            String yearMonth,
            String workDate,
            String targetSubsystemNo,
            String causeSubsystemNo,
            String categoryCode,
            String hsSyubetu,
            String subject,
            String hours,
            String tmrNo,
            String workRequestNo,
            String workRequesterName,
            int existingDailyMinutes) {

        List<ValidationError> errors = new ArrayList<>();

        // VR-001: workDate 必須 + YYYY-MM-DD 形式
        boolean workDateValid = validateWorkDate(workDate, errors);

        // VR-002: workDate が対象 yearMonth 内
        if (workDateValid) {
            validateWorkDateWithinYearMonth(yearMonth, workDate, errors);
        }

        // VR-003: targetSubsystemNo 必須
        validateRequired(targetSubsystemNo, "targetSubsystemNo", errors);

        // VR-004: causeSubsystemNo 必須
        validateRequired(causeSubsystemNo, "causeSubsystemNo", errors);

        // VR-005: categoryCode 必須
        validateRequired(categoryCode, "categoryCode", errors);

        // VR-006: subject 必須 + 128バイト以下
        validateSubject(subject, errors);

        // VR-007: 禁止語句チェック (hsSyubetu="0" の場合のみ)
        validateProhibitedWords(subject, hsSyubetu, errors);

        // VR-008, VR-009, VR-010: hours 検証
        validateHours(hours, existingDailyMinutes, errors);

        // VR-011: tmrNo
        validateTmrNo(tmrNo, errors);

        // VR-012: workRequestNo
        validateWorkRequestNo(workRequestNo, errors);

        // VR-013: workRequesterName
        validateWorkRequesterName(workRequesterName, errors);

        // VR-014: カテゴリ依存の必須チェック
        validateConditionalRequired(categoryCode, workRequestNo,
                workRequesterName, errors);

        return errors;
    }

    /**
     * 単一フィールドの更新バリデーション（PATCH /work-hours/{id} 用）。
     *
     * @param field                更新対象フィールド名
     * @param value                更新値
     * @param existingDailyMinutes 同日の既存レコード合計分数（このレコードを除く）
     * @param hsSyubetu            保守種別（subject の禁止語句チェックに使用）
     * @return バリデーションエラーのリスト
     */
    public List<ValidationError> validateFieldUpdate(
            String field, String value,
            int existingDailyMinutes, String hsSyubetu) {

        List<ValidationError> errors = new ArrayList<>();

        switch (field) {
            case "subject":
                validateSubject(value, errors);
                validateProhibitedWords(value, hsSyubetu, errors);
                break;
            case "hours":
                validateHours(value, existingDailyMinutes, errors);
                break;
            case "tmrNo":
                validateTmrNo(value, errors);
                break;
            case "workRequestNo":
                validateWorkRequestNo(value, errors);
                break;
            case "workRequesterName":
                validateWorkRequesterName(value, errors);
                break;
            default:
                // 未知のフィールドはバリデーション対象外
                break;
        }

        return errors;
    }

    /**
     * VR-015: 同一サブシステム＋同一件名でのカテゴリ一意性を検証する。
     *
     * <p>同一月内の既存レコードに対して、同じサブシステム番号＋件名の組み合わせで
     * 異なるカテゴリコードが設定されていないか検査する。
     *
     * @param subsystemNo     対象サブシステム番号
     * @param subject         件名
     * @param categoryCode    カテゴリコード
     * @param existingRecords 同月の既存レコード（各要素: [subsystemNo, subject, categoryCode]）
     * @return カテゴリ不一致の場合は ValidationError、一致または該当なしの場合は empty
     */
    public Optional<ValidationError> validateCategoryUniqueness(
            String subsystemNo, String subject, String categoryCode,
            List<String[]> existingRecords) {

        for (String[] record : existingRecords) {
            String existingSubsystem = record[0];
            String existingSubject = record[1];
            String existingCategory = record[2];

            if (subsystemNo.equals(existingSubsystem)
                    && subject.equals(existingSubject)
                    && !categoryCode.equals(existingCategory)) {
                return Optional.of(new ValidationError(
                        "CZ-132",
                        "同一サブシステム・同一件名で異なるカテゴリは設定できません",
                        "categoryCode"));
            }
        }

        return Optional.empty();
    }

    /**
     * CZ 独自ルールに基づくバイト長を計算する。
     *
     * <ul>
     *   <li>全角文字: 2バイト</li>
     *   <li>半角文字: 1バイト</li>
     *   <li>半角カタカナ (U+FF61〜U+FF9F): 2バイト</li>
     * </ul>
     *
     * @param text 計算対象の文字列
     * @return CZ バイト長
     */
    public int calculateByteLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int byteLength = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isHalfWidthKatakana(ch)) {
                // 半角カタカナ: 2バイト
                byteLength += 2;
            } else if (isHalfWidth(ch)) {
                // 半角: 1バイト
                byteLength += 1;
            } else {
                // 全角: 2バイト
                byteLength += 2;
            }
        }
        return byteLength;
    }

    /**
     * テキストに禁止語句が含まれているかを判定する。
     *
     * @param text 検査対象テキスト
     * @return 禁止語句が含まれている場合 true
     */
    public boolean containsProhibitedWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return PROHIBITED_WORDS.stream().anyMatch(text::contains);
    }

    /**
     * 時間文字列（HH:mm または HH）を分数に変換する。
     *
     * @param hours 時間文字列
     * @return 分数。不正な形式の場合は -1
     */
    public int parseHoursToMinutes(String hours) {
        if (hours == null || hours.isEmpty()) {
            return -1;
        }

        // HH:mm 形式
        Matcher hhmmMatcher = HOURS_PATTERN.matcher(hours);
        if (hhmmMatcher.matches()) {
            try {
                int h = Integer.parseInt(hhmmMatcher.group(1));
                int m = Integer.parseInt(hhmmMatcher.group(2));
                if (m >= 60) {
                    return -1;
                }
                return h * 60 + m;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        // HH のみ形式
        Matcher hhMatcher = HOURS_ONLY_PATTERN.matcher(hours);
        if (hhMatcher.matches()) {
            try {
                int h = Integer.parseInt(hhMatcher.group(1));
                return h * 60;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        return -1;
    }

    // ========================================================================
    // プライベートバリデーションメソッド
    // ========================================================================

    /**
     * VR-001: workDate の必須・形式チェック。
     *
     * @return 日付が有効な場合 true（VR-002 の前提条件チェック用）
     */
    private boolean validateWorkDate(String workDate, List<ValidationError> errors) {
        if (isNullOrEmpty(workDate)) {
            errors.add(new ValidationError(
                    "CZ-126", "作業日は必須です", "workDate"));
            return false;
        }

        try {
            LocalDate.parse(workDate, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError(
                    "CZ-126", "作業日の形式が不正です（YYYY-MM-DD）", "workDate"));
            return false;
        }
    }

    /**
     * VR-002: workDate が対象 yearMonth 内かチェック。
     */
    private void validateWorkDateWithinYearMonth(
            String yearMonth, String workDate, List<ValidationError> errors) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
            LocalDate date = LocalDate.parse(workDate, DATE_FORMATTER);
            YearMonth dateYm = YearMonth.from(date);

            if (!ym.equals(dateYm)) {
                errors.add(new ValidationError(
                        "CZ-144",
                        "作業日が対象年月の範囲外です",
                        "workDate"));
            }
        } catch (DateTimeParseException e) {
            // yearMonth のパースエラーはここでは無視（別途バリデーション想定）
        }
    }

    /**
     * 必須フィールドチェック共通処理。
     */
    private void validateRequired(String value, String field,
                                  List<ValidationError> errors) {
        if (isNullOrEmpty(value)) {
            errors.add(new ValidationError(
                    "CZ-126", field + "は必須です", field));
        }
    }

    /**
     * VR-006: subject 必須 + 128バイト以下チェック。
     */
    private void validateSubject(String subject, List<ValidationError> errors) {
        if (isNullOrEmpty(subject)) {
            errors.add(new ValidationError(
                    "CZ-126", "件名は必須です", "subject"));
            return;
        }

        int byteLen = calculateByteLength(subject);
        if (byteLen > MAX_SUBJECT_BYTES) {
            errors.add(new ValidationError(
                    "CZ-126",
                    "件名は128バイト以下で入力してください",
                    "subject"));
        }
    }

    /**
     * VR-007: hsSyubetu="0" 時の禁止語句チェック。
     */
    private void validateProhibitedWords(String subject, String hsSyubetu,
                                         List<ValidationError> errors) {
        if (subject == null || hsSyubetu == null) {
            return;
        }
        if ("0".equals(hsSyubetu) && containsProhibitedWord(subject)) {
            errors.add(new ValidationError(
                    "CZ-141",
                    "件名に禁止語句が含まれています",
                    "subject"));
        }
    }

    /**
     * VR-008, VR-009, VR-010: hours の必須・形式・日次合計チェック。
     */
    private void validateHours(String hours, int existingDailyMinutes,
                               List<ValidationError> errors) {
        // VR-008: 必須チェック
        if (isNullOrEmpty(hours)) {
            errors.add(new ValidationError(
                    "CZ-126", "時間は必須です", "hours"));
            return;
        }

        // VR-009: 形式チェック
        int minutes = parseHoursToMinutes(hours);
        if (minutes < 0) {
            errors.add(new ValidationError(
                    "CZ-125", "時間の形式が不正です（HH:mm）", "hours"));
            return;
        }

        // VR-008: 0 より大きいか
        if (minutes == 0) {
            errors.add(new ValidationError(
                    "CZ-126", "時間は0より大きい値を入力してください", "hours"));
            return;
        }

        // VR-009: 15分刻みチェック
        if (minutes % 15 != 0) {
            errors.add(new ValidationError(
                    "CZ-147",
                    "時間は15分刻みで入力してください",
                    "hours"));
            return;
        }

        // VR-010: 日次合計24時間以下
        int dailyTotal = existingDailyMinutes + minutes;
        if (dailyTotal > MAX_DAILY_MINUTES) {
            errors.add(new ValidationError(
                    "CZ-146",
                    "1日の合計時間が24時間を超えています",
                    "hours"));
        }
    }

    /**
     * VR-011: tmrNo の最大5文字・半角英数字チェック。
     */
    private void validateTmrNo(String tmrNo, List<ValidationError> errors) {
        if (isNullOrEmpty(tmrNo)) {
            return; // 任意項目
        }

        if (tmrNo.length() > MAX_TMR_NO_LENGTH) {
            errors.add(new ValidationError(
                    "VR-011",
                    "TMR番号は5文字以下で入力してください",
                    "tmrNo"));
            return;
        }

        if (!TMR_NO_PATTERN.matcher(tmrNo).matches()) {
            errors.add(new ValidationError(
                    "VR-011",
                    "TMR番号は半角英数字のみ入力可能です",
                    "tmrNo"));
        }
    }

    /**
     * VR-012: workRequestNo の空 OR 7文字チェック。
     */
    private void validateWorkRequestNo(String workRequestNo,
                                       List<ValidationError> errors) {
        if (isNullOrEmpty(workRequestNo)) {
            return; // 任意項目
        }

        if (workRequestNo.length() != WORK_REQUEST_NO_LENGTH) {
            errors.add(new ValidationError(
                    "CZ-137",
                    "作業依頼番号は7文字で入力してください",
                    "workRequestNo"));
        }
    }

    /**
     * VR-013: workRequesterName の最大40文字チェック。
     */
    private void validateWorkRequesterName(String workRequesterName,
                                           List<ValidationError> errors) {
        if (isNullOrEmpty(workRequesterName)) {
            return; // 任意項目
        }

        if (workRequesterName.length() > MAX_WORK_REQUESTER_NAME_LENGTH) {
            errors.add(new ValidationError(
                    "VR-013",
                    "作業依頼者名は40文字以下で入力してください",
                    "workRequesterName"));
        }
    }

    /**
     * VR-014: 特定カテゴリでの workRequestNo/workRequesterName 必須チェック。
     */
    private void validateConditionalRequired(
            String categoryCode, String workRequestNo,
            String workRequesterName, List<ValidationError> errors) {

        if (categoryCode == null
                || !CATEGORIES_REQUIRING_REQUEST_INFO.contains(categoryCode)) {
            return;
        }

        if (isNullOrEmpty(workRequestNo)) {
            errors.add(new ValidationError(
                    "CZ-142",
                    "このカテゴリでは作業依頼番号は必須です",
                    "workRequestNo"));
        }

        if (isNullOrEmpty(workRequesterName)) {
            errors.add(new ValidationError(
                    "CZ-142",
                    "このカテゴリでは作業依頼者名は必須です",
                    "workRequesterName"));
        }
    }

    // ========================================================================
    // ユーティリティ
    // ========================================================================

    /**
     * 文字が半角カタカナ (U+FF61〜U+FF9F) かを判定する。
     */
    private boolean isHalfWidthKatakana(char ch) {
        return ch >= HALF_WIDTH_KATAKANA_START && ch <= HALF_WIDTH_KATAKANA_END;
    }

    /**
     * 文字が半角（ASCII範囲 U+0020〜U+007E）かを判定する。
     * 半角カタカナは含まない（別途判定）。
     */
    private boolean isHalfWidth(char ch) {
        return ch <= 0x007E;
    }

    /**
     * null または空文字かを判定する。
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}

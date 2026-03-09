package com.example.czConsv.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SortParser 単体テスト。
 *
 * <p>"column:direction" 形式の sort パラメータを ORDER BY 句に変換し、
 * ホワイトリスト外のカラムを拒否することを検証する。
 */
class SortParserTest {

    private static final Set<String> ALLOWED = Set.of(
            "workDate", "status", "hours", "staffId", "createdAt", "updatedAt"
    );

    // =========================================================================
    // 正常系
    // =========================================================================

    @Test
    void singleColumnAsc() {
        String result = SortParser.parse("workDate:asc", ALLOWED);
        assertThat(result).isEqualTo(" ORDER BY work_date ASC");
    }

    @Test
    void singleColumnDesc() {
        String result = SortParser.parse("status:desc", ALLOWED);
        assertThat(result).isEqualTo(" ORDER BY status DESC");
    }

    @Test
    void multipleColumns() {
        String result = SortParser.parse("workDate:asc,status:desc", ALLOWED);
        assertThat(result).isEqualTo(" ORDER BY work_date ASC, status DESC");
    }

    @Test
    void caseInsensitiveDirection() {
        String result = SortParser.parse("workDate:ASC", ALLOWED);
        assertThat(result).isEqualTo(" ORDER BY work_date ASC");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void nullOrEmptyReturnsEmpty(String input) {
        String result = SortParser.parse(input, ALLOWED);
        assertThat(result).isEmpty();
    }

    @Test
    void camelCaseToSnakeCase() {
        String result = SortParser.parse("createdAt:asc", ALLOWED);
        assertThat(result).isEqualTo(" ORDER BY created_at ASC");
    }

    @Test
    void columnWithoutDirectionDefaultsToAsc() {
        String result = SortParser.parse("status", ALLOWED);
        assertThat(result).isEqualTo(" ORDER BY status ASC");
    }

    // =========================================================================
    // 異常系 — ホワイトリスト違反
    // =========================================================================

    @Test
    void rejectsUnknownColumn() {
        assertThatThrownBy(() -> SortParser.parse("injected:asc", ALLOWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("injected");
    }

    @Test
    void rejectsSqlInjectionAttempt() {
        assertThatThrownBy(() ->
                SortParser.parse("1 OR 1=1; DROP TABLE users; --:asc", ALLOWED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidDirection() {
        assertThatThrownBy(() -> SortParser.parse("status:SIDEWAYS", ALLOWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SIDEWAYS");
    }

    @Test
    void rejectsPartiallyInvalidColumns() {
        // 最初のカラムが有効でも2番目が不正なら全体拒否
        assertThatThrownBy(() -> SortParser.parse("workDate:asc,evil:desc", ALLOWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evil");
    }

    // =========================================================================
    // camelCase → snake_case 変換
    // =========================================================================

    @ParameterizedTest
    @CsvSource({
            "workDate,   work_date",
            "staffId,    staff_id",
            "createdAt,  created_at",
            "updatedAt,  updated_at",
            "status,     status",
            "hours,      hours"
    })
    void camelToSnake(String camel, String expected) {
        assertThat(SortParser.toSnakeCase(camel.trim())).isEqualTo(expected.trim());
    }
}

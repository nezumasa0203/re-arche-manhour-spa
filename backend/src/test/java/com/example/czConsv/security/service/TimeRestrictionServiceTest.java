package com.example.czConsv.security.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeRestrictionService の単体テスト。
 *
 * <p>仕様書セクション 11 に基づく月初アクセス制限ロジックを検証する。
 * <ul>
 *   <li>ロール 940: 月初1日目 → アクセス制限 (CZ-102)</li>
 *   <li>ロール 941: 月初2日目 → アクセス制限 (CZ-102)</li>
 *   <li>管理スタッフロール 931-935 は制限対象外（免除）</li>
 * </ul>
 *
 * <p>{@link Clock} を注入し、日付を固定することで決定論的なテストを実現する。
 */
@DisplayName("TimeRestrictionService: 月初アクセス制限")
class TimeRestrictionServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

    // ========================================================================
    // ヘルパー
    // ========================================================================

    /**
     * 指定した日付で固定された Clock を返す。
     */
    private Clock fixedClockOf(int year, int month, int dayOfMonth) {
        LocalDate date = LocalDate.of(year, month, dayOfMonth);
        Instant instant = date.atStartOfDay(ZONE).toInstant();
        return Clock.fixed(instant, ZONE);
    }

    /**
     * 指定した日付に固定された TimeRestrictionService を生成する。
     */
    private TimeRestrictionService serviceAt(int year, int month, int dayOfMonth) {
        return new TimeRestrictionService(fixedClockOf(year, month, dayOfMonth));
    }

    // ========================================================================
    // 月初1日目のテスト
    // ========================================================================

    @Nested
    @DisplayName("月初1日目の制限")
    class Day1Restriction {

        @Test
        @DisplayName("1日目 — 非スタッフユーザー（ロール940）はアクセス制限される (CZ-102)")
        void day1_nonStaffUser_restricted() {
            TimeRestrictionService service = serviceAt(2026, 3, 1);

            assertTrue(service.isRestricted(940),
                    "ロール 940 は1日目にアクセス制限されること");
        }

        @Test
        @DisplayName("1日目 — 管理スタッフロール 931 は制限対象外（免除）")
        void day1_staffRole931_exempt() {
            TimeRestrictionService service = serviceAt(2026, 3, 1);

            assertFalse(service.isRestricted(931),
                    "管理スタッフロール 931 は1日目でもアクセス制限されないこと");
        }

        @Test
        @DisplayName("1日目 — 管理スタッフロール 935 は制限対象外（免除）")
        void day1_staffRole935_exempt() {
            TimeRestrictionService service = serviceAt(2026, 3, 1);

            assertFalse(service.isRestricted(935),
                    "管理スタッフロール 935 は1日目でもアクセス制限されないこと");
        }

        @Test
        @DisplayName("1日目 — ロール 936 は免除範囲外のため制限される")
        void day1_staffRole936_restricted() {
            TimeRestrictionService service = serviceAt(2026, 3, 1);

            assertTrue(service.isRestricted(936),
                    "ロール 936 は管理スタッフ範囲 (931-935) 外なのでアクセス制限されること");
        }
    }

    // ========================================================================
    // 月初2日目のテスト
    // ========================================================================

    @Nested
    @DisplayName("月初2日目の制限")
    class Day2Restriction {

        @Test
        @DisplayName("2日目 — 非スタッフユーザーはアクセス制限される (CZ-102)")
        void day2_nonStaffUser_restricted() {
            TimeRestrictionService service = serviceAt(2026, 3, 2);

            assertTrue(service.isRestricted(941),
                    "ロール 941 は2日目にアクセス制限されること");
        }

        @Test
        @DisplayName("2日目 — 管理スタッフロール 932 は制限対象外（免除）")
        void day2_staffRole932_exempt() {
            TimeRestrictionService service = serviceAt(2026, 3, 2);

            assertFalse(service.isRestricted(932),
                    "管理スタッフロール 932 は2日目でもアクセス制限されないこと");
        }
    }

    // ========================================================================
    // 3日目以降のテスト（制限なし）
    // ========================================================================

    @Nested
    @DisplayName("3日目以降は制限なし")
    class Day3AndBeyond {

        @Test
        @DisplayName("3日目以降 — すべてのユーザーがアクセス可能")
        void day3_noRestriction() {
            TimeRestrictionService service = serviceAt(2026, 3, 3);

            assertFalse(service.isRestricted(940),
                    "ロール 940 でも3日目以降はアクセス制限されないこと");
            assertFalse(service.isRestricted(941),
                    "ロール 941 でも3日目以降はアクセス制限されないこと");
            assertFalse(service.isRestricted(931),
                    "管理スタッフロール 931 は当然制限されないこと");
        }

        @Test
        @DisplayName("15日目 — 制限なし")
        void day15_noRestriction() {
            TimeRestrictionService service = serviceAt(2026, 3, 15);

            assertFalse(service.isRestricted(940),
                    "15日目はどのロールでもアクセス制限されないこと");
            assertFalse(service.isRestricted(null),
                    "15日目は staffRole が null でもアクセス制限されないこと");
        }

        @ParameterizedTest(name = "月末 {0} 日目 — 制限なし")
        @DisplayName("月末日（28-31日）は制限なし")
        @ValueSource(ints = {28, 29, 30, 31})
        void lastDaysOfMonth_noRestriction(int day) {
            // 1月は31日まであるので全日テスト可能
            TimeRestrictionService service = serviceAt(2026, 1, day);

            assertFalse(service.isRestricted(940),
                    day + "日目はアクセス制限されないこと");
            assertFalse(service.isRestricted(null),
                    day + "日目は staffRole が null でもアクセス制限されないこと");
        }
    }

    // ========================================================================
    // null staffRole のテスト
    // ========================================================================

    @Nested
    @DisplayName("staffRole が null の場合")
    class NullStaffRole {

        @Test
        @DisplayName("1日目 — staffRole が null の場合はアクセス制限される")
        void day1_nullStaffRole_restricted() {
            TimeRestrictionService service = serviceAt(2026, 3, 1);

            assertTrue(service.isRestricted(null),
                    "staffRole が null の場合、1日目にアクセス制限されること");
        }

        @Test
        @DisplayName("2日目 — staffRole が null の場合はアクセス制限される")
        void day2_nullStaffRole_restricted() {
            TimeRestrictionService service = serviceAt(2026, 3, 2);

            assertTrue(service.isRestricted(null),
                    "staffRole が null の場合、2日目にアクセス制限されること");
        }
    }

    // ========================================================================
    // パラメタライズドテスト: 日付 × staffRole の組み合わせ
    // ========================================================================

    @Nested
    @DisplayName("日付と staffRole の組み合わせテスト")
    class ParameterizedCombinations {

        @ParameterizedTest(name = "日={0}, ロール={1} → 制限={2}")
        @DisplayName("月初制限 — 日付・ロール組み合わせ")
        @CsvSource({
                // 1日目: 非免除ロール → 制限
                "1, 940,  true",
                "1, 941,  true",
                "1, 936,  true",
                "1, 100,  true",
                // 1日目: 免除ロール (931-935) → 制限なし
                "1, 931, false",
                "1, 932, false",
                "1, 933, false",
                "1, 934, false",
                "1, 935, false",
                // 2日目: 非免除ロール → 制限
                "2, 940,  true",
                "2, 941,  true",
                "2, 936,  true",
                // 2日目: 免除ロール (931-935) → 制限なし
                "2, 931, false",
                "2, 932, false",
                "2, 933, false",
                "2, 934, false",
                "2, 935, false",
                // 3日目以降: 全ロール → 制限なし
                "3,  940, false",
                "3,  941, false",
                "3,  931, false",
                "10, 940, false",
                "15, 941, false",
                "20, 936, false",
                "28, 940, false",
                "31, 935, false"
        })
        void dayAndRoleCombination(int dayOfMonth, int staffRole, boolean expectedRestricted) {
            // 1月は31日まであるのですべてのテストケースを網羅できる
            TimeRestrictionService service = serviceAt(2026, 1, dayOfMonth);

            assertEquals(expectedRestricted, service.isRestricted(staffRole),
                    String.format("%d日目・ロール%d の制限結果が期待値 %s と一致すること",
                            dayOfMonth, staffRole, expectedRestricted));
        }

        @ParameterizedTest(name = "日={0}, staffRole=null → 制限={1}")
        @DisplayName("staffRole null — 日付別制限")
        @CsvSource({
                "1,  true",
                "2,  true",
                "3,  false",
                "15, false",
                "28, false",
                "31, false"
        })
        void dayWithNullRoleCombination(int dayOfMonth, boolean expectedRestricted) {
            // 1月は31日まであるのですべてのテストケースを網羅できる
            TimeRestrictionService service = serviceAt(2026, 1, dayOfMonth);

            assertEquals(expectedRestricted, service.isRestricted(null),
                    String.format("%d日目・staffRole=null の制限結果が期待値 %s と一致すること",
                            dayOfMonth, expectedRestricted));
        }
    }
}

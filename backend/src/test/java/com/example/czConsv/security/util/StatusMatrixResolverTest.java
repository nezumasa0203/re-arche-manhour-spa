package com.example.czConsv.security.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatusMatrixResolver の全パターン網羅テスト。
 *
 * 12ステータスキー × 2系列（担当者/管理者）× 各操作 = 全144パターンを検証する。
 * - 担当者系列 (tan): 12キー × 5操作 = 60パターン
 * - 管理者系列 (man): 12キー × 7操作 = 84パターン
 */
@DisplayName("StatusMatrixResolver: ステータスマトリクス解決テスト")
class StatusMatrixResolverTest {

    // ========================================================================
    // 担当者系列 (tan) — isTab010Bit2 = true — 全60パターン
    // ========================================================================

    @Nested
    @DisplayName("担当者系列 (isTab010Bit2=true) の全60パターン")
    class TanSeriesTest {

        @ParameterizedTest(name = "担当者系列 statusKey={0} op={1} → {2}")
        @CsvSource({
                // --- statusKey=000 ---
                "000, add,    9",
                "000, copy,   1",
                "000, delete, 1",
                "000, update, 1",
                "000, view,   1",
                // --- statusKey=010 ---
                "010, add,    9",
                "010, copy,   0",
                "010, delete, 0",
                "010, update, 0",
                "010, view,   1",
                // --- statusKey=011 ---
                "011, add,    9",
                "011, copy,   0",
                "011, delete, 0",
                "011, update, 0",
                "011, view,   1",
                // --- statusKey=100 ---
                "100, add,    9",
                "100, copy,   1",
                "100, delete, 1",
                "100, update, 1",
                "100, view,   1",
                // --- statusKey=110 ---
                "110, add,    9",
                "110, copy,   0",
                "110, delete, 0",
                "110, update, 0",
                "110, view,   1",
                // --- statusKey=111 ---
                "111, add,    9",
                "111, copy,   0",
                "111, delete, 0",
                "111, update, 0",
                "111, view,   1",
                // --- statusKey=200 ---
                "200, add,    9",
                "200, copy,   1",
                "200, delete, 0",
                "200, update, 9",
                "200, view,   1",
                // --- statusKey=210 ---
                "210, add,    9",
                "210, copy,   0",
                "210, delete, 0",
                "210, update, 9",
                "210, view,   1",
                // --- statusKey=211 ---
                "211, add,    9",
                "211, copy,   0",
                "211, delete, 0",
                "211, update, 9",
                "211, view,   1",
                // --- statusKey=900 ---
                "900, add,    1",
                "900, copy,   9",
                "900, delete, 9",
                "900, update, 9",
                "900, view,   9",
                // --- statusKey=910 ---
                "910, add,    0",
                "910, copy,   9",
                "910, delete, 9",
                "910, update, 9",
                "910, view,   9",
                // --- statusKey=911 ---
                "911, add,    0",
                "911, copy,   9",
                "911, delete, 9",
                "911, update, 9",
                "911, view,   9"
        })
        @DisplayName("担当者系列: resolveOperation() 全60パターン")
        void tanSeriesAllPatterns(String statusKey, String operation, int expected) {
            assertEquals(expected,
                    StatusMatrixResolver.resolveOperation(statusKey, true, operation),
                    String.format("担当者系列: statusKey=%s, op=%s の期待値は %d", statusKey, operation, expected));
        }
    }

    // ========================================================================
    // 管理者系列 (man) — isTab010Bit2 = false — 全84パターン
    // ========================================================================

    @Nested
    @DisplayName("管理者系列 (isTab010Bit2=false) の全84パターン")
    class ManSeriesTest {

        @ParameterizedTest(name = "管理者系列 statusKey={0} op={1} → {2}")
        @CsvSource({
                // --- statusKey=000 ---
                "000, add,          9",
                "000, copy,         1",
                "000, delete,       1",
                "000, update,       1",
                "000, view,         1",
                "000, statusUpdate, 0",
                "000, statusView,   1",
                // --- statusKey=010 ---
                "010, add,          9",
                "010, copy,         1",
                "010, delete,       1",
                "010, update,       1",
                "010, view,         1",
                "010, statusUpdate, 0",
                "010, statusView,   1",
                // --- statusKey=011 ---
                "011, add,          9",
                "011, copy,         1",
                "011, delete,       1",
                "011, update,       1",
                "011, view,         1",
                "011, statusUpdate, 0",
                "011, statusView,   1",
                // --- statusKey=100 ---
                "100, add,          9",
                "100, copy,         1",
                "100, delete,       1",
                "100, update,       1",
                "100, view,         1",
                "100, statusUpdate, 1",
                "100, statusView,   1",
                // --- statusKey=110 ---
                "110, add,          9",
                "110, copy,         1",
                "110, delete,       1",
                "110, update,       1",
                "110, view,         1",
                "110, statusUpdate, 1",
                "110, statusView,   1",
                // --- statusKey=111 ---
                "111, add,          9",
                "111, copy,         1",
                "111, delete,       1",
                "111, update,       1",
                "111, view,         1",
                "111, statusUpdate, 1",
                "111, statusView,   1",
                // --- statusKey=200 ---
                "200, add,          9",
                "200, copy,         1",
                "200, delete,       0",
                "200, update,       1",
                "200, view,         1",
                "200, statusUpdate, 1",
                "200, statusView,   1",
                // --- statusKey=210 ---
                "210, add,          9",
                "210, copy,         1",
                "210, delete,       0",
                "210, update,       1",
                "210, view,         1",
                "210, statusUpdate, 1",
                "210, statusView,   1",
                // --- statusKey=211 ---
                "211, add,          9",
                "211, copy,         1",
                "211, delete,       0",
                "211, update,       1",
                "211, view,         1",
                "211, statusUpdate, 1",
                "211, statusView,   1",
                // --- statusKey=900 ---
                "900, add,          1",
                "900, copy,         9",
                "900, delete,       9",
                "900, update,       9",
                "900, view,         9",
                "900, statusUpdate, 9",
                "900, statusView,   9",
                // --- statusKey=910 ---
                "910, add,          1",
                "910, copy,         9",
                "910, delete,       9",
                "910, update,       9",
                "910, view,         9",
                "910, statusUpdate, 9",
                "910, statusView,   9",
                // --- statusKey=911 ---
                "911, add,          1",
                "911, copy,         9",
                "911, delete,       9",
                "911, update,       9",
                "911, view,         9",
                "911, statusUpdate, 9",
                "911, statusView,   9"
        })
        @DisplayName("管理者系列: resolveOperation() 全84パターン")
        void manSeriesAllPatterns(String statusKey, String operation, int expected) {
            assertEquals(expected,
                    StatusMatrixResolver.resolveOperation(statusKey, false, operation),
                    String.format("管理者系列: statusKey=%s, op=%s の期待値は %d", statusKey, operation, expected));
        }
    }

    // ========================================================================
    // resolve() メソッドのマップ検証
    // ========================================================================

    @Nested
    @DisplayName("resolve() メソッドのマップ検証")
    class ResolveMapTest {

        @Test
        @DisplayName("担当者系列: statusKey=000 で5操作が正しいマップで返される")
        void tanSeries_statusKey000_returnsCorrectMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("000", true);

            assertEquals(5, result.size(), "担当者系列のマップサイズは5");
            assertEquals(9, result.get(StatusMatrixResolver.OP_ADD));
            assertEquals(1, result.get(StatusMatrixResolver.OP_COPY));
            assertEquals(1, result.get(StatusMatrixResolver.OP_DELETE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_VIEW));
        }

        @Test
        @DisplayName("担当者系列: statusKey=200 で5操作が正しいマップで返される")
        void tanSeries_statusKey200_returnsCorrectMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("200", true);

            assertEquals(5, result.size(), "担当者系列のマップサイズは5");
            assertEquals(9, result.get(StatusMatrixResolver.OP_ADD));
            assertEquals(1, result.get(StatusMatrixResolver.OP_COPY));
            assertEquals(0, result.get(StatusMatrixResolver.OP_DELETE));
            assertEquals(9, result.get(StatusMatrixResolver.OP_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_VIEW));
        }

        @Test
        @DisplayName("担当者系列: statusKey=900 で5操作が正しいマップで返される")
        void tanSeries_statusKey900_returnsCorrectMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("900", true);

            assertEquals(5, result.size(), "担当者系列のマップサイズは5");
            assertEquals(1, result.get(StatusMatrixResolver.OP_ADD));
            assertEquals(9, result.get(StatusMatrixResolver.OP_COPY));
            assertEquals(9, result.get(StatusMatrixResolver.OP_DELETE));
            assertEquals(9, result.get(StatusMatrixResolver.OP_UPDATE));
            assertEquals(9, result.get(StatusMatrixResolver.OP_VIEW));
        }

        @Test
        @DisplayName("管理者系列: statusKey=000 で7操作が正しいマップで返される")
        void manSeries_statusKey000_returnsCorrectMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("000", false);

            assertEquals(7, result.size(), "管理者系列のマップサイズは7");
            assertEquals(9, result.get(StatusMatrixResolver.OP_ADD));
            assertEquals(1, result.get(StatusMatrixResolver.OP_COPY));
            assertEquals(1, result.get(StatusMatrixResolver.OP_DELETE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_VIEW));
            assertEquals(0, result.get(StatusMatrixResolver.OP_STATUS_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_STATUS_VIEW));
        }

        @Test
        @DisplayName("管理者系列: statusKey=100 で statusUpdate=1 となる")
        void manSeries_statusKey100_statusUpdateEnabled() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("100", false);

            assertEquals(7, result.size(), "管理者系列のマップサイズは7");
            assertEquals(9, result.get(StatusMatrixResolver.OP_ADD));
            assertEquals(1, result.get(StatusMatrixResolver.OP_COPY));
            assertEquals(1, result.get(StatusMatrixResolver.OP_DELETE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_VIEW));
            assertEquals(1, result.get(StatusMatrixResolver.OP_STATUS_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_STATUS_VIEW));
        }

        @Test
        @DisplayName("管理者系列: statusKey=200 で delete=0 となる")
        void manSeries_statusKey200_deleteDisabled() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("200", false);

            assertEquals(7, result.size(), "管理者系列のマップサイズは7");
            assertEquals(9, result.get(StatusMatrixResolver.OP_ADD));
            assertEquals(1, result.get(StatusMatrixResolver.OP_COPY));
            assertEquals(0, result.get(StatusMatrixResolver.OP_DELETE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_VIEW));
            assertEquals(1, result.get(StatusMatrixResolver.OP_STATUS_UPDATE));
            assertEquals(1, result.get(StatusMatrixResolver.OP_STATUS_VIEW));
        }

        @Test
        @DisplayName("管理者系列: statusKey=900 で add=1, 他は全て9(非表示)")
        void manSeries_statusKey900_returnsCorrectMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("900", false);

            assertEquals(7, result.size(), "管理者系列のマップサイズは7");
            assertEquals(1, result.get(StatusMatrixResolver.OP_ADD));
            assertEquals(9, result.get(StatusMatrixResolver.OP_COPY));
            assertEquals(9, result.get(StatusMatrixResolver.OP_DELETE));
            assertEquals(9, result.get(StatusMatrixResolver.OP_UPDATE));
            assertEquals(9, result.get(StatusMatrixResolver.OP_VIEW));
            assertEquals(9, result.get(StatusMatrixResolver.OP_STATUS_UPDATE));
            assertEquals(9, result.get(StatusMatrixResolver.OP_STATUS_VIEW));
        }

        @Test
        @DisplayName("担当者系列: マップに statusUpdate, statusView が含まれないこと")
        void tanSeries_doesNotContainStatusOps() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("000", true);

            assertFalse(result.containsKey(StatusMatrixResolver.OP_STATUS_UPDATE),
                    "担当者系列にstatusUpdateは含まれない");
            assertFalse(result.containsKey(StatusMatrixResolver.OP_STATUS_VIEW),
                    "担当者系列にstatusViewは含まれない");
        }

        @Test
        @DisplayName("管理者系列: マップに statusUpdate, statusView が含まれること")
        void manSeries_containsStatusOps() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("000", false);

            assertTrue(result.containsKey(StatusMatrixResolver.OP_STATUS_UPDATE),
                    "管理者系列にstatusUpdateが含まれる");
            assertTrue(result.containsKey(StatusMatrixResolver.OP_STATUS_VIEW),
                    "管理者系列にstatusViewが含まれる");
        }
    }

    // ========================================================================
    // 不明なステータスキーの検証
    // ========================================================================

    @Nested
    @DisplayName("不明なステータスキーの検証")
    class UnknownStatusKeyTest {

        @Test
        @DisplayName("不明なステータスキーで resolve() は空マップを返す（担当者系列）")
        void unknownStatusKey_tan_returnsEmptyMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("999", true);

            assertNotNull(result, "結果はnullではない");
            assertTrue(result.isEmpty(), "不明キーの場合は空マップ");
        }

        @Test
        @DisplayName("不明なステータスキーで resolve() は空マップを返す（管理者系列）")
        void unknownStatusKey_man_returnsEmptyMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("999", false);

            assertNotNull(result, "結果はnullではない");
            assertTrue(result.isEmpty(), "不明キーの場合は空マップ");
        }

        @Test
        @DisplayName("不明なステータスキーで resolveOperation() は HIDDEN(9) を返す")
        void unknownStatusKey_resolveOperation_returnsHidden() {
            assertEquals(StatusMatrixResolver.HIDDEN,
                    StatusMatrixResolver.resolveOperation("999", true, "add"),
                    "不明キーの場合はHIDDEN(9)");
            assertEquals(StatusMatrixResolver.HIDDEN,
                    StatusMatrixResolver.resolveOperation("ABC", false, "view"),
                    "不明キーの場合はHIDDEN(9)");
        }

        @Test
        @DisplayName("null ステータスキーで resolve() は空マップを返す")
        void nullStatusKey_returnsEmptyMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve(null, true);

            assertNotNull(result, "結果はnullではない");
            assertTrue(result.isEmpty(), "nullキーの場合は空マップ");
        }

        @Test
        @DisplayName("空文字ステータスキーで resolve() は空マップを返す")
        void emptyStatusKey_returnsEmptyMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("", false);

            assertNotNull(result, "結果はnullではない");
            assertTrue(result.isEmpty(), "空文字キーの場合は空マップ");
        }

        @Test
        @DisplayName("不明な操作名で resolveOperation() は HIDDEN(9) を返す")
        void unknownOperation_returnsHidden() {
            assertEquals(StatusMatrixResolver.HIDDEN,
                    StatusMatrixResolver.resolveOperation("000", true, "unknownOp"),
                    "不明な操作名の場合はHIDDEN(9)");
        }
    }

    // ========================================================================
    // resolveOperation() の利便性メソッド検証
    // ========================================================================

    @Nested
    @DisplayName("resolveOperation() 利便性メソッドの検証")
    class ResolveOperationTest {

        @Test
        @DisplayName("resolveOperation() と resolve().get() の結果が一致する（担当者系列）")
        void resolveOperation_matchesResolveMap_tan() {
            String[] ops = {
                    StatusMatrixResolver.OP_ADD,
                    StatusMatrixResolver.OP_COPY,
                    StatusMatrixResolver.OP_DELETE,
                    StatusMatrixResolver.OP_UPDATE,
                    StatusMatrixResolver.OP_VIEW
            };
            String[] keys = {"000", "010", "011", "100", "110", "111",
                    "200", "210", "211", "900", "910", "911"};

            for (String key : keys) {
                Map<String, Integer> map = StatusMatrixResolver.resolve(key, true);
                for (String op : ops) {
                    int fromMap = map.get(op);
                    int fromMethod = StatusMatrixResolver.resolveOperation(key, true, op);
                    assertEquals(fromMap, fromMethod,
                            String.format("担当者系列: key=%s, op=%s で resolve()とresolveOperation()が一致", key, op));
                }
            }
        }

        @Test
        @DisplayName("resolveOperation() と resolve().get() の結果が一致する（管理者系列）")
        void resolveOperation_matchesResolveMap_man() {
            String[] ops = {
                    StatusMatrixResolver.OP_ADD,
                    StatusMatrixResolver.OP_COPY,
                    StatusMatrixResolver.OP_DELETE,
                    StatusMatrixResolver.OP_UPDATE,
                    StatusMatrixResolver.OP_VIEW,
                    StatusMatrixResolver.OP_STATUS_UPDATE,
                    StatusMatrixResolver.OP_STATUS_VIEW
            };
            String[] keys = {"000", "010", "011", "100", "110", "111",
                    "200", "210", "211", "900", "910", "911"};

            for (String key : keys) {
                Map<String, Integer> map = StatusMatrixResolver.resolve(key, false);
                for (String op : ops) {
                    int fromMap = map.get(op);
                    int fromMethod = StatusMatrixResolver.resolveOperation(key, false, op);
                    assertEquals(fromMap, fromMethod,
                            String.format("管理者系列: key=%s, op=%s で resolve()とresolveOperation()が一致", key, op));
                }
            }
        }
    }

    // ========================================================================
    // 定数値の検証
    // ========================================================================

    @Nested
    @DisplayName("定数値の検証")
    class ConstantsTest {

        @Test
        @DisplayName("ENABLED = 1, DISABLED = 0, HIDDEN = 9 であること")
        void constantValues() {
            assertEquals(1, StatusMatrixResolver.ENABLED);
            assertEquals(0, StatusMatrixResolver.DISABLED);
            assertEquals(9, StatusMatrixResolver.HIDDEN);
        }

        @Test
        @DisplayName("操作名定数が正しい文字列であること")
        void operationConstants() {
            assertEquals("add", StatusMatrixResolver.OP_ADD);
            assertEquals("copy", StatusMatrixResolver.OP_COPY);
            assertEquals("delete", StatusMatrixResolver.OP_DELETE);
            assertEquals("update", StatusMatrixResolver.OP_UPDATE);
            assertEquals("view", StatusMatrixResolver.OP_VIEW);
            assertEquals("statusUpdate", StatusMatrixResolver.OP_STATUS_UPDATE);
            assertEquals("statusView", StatusMatrixResolver.OP_STATUS_VIEW);
        }
    }

    // ========================================================================
    // マップの不変性検証
    // ========================================================================

    @Nested
    @DisplayName("マップの不変性検証")
    class ImmutabilityTest {

        @Test
        @DisplayName("resolve() が返すマップは変更不可であること")
        void resolveReturnsUnmodifiableMap() {
            Map<String, Integer> result = StatusMatrixResolver.resolve("000", true);

            assertThrows(UnsupportedOperationException.class, () -> result.put("add", 0),
                    "返却マップへのputは例外が発生する");
        }
    }
}

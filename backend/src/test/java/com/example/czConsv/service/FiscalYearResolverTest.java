package com.example.czConsv.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FiscalYearResolver の単体テスト。
 *
 * <p>年度・半期の解決ロジックを検証する。3つの年度ルールが存在する:
 * <ul>
 *   <li>2014年以前（旧制度）: 上期=4-9月、下期=10-翌3月</li>
 *   <li>2015年（過渡期）: 上期=4-9月、下期=10-12月（3ヶ月のみ）</li>
 *   <li>2016年以降（新制度）: 上期=1-6月、下期=7-12月</li>
 * </ul>
 *
 * <p>踏襲必須ビジネスルール（年度期間ルール 3 件）を 100% カバーする。
 */
@DisplayName("FiscalYearResolver: 年度半期解決ロジック")
class FiscalYearResolverTest {

    private final FiscalYearResolver resolver = new FiscalYearResolver();

    // ========================================================================
    // getMonthList テスト
    // ========================================================================

    @Nested
    @DisplayName("getMonthList: 年度・半期に対応する月リストを返す")
    class GetMonthListTests {

        // ----------------------------------------------------------------
        // 2014年以前（旧制度: 4-9月 / 10-翌3月）
        // ----------------------------------------------------------------

        @Test
        @DisplayName("2014年 上期 → 2014-04 ~ 2014-09（6ヶ月）")
        void year2014_firstHalf() {
            List<String> result = resolver.getMonthList(2014,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("2014-04", "2014-05", "2014-06",
                            "2014-07", "2014-08", "2014-09"),
                    result,
                    "2014年上期は4月~9月の6ヶ月であること");
        }

        @Test
        @DisplayName("2014年 下期 → 2014-10 ~ 2015-03（6ヶ月、翌年3月まで）")
        void year2014_secondHalf() {
            List<String> result = resolver.getMonthList(2014,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("2014-10", "2014-11", "2014-12",
                            "2015-01", "2015-02", "2015-03"),
                    result,
                    "2014年下期は10月~翌年3月の6ヶ月であること");
        }

        @Test
        @DisplayName("2013年 上期 → 2013-04 ~ 2013-09（旧制度を適用）")
        void year2013_firstHalf() {
            List<String> result = resolver.getMonthList(2013,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("2013-04", "2013-05", "2013-06",
                            "2013-07", "2013-08", "2013-09"),
                    result,
                    "2013年上期も旧制度（4月~9月）であること");
        }

        @Test
        @DisplayName("2010年 下期 → 2010-10 ~ 2011-03（旧制度を適用）")
        void year2010_secondHalf() {
            List<String> result = resolver.getMonthList(2010,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("2010-10", "2010-11", "2010-12",
                            "2011-01", "2011-02", "2011-03"),
                    result,
                    "2010年下期も旧制度（10月~翌年3月）であること");
        }

        // ----------------------------------------------------------------
        // 2015年（過渡期: 4-9月 / 10-12月のみ）
        // ----------------------------------------------------------------

        @Test
        @DisplayName("2015年 上期 → 2015-04 ~ 2015-09（6ヶ月、旧制度と同じ）")
        void year2015_firstHalf() {
            List<String> result = resolver.getMonthList(2015,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("2015-04", "2015-05", "2015-06",
                            "2015-07", "2015-08", "2015-09"),
                    result,
                    "2015年上期は4月~9月の6ヶ月であること");
        }

        @Test
        @DisplayName("2015年 下期 → 2015-10 ~ 2015-12（特殊: 3ヶ月のみ!）")
        void year2015_secondHalf() {
            List<String> result = resolver.getMonthList(2015,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("2015-10", "2015-11", "2015-12"),
                    result,
                    "2015年下期は10月~12月の3ヶ月のみであること（過渡期特殊ルール）");
        }

        // ----------------------------------------------------------------
        // 2016年以降（新制度: 1-6月 / 7-12月）
        // ----------------------------------------------------------------

        @Test
        @DisplayName("2016年 上期 → 2016-01 ~ 2016-06（6ヶ月）")
        void year2016_firstHalf() {
            List<String> result = resolver.getMonthList(2016,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("2016-01", "2016-02", "2016-03",
                            "2016-04", "2016-05", "2016-06"),
                    result,
                    "2016年上期は1月~6月の6ヶ月であること");
        }

        @Test
        @DisplayName("2016年 下期 → 2016-07 ~ 2016-12（6ヶ月）")
        void year2016_secondHalf() {
            List<String> result = resolver.getMonthList(2016,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("2016-07", "2016-08", "2016-09",
                            "2016-10", "2016-11", "2016-12"),
                    result,
                    "2016年下期は7月~12月の6ヶ月であること");
        }

        @Test
        @DisplayName("2024年 上期 → 2024-01 ~ 2024-06（新制度を適用）")
        void year2024_firstHalf() {
            List<String> result = resolver.getMonthList(2024,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("2024-01", "2024-02", "2024-03",
                            "2024-04", "2024-05", "2024-06"),
                    result,
                    "2024年上期も新制度（1月~6月）であること");
        }

        @Test
        @DisplayName("2024年 下期 → 2024-07 ~ 2024-12（新制度を適用）")
        void year2024_secondHalf() {
            List<String> result = resolver.getMonthList(2024,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("2024-07", "2024-08", "2024-09",
                            "2024-10", "2024-11", "2024-12"),
                    result,
                    "2024年下期も新制度（7月~12月）であること");
        }

        // ----------------------------------------------------------------
        // サイズ検証（3ヶ月 vs 6ヶ月）
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "年度={0}, 半期={1} → サイズ={2}")
        @DisplayName("月リストのサイズが正しいこと")
        @CsvSource({
                "2010, FIRST,  6",
                "2010, SECOND, 6",
                "2014, FIRST,  6",
                "2014, SECOND, 6",
                "2015, FIRST,  6",
                "2015, SECOND, 3",  // 2015年下期は3ヶ月のみ!
                "2016, FIRST,  6",
                "2016, SECOND, 6",
                "2024, FIRST,  6",
                "2024, SECOND, 6"
        })
        void monthListSize(int fiscalYear, FiscalYearResolver.HalfPeriod halfPeriod,
                           int expectedSize) {
            List<String> result = resolver.getMonthList(fiscalYear, halfPeriod);

            assertEquals(expectedSize, result.size(),
                    String.format("%d年%sの月リストサイズが%dであること",
                            fiscalYear, halfPeriod, expectedSize));
        }
    }

    // ========================================================================
    // resolveFiscalYear テスト
    // ========================================================================

    @Nested
    @DisplayName("resolveFiscalYear: 年月から所属する会計年度を解決する")
    class ResolveFiscalYearTests {

        // ----------------------------------------------------------------
        // 2016年以降（新制度: 1-12月 → 同年）
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "\"{0}\" → FY{1}")
        @DisplayName("2016年以降 — 年月 → 同一会計年度")
        @CsvSource({
                "2016-01, 2016",
                "2016-06, 2016",
                "2016-07, 2016",
                "2016-12, 2016",
                "2025-06, 2025",
                "2025-07, 2025",
                "2024-01, 2024",
                "2024-12, 2024"
        })
        void newSystem_resolvesToSameYear(String yearMonth, int expectedFiscalYear) {
            assertEquals(expectedFiscalYear, resolver.resolveFiscalYear(yearMonth),
                    String.format("\"%s\" は FY%d に属すること", yearMonth, expectedFiscalYear));
        }

        // ----------------------------------------------------------------
        // 2015年（過渡期）
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "\"{0}\" → FY{1}")
        @DisplayName("2015年 — 過渡期の年度解決")
        @CsvSource({
                "2015-04, 2015",   // 上期
                "2015-09, 2015",   // 上期末
                "2015-10, 2015",   // 下期
                "2015-12, 2015"    // 下期末
        })
        void transitionalYear2015(String yearMonth, int expectedFiscalYear) {
            assertEquals(expectedFiscalYear, resolver.resolveFiscalYear(yearMonth),
                    String.format("\"%s\" は FY%d に属すること（過渡期）", yearMonth, expectedFiscalYear));
        }

        // ----------------------------------------------------------------
        // 2014年以前（旧制度: 1-3月 → 前年度に属する!）
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "\"{0}\" → FY{1}")
        @DisplayName("旧制度 — 1-3月は前年度に属する")
        @CsvSource({
                "2015-01, 2014",   // 2015年1月 → FY2014下期!
                "2015-02, 2014",
                "2015-03, 2014",
                "2014-04, 2014",   // 上期開始
                "2014-09, 2014",   // 上期末
                "2014-10, 2014",   // 下期開始
                "2014-12, 2014",   // 12月
                "2013-01, 2012",   // 2013年1月 → FY2012下期
                "2013-02, 2012",
                "2013-03, 2012",
                "2013-04, 2013",   // 2013年4月 → FY2013上期
                "2013-12, 2013"    // 2013年12月 → FY2013下期
        })
        void oldSystem_janToMarchBelongsToPreviousFiscalYear(String yearMonth,
                                                             int expectedFiscalYear) {
            assertEquals(expectedFiscalYear, resolver.resolveFiscalYear(yearMonth),
                    String.format("\"%s\" は FY%d に属すること（旧制度）", yearMonth, expectedFiscalYear));
        }

        // ----------------------------------------------------------------
        // 境界値テスト: 制度変更の境界
        // ----------------------------------------------------------------

        @Test
        @DisplayName("境界: 2015-01 → FY2014（旧制度下期に属する）")
        void boundary_2015_01_belongsTo2014() {
            assertEquals(2014, resolver.resolveFiscalYear("2015-01"),
                    "2015年1月は旧制度の FY2014 下期に属すること");
        }

        @Test
        @DisplayName("境界: 2015-03 → FY2014（旧制度下期の最終月）")
        void boundary_2015_03_belongsTo2014() {
            assertEquals(2014, resolver.resolveFiscalYear("2015-03"),
                    "2015年3月は旧制度の FY2014 下期の最終月であること");
        }

        @Test
        @DisplayName("境界: 2015-04 → FY2015（過渡期上期の開始月）")
        void boundary_2015_04_belongsTo2015() {
            assertEquals(2015, resolver.resolveFiscalYear("2015-04"),
                    "2015年4月は FY2015 過渡期上期の開始月であること");
        }

        @Test
        @DisplayName("境界: 2016-01 → FY2016（新制度の開始）")
        void boundary_2016_01_belongsTo2016() {
            assertEquals(2016, resolver.resolveFiscalYear("2016-01"),
                    "2016年1月は新制度の FY2016 上期に属すること");
        }
    }

    // ========================================================================
    // isFirstHalf テスト
    // ========================================================================

    @Nested
    @DisplayName("isFirstHalf: 年月が上期に属するかを判定する")
    class IsFirstHalfTests {

        // ----------------------------------------------------------------
        // 2016年以降（新制度: 1-6月=上期, 7-12月=下期）
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "\"{0}\" → isFirstHalf={1}")
        @DisplayName("新制度 — 1-6月が上期、7-12月が下期")
        @CsvSource({
                "2016-01, true",
                "2016-03, true",
                "2016-06, true",
                "2016-07, false",
                "2016-09, false",
                "2016-12, false",
                "2025-01, true",
                "2025-06, true",
                "2025-07, false",
                "2025-12, false"
        })
        void newSystem(String yearMonth, boolean expectedFirstHalf) {
            assertEquals(expectedFirstHalf, resolver.isFirstHalf(yearMonth),
                    String.format("\"%s\" の上期判定が %s であること", yearMonth, expectedFirstHalf));
        }

        // ----------------------------------------------------------------
        // 2015年（過渡期: 4-9月=上期, 10-12月=下期）
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "\"{0}\" → isFirstHalf={1}")
        @DisplayName("過渡期 2015年 — 4-9月が上期、10-12月が下期")
        @CsvSource({
                "2015-04, true",
                "2015-06, true",
                "2015-09, true",
                "2015-10, false",
                "2015-11, false",
                "2015-12, false"
        })
        void transitionalYear2015(String yearMonth, boolean expectedFirstHalf) {
            assertEquals(expectedFirstHalf, resolver.isFirstHalf(yearMonth),
                    String.format("\"%s\" の上期判定が %s であること（過渡期）",
                            yearMonth, expectedFirstHalf));
        }

        // ----------------------------------------------------------------
        // 2014年以前（旧制度: 4-9月=上期, 10-翌3月=下期）
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "\"{0}\" → isFirstHalf={1}")
        @DisplayName("旧制度 — 4-9月が上期、10月-翌3月が下期")
        @CsvSource({
                "2014-04, true",
                "2014-06, true",
                "2014-09, true",
                "2014-10, false",
                "2014-12, false",
                "2015-01, false",   // FY2014下期に属する
                "2015-02, false",
                "2015-03, false",
                "2013-04, true",
                "2013-09, true",
                "2013-10, false",
                "2013-01, false"    // FY2012下期に属する
        })
        void oldSystem(String yearMonth, boolean expectedFirstHalf) {
            assertEquals(expectedFirstHalf, resolver.isFirstHalf(yearMonth),
                    String.format("\"%s\" の上期判定が %s であること（旧制度）",
                            yearMonth, expectedFirstHalf));
        }
    }

    // ========================================================================
    // getMonthLabels テスト
    // ========================================================================

    @Nested
    @DisplayName("getMonthLabels: 月ラベル（表示用）を返す")
    class GetMonthLabelsTests {

        @Test
        @DisplayName("2016年 上期 → [\"01月\",\"02月\",\"03月\",\"04月\",\"05月\",\"06月\"]")
        void year2016_firstHalf_labels() {
            List<String> result = resolver.getMonthLabels(2016,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("01月", "02月", "03月", "04月", "05月", "06月"),
                    result,
                    "2016年上期のラベルが01月~06月であること");
        }

        @Test
        @DisplayName("2016年 下期 → [\"07月\",\"08月\",\"09月\",\"10月\",\"11月\",\"12月\"]")
        void year2016_secondHalf_labels() {
            List<String> result = resolver.getMonthLabels(2016,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("07月", "08月", "09月", "10月", "11月", "12月"),
                    result,
                    "2016年下期のラベルが07月~12月であること");
        }

        @Test
        @DisplayName("2015年 下期 → [\"10月\",\"11月\",\"12月\"]（3ヶ月のみ!）")
        void year2015_secondHalf_labels() {
            List<String> result = resolver.getMonthLabels(2015,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("10月", "11月", "12月"),
                    result,
                    "2015年下期のラベルは10月~12月の3つのみであること（過渡期特殊ルール）");
        }

        @Test
        @DisplayName("2015年 上期 → [\"04月\",\"05月\",\"06月\",\"07月\",\"08月\",\"09月\"]")
        void year2015_firstHalf_labels() {
            List<String> result = resolver.getMonthLabels(2015,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("04月", "05月", "06月", "07月", "08月", "09月"),
                    result,
                    "2015年上期のラベルが04月~09月であること");
        }

        @Test
        @DisplayName("2014年 上期 → [\"04月\",\"05月\",\"06月\",\"07月\",\"08月\",\"09月\"]")
        void year2014_firstHalf_labels() {
            List<String> result = resolver.getMonthLabels(2014,
                    FiscalYearResolver.HalfPeriod.FIRST);

            assertEquals(
                    List.of("04月", "05月", "06月", "07月", "08月", "09月"),
                    result,
                    "2014年上期のラベルが04月~09月であること");
        }

        @Test
        @DisplayName("2014年 下期 → [\"10月\",\"11月\",\"12月\",\"01月\",\"02月\",\"03月\"]")
        void year2014_secondHalf_labels() {
            List<String> result = resolver.getMonthLabels(2014,
                    FiscalYearResolver.HalfPeriod.SECOND);

            assertEquals(
                    List.of("10月", "11月", "12月", "01月", "02月", "03月"),
                    result,
                    "2014年下期のラベルが10月~翌年03月であること");
        }

        // ----------------------------------------------------------------
        // ラベル数とリストサイズの一致
        // ----------------------------------------------------------------

        @ParameterizedTest(name = "年度={0}, 半期={1}")
        @DisplayName("月ラベル数が月リスト数と一致すること")
        @CsvSource({
                "2010, FIRST",
                "2010, SECOND",
                "2014, FIRST",
                "2014, SECOND",
                "2015, FIRST",
                "2015, SECOND",
                "2016, FIRST",
                "2016, SECOND",
                "2024, FIRST",
                "2024, SECOND"
        })
        void labelCountMatchesMonthListCount(int fiscalYear,
                                              FiscalYearResolver.HalfPeriod halfPeriod) {
            List<String> months = resolver.getMonthList(fiscalYear, halfPeriod);
            List<String> labels = resolver.getMonthLabels(fiscalYear, halfPeriod);

            assertEquals(months.size(), labels.size(),
                    String.format("%d年%sの月ラベル数が月リスト数と一致すること",
                            fiscalYear, halfPeriod));
        }
    }

    // ========================================================================
    // 整合性テスト: メソッド間の一貫性
    // ========================================================================

    @Nested
    @DisplayName("整合性テスト: 各メソッドの結果が一貫していること")
    class ConsistencyTests {

        /**
         * getMonthList で返される全月が resolveFiscalYear で同じ年度に解決されることを検証。
         */
        @ParameterizedTest(name = "年度={0}, 半期={1}")
        @DisplayName("getMonthList の全月が resolveFiscalYear で同一年度に解決される")
        @CsvSource({
                "2010, FIRST",
                "2010, SECOND",
                "2014, FIRST",
                "2014, SECOND",
                "2015, FIRST",
                "2015, SECOND",
                "2016, FIRST",
                "2016, SECOND",
                "2024, FIRST",
                "2024, SECOND"
        })
        void allMonthsResolveToSameFiscalYear(int fiscalYear,
                                               FiscalYearResolver.HalfPeriod halfPeriod) {
            List<String> months = resolver.getMonthList(fiscalYear, halfPeriod);

            for (String month : months) {
                assertEquals(fiscalYear, resolver.resolveFiscalYear(month),
                        String.format("月 \"%s\" は FY%d に解決されること", month, fiscalYear));
            }
        }

        /**
         * getMonthList(FIRST) の全月が isFirstHalf=true であることを検証。
         */
        @ParameterizedTest(name = "年度={0}")
        @DisplayName("上期の月リストの全月が isFirstHalf=true")
        @CsvSource({
                "2010", "2013", "2014", "2015", "2016", "2024"
        })
        void firstHalfMonthsAreAllFirstHalf(int fiscalYear) {
            List<String> months = resolver.getMonthList(fiscalYear,
                    FiscalYearResolver.HalfPeriod.FIRST);

            for (String month : months) {
                assertTrue(resolver.isFirstHalf(month),
                        String.format("上期月 \"%s\" は isFirstHalf=true であること", month));
            }
        }

        /**
         * getMonthList(SECOND) の全月が isFirstHalf=false であることを検証。
         */
        @ParameterizedTest(name = "年度={0}")
        @DisplayName("下期の月リストの全月が isFirstHalf=false")
        @CsvSource({
                "2010", "2013", "2014", "2015", "2016", "2024"
        })
        void secondHalfMonthsAreAllSecondHalf(int fiscalYear) {
            List<String> months = resolver.getMonthList(fiscalYear,
                    FiscalYearResolver.HalfPeriod.SECOND);

            for (String month : months) {
                assertFalse(resolver.isFirstHalf(month),
                        String.format("下期月 \"%s\" は isFirstHalf=false であること", month));
            }
        }
    }
}

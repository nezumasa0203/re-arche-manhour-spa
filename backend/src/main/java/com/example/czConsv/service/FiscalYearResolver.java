package com.example.czConsv.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 年度・半期解決サービス。
 *
 * <p>CZ システムの年度・半期マッピングは年代によって異なる:
 * <ul>
 *   <li><b>2014年以前（旧制度）</b>: 上期=4-9月、下期=10月-翌年3月</li>
 *   <li><b>2015年（過渡期）</b>: 上期=4-9月、下期=10-12月（3ヶ月のみ）</li>
 *   <li><b>2016年以降（新制度）</b>: 上期=1-6月、下期=7-12月</li>
 * </ul>
 *
 * <p>踏襲必須ビジネスルール（年度期間ルール 3 件）に基づく実装。
 * 05_gap_analysis.md セクション 4 参照。
 */
@Service
public class FiscalYearResolver {

    /**
     * 半期を表す列挙型。
     */
    public enum HalfPeriod {
        /** 上期 */
        FIRST,
        /** 下期 */
        SECOND
    }

    /**
     * 指定した会計年度・半期に対応する年月リストを返す。
     *
     * <p>戻り値のフォーマットは "yyyy-MM"（例: "2016-01"）。
     *
     * <p>例:
     * <ul>
     *   <li>{@code getMonthList(2016, FIRST)} → ["2016-01", ..., "2016-06"]</li>
     *   <li>{@code getMonthList(2015, SECOND)} → ["2015-10", "2015-11", "2015-12"]（3ヶ月）</li>
     *   <li>{@code getMonthList(2014, SECOND)} → ["2014-10", ..., "2015-03"]（翌年3月まで）</li>
     * </ul>
     *
     * @param fiscalYear 会計年度
     * @param halfPeriod 半期（FIRST=上期, SECOND=下期）
     * @return 年月文字列のリスト（不変）
     */
    public List<String> getMonthList(int fiscalYear, HalfPeriod halfPeriod) {
        List<String> months = new ArrayList<>();

        if (fiscalYear >= 2016) {
            // 新制度: 上期 1-6月、下期 7-12月
            if (halfPeriod == HalfPeriod.FIRST) {
                for (int m = 1; m <= 6; m++) {
                    months.add(formatYearMonth(fiscalYear, m));
                }
            } else {
                for (int m = 7; m <= 12; m++) {
                    months.add(formatYearMonth(fiscalYear, m));
                }
            }
        } else if (fiscalYear == 2015) {
            // 過渡期: 上期 4-9月、下期 10-12月（3ヶ月のみ）
            if (halfPeriod == HalfPeriod.FIRST) {
                for (int m = 4; m <= 9; m++) {
                    months.add(formatYearMonth(2015, m));
                }
            } else {
                for (int m = 10; m <= 12; m++) {
                    months.add(formatYearMonth(2015, m));
                }
            }
        } else {
            // 旧制度（2014年以前）: 上期 4-9月、下期 10月-翌年3月
            if (halfPeriod == HalfPeriod.FIRST) {
                for (int m = 4; m <= 9; m++) {
                    months.add(formatYearMonth(fiscalYear, m));
                }
            } else {
                for (int m = 10; m <= 12; m++) {
                    months.add(formatYearMonth(fiscalYear, m));
                }
                for (int m = 1; m <= 3; m++) {
                    months.add(formatYearMonth(fiscalYear + 1, m));
                }
            }
        }

        return Collections.unmodifiableList(months);
    }

    /**
     * 指定した年月が属する会計年度を解決する。
     *
     * <p>例:
     * <ul>
     *   <li>{@code resolveFiscalYear("2016-03")} → 2016</li>
     *   <li>{@code resolveFiscalYear("2015-01")} → 2014（旧制度下期に属する）</li>
     *   <li>{@code resolveFiscalYear("2015-10")} → 2015</li>
     * </ul>
     *
     * @param yearMonth 年月文字列（"yyyy-MM" 形式）
     * @return 所属する会計年度
     */
    public int resolveFiscalYear(String yearMonth) {
        int[] parsed = parseYearMonth(yearMonth);
        int year = parsed[0];
        int month = parsed[1];

        if (year >= 2016) {
            // 新制度: 年 = 会計年度
            return year;
        }

        if (year == 2015 && month >= 4) {
            // 2015年4月以降 → FY2015（過渡期）
            return 2015;
        }

        // 旧制度の判定（2015年1-3月を含む）
        // 旧制度では4月始まり: 4-12月 → 同年、1-3月 → 前年
        if (month >= 4) {
            return year;
        } else {
            // 1-3月は前年度に属する
            return year - 1;
        }
    }

    /**
     * 指定した年月が上期に属するかを判定する。
     *
     * <p>例:
     * <ul>
     *   <li>{@code isFirstHalf("2016-03")} → true（新制度: 1-6月は上期）</li>
     *   <li>{@code isFirstHalf("2014-06")} → true（旧制度: 4-9月は上期）</li>
     *   <li>{@code isFirstHalf("2014-12")} → false（旧制度: 10-翌3月は下期）</li>
     *   <li>{@code isFirstHalf("2015-01")} → false（FY2014下期に属する）</li>
     * </ul>
     *
     * @param yearMonth 年月文字列（"yyyy-MM" 形式）
     * @return 上期に属する場合 true
     */
    public boolean isFirstHalf(String yearMonth) {
        int[] parsed = parseYearMonth(yearMonth);
        int year = parsed[0];
        int month = parsed[1];

        if (year >= 2016) {
            // 新制度: 1-6月=上期、7-12月=下期
            return month <= 6;
        }

        if (year == 2015 && month >= 4) {
            // 過渡期: 4-9月=上期、10-12月=下期
            return month <= 9;
        }

        // 旧制度（2015年1-3月を含む）
        // 4-9月=上期、10-12月 and 1-3月=下期
        return month >= 4 && month <= 9;
    }

    /**
     * 表示用の月ラベルリストを返す。
     *
     * <p>{@link #getMonthList(int, HalfPeriod)} の各月に対応するラベル
     * （例: "01月", "02月", ...）を返す。
     *
     * @param fiscalYear 会計年度
     * @param halfPeriod 半期（FIRST=上期, SECOND=下期）
     * @return 月ラベル文字列のリスト（不変）
     */
    public List<String> getMonthLabels(int fiscalYear, HalfPeriod halfPeriod) {
        List<String> months = getMonthList(fiscalYear, halfPeriod);
        List<String> labels = new ArrayList<>(months.size());

        for (String yearMonth : months) {
            // "yyyy-MM" から "MM月" を生成
            String monthPart = yearMonth.substring(5); // "MM"
            labels.add(monthPart + "月");
        }

        return Collections.unmodifiableList(labels);
    }

    // ========================================================================
    // 内部ヘルパー
    // ========================================================================

    /**
     * 年月文字列を年と月の配列にパースする。
     *
     * @param yearMonth "yyyy-MM" 形式の文字列
     * @return [年, 月] の配列
     */
    private int[] parseYearMonth(String yearMonth) {
        String[] parts = yearMonth.split("-");
        return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1])
        };
    }

    /**
     * 年と月から "yyyy-MM" 形式の文字列を生成する。
     *
     * @param year 年
     * @param month 月（1-12）
     * @return "yyyy-MM" 形式の文字列
     */
    private String formatYearMonth(int year, int month) {
        return String.format("%04d-%02d", year, month);
    }
}

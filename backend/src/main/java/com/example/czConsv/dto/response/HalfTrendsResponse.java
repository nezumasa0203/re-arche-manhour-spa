package com.example.czConsv.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /half-trends/* レスポンス DTO。
 *
 * <p>半期トレンド（6ヶ月推移）データを返す。
 */
public record HalfTrendsResponse(
        List<HalfTrendsRow> rows,
        DrilldownContext drilldown
) {

    /**
     * トレンド行（サブシステムまたはカテゴリ単位）。
     */
    public record HalfTrendsRow(
            String key,
            String label,
            List<MonthValue> months,
            BigDecimal total
    ) {
    }

    /**
     * 月別値。
     */
    public record MonthValue(
            String yearMonth,
            BigDecimal value
    ) {
    }

    /**
     * ドリルダウンコンテキスト（上位の絞り込み条件）。
     */
    public record DrilldownContext(
            String yearHalf,
            String systemNo,
            String subsystemNo,
            String categoryCode
    ) {
    }
}

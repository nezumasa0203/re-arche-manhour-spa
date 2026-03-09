package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz13SubsysSumDao;
import com.example.czConsv.dto.response.HalfTrendsResponse;
import com.example.czConsv.dto.response.HalfTrendsResponse.DrilldownContext;
import com.example.czConsv.dto.response.HalfTrendsResponse.HalfTrendsRow;
import com.example.czConsv.dto.response.HalfTrendsResponse.MonthValue;
import com.example.czConsv.entity.Tcz13SubsysSum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 半期トレンド集計サービス。
 *
 * <p>3 階層ドリルダウン集計を提供する:
 * <ul>
 *   <li>STEP_0: {@link #getCategories} — 分類別（hsKategoriId）集計</li>
 *   <li>STEP_1: {@link #getSystems} — システム別（sknno）集計</li>
 *   <li>STEP_2: {@link #getSubsystems} — サブシステム別（subsknno）集計</li>
 * </ul>
 *
 * <p>yearHalf パラメータ形式: {@code "YYYY-H"}（例: {@code "2016-1"} = FY2016 上期）。
 * FiscalYearResolver により 2015 年度の特殊 3 ヶ月下期を含む全パターンを正確に処理する。
 */
@Service
public class HalfTrendsService {

    private final Tcz13SubsysSumDao subsysSumDao;
    private final FiscalYearResolver fiscalYearResolver;

    public HalfTrendsService(Tcz13SubsysSumDao subsysSumDao,
                              FiscalYearResolver fiscalYearResolver) {
        this.subsysSumDao = subsysSumDao;
        this.fiscalYearResolver = fiscalYearResolver;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * STEP_0: 分類別（hsKategoriId）集計を返す。
     *
     * @param yearHalf    年度半期（"YYYY-H" 形式）
     * @param displayMode "hours" または "cost"
     * @param filterType  "all" / "system" / "my"
     * @return HalfTrendsResponse
     */
    public HalfTrendsResponse getCategories(String yearHalf, String displayMode, String filterType) {
        List<String> months = resolveMonths(yearHalf);
        List<Tcz13SubsysSum> records = fetchFiltered(months, filterType);

        Map<String, List<Tcz13SubsysSum>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.hsKategoriId,
                        LinkedHashMap::new, Collectors.toList()));

        List<HalfTrendsRow> rows = buildRows(grouped, months, displayMode);
        DrilldownContext drilldown = new DrilldownContext(yearHalf, null, null, null);
        return new HalfTrendsResponse(rows, drilldown);
    }

    /**
     * STEP_1: カテゴリで絞り込み、システム別（sknno）集計を返す。
     *
     * @param yearHalf     年度半期（"YYYY-H" 形式）
     * @param categoryCode 上位カテゴリコード（hsKategoriId）
     * @param displayMode  "hours" または "cost"
     * @param filterType   "all" / "system" / "my"
     * @return HalfTrendsResponse
     */
    public HalfTrendsResponse getSystems(String yearHalf, String categoryCode,
                                          String displayMode, String filterType) {
        List<String> months = resolveMonths(yearHalf);
        List<Tcz13SubsysSum> records = fetchFiltered(months, filterType).stream()
                .filter(r -> categoryCode.equals(r.hsKategoriId))
                .collect(Collectors.toList());

        Map<String, List<Tcz13SubsysSum>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.sknno,
                        LinkedHashMap::new, Collectors.toList()));

        List<HalfTrendsRow> rows = buildRows(grouped, months, displayMode);
        DrilldownContext drilldown = new DrilldownContext(yearHalf, null, null, categoryCode);
        return new HalfTrendsResponse(rows, drilldown);
    }

    /**
     * STEP_2: カテゴリ＋システムで絞り込み、サブシステム別（subsknno）集計を返す。
     *
     * @param yearHalf     年度半期（"YYYY-H" 形式）
     * @param systemNo     上位システム番号（sknno）
     * @param categoryCode 上位カテゴリコード（hsKategoriId）
     * @param displayMode  "hours" または "cost"
     * @param filterType   "all" / "system" / "my"
     * @return HalfTrendsResponse
     */
    public HalfTrendsResponse getSubsystems(String yearHalf, String systemNo, String categoryCode,
                                             String displayMode, String filterType) {
        List<String> months = resolveMonths(yearHalf);
        List<Tcz13SubsysSum> records = fetchFiltered(months, filterType).stream()
                .filter(r -> categoryCode.equals(r.hsKategoriId))
                .filter(r -> systemNo.equals(r.sknno))
                .collect(Collectors.toList());

        Map<String, List<Tcz13SubsysSum>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.subsknno,
                        LinkedHashMap::new, Collectors.toList()));

        List<HalfTrendsRow> rows = buildRows(grouped, months, displayMode);
        DrilldownContext drilldown = new DrilldownContext(yearHalf, systemNo, null, categoryCode);
        return new HalfTrendsResponse(rows, drilldown);
    }

    // =========================================================================
    // 内部ヘルパー
    // =========================================================================

    /**
     * yearHalf 文字列（"YYYY-H"）を月リストに変換する。
     * FiscalYearResolver に委譲して 2015 年度特殊ケースも処理する。
     */
    private List<String> resolveMonths(String yearHalf) {
        String[] parts = yearHalf.split("-");
        int fiscalYear = Integer.parseInt(parts[0]);
        int half = Integer.parseInt(parts[1]);
        FiscalYearResolver.HalfPeriod period =
                half == 1 ? FiscalYearResolver.HalfPeriod.FIRST : FiscalYearResolver.HalfPeriod.SECOND;
        return fiscalYearResolver.getMonthList(fiscalYear, period);
    }

    /**
     * 全レコードを取得し、対象月・論理削除を除外して返す。
     * filterType "system"/"my" は将来的に CzSecurityContext 連携で対応予定。
     */
    private List<Tcz13SubsysSum> fetchFiltered(List<String> months, String filterType) {
        Set<String> monthKeys = months.stream()
                .map(m -> m.replace("-", ""))
                .collect(Collectors.toSet());

        return subsysSumDao.selectAll().stream()
                .filter(r -> "0".equals(r.delflg))
                .filter(r -> monthKeys.contains(r.yyyymm))
                .collect(Collectors.toList());
    }

    /**
     * グループ化済みマップから HalfTrendsRow リストを構築する。
     * 各行に全月の MonthValue（データなし月は BigDecimal.ZERO）と合計を付与する。
     */
    private List<HalfTrendsRow> buildRows(Map<String, List<Tcz13SubsysSum>> grouped,
                                           List<String> months, String displayMode) {
        List<HalfTrendsRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<Tcz13SubsysSum>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            List<Tcz13SubsysSum> recs = entry.getValue();

            // 月ごとに集計マップを作成
            Map<String, BigDecimal> monthTotals = new LinkedHashMap<>();
            for (String m : months) {
                monthTotals.put(m, BigDecimal.ZERO);
            }
            for (Tcz13SubsysSum r : recs) {
                String yearMonth = r.yyyymm.substring(0, 4) + "-" + r.yyyymm.substring(4);
                BigDecimal value = selectValue(r, displayMode);
                monthTotals.merge(yearMonth, value, BigDecimal::add);
            }

            List<MonthValue> monthValues = months.stream()
                    .map(m -> new MonthValue(m, monthTotals.getOrDefault(m, BigDecimal.ZERO)))
                    .collect(Collectors.toList());

            BigDecimal total = monthValues.stream()
                    .map(MonthValue::value)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(new HalfTrendsRow(key, key, monthValues, total));
        }

        rows.sort(Comparator.comparing(HalfTrendsRow::key));
        return rows;
    }

    /** displayMode に応じた集計値を選択する。現時点では "hours" のみ実装。 */
    private BigDecimal selectValue(Tcz13SubsysSum r, String displayMode) {
        // "cost" 対応は別フィールド追加後に拡張
        return r.hsKousuu != null ? r.hsKousuu : BigDecimal.ZERO;
    }
}

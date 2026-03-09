package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
import com.example.czConsv.dao.Tcz13SubsysSumDao;
import com.example.czConsv.dto.response.MonthlyBreakdownResponse;
import com.example.czConsv.dto.response.MonthlyBreakdownResponse.BreakdownContext;
import com.example.czConsv.dto.response.MonthlyBreakdownResponse.BreakdownRow;
import com.example.czConsv.dto.response.MonthlyBreakdownResponse.MonthValue;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.entity.Tcz13SubsysSum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 月別内訳集計サービス。
 *
 * <p>単一月を指定した 3 階層ドリルダウン集計と詳細取得を提供する:
 * <ul>
 *   <li>STEP_0: {@link #getCategories} — 分類別（hsKategoriId）集計</li>
 *   <li>STEP_1: {@link #getSystems} — システム別（sknno）集計</li>
 *   <li>STEP_2: {@link #getSubsystems} — サブシステム別（subsknno）集計</li>
 *   <li>DETAIL: {@link #getDetail} — 個別工数（Tcz01HosyuKousuu）の集計</li>
 * </ul>
 *
 * <p>yearMonth パラメータ形式: {@code "yyyy-MM"}（例: {@code "2025-02"}）。
 */
@Service
public class MonthlyBreakdownService {

    private final Tcz13SubsysSumDao subsysSumDao;
    private final Tcz01HosyuKousuuDao workHoursDao;

    public MonthlyBreakdownService(Tcz13SubsysSumDao subsysSumDao,
                                    Tcz01HosyuKousuuDao workHoursDao) {
        this.subsysSumDao = subsysSumDao;
        this.workHoursDao = workHoursDao;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * STEP_0: 分類別（hsKategoriId）集計を返す。
     *
     * @param yearMonth   対象年月（"yyyy-MM" 形式）
     * @param displayMode "hours" または "cost"
     * @param filterType  "all" / "system" / "my"
     * @return MonthlyBreakdownResponse
     */
    public MonthlyBreakdownResponse getCategories(String yearMonth, String displayMode, String filterType) {
        List<Tcz13SubsysSum> records = fetchSumFiltered(yearMonth, filterType);

        Map<String, List<Tcz13SubsysSum>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.hsKategoriId,
                        LinkedHashMap::new, Collectors.toList()));

        List<BreakdownRow> rows = buildSumRows(grouped, yearMonth, displayMode);
        BreakdownContext ctx = new BreakdownContext(yearMonth, null, null, null);
        return new MonthlyBreakdownResponse(rows, ctx);
    }

    /**
     * STEP_1: カテゴリで絞り込み、システム別（sknno）集計を返す。
     *
     * @param yearMonth    対象年月（"yyyy-MM" 形式）
     * @param categoryCode 上位カテゴリコード（hsKategoriId）
     * @param displayMode  "hours" または "cost"
     * @param filterType   "all" / "system" / "my"
     * @return MonthlyBreakdownResponse
     */
    public MonthlyBreakdownResponse getSystems(String yearMonth, String categoryCode,
                                                String displayMode, String filterType) {
        List<Tcz13SubsysSum> records = fetchSumFiltered(yearMonth, filterType).stream()
                .filter(r -> categoryCode.equals(r.hsKategoriId))
                .collect(Collectors.toList());

        Map<String, List<Tcz13SubsysSum>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.sknno,
                        LinkedHashMap::new, Collectors.toList()));

        List<BreakdownRow> rows = buildSumRows(grouped, yearMonth, displayMode);
        BreakdownContext ctx = new BreakdownContext(yearMonth, null, null, categoryCode);
        return new MonthlyBreakdownResponse(rows, ctx);
    }

    /**
     * STEP_2: カテゴリ＋システムで絞り込み、サブシステム別（subsknno）集計を返す。
     *
     * @param yearMonth    対象年月（"yyyy-MM" 形式）
     * @param systemNo     上位システム番号（sknno）
     * @param categoryCode 上位カテゴリコード（hsKategoriId）
     * @param displayMode  "hours" または "cost"
     * @param filterType   "all" / "system" / "my"
     * @return MonthlyBreakdownResponse
     */
    public MonthlyBreakdownResponse getSubsystems(String yearMonth, String systemNo, String categoryCode,
                                                   String displayMode, String filterType) {
        List<Tcz13SubsysSum> records = fetchSumFiltered(yearMonth, filterType).stream()
                .filter(r -> categoryCode.equals(r.hsKategoriId))
                .filter(r -> systemNo.equals(r.sknno))
                .collect(Collectors.toList());

        Map<String, List<Tcz13SubsysSum>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.subsknno,
                        LinkedHashMap::new, Collectors.toList()));

        List<BreakdownRow> rows = buildSumRows(grouped, yearMonth, displayMode);
        BreakdownContext ctx = new BreakdownContext(yearMonth, systemNo, null, categoryCode);
        return new MonthlyBreakdownResponse(rows, ctx);
    }

    /**
     * DETAIL: 個別工数（Tcz01HosyuKousuu）をカテゴリ＋システム＋サブシステムで絞り込み、
     * 集計合計を返す。
     *
     * @param yearMonth    対象年月（"yyyy-MM" 形式）
     * @param systemNo     システム番号（taisyoSknno）
     * @param subsystemNo  サブシステム番号（taisyoSubsysno）
     * @param categoryCode カテゴリコード（hsKategori）
     * @param displayMode  "hours" または "cost"
     * @param filterType   "all" / "system" / "my"
     * @return MonthlyBreakdownResponse
     */
    public MonthlyBreakdownResponse getDetail(String yearMonth, String systemNo, String subsystemNo,
                                               String categoryCode, String displayMode, String filterType) {
        String yyyymm = yearMonth.replace("-", "");

        List<Tcz01HosyuKousuu> records = workHoursDao.selectAll().stream()
                .filter(r -> "0".equals(r.delflg))
                .filter(r -> yyyymm.equals(r.yearHalf))
                .filter(r -> systemNo.equals(r.taisyoSknno))
                .filter(r -> subsystemNo.equals(r.taisyoSubsysno))
                .filter(r -> categoryCode.equals(r.hsKategori))
                .collect(Collectors.toList());

        BigDecimal total = records.stream()
                .map(r -> r.kousuu != null ? r.kousuu : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MonthValue mv = new MonthValue(yearMonth, total);
        BreakdownRow row = new BreakdownRow(categoryCode, categoryCode, List.of(mv), total);
        List<BreakdownRow> rows = records.isEmpty() ? List.of() : List.of(row);

        BreakdownContext ctx = new BreakdownContext(yearMonth, systemNo, subsystemNo, categoryCode);
        return new MonthlyBreakdownResponse(rows, ctx);
    }

    // =========================================================================
    // 内部ヘルパー
    // =========================================================================

    /** Tcz13SubsysSum を対象月・論理削除でフィルタして返す。 */
    private List<Tcz13SubsysSum> fetchSumFiltered(String yearMonth, String filterType) {
        String yyyymm = yearMonth.replace("-", "");
        return subsysSumDao.selectAll().stream()
                .filter(r -> "0".equals(r.delflg))
                .filter(r -> yyyymm.equals(r.yyyymm))
                .collect(Collectors.toList());
    }

    /** グループ化済みマップから BreakdownRow リストを構築する（単一月）。 */
    private List<BreakdownRow> buildSumRows(Map<String, List<Tcz13SubsysSum>> grouped,
                                             String yearMonth, String displayMode) {
        List<BreakdownRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<Tcz13SubsysSum>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            BigDecimal monthTotal = entry.getValue().stream()
                    .map(r -> r.hsKousuu != null ? r.hsKousuu : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            MonthValue mv = new MonthValue(yearMonth, monthTotal);
            rows.add(new BreakdownRow(key, key, List.of(mv), monthTotal));
        }

        rows.sort(Comparator.comparing(BreakdownRow::key));
        return rows;
    }
}

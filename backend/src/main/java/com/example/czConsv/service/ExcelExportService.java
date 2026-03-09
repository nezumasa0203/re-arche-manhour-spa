package com.example.czConsv.service;

import com.example.czConsv.dto.response.HalfTrendsResponse;
import com.example.czConsv.dto.response.MonthlyBreakdownResponse;
import com.example.czConsv.dto.response.WorkHoursListResponse;
import com.example.czConsv.dto.response.WorkStatusListResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Excel (.xlsx) エクスポートサービス。
 *
 * <p>6 テンプレートの .xlsx ファイルを生成する:
 * <ol>
 *   <li>{@link #exportWorkHoursDetail} — 工数明細</li>
 *   <li>{@link #exportWorkStatus} — 工数状況一覧</li>
 *   <li>{@link #exportHalfTrends} — 半期推移</li>
 *   <li>{@link #exportMonthlyBreakdownStandard} — 月別内訳（標準）</li>
 *   <li>{@link #exportMonthlyBreakdownManagement} — 月別内訳（管理用）</li>
 *   <li>{@link #exportMonthlyBreakdownDetail} — 月別内訳（管理詳細）</li>
 * </ol>
 *
 * <p>SXSSFWorkbook（ストリーミング）を使用して大量データ時のメモリ消費を抑制する。
 * 行数上限は {@link #MAX_ROWS} で設定。
 */
@Service
public class ExcelExportService {

    /** 1 シートあたりの最大行数（超過時は切り捨て）。 */
    static final int MAX_ROWS = 10_000;

    /** SXSSF のウィンドウサイズ（メモリ保持行数）。 */
    private static final int SXSSF_WINDOW = 100;

    // =========================================================================
    // Public API — 6 テンプレート
    // =========================================================================

    /**
     * 工数明細 (.xlsx) を生成する。
     *
     * @param records   工数レコードリスト
     * @param yearMonth 対象年月（"yyyy-MM" 形式）
     * @return xlsx バイト配列
     */
    public byte[] exportWorkHoursDetail(List<WorkHoursListResponse.WorkHoursRecord> records,
                                         String yearMonth) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(SXSSF_WINDOW)) {
            Sheet sheet = wb.createSheet("工数明細_" + yearMonth.replace("-", ""));

            String[] headers = {"作業日", "対象サブシステム", "カテゴリ", "件名", "工数(h)", "TMR番号", "依頼書No", "依頼者名", "ステータス"};
            createHeaderRow(wb, sheet, headers);

            BigDecimal totalHours = BigDecimal.ZERO;
            int rowNum = 1;
            for (WorkHoursListResponse.WorkHoursRecord r : records) {
                if (rowNum > MAX_ROWS) {
                    break;
                }
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, r.workDate());
                setCell(row, 1, r.targetSubsystem() != null ? r.targetSubsystem().subsystemName() : "");
                setCell(row, 2, r.category() != null ? r.category().categoryName() : "");
                setCell(row, 3, r.subject());
                BigDecimal hours = parseHours(r.hours());
                setCellNum(row, 4, hours.doubleValue());
                setCell(row, 5, r.tmrNo());
                setCell(row, 6, r.workRequestNo());
                setCell(row, 7, r.workRequesterName());
                setCell(row, 8, r.status());
                totalHours = totalHours.add(hours);
            }

            Row totalRow = sheet.createRow(rowNum);
            setCell(totalRow, 3, "合計");
            setCellNum(totalRow, 4, totalHours.doubleValue());

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("工数明細 Excel 生成に失敗しました", e);
        }
    }

    /**
     * 工数状況一覧 (.xlsx) を生成する。
     *
     * @param records   工数状況レコードリスト
     * @param yearMonth 対象年月（"yyyy-MM" 形式）
     * @return xlsx バイト配列
     */
    public byte[] exportWorkStatus(List<WorkStatusListResponse.WorkStatusRecord> records,
                                    String yearMonth) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(SXSSF_WINDOW)) {
            Sheet sheet = wb.createSheet("工数状況_" + yearMonth.replace("-", ""));

            String[] headers = {"担当者ID", "担当者名", "組織コード", "組織名", "対象年月", "ステータス", "合計工数(h)", "件数"};
            createHeaderRow(wb, sheet, headers);

            BigDecimal totalHours = BigDecimal.ZERO;
            int rowNum = 1;
            for (WorkStatusListResponse.WorkStatusRecord r : records) {
                if (rowNum > MAX_ROWS) {
                    break;
                }
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, r.staffId());
                setCell(row, 1, r.staffName());
                setCell(row, 2, r.organizationCode());
                setCell(row, 3, r.organizationName());
                setCell(row, 4, r.yearMonth());
                setCell(row, 5, r.status());
                BigDecimal hours = r.totalHours() != null ? r.totalHours() : BigDecimal.ZERO;
                setCellNum(row, 6, hours.doubleValue());
                setCellNum(row, 7, r.recordCount());
                totalHours = totalHours.add(hours);
            }

            Row totalRow = sheet.createRow(rowNum);
            setCell(totalRow, 1, "合計");
            setCellNum(totalRow, 6, totalHours.doubleValue());

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("工数状況一覧 Excel 生成に失敗しました", e);
        }
    }

    /**
     * 半期推移 (.xlsx) を生成する。
     *
     * @param response HalfTrendsResponse
     * @param yearHalf 年度半期（例: "2025-1"）
     * @return xlsx バイト配列
     */
    public byte[] exportHalfTrends(HalfTrendsResponse response, String yearHalf) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(SXSSF_WINDOW)) {
            Sheet sheet = wb.createSheet("半期推移_" + yearHalf.replace("-", ""));

            // ヘッダー: 分類/システム + 月ラベル列 + 合計
            int monthCount = response.rows().isEmpty() ? 0
                    : response.rows().get(0).months().size();
            String[] headers = buildTrendsHeaders(monthCount, response);
            createHeaderRow(wb, sheet, headers);

            int rowNum = 1;
            for (HalfTrendsResponse.HalfTrendsRow r : response.rows()) {
                if (rowNum > MAX_ROWS) {
                    break;
                }
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, r.key());
                setCell(row, 1, r.label());
                for (int i = 0; i < r.months().size(); i++) {
                    BigDecimal val = r.months().get(i).value();
                    setCellNum(row, 2 + i, val != null ? val.doubleValue() : 0.0);
                }
                BigDecimal total = r.total() != null ? r.total() : BigDecimal.ZERO;
                setCellNum(row, 2 + r.months().size(), total.doubleValue());
            }

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("半期推移 Excel 生成に失敗しました", e);
        }
    }

    /**
     * 月別内訳（標準）(.xlsx) を生成する。
     *
     * @param response MonthlyBreakdownResponse
     * @return xlsx バイト配列
     */
    public byte[] exportMonthlyBreakdownStandard(MonthlyBreakdownResponse response) {
        return exportBreakdown(response, "月別内訳標準");
    }

    /**
     * 月別内訳（管理用）(.xlsx) を生成する。
     *
     * @param response MonthlyBreakdownResponse
     * @return xlsx バイト配列
     */
    public byte[] exportMonthlyBreakdownManagement(MonthlyBreakdownResponse response) {
        return exportBreakdown(response, "月別内訳管理用");
    }

    /**
     * 月別内訳（管理詳細）(.xlsx) を生成する。
     *
     * @param response MonthlyBreakdownResponse
     * @return xlsx バイト配列
     */
    public byte[] exportMonthlyBreakdownDetail(MonthlyBreakdownResponse response) {
        return exportBreakdown(response, "月別内訳管理詳細");
    }

    // =========================================================================
    // Static utility
    // =========================================================================

    /**
     * Content-Disposition に使用するファイル名を生成する。
     *
     * <p>例: {@code buildFileName("work_hours", "2025-02")} → {@code "work_hours_202502.xlsx"}
     *
     * @param prefix    ファイル名プレフィックス（例: "work_hours"）
     * @param qualifier 年月または年度半期文字列
     * @return .xlsx ファイル名
     */
    public static String buildFileName(String prefix, String qualifier) {
        // "yyyy-MM" → "yyyyMM"（例: "2025-02" → "202502"）
        // "yyyy-H"  → そのまま（例: "2025-1" → "2025-1"）
        String suffix = qualifier.matches("\\d{4}-\\d{2}")
                ? qualifier.replace("-", "")
                : qualifier;
        return prefix + "_" + suffix + ".xlsx";
    }

    // =========================================================================
    // 内部ヘルパー
    // =========================================================================

    /** MonthlyBreakdownResponse → xlsx の共通実装。 */
    private byte[] exportBreakdown(MonthlyBreakdownResponse response, String sheetName) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(SXSSF_WINDOW)) {
            Sheet sheet = wb.createSheet(sheetName);

            String[] headers = {"コード", "名称", "工数(h)", "合計(h)"};
            createHeaderRow(wb, sheet, headers);

            BigDecimal grandTotal = BigDecimal.ZERO;
            int rowNum = 1;
            for (MonthlyBreakdownResponse.BreakdownRow r : response.rows()) {
                if (rowNum > MAX_ROWS) {
                    break;
                }
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, r.key());
                setCell(row, 1, r.label());
                BigDecimal monthVal = r.months().isEmpty() ? BigDecimal.ZERO
                        : r.months().get(0).value();
                setCellNum(row, 2, monthVal != null ? monthVal.doubleValue() : 0.0);
                BigDecimal total = r.total() != null ? r.total() : BigDecimal.ZERO;
                setCellNum(row, 3, total.doubleValue());
                grandTotal = grandTotal.add(total);
            }

            Row totalRow = sheet.createRow(rowNum);
            setCell(totalRow, 1, "合計");
            setCellNum(totalRow, 3, grandTotal.doubleValue());

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException(sheetName + " Excel 生成に失敗しました", e);
        }
    }

    /** ヘッダー行を生成し、背景色・ボールドフォントを適用する。 */
    private void createHeaderRow(SXSSFWorkbook wb, Sheet sheet, String[] headers) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);

        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);

        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    /** 半期推移のヘッダー配列を生成する。 */
    private String[] buildTrendsHeaders(int monthCount, HalfTrendsResponse response) {
        String[] headers = new String[2 + monthCount + 1];
        headers[0] = "コード";
        headers[1] = "名称";
        if (!response.rows().isEmpty()) {
            List<HalfTrendsResponse.MonthValue> months = response.rows().get(0).months();
            for (int i = 0; i < months.size(); i++) {
                headers[2 + i] = months.get(i).yearMonth();
            }
        } else {
            for (int i = 0; i < monthCount; i++) {
                headers[2 + i] = "月" + (i + 1);
            }
        }
        headers[2 + monthCount] = "合計";
        return headers;
    }

    private void setCell(Row row, int col, String value) {
        row.createCell(col).setCellValue(value != null ? value : "");
    }

    private void setCellNum(Row row, int col, double value) {
        row.createCell(col).setCellValue(value);
    }

    private BigDecimal parseHours(String hours) {
        if (hours == null || hours.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(hours);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private byte[] toBytes(SXSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.dispose();
        return out.toByteArray();
    }
}

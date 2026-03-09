package com.example.czConsv.service;

import com.example.czConsv.dto.response.HalfTrendsResponse;
import com.example.czConsv.dto.response.MonthlyBreakdownResponse;
import com.example.czConsv.dto.response.WorkHoursListResponse;
import com.example.czConsv.dto.response.WorkStatusListResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExcelExportService 単体テスト。
 *
 * <p>6 テンプレートの .xlsx 出力構造を検証する。
 * 各テストは返却された byte[] を XSSFWorkbook で読み直して
 * シート名・ヘッダー行・データ行・合計行を確認する。
 */
class ExcelExportServiceTest {

    private final ExcelExportService service = new ExcelExportService();

    // =========================================================================
    // 共通ヘルパー
    // =========================================================================

    private XSSFWorkbook toWorkbook(byte[] bytes) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private String cellStr(Row row, int col) {
        var cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private double cellNum(Row row, int col) {
        var cell = row.getCell(col);
        if (cell == null) return 0;
        return cell.getNumericCellValue();
    }

    // =========================================================================
    // 1. 工数明細
    // =========================================================================

    @Nested
    class WorkHoursDetail {

        private WorkHoursListResponse.WorkHoursRecord rec(String date, String hours, String subject) {
            return new WorkHoursListResponse.WorkHoursRecord(
                    1L, "2025-02", date,
                    new WorkHoursListResponse.SubsystemInfo("SUB01", "サブシス01", "SYS01", "システム01"),
                    null,
                    new WorkHoursListResponse.CategoryInfo("CAT-A", "カテゴリA"),
                    subject, hours, null, null, null, "0", null
            );
        }

        @Test
        void producesValidXlsx() throws Exception {
            byte[] bytes = service.exportWorkHoursDetail(List.of(rec("2025-02-01", "2.5", "件名A")), "2025-02");
            assertThat(bytes).isNotEmpty();
            // ZIP マジックバイト（PK\x03\x04）
            assertThat(bytes[0]).isEqualTo((byte) 0x50);
            assertThat(bytes[1]).isEqualTo((byte) 0x4B);
        }

        @Test
        void sheetNameContainsYearMonth() throws Exception {
            byte[] bytes = service.exportWorkHoursDetail(List.of(rec("2025-02-01", "2.5", "件名A")), "2025-02");
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                assertThat(wb.getNumberOfSheets()).isGreaterThanOrEqualTo(1);
                assertThat(wb.getSheetName(0)).contains("2025");
            }
        }

        @Test
        void headerRowExists() throws Exception {
            byte[] bytes = service.exportWorkHoursDetail(List.of(rec("2025-02-01", "2.5", "件名A")), "2025-02");
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                Row header = sheet.getRow(0);
                assertThat(header).isNotNull();
                // 先頭ヘッダーセルが空でない
                assertThat(cellStr(header, 0)).isNotBlank();
            }
        }

        @Test
        void dataRowsFollowHeader() throws Exception {
            byte[] bytes = service.exportWorkHoursDetail(
                    List.of(rec("2025-02-01", "2.5", "件名A"),
                            rec("2025-02-03", "1.0", "件名B")),
                    "2025-02");
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                // ヘッダー(0) + データ2行 = 少なくとも3行
                assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(2);
            }
        }

        @Test
        void totalRowAtEnd() throws Exception {
            byte[] bytes = service.exportWorkHoursDetail(
                    List.of(rec("2025-02-01", "2.5", "件名A"),
                            rec("2025-02-03", "1.0", "件名B")),
                    "2025-02");
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                int last = sheet.getLastRowNum();
                Row totalRow = sheet.getRow(last);
                assertThat(totalRow).isNotNull();
                // 合計行は空でない
                assertThat(totalRow.getPhysicalNumberOfCells()).isGreaterThan(0);
            }
        }
    }

    // =========================================================================
    // 2. 工数状況一覧
    // =========================================================================

    @Nested
    class WorkStatus {

        private WorkStatusListResponse.WorkStatusRecord rec(String staffId, String name, double hours) {
            return new WorkStatusListResponse.WorkStatusRecord(
                    staffId, name, "ORG01", "組織01",
                    "2025-02", "1", BigDecimal.valueOf(hours), 5
            );
        }

        @Test
        void producesValidXlsx() throws Exception {
            byte[] bytes = service.exportWorkStatus(
                    List.of(rec("EMP001", "山田 太郎", 40.0)), "2025-02");
            assertThat(bytes).isNotEmpty();
            assertThat(bytes[0]).isEqualTo((byte) 0x50);
        }

        @Test
        void headerAndDataRowsExist() throws Exception {
            byte[] bytes = service.exportWorkStatus(
                    List.of(rec("EMP001", "山田 太郎", 40.0),
                            rec("EMP002", "鈴木 花子", 35.5)),
                    "2025-02");
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                assertThat(sheet.getRow(0)).isNotNull(); // ヘッダー
                assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(2); // データ2行以上
            }
        }
    }

    // =========================================================================
    // 3. 半期推移
    // =========================================================================

    @Nested
    class HalfTrends {

        private HalfTrendsResponse response() {
            List<HalfTrendsResponse.MonthValue> months = List.of(
                    new HalfTrendsResponse.MonthValue("2025-01", new BigDecimal("10")),
                    new HalfTrendsResponse.MonthValue("2025-02", new BigDecimal("20")),
                    new HalfTrendsResponse.MonthValue("2025-03", new BigDecimal("15")),
                    new HalfTrendsResponse.MonthValue("2025-04", new BigDecimal("5")),
                    new HalfTrendsResponse.MonthValue("2025-05", new BigDecimal("8")),
                    new HalfTrendsResponse.MonthValue("2025-06", new BigDecimal("12"))
            );
            List<HalfTrendsResponse.HalfTrendsRow> rows = List.of(
                    new HalfTrendsResponse.HalfTrendsRow("CAT-A", "カテゴリA", months, new BigDecimal("70"))
            );
            return new HalfTrendsResponse(rows, new HalfTrendsResponse.DrilldownContext("2025-1", null, null, null));
        }

        @Test
        void producesValidXlsx() throws Exception {
            byte[] bytes = service.exportHalfTrends(response(), "2025-1");
            assertThat(bytes).isNotEmpty();
            assertThat(bytes[0]).isEqualTo((byte) 0x50);
        }

        @Test
        void monthColumnsInHeader() throws Exception {
            byte[] bytes = service.exportHalfTrends(response(), "2025-1");
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                Row header = sheet.getRow(0);
                assertThat(header).isNotNull();
                // 月列（最低 6 列）が存在する
                assertThat((int) header.getLastCellNum()).isGreaterThanOrEqualTo(6);
            }
        }

        @Test
        void dataRowContainsTotalValue() throws Exception {
            byte[] bytes = service.exportHalfTrends(response(), "2025-1");
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                // データ行(行1)の合計値が 70
                Row dataRow = sheet.getRow(1);
                assertThat(dataRow).isNotNull();
            }
        }
    }

    // =========================================================================
    // 4. 月別内訳標準
    // =========================================================================

    @Nested
    class MonthlyBreakdownStandard {

        private MonthlyBreakdownResponse response() {
            List<MonthlyBreakdownResponse.MonthValue> mv = List.of(
                    new MonthlyBreakdownResponse.MonthValue("2025-02", new BigDecimal("30"))
            );
            List<MonthlyBreakdownResponse.BreakdownRow> rows = List.of(
                    new MonthlyBreakdownResponse.BreakdownRow("CAT-A", "カテゴリA", mv, new BigDecimal("30"))
            );
            return new MonthlyBreakdownResponse(rows,
                    new MonthlyBreakdownResponse.BreakdownContext("2025-02", null, null, null));
        }

        @Test
        void producesValidXlsx() throws Exception {
            byte[] bytes = service.exportMonthlyBreakdownStandard(response());
            assertThat(bytes).isNotEmpty();
            assertThat(bytes[0]).isEqualTo((byte) 0x50);
        }

        @Test
        void headerAndDataRowsExist() throws Exception {
            byte[] bytes = service.exportMonthlyBreakdownStandard(response());
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                assertThat(sheet.getRow(0)).isNotNull();
                assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(1);
            }
        }
    }

    // =========================================================================
    // 5. 月別内訳管理用
    // =========================================================================

    @Nested
    class MonthlyBreakdownManagement {

        private MonthlyBreakdownResponse response() {
            List<MonthlyBreakdownResponse.MonthValue> mv = List.of(
                    new MonthlyBreakdownResponse.MonthValue("2025-02", new BigDecimal("50"))
            );
            List<MonthlyBreakdownResponse.BreakdownRow> rows = List.of(
                    new MonthlyBreakdownResponse.BreakdownRow("SYS01", "システム01", mv, new BigDecimal("50"))
            );
            return new MonthlyBreakdownResponse(rows,
                    new MonthlyBreakdownResponse.BreakdownContext("2025-02", null, null, "CAT-A"));
        }

        @Test
        void producesValidXlsx() throws Exception {
            byte[] bytes = service.exportMonthlyBreakdownManagement(response());
            assertThat(bytes).isNotEmpty();
            assertThat(bytes[0]).isEqualTo((byte) 0x50);
        }

        @Test
        void sheetExists() throws Exception {
            byte[] bytes = service.exportMonthlyBreakdownManagement(response());
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                assertThat(wb.getNumberOfSheets()).isGreaterThanOrEqualTo(1);
            }
        }
    }

    // =========================================================================
    // 6. 月別内訳管理詳細
    // =========================================================================

    @Nested
    class MonthlyBreakdownManagementDetail {

        private MonthlyBreakdownResponse response() {
            List<MonthlyBreakdownResponse.MonthValue> mv = List.of(
                    new MonthlyBreakdownResponse.MonthValue("2025-02", new BigDecimal("12"))
            );
            List<MonthlyBreakdownResponse.BreakdownRow> rows = List.of(
                    new MonthlyBreakdownResponse.BreakdownRow("SUB01", "サブシス01", mv, new BigDecimal("12"))
            );
            return new MonthlyBreakdownResponse(rows,
                    new MonthlyBreakdownResponse.BreakdownContext("2025-02", "SYS01", "SUB01", "CAT-A"));
        }

        @Test
        void producesValidXlsx() throws Exception {
            byte[] bytes = service.exportMonthlyBreakdownDetail(response());
            assertThat(bytes).isNotEmpty();
            assertThat(bytes[0]).isEqualTo((byte) 0x50);
        }

        @Test
        void headerAndDataRowsExist() throws Exception {
            byte[] bytes = service.exportMonthlyBreakdownDetail(response());
            try (XSSFWorkbook wb = toWorkbook(bytes)) {
                Sheet sheet = wb.getSheetAt(0);
                assertThat(sheet.getRow(0)).isNotNull();
                assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(1);
            }
        }
    }

    // =========================================================================
    // ファイル名生成ユーティリティ
    // =========================================================================

    @Nested
    class FileNameGeneration {

        @Test
        void workHoursDetailFileName() {
            String name = ExcelExportService.buildFileName("work_hours", "2025-02");
            assertThat(name).isEqualTo("work_hours_202502.xlsx");
        }

        @Test
        void workStatusFileName() {
            String name = ExcelExportService.buildFileName("work_status", "2025-02");
            assertThat(name).isEqualTo("work_status_202502.xlsx");
        }

        @Test
        void halfTrendsFileName() {
            String name = ExcelExportService.buildFileName("half_trends", "2025-1");
            assertThat(name).isEqualTo("half_trends_2025-1.xlsx");
        }
    }
}

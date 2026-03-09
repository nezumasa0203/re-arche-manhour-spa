package com.example.czConsv.controller;

import com.example.czConsv.dto.response.MonthlyBreakdownResponse;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.MonthlyBreakdownService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 月別内訳 REST コントローラー。
 *
 * <p>エンドポイント一覧:
 * <ul>
 *   <li>GET /api/monthly-breakdown/categories    — 分類別集計</li>
 *   <li>GET /api/monthly-breakdown/systems        — システム別集計</li>
 *   <li>GET /api/monthly-breakdown/subsystems     — サブシステム別集計</li>
 *   <li>GET /api/monthly-breakdown/detail         — 明細</li>
 *   <li>GET /api/monthly-breakdown/export/excel   — Excel 出力（type パラメータで種別指定）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/monthly-breakdown")
public class MonthlyBreakdownController {

    private final MonthlyBreakdownService monthlyBreakdownService;
    private final ExcelExportService excelExportService;

    public MonthlyBreakdownController(MonthlyBreakdownService monthlyBreakdownService,
                                      ExcelExportService excelExportService) {
        this.monthlyBreakdownService = monthlyBreakdownService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/categories")
    public ResponseEntity<MonthlyBreakdownResponse> getCategories(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        return ResponseEntity.ok(
                monthlyBreakdownService.getCategories(yearMonth, displayMode, filterType));
    }

    @GetMapping("/systems")
    public ResponseEntity<MonthlyBreakdownResponse> getSystems(
            @RequestParam String yearMonth,
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        return ResponseEntity.ok(
                monthlyBreakdownService.getSystems(yearMonth, categoryCode, displayMode, filterType));
    }

    @GetMapping("/subsystems")
    public ResponseEntity<MonthlyBreakdownResponse> getSubsystems(
            @RequestParam String yearMonth,
            @RequestParam String systemNo,
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        return ResponseEntity.ok(
                monthlyBreakdownService.getSubsystems(
                        yearMonth, systemNo, categoryCode, displayMode, filterType));
    }

    @GetMapping("/detail")
    public ResponseEntity<MonthlyBreakdownResponse> getDetail(
            @RequestParam String yearMonth,
            @RequestParam String systemNo,
            @RequestParam String subsystemNo,
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        return ResponseEntity.ok(
                monthlyBreakdownService.getDetail(
                        yearMonth, systemNo, subsystemNo, categoryCode, displayMode, filterType));
    }

    /**
     * Excel 出力。type パラメータで出力種別を選択。
     *
     * @param type standard / management / management-detail (default: standard)
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType,
            @RequestParam(defaultValue = "standard") String type) {
        MonthlyBreakdownResponse response =
                monthlyBreakdownService.getCategories(yearMonth, displayMode, filterType);

        byte[] bytes = switch (type) {
            case "management" -> excelExportService.exportMonthlyBreakdownManagement(response);
            case "management-detail" -> excelExportService.exportMonthlyBreakdownDetail(response);
            default -> excelExportService.exportMonthlyBreakdownStandard(response);
        };

        String filename = ExcelExportService.buildFileName("monthly_breakdown_" + type, yearMonth);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}

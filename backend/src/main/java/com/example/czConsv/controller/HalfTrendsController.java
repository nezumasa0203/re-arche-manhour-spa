package com.example.czConsv.controller;

import com.example.czConsv.dto.response.HalfTrendsResponse;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.HalfTrendsService;
import com.example.czConsv.util.ControllerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 半期推移 REST コントローラー。
 *
 * <p>エンドポイント一覧:
 * <ul>
 *   <li>GET /api/half-trends/categories   — STEP_0: 分類別集計</li>
 *   <li>GET /api/half-trends/systems      — STEP_1: システム別集計</li>
 *   <li>GET /api/half-trends/subsystems   — STEP_2: サブシステム別集計</li>
 *   <li>GET /api/half-trends/export/excel — Excel 出力</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/half-trends")
public class HalfTrendsController {

    private final HalfTrendsService halfTrendsService;
    private final ExcelExportService excelExportService;

    public HalfTrendsController(HalfTrendsService halfTrendsService,
                                ExcelExportService excelExportService) {
        this.halfTrendsService = halfTrendsService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/categories")
    public ResponseEntity<HalfTrendsResponse> getCategories(
            @RequestParam String yearHalf,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        return ResponseEntity.ok(
                halfTrendsService.getCategories(yearHalf, displayMode, filterType));
    }

    @GetMapping("/systems")
    public ResponseEntity<HalfTrendsResponse> getSystems(
            @RequestParam String yearHalf,
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        return ResponseEntity.ok(
                halfTrendsService.getSystems(yearHalf, categoryCode, displayMode, filterType));
    }

    @GetMapping("/subsystems")
    public ResponseEntity<HalfTrendsResponse> getSubsystems(
            @RequestParam String yearHalf,
            @RequestParam String systemNo,
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        return ResponseEntity.ok(
                halfTrendsService.getSubsystems(
                        yearHalf, systemNo, categoryCode, displayMode, filterType));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam String yearHalf,
            @RequestParam(defaultValue = "hours") String displayMode,
            @RequestParam(defaultValue = "all") String filterType) {
        HalfTrendsResponse response =
                halfTrendsService.getCategories(yearHalf, displayMode, filterType);
        byte[] bytes = excelExportService.exportHalfTrends(response, yearHalf);
        String filename = ExcelExportService.buildFileName("half_trends", yearHalf);
        return ResponseEntity.ok()
                .headers(ControllerSupport.excelHeaders(filename))
                .body(bytes);
    }
}

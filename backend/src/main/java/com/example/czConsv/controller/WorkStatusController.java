package com.example.czConsv.controller;

import com.example.czConsv.dto.request.ApproveRequest;
import com.example.czConsv.dto.request.MonthlyControlRequest;
import com.example.czConsv.dto.request.RevertRequest;
import com.example.czConsv.entity.Mcz04Ctrl;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.MonthlyControlService;
import com.example.czConsv.service.WorkStatusService;
import com.example.czConsv.util.ControllerSupport;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工数状況管理 REST コントローラー。
 *
 * <p>エンドポイント一覧:
 * <ul>
 *   <li>GET    /api/work-status                    — 一覧検索</li>
 *   <li>PATCH  /api/work-status/{id}/hours         — 工数インライン編集</li>
 *   <li>POST   /api/work-status/approve            — 承認</li>
 *   <li>POST   /api/work-status/revert             — 差戻</li>
 *   <li>POST   /api/work-status/monthly-confirm    — 月次確定</li>
 *   <li>POST   /api/work-status/monthly-aggregate  — 月次集計</li>
 *   <li>POST   /api/work-status/monthly-unconfirm  — 月次確定解除</li>
 *   <li>GET    /api/work-status/export/excel       — Excel 出力</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/work-status")
public class WorkStatusController {

    private final WorkStatusService workStatusService;
    private final MonthlyControlService monthlyControlService;
    private final ExcelExportService excelExportService;

    public WorkStatusController(WorkStatusService workStatusService,
                                MonthlyControlService monthlyControlService,
                                ExcelExportService excelExportService) {
        this.workStatusService = workStatusService;
        this.monthlyControlService = monthlyControlService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public ResponseEntity<List<Tcz01HosyuKousuu>> search(
            @RequestParam String yearMonth,
            @RequestParam String organizationCode,
            @RequestParam(required = false) String staffId) {
        return ResponseEntity.ok(
                workStatusService.search(yearMonth, organizationCode, staffId));
    }

    @PatchMapping("/{id}/hours")
    public ResponseEntity<Tcz01HosyuKousuu> updateHours(
            @PathVariable Long id,
            @RequestParam String hours,
            @RequestParam String updatedAt) {
        LocalDateTime updatedAtDt = LocalDateTime.parse(updatedAt);
        return ResponseEntity.ok(
                workStatusService.updateHours(id, ControllerSupport.resolveSkbtcd(), hours, updatedAtDt));
    }

    @PostMapping("/approve")
    public ResponseEntity<Map<String, Integer>> approve(
            @Valid @RequestBody ApproveRequest request) {
        int count = workStatusService.approve(request.ids(), ControllerSupport.resolveSkbtcd());
        return ResponseEntity.ok(Map.of("approvedCount", count));
    }

    @PostMapping("/revert")
    public ResponseEntity<Map<String, Integer>> revert(
            @Valid @RequestBody RevertRequest request) {
        int count = workStatusService.revert(request.ids(), ControllerSupport.resolveSkbtcd());
        return ResponseEntity.ok(Map.of("revertedCount", count));
    }

    @PostMapping("/monthly-confirm")
    public ResponseEntity<Mcz04Ctrl> monthlyConfirm(
            @Valid @RequestBody MonthlyControlRequest request) {
        return ResponseEntity.ok(
                monthlyControlService.monthlyConfirm(
                        request.yearMonth(), request.organizationCode()));
    }

    @PostMapping("/monthly-aggregate")
    public ResponseEntity<Mcz04Ctrl> monthlyAggregate(
            @Valid @RequestBody MonthlyControlRequest request) {
        return ResponseEntity.ok(
                monthlyControlService.monthlyAggregate(
                        request.yearMonth(), request.organizationCode()));
    }

    @PostMapping("/monthly-unconfirm")
    public ResponseEntity<Mcz04Ctrl> monthlyUnconfirm(
            @Valid @RequestBody MonthlyControlRequest request) {
        return ResponseEntity.ok(
                monthlyControlService.monthlyUnconfirm(
                        request.yearMonth(), request.organizationCode()));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam String yearMonth,
            @RequestParam String organizationCode,
            @RequestParam(required = false) String staffId) {
        List<Tcz01HosyuKousuu> records =
                workStatusService.search(yearMonth, organizationCode, staffId);
        byte[] bytes = excelExportService.exportWorkStatus(List.of(), yearMonth);
        String filename = ExcelExportService.buildFileName("work_status", yearMonth);
        return ResponseEntity.ok()
                .headers(ControllerSupport.excelHeaders(filename))
                .body(bytes);
    }
}

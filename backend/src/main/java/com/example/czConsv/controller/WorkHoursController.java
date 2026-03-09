package com.example.czConsv.controller;

import com.example.czConsv.dto.request.BatchConfirmRequest;
import com.example.czConsv.dto.request.BatchRevertRequest;
import com.example.czConsv.dto.request.WorkHoursCreateRequest;
import com.example.czConsv.dto.request.WorkHoursCopyRequest;
import com.example.czConsv.dto.request.WorkHoursTransferRequest;
import com.example.czConsv.dto.request.WorkHoursUpdateRequest;
import com.example.czConsv.dto.response.WorkHoursListResponse;
import com.example.czConsv.dto.response.WorkHoursListResponse.WorkHoursRecord;
import com.example.czConsv.dto.response.WorkHoursMapper;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.WorkHoursService;
import com.example.czConsv.util.ControllerSupport;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 工数管理 REST コントローラー。
 *
 * <p>エンドポイント一覧:
 * <ul>
 *   <li>GET    /api/work-hours             — 月次一覧取得</li>
 *   <li>POST   /api/work-hours             — 新規作成</li>
 *   <li>PATCH  /api/work-hours/{id}        — フィールド単位更新</li>
 *   <li>DELETE /api/work-hours/{id}        — 論理削除</li>
 *   <li>POST   /api/work-hours/copy        — コピー</li>
 *   <li>POST   /api/work-hours/transfer    — 翌月転写</li>
 *   <li>POST   /api/work-hours/batch-confirm  — 一括確定</li>
 *   <li>POST   /api/work-hours/batch-revert   — 一括差戻</li>
 *   <li>GET    /api/work-hours/export/excel   — Excel 出力</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/work-hours")
public class WorkHoursController {

    private final WorkHoursService workHoursService;
    private final ExcelExportService excelExportService;

    public WorkHoursController(WorkHoursService workHoursService,
                                ExcelExportService excelExportService) {
        this.workHoursService = workHoursService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public ResponseEntity<WorkHoursListResponse> list(
            @RequestParam String staffId,
            @RequestParam String yearMonth) {
        CzPrincipal principal = CzSecurityContext.require();
        List<Tcz01HosyuKousuu> entities = workHoursService.fetchByMonth(staffId, yearMonth);
        return ResponseEntity.ok(WorkHoursMapper.toListResponse(entities, yearMonth, principal));
    }

    @PostMapping
    public ResponseEntity<WorkHoursRecord> create(
            @Valid @RequestBody WorkHoursCreateRequest request) {
        Tcz01HosyuKousuu created = workHoursService.create(request);
        return ResponseEntity.status(201).body(WorkHoursMapper.toRecord(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkHoursRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkHoursUpdateRequest request) {
        Tcz01HosyuKousuu updated = workHoursService.updateField(
                id, ControllerSupport.resolveSkbtcd(), request.field(), request.value(), request.updatedAt());
        return ResponseEntity.ok(WorkHoursMapper.toRecord(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Integer>> delete(@PathVariable Long id) {
        int count = workHoursService.delete(List.of(id), ControllerSupport.resolveSkbtcd());
        return ResponseEntity.ok(Map.of("deletedCount", count));
    }

    @PostMapping("/copy")
    public ResponseEntity<List<WorkHoursRecord>> copy(
            @Valid @RequestBody WorkHoursCopyRequest request) {
        List<Tcz01HosyuKousuu> copied = workHoursService.copy(request.ids(), ControllerSupport.resolveSkbtcd());
        return ResponseEntity.ok(copied.stream().map(WorkHoursMapper::toRecord).toList());
    }

    @PostMapping("/transfer")
    public ResponseEntity<List<WorkHoursRecord>> transfer(
            @Valid @RequestBody WorkHoursTransferRequest request) {
        List<Tcz01HosyuKousuu> transferred = workHoursService.transferNextMonth(
                request.ids(), ControllerSupport.resolveSkbtcd(), request.targetMonths());
        return ResponseEntity.ok(transferred.stream().map(WorkHoursMapper::toRecord).toList());
    }

    @PostMapping("/batch-confirm")
    public ResponseEntity<Map<String, Object>> batchConfirm(
            @Valid @RequestBody BatchConfirmRequest request) {
        int count = workHoursService.batchConfirm(request.yearMonth());
        return ResponseEntity.ok(Map.of("confirmedCount", count));
    }

    @PostMapping("/batch-revert")
    public ResponseEntity<Map<String, Object>> batchRevert(
            @Valid @RequestBody BatchRevertRequest request) {
        int count = workHoursService.batchRevert(request.yearMonth());
        return ResponseEntity.ok(Map.of("revertedCount", count));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam String staffId,
            @RequestParam String yearMonth) {
        workHoursService.fetchByMonth(staffId, yearMonth);
        byte[] bytes = excelExportService.exportWorkHoursDetail(List.of(), yearMonth);
        String filename = ExcelExportService.buildFileName("work_hours", yearMonth);
        return ResponseEntity.ok()
                .headers(ControllerSupport.excelHeaders(filename))
                .body(bytes);
    }
}

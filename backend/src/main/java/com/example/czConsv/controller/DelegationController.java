package com.example.czConsv.controller;

import com.example.czConsv.dto.request.DelegationSwitchRequest;
import com.example.czConsv.dto.response.DelegationResponse;
import com.example.czConsv.entity.Tcz16TntBusyoRireki;
import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.service.MasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代行機能 REST コントローラー。
 *
 * <p>エンドポイント一覧:
 * <ul>
 *   <li>GET  /api/delegation/available-staff — 代行可能担当者一覧</li>
 *   <li>POST /api/delegation/switch          — 代行モード切替</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/delegation")
public class DelegationController {

    private final MasterService masterService;

    public DelegationController(MasterService masterService) {
        this.masterService = masterService;
    }

    @GetMapping("/available-staff")
    public ResponseEntity<List<Tcz16TntBusyoRireki>> getAvailableStaff() {
        CzPrincipal principal = CzSecurityContext.require();
        if (!principal.permissions().canDelegate()) {
            throw new CzBusinessException("CZ-106", "代行権限がありません");
        }
        return ResponseEntity.ok(masterService.searchStaff(null));
    }

    @PostMapping("/switch")
    public ResponseEntity<DelegationResponse> switchDelegation(
            @RequestBody DelegationSwitchRequest request) {
        CzPrincipal principal = CzSecurityContext.require();

        if (request.targetStaffId() == null) {
            return ResponseEntity.ok(DelegationResponse.cancelled());
        }

        if (!principal.permissions().canDelegate()) {
            throw new CzBusinessException("CZ-106", "代行権限がありません");
        }

        // 自分自身への代行は不可
        if (request.targetStaffId().equals(principal.userId())) {
            throw new CzBusinessException("CZ-119", "自分自身への代行はできません");
        }

        return ResponseEntity.ok(
                new DelegationResponse(request.targetStaffId(), null, true));
    }
}

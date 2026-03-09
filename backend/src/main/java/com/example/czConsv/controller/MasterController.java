package com.example.czConsv.controller;

import com.example.czConsv.dto.response.MasterListResponse;
import com.example.czConsv.service.MasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * マスタ参照 REST コントローラー。
 *
 * <p>エンドポイント一覧:
 * <ul>
 *   <li>GET /api/master/organizations  — 組織一覧</li>
 *   <li>GET /api/master/systems        — システム一覧</li>
 *   <li>GET /api/master/subsystems     — サブシステム一覧（キーワード検索）</li>
 *   <li>GET /api/master/staff          — 担当者検索</li>
 *   <li>GET /api/master/categories     — カテゴリ一覧（年度指定）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/master")
public class MasterController {

    private final MasterService masterService;

    public MasterController(MasterService masterService) {
        this.masterService = masterService;
    }

    @GetMapping("/organizations")
    public ResponseEntity<MasterListResponse> getOrganizations() {
        List<Object> items = List.copyOf(masterService.getOrganizations());
        return ResponseEntity.ok(new MasterListResponse(items, items.size(), 1, items.size()));
    }

    @GetMapping("/systems")
    public ResponseEntity<MasterListResponse> getSystems() {
        List<Object> items = List.copyOf(masterService.getSystems());
        return ResponseEntity.ok(new MasterListResponse(items, items.size(), 1, items.size()));
    }

    @GetMapping("/subsystems")
    public ResponseEntity<MasterListResponse> getSubsystems(
            @RequestParam(required = false) String keyword) {
        List<Object> items = List.copyOf(masterService.getSubsystems(keyword));
        return ResponseEntity.ok(new MasterListResponse(items, items.size(), 1, items.size()));
    }

    @GetMapping("/staff")
    public ResponseEntity<MasterListResponse> searchStaff(
            @RequestParam(required = false) String name) {
        List<Object> items = List.copyOf(masterService.searchStaff(name));
        return ResponseEntity.ok(new MasterListResponse(items, items.size(), 1, items.size()));
    }

    @GetMapping("/categories")
    public ResponseEntity<MasterListResponse> getCategories(
            @RequestParam int fiscalYear) {
        List<Object> items = List.copyOf(masterService.getCategories(fiscalYear));
        return ResponseEntity.ok(new MasterListResponse(items, items.size(), 1, items.size()));
    }
}

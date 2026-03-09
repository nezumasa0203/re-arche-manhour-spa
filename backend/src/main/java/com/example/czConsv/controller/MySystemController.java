package com.example.czConsv.controller;

import com.example.czConsv.dto.request.MySystemCreateRequest;
import com.example.czConsv.entity.Tcz19MySys;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.service.MySystemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MY システム CRUD REST コントローラー。
 *
 * <p>エンドポイント一覧:
 * <ul>
 *   <li>GET    /api/my-systems          — 一覧取得</li>
 *   <li>POST   /api/my-systems          — 登録</li>
 *   <li>DELETE /api/my-systems/{sysNo}  — 削除</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/my-systems")
public class MySystemController {

    private final MySystemService mySystemService;

    public MySystemController(MySystemService mySystemService) {
        this.mySystemService = mySystemService;
    }

    @GetMapping
    public ResponseEntity<List<Tcz19MySys>> list() {
        String userId = CzSecurityContext.require().userId();
        return ResponseEntity.ok(mySystemService.getMySystemList(userId));
    }

    @PostMapping
    public ResponseEntity<Tcz19MySys> register(
            @Valid @RequestBody MySystemCreateRequest request) {
        String userId = CzSecurityContext.require().userId();
        return ResponseEntity.status(201)
                .body(mySystemService.registerMySystem(userId, request.systemNo()));
    }

    @DeleteMapping("/{systemNo}")
    public ResponseEntity<Map<String, String>> remove(@PathVariable String systemNo) {
        String userId = CzSecurityContext.require().userId();
        mySystemService.removeMySystem(userId, systemNo);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}

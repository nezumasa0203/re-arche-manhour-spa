package com.example.czConsv.controller;

import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.util.StatusMatrixResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 現在の認証ユーザー情報を返す。
     * DevActorSwitcher やデバッグ用。
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        CzPrincipal principal = CzSecurityContext.get();
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", principal.userId());
        result.put("userName", principal.userName());
        result.put("email", principal.email());
        result.put("organizationCode", principal.organizationCode());
        result.put("organizationName", principal.organizationName());
        result.put("jinjiMode", principal.permissions().jinjiMode());
        result.put("employmentType", principal.permissions().employmentType().name());
        result.put("staffRole", principal.permissions().staffRole());
        result.put("canDelegate", principal.permissions().canDelegate());
        result.put("delegationStaffId", principal.delegationStaffId());

        // 権限サマリー
        Map<String, Object> perms = new LinkedHashMap<>();
        perms.put("canReport", principal.permissions().canReport());
        perms.put("canManageReports", principal.permissions().canManageReports());
        perms.put("canFullManage", principal.permissions().canFullManage());
        perms.put("canOutputMaintenanceHours", principal.permissions().canOutputMaintenanceHours());
        perms.put("canNavigateBetweenForms", principal.permissions().canNavigateBetweenForms());
        perms.put("canInputPeriodCondition", principal.permissions().canInputPeriodCondition());
        perms.put("canAggregatePeriod", principal.permissions().canAggregatePeriod());
        result.put("permissions", perms);

        // データアクセス権限
        Map<String, Object> dataAuth = new LinkedHashMap<>();
        dataAuth.put("ref", principal.permissions().dataAuthority().ref());
        dataAuth.put("ins", principal.permissions().dataAuthority().ins());
        dataAuth.put("upd", principal.permissions().dataAuthority().upd());
        result.put("dataAuthority", dataAuth);

        return ResponseEntity.ok(result);
    }

    /**
     * 指定ステータスキーに対するボタン制御マトリクスを返す。
     */
    @GetMapping("/status-matrix")
    public ResponseEntity<Map<String, Integer>> statusMatrix(
            @RequestParam String statusKey) {
        CzPrincipal principal = CzSecurityContext.require();
        boolean useTan = principal.permissions().useTanSeries();
        Map<String, Integer> matrix = StatusMatrixResolver.resolve(statusKey, useTan);
        return ResponseEntity.ok(matrix);
    }
}

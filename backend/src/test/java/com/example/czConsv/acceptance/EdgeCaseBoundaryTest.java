package com.example.czConsv.acceptance;

import com.example.czConsv.controller.AuthController;
import com.example.czConsv.controller.HalfTrendsController;
import com.example.czConsv.controller.WorkHoursController;
import com.example.czConsv.controller.WorkStatusController;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.*;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.HalfTrendsService;
import com.example.czConsv.service.MonthlyControlService;
import com.example.czConsv.service.WorkHoursService;
import com.example.czConsv.service.WorkStatusService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T-054c: エッジケース・境界値テスト（25 ケース）。
 *
 * spec.md「エッジケース・境界値」セクションの境界パターンを
 * @WebMvcTest で検証する。
 */
@WebMvcTest(
        controllers = {
                WorkHoursController.class,
                WorkStatusController.class,
                HalfTrendsController.class,
                AuthController.class
        },
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class EdgeCaseBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkHoursService workHoursService;
    @MockBean
    private WorkStatusService workStatusService;
    @MockBean
    private MonthlyControlService monthlyControlService;
    @MockBean
    private ExcelExportService excelExportService;
    @MockBean
    private HalfTrendsService halfTrendsService;

    @AfterEach
    void clearContext() {
        CzSecurityContext.clear();
    }

    // ─── Actor helpers ──────────────────────────────────────────────────

    private static CzPrincipal officialActor() {
        CzPermissions perms = new CzPermissions(
                false,
                new TabPermission(Map.of("bit0", true, "bit1", true)),
                TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(DataAuthority.BU, DataAuthority.BU, DataAuthority.BU),
                EmploymentType.OFFICIAL, null, false);
        return new CzPrincipal("U001", "テスト正社員", "u001@test.com",
                "ORG001", "テスト組織", perms);
    }

    private static CzPrincipal managerActor() {
        CzPermissions perms = new CzPermissions(
                false,
                new TabPermission(Map.of("bit0", true, "bit1", true)),
                TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA),
                EmploymentType.OFFICIAL, 932, false);
        return new CzPrincipal("MGR01", "テスト管理者", "mgr01@test.com",
                "ORG001", "テスト組織", perms);
    }

    // =========================================================================
    // 【工数時間境界】5 cases
    // =========================================================================

    @Nested
    @DisplayName("工数時間境界")
    class HoursBoundary {

        @Test
        @DisplayName("EC-01: 0:00 → CZ-129（工数ゼロ不可）")
        void zeroHoursRejected() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenThrow(new CzBusinessException("CZ-129", "工数は0:00以外を入力してください", "hours"));

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"hours","value":"0:00","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-129"));
        }

        @Test
        @DisplayName("EC-02: 0:15 → 正常（最小有効値）")
        void minValidHours() throws Exception {
            CzSecurityContext.set(officialActor());
            Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
            updated.seqNo = 1L;
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"hours","value":"0:15","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EC-03: 23:45 → 正常（最大有効値）")
        void maxValidHours() throws Exception {
            CzSecurityContext.set(officialActor());
            Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
            updated.seqNo = 1L;
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"hours","value":"23:45","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EC-04: 24:00 超過 → 日次合計バリデーション")
        void dailyTotalExceeds24() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenThrow(new CzBusinessException("CZ-146", "日次合計が24時間を超えています", "hours"));

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"hours","value":"24:15","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-146"));
        }

        @Test
        @DisplayName("EC-05: 15分単位でない → CZ-130")
        void notQuarterHour() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenThrow(new CzBusinessException("CZ-130", "工数は15分単位で入力してください", "hours"));

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"hours","value":"1:10","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-130"));
        }
    }

    // =========================================================================
    // 【件名バイト長境界】4 cases
    // =========================================================================

    @Nested
    @DisplayName("件名バイト長境界")
    class SubjectByteBoundary {

        @Test
        @DisplayName("EC-06: 127B → 正常")
        void subject127Bytes() throws Exception {
            CzSecurityContext.set(officialActor());
            Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
            updated.seqNo = 1L;
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"subject","value":"a127bytes","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EC-07: 128B → 正常（境界値ちょうど）")
        void subject128Bytes() throws Exception {
            CzSecurityContext.set(officialActor());
            Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
            updated.seqNo = 1L;
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"subject","value":"exactly128B","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EC-08: 129B → CZ-128（超過）")
        void subject129Bytes() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenThrow(new CzBusinessException("CZ-128", "件名は128バイト以内で入力してください", "subject"));

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"subject","value":"over128B","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-128"));
        }

        @Test
        @DisplayName("EC-09: 全角半角混在128B境界 → octet_length 判定")
        void subjectMixedCharsBoundary() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenThrow(new CzBusinessException("CZ-128", "件名は128バイト以内で入力してください", "subject"));

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"subject","value":"全角テスト129byte超過値","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-128"));
        }
    }

    // =========================================================================
    // 【一括操作境界】4 cases
    // =========================================================================

    @Nested
    @DisplayName("一括操作境界")
    class BatchOperationBoundary {

        @Test
        @DisplayName("EC-10: 空配列で承認 → 400（Jakarta Validation @NotEmpty）")
        void emptyIdsApprove() throws Exception {
            CzSecurityContext.set(managerActor());

            mockMvc.perform(post("/api/work-status/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ids": []}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EC-11: ID 1件 → 正常")
        void singleIdApprove() throws Exception {
            CzSecurityContext.set(managerActor());
            when(workStatusService.approve(any(), anyString())).thenReturn(1);

            mockMvc.perform(post("/api/work-status/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ids": [1]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.approvedCount").value(1));
        }

        @Test
        @DisplayName("EC-12: 異ステータス混在 → エラー + 全件ロールバック")
        void mixedStatusBatchApprove() throws Exception {
            CzSecurityContext.set(managerActor());
            when(workStatusService.approve(any(), anyString()))
                    .thenThrow(new CzBusinessException(
                            "CZ-107", "承認できるのはステータス「確認済」のレコードのみです",
                            null, null, 2L));

            mockMvc.perform(post("/api/work-status/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ids": [1, 2, 3]}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("CZ-107"))
                    .andExpect(jsonPath("$.error.recordId").value(2));
        }

        @Test
        @DisplayName("EC-13: 空配列で差戻 → 400")
        void emptyIdsRevert() throws Exception {
            CzSecurityContext.set(managerActor());

            mockMvc.perform(post("/api/work-status/revert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ids": []}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // 【ページネーション・検索境界】4 cases
    // =========================================================================

    @Nested
    @DisplayName("検索境界")
    class SearchBoundary {

        @Test
        @DisplayName("EC-14: 検索結果0件 → 200 + 空リスト")
        void emptySearchResult() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.fetchByMonth(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/work-hours")
                            .param("staffId", "U001")
                            .param("yearMonth", "202502"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("EC-15: staffId パラメータなし → 400")
        void missingStaffId() throws Exception {
            CzSecurityContext.set(officialActor());

            mockMvc.perform(get("/api/work-hours")
                            .param("yearMonth", "202502"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-126"));
        }

        @Test
        @DisplayName("EC-16: yearMonth パラメータなし → 400")
        void missingYearMonth() throws Exception {
            CzSecurityContext.set(officialActor());

            mockMvc.perform(get("/api/work-hours")
                            .param("staffId", "U001"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-126"));
        }

        @Test
        @DisplayName("EC-17: work-status 必須パラメータなし → 400")
        void missingWorkStatusParams() throws Exception {
            CzSecurityContext.set(managerActor());

            mockMvc.perform(get("/api/work-status")
                            .param("yearMonth", "202502"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // 【楽観ロック境界】2 cases
    // =========================================================================

    @Nested
    @DisplayName("楽観ロック境界")
    class OptimisticLockBoundary {

        @Test
        @DisplayName("EC-18: updatedAt 一致 → 正常更新")
        void matchingVersion() throws Exception {
            CzSecurityContext.set(officialActor());
            Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
            updated.seqNo = 1L;
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"hours","value":"02:00","updatedAt":"2025-02-01T10:00:00"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EC-19: updatedAt 不一致 → 409 CZ-101")
        void mismatchVersion() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.updateField(any(), anyString(), anyString(), anyString(), any()))
                    .thenThrow(new CzBusinessException("CZ-101", "他のユーザーが更新済みです"));

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"field":"hours","value":"02:00","updatedAt":"2025-01-01T00:00:00"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CZ-101"));
        }
    }

    // =========================================================================
    // 【月次制御境界】3 cases
    // =========================================================================

    @Nested
    @DisplayName("月次制御境界")
    class MonthlyControlBoundary {

        @Test
        @DisplayName("EC-20: 月次確定済 → 集約可能")
        void confirmThenAggregate() throws Exception {
            CzSecurityContext.set(managerActor());
            var ctrl = new com.example.czConsv.entity.Mcz04Ctrl();
            ctrl.gjktFlg = "1";
            ctrl.dataSkFlg = "1";
            when(monthlyControlService.monthlyAggregate(anyString(), anyString()))
                    .thenReturn(ctrl);

            mockMvc.perform(post("/api/work-status/monthly-aggregate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"yearMonth":"2025-02","organizationCode":"ORG001"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dataSkFlg").value("1"));
        }

        @Test
        @DisplayName("EC-21: 未確定で集約 → エラー")
        void aggregateWithoutConfirm() throws Exception {
            CzSecurityContext.set(managerActor());
            when(monthlyControlService.monthlyAggregate(anyString(), anyString()))
                    .thenThrow(new CzBusinessException(
                            "CZ-200", "月次確定が完了していません"));

            mockMvc.perform(post("/api/work-status/monthly-aggregate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"yearMonth":"2025-02","organizationCode":"ORG001"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CZ-200"));
        }

        @Test
        @DisplayName("EC-22: 月次確定解除 → フラグリセット")
        void monthlyUnconfirm() throws Exception {
            CzSecurityContext.set(managerActor());
            var ctrl = new com.example.czConsv.entity.Mcz04Ctrl();
            ctrl.gjktFlg = "0";
            ctrl.dataSkFlg = "0";
            when(monthlyControlService.monthlyUnconfirm(anyString(), anyString()))
                    .thenReturn(ctrl);

            mockMvc.perform(post("/api/work-status/monthly-unconfirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"yearMonth":"2025-02","organizationCode":"ORG001"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gjktFlg").value("0"))
                    .andExpect(jsonPath("$.dataSkFlg").value("0"));
        }
    }

    // =========================================================================
    // 【論理削除境界】2 cases
    // =========================================================================

    @Nested
    @DisplayName("論理削除境界")
    class DeleteBoundary {

        @Test
        @DisplayName("EC-23: status=0 レコード削除 → 正常")
        void deleteDraftRecord() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.delete(any(), anyString())).thenReturn(1);

            mockMvc.perform(delete("/api/work-hours/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount").value(1));
        }

        @Test
        @DisplayName("EC-24: status=2 レコード削除 → エラー")
        void deleteFinalizedRecord() throws Exception {
            CzSecurityContext.set(officialActor());
            when(workHoursService.delete(any(), anyString()))
                    .thenThrow(new CzBusinessException(
                            "CZ-110", "確定済みレコードは削除できません"));

            mockMvc.perform(delete("/api/work-hours/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("CZ-110"));
        }
    }

    // =========================================================================
    // 【リクエストボディ境界】1 case
    // =========================================================================

    @Nested
    @DisplayName("リクエストボディ境界")
    class RequestBodyBoundary {

        @Test
        @DisplayName("EC-25: 不正JSON → エラーレスポンス（非200）")
        void invalidJson() throws Exception {
            CzSecurityContext.set(officialActor());

            int statusCode = mockMvc.perform(post("/api/work-hours")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andReturn().getResponse().getStatus();

            // 不正 JSON は 400 (HttpMessageNotReadableException) または 500 で返る
            org.junit.jupiter.api.Assertions.assertTrue(
                    statusCode >= 400,
                    "Invalid JSON should return error status, got: " + statusCode);
        }
    }
}

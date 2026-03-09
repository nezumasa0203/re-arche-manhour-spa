package com.example.czConsv.controller;

import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.*;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.WorkHoursService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T-055: 楽観的ロック競合テスト。
 *
 * PATCH /work-hours/{id} で updatedAt 不一致時に 409 + CZ-101 が返ることを検証する。
 * 同時更新シナリオ: ユーザーA取得→ユーザーB更新→ユーザーA更新試行→409。
 */
@WebMvcTest(
        controllers = WorkHoursController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class OptimisticLockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkHoursService workHoursService;
    @MockBean
    private ExcelExportService excelExportService;

    private static final LocalDateTime ORIGINAL_TS = LocalDateTime.of(2025, 2, 1, 10, 0, 0);
    private static final LocalDateTime UPDATED_TS = LocalDateTime.of(2025, 2, 1, 10, 5, 0);

    @BeforeEach
    void setUp() {
        CzPermissions perms = new CzPermissions(
                false,
                new TabPermission(Map.of("bit0", true, "bit1", true)),
                TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(DataAuthority.BU, DataAuthority.BU, DataAuthority.BU),
                EmploymentType.OFFICIAL, null, false);
        CzSecurityContext.set(new CzPrincipal(
                "U001", "テストユーザー", "u001@test.com",
                "ORG001", "テスト組織", perms));
    }

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    @Test
    @DisplayName("updatedAt が一致する場合は正常に更新できる")
    void updateSucceedsWithMatchingTimestamp() throws Exception {
        Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
        updated.seqNo = 1L;
        updated.status = "0";
        updated.upddate = UPDATED_TS;

        when(workHoursService.updateField(eq(1L), anyString(), eq("hours"), eq("02:30"), eq(ORIGINAL_TS)))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/work-hours/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"hours","value":"02:30","updatedAt":"2025-02-01T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seqNo").value(1));
    }

    @Test
    @DisplayName("updatedAt 不一致（他ユーザー更新済み）→ 409 + CZ-101")
    void updateFailsWithStaleTimestamp() throws Exception {
        when(workHoursService.updateField(eq(1L), anyString(), eq("hours"), eq("02:30"), eq(ORIGINAL_TS)))
                .thenThrow(new CzBusinessException("CZ-101", "他のユーザーが更新済みです"));

        mockMvc.perform(patch("/api/work-hours/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"hours","value":"02:30","updatedAt":"2025-02-01T10:00:00"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CZ-101"));
    }

    @Test
    @DisplayName("同時更新シナリオ: ユーザーA成功 → ユーザーB競合")
    void concurrentUpdateScenario() throws Exception {
        // ユーザーA の更新は成功
        Tcz01HosyuKousuu updatedByA = new Tcz01HosyuKousuu();
        updatedByA.seqNo = 1L;
        updatedByA.upddate = UPDATED_TS;

        when(workHoursService.updateField(eq(1L), anyString(), eq("subject"), eq("A更新"), eq(ORIGINAL_TS)))
                .thenReturn(updatedByA);

        mockMvc.perform(patch("/api/work-hours/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"subject","value":"A更新","updatedAt":"2025-02-01T10:00:00"}
                                """))
                .andExpect(status().isOk());

        // ユーザーB は古い updatedAt で更新試行 → CZ-101
        when(workHoursService.updateField(eq(1L), anyString(), eq("subject"), eq("B更新"), eq(ORIGINAL_TS)))
                .thenThrow(new CzBusinessException("CZ-101", "他のユーザーが更新済みです"));

        mockMvc.perform(patch("/api/work-hours/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"subject","value":"B更新","updatedAt":"2025-02-01T10:00:00"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CZ-101"))
                .andExpect(jsonPath("$.error.message").value("他のユーザーが更新済みです"));
    }

    @Test
    @DisplayName("削除でも楽観ロック競合が発生する場合 → 409")
    void deleteWithOptimisticLock() throws Exception {
        when(workHoursService.delete(any(), anyString()))
                .thenThrow(new CzBusinessException("CZ-101", "他のユーザーが更新済みです"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/work-hours/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CZ-101"));
    }
}

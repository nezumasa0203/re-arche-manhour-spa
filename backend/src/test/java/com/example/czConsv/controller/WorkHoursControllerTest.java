package com.example.czConsv.controller;

import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.WorkHoursService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WorkHoursController @WebMvcTest スライステスト。
 * Security を除外し、CzSecurityContext を BeforeEach で直接設定する。
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
class WorkHoursControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkHoursService workHoursService;

    @MockBean
    private ExcelExportService excelExportService;

    /** テスト用プリンシパル（通常モード: jinjiMode=false → skbtcd="00"）。 */
    private CzPrincipal principal;

    @BeforeEach
    void setUpSecurityContext() {
        CzPermissions permissions = new CzPermissions(
                false,                          // jinjiMode = false → skbtcd="00"
                TabPermission.EMPTY,
                TabPermission.EMPTY,
                TabPermission.EMPTY,
                new DataAuthority(null, null, null),
                EmploymentType.OFFICIAL,
                1,
                false);
        principal = new CzPrincipal(
                "U001", "Test User", "test@example.com",
                "ORG001", "テスト組織", permissions);
        CzSecurityContext.set(principal);
    }

    @AfterEach
    void clearSecurityContext() {
        CzSecurityContext.clear();
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /api/work-hours
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class ListEndpoint {

        @Test
        void returnsRecordsForGivenMonth() throws Exception {
            Tcz01HosyuKousuu record = new Tcz01HosyuKousuu();
            record.seqNo = 1L;
            when(workHoursService.fetchByMonth("U001", "2025-02"))
                    .thenReturn(List.of(record));

            mockMvc.perform(get("/api/work-hours")
                            .param("staffId", "U001")
                            .param("yearMonth", "2025-02"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].seqNo").value(1));
        }

        @Test
        void returnsEmptyListWhenNoRecords() throws Exception {
            when(workHoursService.fetchByMonth(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/work-hours")
                            .param("staffId", "U001")
                            .param("yearMonth", "2025-02"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /api/work-hours
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class CreateEndpoint {

        @Test
        void returns201OnSuccess() throws Exception {
            Tcz01HosyuKousuu created = new Tcz01HosyuKousuu();
            created.seqNo = 10L;
            when(workHoursService.create(any())).thenReturn(created);

            String body = """
                    {"yearMonth":"2025-02","workDate":"2025-02-01",
                     "targetSubsystemNo":null,"causeSubsystemNo":null,
                     "categoryCode":null,"subject":null,
                     "hours":null,"tmrNo":null,
                     "workRequestNo":null,"workRequesterName":null}
                    """;

            mockMvc.perform(post("/api/work-hours")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.seqNo").value(10));
        }

        @Test
        void returns400WhenYearMonthMissing() throws Exception {
            String body = """
                    {"workDate":"2025-02-01"}
                    """;

            mockMvc.perform(post("/api/work-hours")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PATCH /api/work-hours/{id}
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class UpdateEndpoint {

        @Test
        void updatesFieldAndReturns200() throws Exception {
            Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
            updated.seqNo = 5L;
            when(workHoursService.updateField(eq(5L), eq("00"), eq("subject"),
                    eq("テスト件名"), any(LocalDateTime.class)))
                    .thenReturn(updated);

            String body = """
                    {"field":"subject","value":"テスト件名",
                     "updatedAt":"2025-02-01T10:00:00"}
                    """;

            mockMvc.perform(patch("/api/work-hours/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seqNo").value(5));
        }

        @Test
        void returns400WhenFieldBlank() throws Exception {
            String body = """
                    {"field":"","value":"v","updatedAt":"2025-02-01T10:00:00"}
                    """;

            mockMvc.perform(patch("/api/work-hours/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // DELETE /api/work-hours/{id}
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class DeleteEndpoint {

        @Test
        void returnsDeletedCount() throws Exception {
            when(workHoursService.delete(List.of(3L), "00")).thenReturn(1);

            mockMvc.perform(delete("/api/work-hours/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount").value(1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /api/work-hours/copy
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class CopyEndpoint {

        @Test
        void returnsCopiedRecords() throws Exception {
            Tcz01HosyuKousuu copy = new Tcz01HosyuKousuu();
            copy.seqNo = 20L;
            when(workHoursService.copy(List.of(1L, 2L), "00"))
                    .thenReturn(List.of(copy));

            String body = """
                    {"ids":[1,2]}
                    """;

            mockMvc.perform(post("/api/work-hours/copy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].seqNo").value(20));
        }

        @Test
        void returns400WhenIdsEmpty() throws Exception {
            String body = """
                    {"ids":[]}
                    """;

            mockMvc.perform(post("/api/work-hours/copy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /api/work-hours/transfer
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class TransferEndpoint {

        @Test
        void returnsTransferredRecords() throws Exception {
            Tcz01HosyuKousuu transferred = new Tcz01HosyuKousuu();
            transferred.seqNo = 30L;
            when(workHoursService.transferNextMonth(
                    eq(List.of(1L)), eq("00"), eq(List.of("2025-03"))))
                    .thenReturn(List.of(transferred));

            String body = """
                    {"ids":[1],"targetMonths":["2025-03"]}
                    """;

            mockMvc.perform(post("/api/work-hours/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].seqNo").value(30));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /api/work-hours/batch-confirm
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class BatchConfirmEndpoint {

        @Test
        void returnsConfirmedCount() throws Exception {
            when(workHoursService.batchConfirm("2025-02")).thenReturn(5);

            String body = """
                    {"yearMonth":"2025-02"}
                    """;

            mockMvc.perform(post("/api/work-hours/batch-confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.confirmedCount").value(5));
        }

        @Test
        void returns400WhenYearMonthInvalid() throws Exception {
            String body = """
                    {"yearMonth":"202502"}
                    """;

            mockMvc.perform(post("/api/work-hours/batch-confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /api/work-hours/batch-revert
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class BatchRevertEndpoint {

        @Test
        void returnsRevertedCount() throws Exception {
            when(workHoursService.batchRevert("2025-02")).thenReturn(3);

            String body = """
                    {"yearMonth":"2025-02"}
                    """;

            mockMvc.perform(post("/api/work-hours/batch-revert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.revertedCount").value(3));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /api/work-hours/export/excel
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class ExportExcelEndpoint {

        @Test
        void returnsXlsxBytes() throws Exception {
            byte[] xlsxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK header
            when(workHoursService.fetchByMonth(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());
            when(excelExportService.exportWorkHoursDetail(anyList(), eq("2025-02")))
                    .thenReturn(xlsxBytes);

            mockMvc.perform(get("/api/work-hours/export/excel")
                            .param("staffId", "U001")
                            .param("yearMonth", "2025-02"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // skbtcd resolution: jinjiMode=true → "01"
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    class SkbtcdResolution {

        @Test
        void jinjiModeTrue_usesSkbtcd01() throws Exception {
            // 人事モードのプリンシパルに差し替え
            CzPermissions jinjiPerms = new CzPermissions(
                    true,   // jinjiMode
                    TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, 1, false);
            CzSecurityContext.set(new CzPrincipal(
                    "U002", "Jinji User", "jinji@example.com",
                    "ORG001", "テスト組織", jinjiPerms));

            when(workHoursService.delete(List.of(9L), "01")).thenReturn(1);

            mockMvc.perform(delete("/api/work-hours/9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount").value(1));
        }
    }
}

package com.example.czConsv.controller;

import com.example.czConsv.entity.Mcz04Ctrl;
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
import com.example.czConsv.service.MonthlyControlService;
import com.example.czConsv.service.WorkStatusService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = WorkStatusController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class WorkStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkStatusService workStatusService;

    @MockBean
    private MonthlyControlService monthlyControlService;

    @MockBean
    private ExcelExportService excelExportService;

    @BeforeEach
    void setUp() {
        CzPermissions permissions = new CzPermissions(
                false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(null, null, null), EmploymentType.OFFICIAL, 1, false);
        CzSecurityContext.set(new CzPrincipal(
                "U001", "Test User", "test@example.com",
                "ORG001", "テスト組織", permissions));
    }

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    @Nested
    class SearchEndpoint {

        @Test
        void returnsRecordList() throws Exception {
            Tcz01HosyuKousuu record = new Tcz01HosyuKousuu();
            record.seqNo = 1L;
            when(workStatusService.search("2025-02", "ORG001", null))
                    .thenReturn(List.of(record));

            mockMvc.perform(get("/api/work-status")
                            .param("yearMonth", "2025-02")
                            .param("organizationCode", "ORG001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].seqNo").value(1));
        }

        @Test
        void filtersbyStaffId() throws Exception {
            when(workStatusService.search("2025-02", "ORG001", "U001"))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/work-status")
                            .param("yearMonth", "2025-02")
                            .param("organizationCode", "ORG001")
                            .param("staffId", "U001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    class UpdateHoursEndpoint {

        @Test
        void updatesHoursAndReturns200() throws Exception {
            Tcz01HosyuKousuu updated = new Tcz01HosyuKousuu();
            updated.seqNo = 5L;
            when(workStatusService.updateHours(eq(5L), eq("00"), eq("02:30"), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/work-status/5/hours")
                            .param("hours", "02:30")
                            .param("updatedAt", "2025-02-01T10:00:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seqNo").value(5));
        }
    }

    @Nested
    class ApproveEndpoint {

        @Test
        void returnsApprovedCount() throws Exception {
            when(workStatusService.approve(List.of(1L, 2L), "00")).thenReturn(2);

            mockMvc.perform(post("/api/work-status/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ids\":[1,2]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.approvedCount").value(2));
        }

        @Test
        void returns400WhenIdsEmpty() throws Exception {
            mockMvc.perform(post("/api/work-status/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ids\":[]}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RevertEndpoint {

        @Test
        void returnsRevertedCount() throws Exception {
            when(workStatusService.revert(List.of(3L), "00")).thenReturn(1);

            mockMvc.perform(post("/api/work-status/revert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ids\":[3]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.revertedCount").value(1));
        }
    }

    @Nested
    class MonthlyConfirmEndpoint {

        @Test
        void returnsUpdatedCtrl() throws Exception {
            Mcz04Ctrl ctrl = new Mcz04Ctrl();
            ctrl.yyyymm = "202502";
            when(monthlyControlService.monthlyConfirm("2025-02", "ORG001"))
                    .thenReturn(ctrl);

            mockMvc.perform(post("/api/work-status/monthly-confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"yearMonth\":\"2025-02\",\"organizationCode\":\"ORG001\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.yyyymm").value("202502"));
        }

        @Test
        void returns400WhenYearMonthInvalid() throws Exception {
            mockMvc.perform(post("/api/work-status/monthly-confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"yearMonth\":\"202502\",\"organizationCode\":\"ORG001\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class MonthlyAggregateEndpoint {

        @Test
        void returnsUpdatedCtrl() throws Exception {
            Mcz04Ctrl ctrl = new Mcz04Ctrl();
            ctrl.yyyymm = "202502";
            when(monthlyControlService.monthlyAggregate("2025-02", "ORG001"))
                    .thenReturn(ctrl);

            mockMvc.perform(post("/api/work-status/monthly-aggregate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"yearMonth\":\"2025-02\",\"organizationCode\":\"ORG001\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.yyyymm").value("202502"));
        }
    }

    @Nested
    class MonthlyUnconfirmEndpoint {

        @Test
        void returnsUpdatedCtrl() throws Exception {
            Mcz04Ctrl ctrl = new Mcz04Ctrl();
            ctrl.yyyymm = "202502";
            when(monthlyControlService.monthlyUnconfirm("2025-02", "ORG001"))
                    .thenReturn(ctrl);

            mockMvc.perform(post("/api/work-status/monthly-unconfirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"yearMonth\":\"2025-02\",\"organizationCode\":\"ORG001\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.yyyymm").value("202502"));
        }
    }

    @Nested
    class ExportExcelEndpoint {

        @Test
        void returnsXlsxBytes() throws Exception {
            byte[] bytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
            when(workStatusService.search(anyString(), anyString(), isNull()))
                    .thenReturn(Collections.emptyList());
            when(excelExportService.exportWorkStatus(any(), eq("2025-02")))
                    .thenReturn(bytes);

            mockMvc.perform(get("/api/work-status/export/excel")
                            .param("yearMonth", "2025-02")
                            .param("organizationCode", "ORG001"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
    }
}

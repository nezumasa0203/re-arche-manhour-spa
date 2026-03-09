package com.example.czConsv.controller;

import com.example.czConsv.dto.response.HalfTrendsResponse;
import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.HalfTrendsService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HalfTrendsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class HalfTrendsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HalfTrendsService halfTrendsService;

    @MockBean
    private ExcelExportService excelExportService;

    private static final HalfTrendsResponse EMPTY_RESPONSE =
            new HalfTrendsResponse(Collections.emptyList(), null);

    @BeforeEach
    void setUp() {
        CzPermissions permissions = new CzPermissions(
                false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(null, null, null), EmploymentType.OFFICIAL, 1, false);
        CzSecurityContext.set(new CzPrincipal(
                "U001", "Test", "test@example.com", "ORG001", "テスト組織", permissions));
    }

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    @Nested
    class GetCategoriesEndpoint {

        @Test
        void returnsCategories() throws Exception {
            when(halfTrendsService.getCategories("2025-1", "hours", "all"))
                    .thenReturn(EMPTY_RESPONSE);

            mockMvc.perform(get("/api/half-trends/categories")
                            .param("yearHalf", "2025-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows").isArray());
        }

        @Test
        void acceptsDisplayModeParam() throws Exception {
            when(halfTrendsService.getCategories("2025-1", "cost", "all"))
                    .thenReturn(EMPTY_RESPONSE);

            mockMvc.perform(get("/api/half-trends/categories")
                            .param("yearHalf", "2025-1")
                            .param("displayMode", "cost"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class GetSystemsEndpoint {

        @Test
        void returnsSystems() throws Exception {
            when(halfTrendsService.getSystems("2025-1", "CAT01", "hours", "all"))
                    .thenReturn(EMPTY_RESPONSE);

            mockMvc.perform(get("/api/half-trends/systems")
                            .param("yearHalf", "2025-1")
                            .param("categoryCode", "CAT01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows").isArray());
        }
    }

    @Nested
    class GetSubsystemsEndpoint {

        @Test
        void returnsSubsystems() throws Exception {
            when(halfTrendsService.getSubsystems("2025-1", "SYS01", "CAT01", "hours", "all"))
                    .thenReturn(EMPTY_RESPONSE);

            mockMvc.perform(get("/api/half-trends/subsystems")
                            .param("yearHalf", "2025-1")
                            .param("systemNo", "SYS01")
                            .param("categoryCode", "CAT01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows").isArray());
        }
    }

    @Nested
    class ExportExcelEndpoint {

        @Test
        void returnsXlsxBytes() throws Exception {
            byte[] bytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
            when(halfTrendsService.getCategories(eq("2025-1"), any(), any()))
                    .thenReturn(EMPTY_RESPONSE);
            when(excelExportService.exportHalfTrends(any(), eq("2025-1")))
                    .thenReturn(bytes);

            mockMvc.perform(get("/api/half-trends/export/excel")
                            .param("yearHalf", "2025-1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
    }
}

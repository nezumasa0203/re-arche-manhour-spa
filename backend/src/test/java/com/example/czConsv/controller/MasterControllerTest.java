package com.example.czConsv.controller;

import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import com.example.czConsv.service.MasterService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MasterController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class MasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MasterService masterService;

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
    class OrganizationsEndpoint {

        @Test
        void returnsOrganizationList() throws Exception {
            when(masterService.getOrganizations()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/master/organizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.totalCount").value(0));
        }
    }

    @Nested
    class SystemsEndpoint {

        @Test
        void returnsSystemList() throws Exception {
            when(masterService.getSystems()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/master/systems"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Nested
    class SubsystemsEndpoint {

        @Test
        void returnsSubsystemsWithKeyword() throws Exception {
            when(masterService.getSubsystems("テスト")).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/master/subsystems")
                            .param("keyword", "テスト"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        void returnsAllSubsystemsWithNoKeyword() throws Exception {
            when(masterService.getSubsystems(null)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/master/subsystems"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Nested
    class StaffEndpoint {

        @Test
        void searchesStaffByName() throws Exception {
            when(masterService.searchStaff("山田")).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/master/staff")
                            .param("name", "山田"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Nested
    class CategoriesEndpoint {

        @Test
        void returnsCategoriesByFiscalYear() throws Exception {
            when(masterService.getCategories(2025)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/master/categories")
                            .param("fiscalYear", "2025"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }
}

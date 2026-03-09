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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DelegationController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class DelegationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MasterService masterService;

    @BeforeEach
    void setUp() {
        // canDelegate=true のプリンシパル
        CzPermissions permissions = new CzPermissions(
                false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(null, null, null), EmploymentType.OFFICIAL, 1, true);
        CzSecurityContext.set(new CzPrincipal(
                "U001", "Test", "test@example.com", "ORG001", "テスト組織", permissions));
    }

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    @Nested
    class AvailableStaffEndpoint {

        @Test
        void returnsAvailableStaff() throws Exception {
            when(masterService.searchStaff(null)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/delegation/available-staff"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    class SwitchEndpoint {

        @Test
        void switchesToTargetStaff() throws Exception {
            mockMvc.perform(post("/api/delegation/switch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetStaffId\":\"U002\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.delegationStaffId").value("U002"))
                    .andExpect(jsonPath("$.isDaiko").value(true));
        }

        @Test
        void cancelsDelegationWhenTargetNull() throws Exception {
            mockMvc.perform(post("/api/delegation/switch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetStaffId\":null}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isDaiko").value(false));
        }
    }
}

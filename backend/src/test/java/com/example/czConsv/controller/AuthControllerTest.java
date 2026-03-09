package com.example.czConsv.controller;

import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CzPermissions permissions = new CzPermissions(
                false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority("KYOKU", "KYOKU", "KYOKU"),
                EmploymentType.OFFICIAL, 1, false);
        CzSecurityContext.set(new CzPrincipal(
                "U001", "Test User", "test@example.com",
                "ORG001", "テスト組織", permissions));
    }

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    @Nested
    class MeEndpoint {

        @Test
        void returnsCurrentUserInfo() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("U001"))
                    .andExpect(jsonPath("$.userName").value("Test User"))
                    .andExpect(jsonPath("$.jinjiMode").value(false))
                    .andExpect(jsonPath("$.permissions").exists())
                    .andExpect(jsonPath("$.dataAuthority").exists());
        }

        @Test
        void returns401WhenNotAuthenticated() throws Exception {
            CzSecurityContext.clear();

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Not authenticated"));
        }
    }

    @Nested
    class StatusMatrixEndpoint {

        @Test
        void returnsMatrixForStatusKey() throws Exception {
            mockMvc.perform(get("/api/auth/status-matrix")
                            .param("statusKey", "000"))
                    .andExpect(status().isOk());
        }

        @Test
        void returnsMissingParamErrorWithoutStatusKey() throws Exception {
            mockMvc.perform(get("/api/auth/status-matrix"))
                    .andExpect(status().isBadRequest());
        }
    }
}

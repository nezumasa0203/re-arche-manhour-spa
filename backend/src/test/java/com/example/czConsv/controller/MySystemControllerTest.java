package com.example.czConsv.controller;

import com.example.czConsv.entity.Tcz19MySys;
import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import com.example.czConsv.service.MySystemService;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MySystemController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class MySystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MySystemService mySystemService;

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
    class ListEndpoint {

        @Test
        void returnsMySystemList() throws Exception {
            when(mySystemService.getMySystemList("U001")).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/my-systems"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    class RegisterEndpoint {

        @Test
        void returns201OnSuccess() throws Exception {
            Tcz19MySys sys = new Tcz19MySys();
            when(mySystemService.registerMySystem("U001", "SYS01")).thenReturn(sys);

            mockMvc.perform(post("/api/my-systems")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"systemNo\":\"SYS01\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        void returns400WhenSystemNoBlank() throws Exception {
            mockMvc.perform(post("/api/my-systems")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"systemNo\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RemoveEndpoint {

        @Test
        void returns200OnDelete() throws Exception {
            doNothing().when(mySystemService).removeMySystem("U001", "SYS01");

            mockMvc.perform(delete("/api/my-systems/SYS01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("deleted"));
        }
    }
}

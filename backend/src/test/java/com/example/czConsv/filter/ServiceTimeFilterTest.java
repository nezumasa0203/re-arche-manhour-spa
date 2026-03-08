package com.example.czConsv.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ServiceTimeFilter のユニットテスト。
 *
 * <p>BR-001: サービス提供時間 6:00〜23:30。
 * POST/PATCH/DELETE は時間外→403 + CZ-102。GET は時間外でも許可。
 */
class ServiceTimeFilterTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filterChain = mock(FilterChain.class);
    }

    // ─── 境界値テスト（POST） ──────────────────────────────

    @Test
    void post_at0559_rejected() throws Exception {
        ServiceTimeFilter filter = filterAt(5, 59);
        MockHttpServletRequest request = postRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("CZ-102"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void post_at0600_allowed() throws Exception {
        ServiceTimeFilter filter = filterAt(6, 0);
        MockHttpServletRequest request = postRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void post_at2330_allowed() throws Exception {
        ServiceTimeFilter filter = filterAt(23, 30);
        MockHttpServletRequest request = postRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void post_at2331_rejected() throws Exception {
        ServiceTimeFilter filter = filterAt(23, 31);
        MockHttpServletRequest request = postRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("CZ-102"));
        verify(filterChain, never()).doFilter(request, response);
    }

    // ─── 時間内テスト ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {6, 7, 12, 18, 23})
    void post_duringServiceHours_allowed(int hour) throws Exception {
        ServiceTimeFilter filter = filterAt(hour, 0);
        MockHttpServletRequest request = postRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ─── 時間外テスト ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void post_beforeServiceHours_rejected(int hour) throws Exception {
        ServiceTimeFilter filter = filterAt(hour, 0);
        MockHttpServletRequest request = postRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    // ─── GET は時間外でも許可 ──────────────────────────────

    @Test
    void get_outsideServiceHours_allowed() throws Exception {
        ServiceTimeFilter filter = filterAt(3, 0);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/work-hours");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void get_at0559_allowed() throws Exception {
        ServiceTimeFilter filter = filterAt(5, 59);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/work-hours");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void get_at2331_allowed() throws Exception {
        ServiceTimeFilter filter = filterAt(23, 31);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/work-hours");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ─── PATCH / DELETE も制限対象 ────────────────────────

    @Test
    void patch_outsideServiceHours_rejected() throws Exception {
        ServiceTimeFilter filter = filterAt(2, 0);
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/work-hours/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void delete_outsideServiceHours_rejected() throws Exception {
        ServiceTimeFilter filter = filterAt(4, 30);
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/work-hours/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    // ─── レスポンス形式確認 ──────────────────────────────

    @Test
    void rejectedResponse_containsErrorJson() throws Exception {
        ServiceTimeFilter filter = filterAt(3, 0);
        MockHttpServletRequest request = postRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"code\":\"CZ-102\""));
        assertTrue(body.contains("\"message\""));
        assertTrue(body.contains("サービス提供時間外"));
    }

    // ─── ヘルパーメソッド ────────────────────────────────

    private ServiceTimeFilter filterAt(int hour, int minute) {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, hour, minute);
        Instant instant = dateTime.atZone(TOKYO).toInstant();
        Clock fixedClock = Clock.fixed(instant, TOKYO);
        return new ServiceTimeFilter(fixedClock);
    }

    private MockHttpServletRequest postRequest() {
        return new MockHttpServletRequest("POST", "/api/work-hours");
    }
}

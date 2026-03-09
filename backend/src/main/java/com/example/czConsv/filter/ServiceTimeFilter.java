package com.example.czConsv.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * BR-001: サービス提供時間チェックフィルタ。
 *
 * <p>6:00〜23:30 の範囲外で POST/PATCH/DELETE リクエストを拒否する。
 * GET リクエストは時間外でも許可（参照のみ可能）。
 *
 * <p>タイムゾーンは {@code Asia/Tokyo} 固定。
 * テスト時は {@link Clock#fixed} を注入して時刻を制御可能。
 * {@code app.service-time.enabled=false} でチェックを無効化可能（dev/E2E 用）。
 */
@Component
public class ServiceTimeFilter extends OncePerRequestFilter {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final LocalTime SERVICE_START = LocalTime.of(6, 0);
    private static final LocalTime SERVICE_END = LocalTime.of(23, 30);

    private final Clock clock;
    private final boolean enabled;

    public ServiceTimeFilter(Clock clock,
                             @Value("${app.service-time.enabled:true}") boolean enabled) {
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // チェック無効化時はそのまま通過
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // GET は時間外でも許可
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        LocalTime now = LocalTime.now(clock.withZone(TOKYO));

        // 6:00 <= now <= 23:30
        if (!now.isBefore(SERVICE_START) && !now.isAfter(SERVICE_END)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 時間外 → 403 + CZ-102
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"error\":{\"code\":\"CZ-102\","
                + "\"message\":\"サービス提供時間外（6:00〜23:30）のためデータ操作できません\"}}"
        );
    }
}

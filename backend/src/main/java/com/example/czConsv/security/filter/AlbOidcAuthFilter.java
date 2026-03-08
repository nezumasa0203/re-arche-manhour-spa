package com.example.czConsv.security.filter;

import com.example.czConsv.dao.Mcz21KanriTaisyoDao;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ALB + Okta OIDC 認証フィルター。
 *
 * 本番: X-Amzn-Oidc-Data ヘッダーから JWT を取得・検証
 * 開発: Auth Mock が発行した JWT を同ヘッダーまたは Authorization ヘッダーから取得
 *
 * JWT クレームから 4層権限モデル (CzPermissions) を構築し、
 * CzPrincipal として CzSecurityContext と SecurityContextHolder にセットする。
 */
public class AlbOidcAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AlbOidcAuthFilter.class);

    private static final String ALB_OIDC_DATA_HEADER = "X-Amzn-Oidc-Data";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DELEGATION_HEADER = "X-Delegation-Staff-Id";

    private final SecretKey secretKey;
    private final Mcz21KanriTaisyoDao kanriTaisyoDao;

    /**
     * DAO なしコンストラクタ（後方互換用・代行モード検証不可）。
     */
    public AlbOidcAuthFilter(String jwtSecret) {
        this(jwtSecret, null);
    }

    /**
     * DAO 付きコンストラクタ（代行モード検証対応）。
     */
    public AlbOidcAuthFilter(String jwtSecret, Mcz21KanriTaisyoDao kanriTaisyoDao) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.kanriTaisyoDao = kanriTaisyoDao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null) {
                Claims claims = parseJwt(token);
                if (claims != null) {
                    CzPrincipal principal = buildPrincipal(claims);

                    // 代行モード処理
                    String delegationStaffId = request.getHeader(DELEGATION_HEADER);
                    if (delegationStaffId != null && !delegationStaffId.isBlank()) {
                        principal = handleDelegation(principal, delegationStaffId.trim(),
                                response);
                        if (principal == null) {
                            // 代行権限検証NG — エラーレスポンス送信済み
                            return;
                        }
                    }

                    CzSecurityContext.set(principal);

                    List<SimpleGrantedAuthority> authorities = buildAuthorities(principal);
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            CzSecurityContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        // 優先1: ALB OIDC ヘッダー (本番環境)
        String albToken = request.getHeader(ALB_OIDC_DATA_HEADER);
        if (albToken != null && !albToken.isBlank()) {
            return albToken;
        }

        // 優先2: Authorization: Bearer (開発環境 Auth Mock)
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private Claims parseJwt(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private CzPrincipal buildPrincipal(Claims claims) {
        String userId = claims.getSubject();
        String userName = claims.get("name", String.class);
        String email = claims.get("email", String.class);
        String orgCode = claims.get("organizationCode", String.class);
        String orgName = claims.get("organizationName", String.class);

        // Layer 1: JinjiMode
        Boolean jinjiMode = claims.get("jinjiMode", Boolean.class);

        // Layer 2: 機能権限 (ビットベース)
        Map<String, Object> permissionsMap = claims.get("permissions", Map.class);
        TabPermission tab010 = parseTab(permissionsMap, "tab010");
        TabPermission tab011 = parseTab(permissionsMap, "tab011");
        TabPermission tab012 = parseTab(permissionsMap, "tab012");

        // Layer 3: データアクセス権限
        Map<String, Object> authMap = claims.get("dataAuthority", Map.class);
        DataAuthority dataAuthority = new DataAuthority(
                getStringOrNull(authMap, "ref"),
                getStringOrNull(authMap, "ins"),
                getStringOrNull(authMap, "upd")
        );

        // Layer 4: 雇用形態
        Integer empTypeCode = claims.get("employmentType", Integer.class);
        EmploymentType employmentType = EmploymentType.fromCode(
                empTypeCode != null ? empTypeCode : 0);

        // 追加属性
        Integer staffRole = claims.get("staffRole", Integer.class);
        Boolean canDelegate = claims.get("canDelegate", Boolean.class);

        CzPermissions permissions = new CzPermissions(
                jinjiMode != null && jinjiMode,
                tab010, tab011, tab012,
                dataAuthority,
                employmentType,
                staffRole,
                canDelegate != null && canDelegate
        );

        return new CzPrincipal(userId, userName, email, orgCode, orgName, permissions);
    }

    @SuppressWarnings("unchecked")
    private TabPermission parseTab(Map<String, Object> permissionsMap, String tabKey) {
        if (permissionsMap == null) {
            return TabPermission.EMPTY;
        }
        Object tabObj = permissionsMap.get(tabKey);
        if (tabObj instanceof Map<?, ?> tabMap) {
            Map<String, Boolean> bits = new HashMap<>();
            tabMap.forEach((k, v) -> {
                if (k instanceof String key && v instanceof Boolean value) {
                    bits.put(key, value);
                }
            });
            return new TabPermission(Collections.unmodifiableMap(bits));
        }
        return TabPermission.EMPTY;
    }

    private String getStringOrNull(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof String s ? s : null;
    }

    /**
     * 代行モード検証。
     *
     * <ol>
     *   <li>CzPermissions.employmentType == SUBCONTRACT (3) であること</li>
     *   <li>CzPermissions.canDelegate == true であること</li>
     *   <li>mcz21_kanri_taisyo テーブルに代行関係が存在すること
     *       (kanritnt_esqid=現ユーザー AND kanritsy_esqid=代行対象者)</li>
     * </ol>
     *
     * @return 代行モード付き CzPrincipal、検証 NG の場合は null（エラーレスポンス送信済み）
     */
    private CzPrincipal handleDelegation(CzPrincipal principal,
                                          String delegationStaffId,
                                          HttpServletResponse response) throws IOException {
        CzPermissions perm = principal.permissions();

        // 条件1: 外部契約者 (SUBCONTRACT) であること
        if (perm.employmentType() != EmploymentType.SUBCONTRACT) {
            log.warn("Delegation denied: user {} is not SUBCONTRACT (type={})",
                    principal.userId(), perm.employmentType());
            sendDelegationError(response);
            return null;
        }

        // 条件2: canDelegate == true であること
        if (!perm.canDelegate()) {
            log.warn("Delegation denied: user {} does not have canDelegate permission",
                    principal.userId());
            sendDelegationError(response);
            return null;
        }

        // 条件3: mcz21_kanri_taisyo に代行関係が存在すること
        if (kanriTaisyoDao != null) {
            boolean exists = kanriTaisyoDao
                    .selectById(delegationStaffId, principal.userId())
                    .isPresent();
            if (!exists) {
                log.warn("Delegation denied: no delegation relationship "
                                + "from {} to {} in mcz21_kanri_taisyo",
                        principal.userId(), delegationStaffId);
                sendDelegationError(response);
                return null;
            }
        }

        // 検証OK → delegationStaffId を設定した新しい CzPrincipal を返す
        return new CzPrincipal(
                principal.userId(), principal.userName(), principal.email(),
                principal.organizationCode(), principal.organizationName(),
                principal.permissions(), delegationStaffId);
    }

    /**
     * 代行権限エラー (CZ-307) を 403 レスポンスとして返す。
     */
    private void sendDelegationError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"errorCode\":\"CZ-307\",\"message\":\"代行権限がありません\"}");
        response.getWriter().flush();
    }

    private List<SimpleGrantedAuthority> buildAuthorities(CzPrincipal principal) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        CzPermissions p = principal.permissions();

        // JinjiMode
        authorities.add(new SimpleGrantedAuthority(
                p.jinjiMode() ? "MODE_JINJI" : "MODE_KANRI"));

        // TAB 010
        if (p.canReport()) {
            authorities.add(new SimpleGrantedAuthority("TAB010_REPORT"));
        }
        if (p.canManageReports()) {
            authorities.add(new SimpleGrantedAuthority("TAB010_MANAGE"));
        }
        if (p.canFullManage()) {
            authorities.add(new SimpleGrantedAuthority("TAB010_FULL"));
        }

        // 雇用形態
        authorities.add(new SimpleGrantedAuthority(
                "EMP_" + p.employmentType().name()));

        // スタッフ種別
        if (p.staffRole() != null) {
            authorities.add(new SimpleGrantedAuthority("STAFF_" + p.staffRole()));
        }

        return authorities;
    }
}

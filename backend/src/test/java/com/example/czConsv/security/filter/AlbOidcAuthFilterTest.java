package com.example.czConsv.security.filter;

import com.example.czConsv.dao.Mcz21KanriTaisyoDao;
import com.example.czConsv.entity.Mcz21KanriTaisyo;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlbOidcAuthFilter の単体テスト。
 * JWT パース、CzPrincipal 構築、4層権限モデルの復元、
 * SecurityContextHolder / CzSecurityContext への設定・クリアを検証する。
 */
@DisplayName("AlbOidcAuthFilter: ALB OIDC 認証フィルター")
class AlbOidcAuthFilterTest {

    /** テスト用シークレット（HS256 に必要な 32 バイト以上） */
    private static final String JWT_SECRET = "dev-mock-secret-key-for-testing!!";

    private SecretKey secretKey;
    private AlbOidcAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        filter = new AlbOidcAuthFilter(JWT_SECRET);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        CzSecurityContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        CzSecurityContext.clear();
    }

    // ========================================================================
    // JWT ビルダーヘルパー
    // ========================================================================

    /**
     * クレームマップから署名済み JWT 文字列を生成する。
     */
    private String buildJwt(Map<String, Object> claims) {
        return Jwts.builder()
                .subject(claims.get("sub").toString())
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 基本的なユーザークレームを生成する（4層権限なし）。
     */
    private Map<String, Object> baseClaims(String userId, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("name", name);
        claims.put("email", userId + "@example.com");
        claims.put("organizationCode", "ORG001");
        claims.put("organizationName", "テスト部門");
        claims.put("jinjiMode", false);
        claims.put("permissions", Map.of(
                "tab010", Map.of("bit0", true, "bit1", false, "bit2", false),
                "tab011", Map.of("bit0", false, "bit1", false),
                "tab012", Map.of("bit0", false, "bit1", false)
        ));
        claims.put("dataAuthority", Map.of(
                "ref", "ZENSYA",
                "ins", "HONBU",
                "upd", "HONBU"
        ));
        claims.put("employmentType", 0);
        claims.put("staffRole", 931);
        claims.put("canDelegate", false);
        return claims;
    }

    /**
     * ACT-03（全権管理者）ペイロードを生成する。
     * tab010.bit2=true, jinjiMode=false
     */
    private Map<String, Object> act03Claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "ACT03-USER");
        claims.put("name", "全権管理者");
        claims.put("email", "act03@example.com");
        claims.put("organizationCode", "ORG100");
        claims.put("organizationName", "管理本部");
        claims.put("jinjiMode", false);
        claims.put("permissions", Map.of(
                "tab010", Map.of("bit0", true, "bit1", true, "bit2", true),
                "tab011", Map.of("bit0", true, "bit1", true),
                "tab012", Map.of("bit0", true, "bit1", true)
        ));
        claims.put("dataAuthority", Map.of(
                "ref", "ZENSYA",
                "ins", "ZENSYA",
                "upd", "ZENSYA"
        ));
        claims.put("employmentType", 0);
        claims.put("staffRole", 931);
        claims.put("canDelegate", false);
        return claims;
    }

    /**
     * ACT-09（外部契約者）ペイロードを生成する。
     * employmentType=3, canDelegate=true
     */
    private Map<String, Object> act09Claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "ACT09-USER");
        claims.put("name", "外部契約者A");
        claims.put("email", "act09@partner.co.jp");
        claims.put("organizationCode", "ORG902");
        claims.put("organizationName", "委託先企業X");
        claims.put("jinjiMode", false);
        claims.put("permissions", Map.of(
                "tab010", Map.of("bit0", true, "bit1", false, "bit2", false),
                "tab011", Map.of("bit0", false, "bit1", false),
                "tab012", Map.of("bit0", false, "bit1", false)
        ));
        claims.put("dataAuthority", Map.of(
                "ref", "KA",
                "ins", "KA",
                "upd", "KA"
        ));
        claims.put("employmentType", 3);
        claims.put("staffRole", 936);
        claims.put("canDelegate", true);
        return claims;
    }

    // ========================================================================
    // 1. X-Amzn-Oidc-Data ヘッダーからの JWT 取得
    // ========================================================================

    @Test
    @DisplayName("X-Amzn-Oidc-Data ヘッダーの有効な JWT で CzPrincipal が構築される")
    void validJwtInAlbHeader_constructsCzPrincipal() throws ServletException, IOException {
        Map<String, Object> claims = baseClaims("U100", "田中太郎");
        String jwt = buildJwt(claims);
        request.addHeader("X-Amzn-Oidc-Data", jwt);

        // フィルター内で CzSecurityContext を確認するためカスタムチェーン
        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        filter.doFilter(request, response, chain);

        CzPrincipal principal = captured.get();
        assertNotNull(principal, "CzPrincipal が構築されること");
        assertEquals("U100", principal.userId());
        assertEquals("田中太郎", principal.userName());
        assertEquals("U100@example.com", principal.email());
        assertEquals("ORG001", principal.organizationCode());
        assertEquals("テスト部門", principal.organizationName());
    }

    // ========================================================================
    // 2. Authorization: Bearer ヘッダーからの JWT 取得
    // ========================================================================

    @Test
    @DisplayName("Authorization: Bearer ヘッダーの有効な JWT で CzPrincipal が構築される")
    void validJwtInBearerHeader_constructsCzPrincipal() throws ServletException, IOException {
        Map<String, Object> claims = baseClaims("U200", "佐藤花子");
        String jwt = buildJwt(claims);
        request.addHeader("Authorization", "Bearer " + jwt);

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        filter.doFilter(request, response, chain);

        CzPrincipal principal = captured.get();
        assertNotNull(principal, "Bearer トークンから CzPrincipal が構築されること");
        assertEquals("U200", principal.userId());
        assertEquals("佐藤花子", principal.userName());
    }

    // ========================================================================
    // 3. X-Amzn-Oidc-Data が Authorization より優先される
    // ========================================================================

    @Test
    @DisplayName("X-Amzn-Oidc-Data が Authorization: Bearer より優先される")
    void albHeaderTakesPriorityOverBearerHeader() throws ServletException, IOException {
        Map<String, Object> albClaims = baseClaims("ALB-USER", "ALB経由ユーザー");
        Map<String, Object> bearerClaims = baseClaims("BEARER-USER", "Bearer経由ユーザー");

        request.addHeader("X-Amzn-Oidc-Data", buildJwt(albClaims));
        request.addHeader("Authorization", "Bearer " + buildJwt(bearerClaims));

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        filter.doFilter(request, response, chain);

        CzPrincipal principal = captured.get();
        assertNotNull(principal, "プリンシパルが構築されること");
        assertEquals("ALB-USER", principal.userId(),
                "X-Amzn-Oidc-Data のトークンが優先されること");
        assertEquals("ALB経由ユーザー", principal.userName());
    }

    // ========================================================================
    // 4. トークンなし — 認証なしでリクエスト続行
    // ========================================================================

    @Test
    @DisplayName("トークンなしの場合、認証なしでリクエストが続行される")
    void noToken_proceedsWithoutAuthentication() throws ServletException, IOException {
        // ヘッダーを設定しない
        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        AtomicReference<Authentication> authCaptured = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            captured.set(CzSecurityContext.get());
            authCaptured.set(SecurityContextHolder.getContext().getAuthentication());
        };

        filter.doFilter(request, response, chain);

        assertNull(captured.get(), "CzSecurityContext にプリンシパルがセットされないこと");
        assertNull(authCaptured.get(), "SecurityContextHolder に Authentication がセットされないこと");
    }

    // ========================================================================
    // 5. 無効な JWT / 期限切れ JWT — 認証なしでリクエスト続行
    // ========================================================================

    @Test
    @DisplayName("無効な JWT の場合、認証なしでリクエストが続行される（警告ログ出力）")
    void invalidJwt_proceedsWithoutAuthentication() throws ServletException, IOException {
        request.addHeader("X-Amzn-Oidc-Data", "this.is.not.a.valid.jwt");

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        filter.doFilter(request, response, chain);

        assertNull(captured.get(), "不正な JWT の場合、CzPrincipal はセットされないこと");
    }

    @Test
    @DisplayName("期限切れの JWT の場合、認証なしでリクエストが続行される")
    void expiredJwt_proceedsWithoutAuthentication() throws ServletException, IOException {
        Map<String, Object> claims = baseClaims("EXPIRED-USER", "期限切れユーザー");
        // 過去の有効期限を設定した JWT を生成
        String expiredJwt = Jwts.builder()
                .subject("EXPIRED-USER")
                .claims(claims)
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(secretKey)
                .compact();

        request.addHeader("X-Amzn-Oidc-Data", expiredJwt);

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        filter.doFilter(request, response, chain);

        assertNull(captured.get(), "期限切れ JWT の場合、CzPrincipal はセットされないこと");
    }

    @Test
    @DisplayName("異なるシークレットで署名された JWT の場合、認証なしでリクエストが続行される")
    void wrongSecret_proceedsWithoutAuthentication() throws ServletException, IOException {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-that-is-32bytes!".getBytes(StandardCharsets.UTF_8));
        String jwt = Jwts.builder()
                .subject("WRONG-KEY-USER")
                .claims(baseClaims("WRONG-KEY-USER", "不正鍵ユーザー"))
                .signWith(wrongKey)
                .compact();

        request.addHeader("X-Amzn-Oidc-Data", jwt);

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        filter.doFilter(request, response, chain);

        assertNull(captured.get(), "異なるシークレットの JWT の場合、CzPrincipal はセットされないこと");
    }

    // ========================================================================
    // 6. 4層権限モデルのクレームから CzPermissions が正しく構築される
    // ========================================================================

    @Test
    @DisplayName("4層権限クレームから CzPermissions が正しく構築される")
    void fullFourLayerClaims_constructsCorrectPermissions() throws ServletException, IOException {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "PERM-USER");
        claims.put("name", "権限テストユーザー");
        claims.put("email", "perm@example.com");
        claims.put("organizationCode", "ORG500");
        claims.put("organizationName", "権限テスト部");

        // Layer 1: jinjiMode
        claims.put("jinjiMode", true);

        // Layer 2: 機能権限 (ビットベース)
        claims.put("permissions", Map.of(
                "tab010", Map.of("bit0", true, "bit1", true, "bit2", false),
                "tab011", Map.of("bit0", true, "bit1", false),
                "tab012", Map.of("bit0", false, "bit1", true)
        ));

        // Layer 3: データアクセス権限
        claims.put("dataAuthority", Map.of(
                "ref", "ZENSYA",
                "ins", "HONBU",
                "upd", "KYOKU"
        ));

        // Layer 4: 雇用形態
        claims.put("employmentType", 1);

        // 追加属性
        claims.put("staffRole", 933);
        claims.put("canDelegate", true);

        String jwt = buildJwt(claims);
        request.addHeader("X-Amzn-Oidc-Data", jwt);

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        filter.doFilter(request, response, chain);

        CzPrincipal principal = captured.get();
        assertNotNull(principal, "CzPrincipal が構築されること");

        CzPermissions perm = principal.permissions();

        // Layer 1: jinjiMode
        assertTrue(perm.jinjiMode(), "jinjiMode が true であること");

        // Layer 2: TabPermission - tab010
        assertTrue(perm.tab010().bit0(), "tab010.bit0 が true であること");
        assertTrue(perm.tab010().bit1(), "tab010.bit1 が true であること");
        assertFalse(perm.tab010().bit2(), "tab010.bit2 が false であること");
        assertTrue(perm.canReport(), "canReport() が true であること");
        assertTrue(perm.canManageReports(), "canManageReports() が true であること");
        assertFalse(perm.canFullManage(), "canFullManage() が false であること");

        // Layer 2: TabPermission - tab011
        assertTrue(perm.tab011().bit0(), "tab011.bit0 が true であること");
        assertFalse(perm.tab011().bit1(), "tab011.bit1 が false であること");
        assertTrue(perm.canOutputMaintenanceHours(), "canOutputMaintenanceHours() が true であること");
        assertFalse(perm.canNavigateBetweenForms(), "canNavigateBetweenForms() が false であること");

        // Layer 2: TabPermission - tab012
        assertFalse(perm.tab012().bit0(), "tab012.bit0 が false であること");
        assertTrue(perm.tab012().bit1(), "tab012.bit1 が true であること");
        assertFalse(perm.canInputPeriodCondition(), "canInputPeriodCondition() が false であること");
        assertTrue(perm.canAggregatePeriod(), "canAggregatePeriod() が true であること");

        // Layer 3: DataAuthority
        DataAuthority da = perm.dataAuthority();
        assertEquals("ZENSYA", da.ref(), "ref が ZENSYA であること");
        assertEquals("HONBU", da.ins(), "ins が HONBU であること");
        assertEquals("KYOKU", da.upd(), "upd が KYOKU であること");
        assertTrue(da.canRef(), "canRef() が true であること");
        assertTrue(da.canIns(), "canIns() が true であること");
        assertTrue(da.canUpd(), "canUpd() が true であること");

        // Layer 4: EmploymentType
        assertEquals(EmploymentType.TEMPORARY_1, perm.employmentType(),
                "employmentType が TEMPORARY_1 であること");

        // 追加属性
        assertEquals(933, perm.staffRole(), "staffRole が 933 であること");
        assertTrue(perm.canDelegate(), "canDelegate が true であること");
    }

    // ========================================================================
    // 7. CzSecurityContext はフィルター完了後にクリアされる
    // ========================================================================

    @Test
    @DisplayName("フィルター完了後、CzSecurityContext がクリアされる")
    void czSecurityContextClearedAfterFilter() throws ServletException, IOException {
        Map<String, Object> claims = baseClaims("CLEAR-USER", "クリアテスト");
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(CzSecurityContext.get(),
                "フィルター完了後、CzSecurityContext がクリアされていること");
    }

    @Test
    @DisplayName("FilterChain で例外が発生しても CzSecurityContext がクリアされる")
    void czSecurityContextClearedEvenOnException() {
        Map<String, Object> claims = baseClaims("EX-USER", "例外テスト");
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));

        FilterChain throwingChain = (req, res) -> {
            // フィルター内ではプリンシパルがセットされていることを確認
            assertNotNull(CzSecurityContext.get(),
                    "例外発生前にプリンシパルがセットされていること");
            throw new ServletException("テスト用の例外");
        };

        assertThrows(ServletException.class,
                () -> filter.doFilter(request, response, throwingChain));

        assertNull(CzSecurityContext.get(),
                "例外発生後も CzSecurityContext がクリアされていること");
    }

    // ========================================================================
    // 8. SecurityContextHolder に Authentication がセットされる
    // ========================================================================

    @Test
    @DisplayName("有効な JWT の場合、SecurityContextHolder に Authentication がセットされる")
    void validJwt_setsSecurityContextHolderAuthentication() throws ServletException, IOException {
        Map<String, Object> claims = baseClaims("AUTH-USER", "認証ユーザー");
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));

        AtomicReference<Authentication> authCaptured = new AtomicReference<>();
        FilterChain chain = (req, res) ->
                authCaptured.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        Authentication auth = authCaptured.get();
        assertNotNull(auth, "Authentication がセットされていること");
        assertTrue(auth.isAuthenticated(), "認証済み状態であること");
        assertInstanceOf(CzPrincipal.class, auth.getPrincipal(),
                "principal が CzPrincipal のインスタンスであること");

        CzPrincipal principal = (CzPrincipal) auth.getPrincipal();
        assertEquals("AUTH-USER", principal.userId());

        // GrantedAuthority の検証
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("MODE_KANRI")),
                "jinjiMode=false の場合 MODE_KANRI 権限が付与されること");
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("EMP_OFFICIAL")),
                "employmentType=0 の場合 EMP_OFFICIAL 権限が付与されること");
    }

    // ========================================================================
    // 9. ACT-03 ペイロード（全権管理者）
    // ========================================================================

    @Test
    @DisplayName("ACT-03: 全権管理者ペイロード — tab010.bit2=true, jinjiMode=false")
    void act03Payload_fullManagePermissions() throws ServletException, IOException {
        Map<String, Object> claims = act03Claims();
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        AtomicReference<Authentication> authCaptured = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            captured.set(CzSecurityContext.get());
            authCaptured.set(SecurityContextHolder.getContext().getAuthentication());
        };

        filter.doFilter(request, response, chain);

        CzPrincipal principal = captured.get();
        assertNotNull(principal, "ACT-03 のプリンシパルが構築されること");
        assertEquals("ACT03-USER", principal.userId());
        assertEquals("全権管理者", principal.userName());
        assertEquals("ORG100", principal.organizationCode());

        CzPermissions perm = principal.permissions();

        // ACT-03 のコア特性: tab010.bit2=true (全管理グループ)
        assertTrue(perm.tab010().bit2(), "tab010.bit2 が true であること（全管理グループ）");
        assertTrue(perm.canFullManage(), "canFullManage() が true であること");
        assertTrue(perm.useTanSeries(), "useTanSeries() が true であること（担当者系列）");

        // jinjiMode=false (管理モード)
        assertFalse(perm.jinjiMode(), "jinjiMode が false であること（管理モード）");

        // tab010 の全ビットが true
        assertTrue(perm.canReport(), "canReport() が true であること");
        assertTrue(perm.canManageReports(), "canManageReports() が true であること");

        // tab011/012 も全ビット true
        assertTrue(perm.canOutputMaintenanceHours(), "canOutputMaintenanceHours() が true であること");
        assertTrue(perm.canNavigateBetweenForms(), "canNavigateBetweenForms() が true であること");
        assertTrue(perm.canInputPeriodCondition(), "canInputPeriodCondition() が true であること");
        assertTrue(perm.canAggregatePeriod(), "canAggregatePeriod() が true であること");

        // データアクセス: 全社レベル
        DataAuthority da = perm.dataAuthority();
        assertEquals("ZENSYA", da.ref(), "ref が ZENSYA であること");
        assertEquals("ZENSYA", da.ins(), "ins が ZENSYA であること");
        assertEquals("ZENSYA", da.upd(), "upd が ZENSYA であること");

        // 正社員
        assertEquals(EmploymentType.OFFICIAL, perm.employmentType());
        assertTrue(perm.isOfficial(), "isOfficial() が true であること");

        // GrantedAuthority の検証
        Authentication auth = authCaptured.get();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("TAB010_FULL")),
                "TAB010_FULL 権限が付与されること");
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("TAB010_MANAGE")),
                "TAB010_MANAGE 権限が付与されること");
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("TAB010_REPORT")),
                "TAB010_REPORT 権限が付与されること");
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("MODE_KANRI")),
                "MODE_KANRI 権限が付与されること");
    }

    // ========================================================================
    // 10. ACT-09 ペイロード（外部契約者）
    // ========================================================================

    @Test
    @DisplayName("ACT-09: 外部契約者ペイロード — employmentType=3, canDelegate=true")
    void act09Payload_subcontractPermissions() throws ServletException, IOException {
        Map<String, Object> claims = act09Claims();
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        AtomicReference<Authentication> authCaptured = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            captured.set(CzSecurityContext.get());
            authCaptured.set(SecurityContextHolder.getContext().getAuthentication());
        };

        filter.doFilter(request, response, chain);

        CzPrincipal principal = captured.get();
        assertNotNull(principal, "ACT-09 のプリンシパルが構築されること");
        assertEquals("ACT09-USER", principal.userId());
        assertEquals("外部契約者A", principal.userName());
        assertEquals("act09@partner.co.jp", principal.email());
        assertEquals("ORG902", principal.organizationCode());
        assertEquals("委託先企業X", principal.organizationName());

        CzPermissions perm = principal.permissions();

        // ACT-09 のコア特性: employmentType=SUBCONTRACT(3)
        assertEquals(EmploymentType.SUBCONTRACT, perm.employmentType(),
                "employmentType が SUBCONTRACT であること");
        assertTrue(perm.isSubcontract(), "isSubcontract() が true であること");
        assertFalse(perm.isOfficial(), "isOfficial() が false であること");

        // canDelegate=true （代行モード許可）
        assertTrue(perm.canDelegate(), "canDelegate が true であること（代行モード許可）");

        // 外部契約者の制限: tab010 は bit0 のみ true
        assertTrue(perm.canReport(), "canReport() が true であること（報告は可能）");
        assertFalse(perm.canManageReports(), "canManageReports() が false であること（管理不可）");
        assertFalse(perm.canFullManage(), "canFullManage() が false であること（全管理不可）");
        assertFalse(perm.useTanSeries(), "useTanSeries() が false であること（管理者系列）");

        // tab011/012 は全て false
        assertFalse(perm.canOutputMaintenanceHours(), "canOutputMaintenanceHours() が false であること");
        assertFalse(perm.canNavigateBetweenForms(), "canNavigateBetweenForms() が false であること");
        assertFalse(perm.canInputPeriodCondition(), "canInputPeriodCondition() が false であること");
        assertFalse(perm.canAggregatePeriod(), "canAggregatePeriod() が false であること");

        // データアクセス: 課レベル（最小範囲）
        DataAuthority da = perm.dataAuthority();
        assertEquals("KA", da.ref(), "ref が KA であること");
        assertEquals("KA", da.ins(), "ins が KA であること");
        assertEquals("KA", da.upd(), "upd が KA であること");

        // staffRole
        assertEquals(936, perm.staffRole(), "staffRole が 936 であること");

        // GrantedAuthority の検証
        Authentication auth = authCaptured.get();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("EMP_SUBCONTRACT")),
                "EMP_SUBCONTRACT 権限が付与されること");
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("TAB010_REPORT")),
                "TAB010_REPORT 権限が付与されること");
        assertTrue(auth.getAuthorities().stream()
                        .noneMatch(a -> a.getAuthority().equals("TAB010_FULL")),
                "TAB010_FULL 権限が付与されないこと");
        assertTrue(auth.getAuthorities().stream()
                        .noneMatch(a -> a.getAuthority().equals("TAB010_MANAGE")),
                "TAB010_MANAGE 権限が付与されないこと");
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("STAFF_936")),
                "STAFF_936 権限が付与されること");
    }

    // ========================================================================
    // 11. 代行モード (Delegation Mode)
    // ========================================================================

    @Test
    @DisplayName("代行成功（DAO あり）: ACT-09 + 代行ヘッダー + DAO に関係あり → delegationStaffId がセットされる")
    void delegationSuccess_withDao_setsDelegationStaffId() throws ServletException, IOException {
        // Arrange: mock DAO returns present
        Mcz21KanriTaisyoDao mockDao = mock(Mcz21KanriTaisyoDao.class);
        Mcz21KanriTaisyo entity = new Mcz21KanriTaisyo();
        entity.kanritsyEsqid = "DELEGATEE-001";
        entity.kanritntEsqid = "ACT09-USER";
        when(mockDao.selectById("DELEGATEE-001", "ACT09-USER"))
                .thenReturn(Optional.of(entity));

        AlbOidcAuthFilter daoFilter = new AlbOidcAuthFilter(JWT_SECRET, mockDao);

        Map<String, Object> claims = act09Claims();
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        request.addHeader("X-Delegation-Staff-Id", "DELEGATEE-001");

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        // Act
        daoFilter.doFilter(request, response, chain);

        // Assert
        CzPrincipal principal = captured.get();
        assertNotNull(principal, "CzPrincipal が構築されること");
        assertEquals("ACT09-USER", principal.userId());
        assertEquals("DELEGATEE-001", principal.delegationStaffId(),
                "delegationStaffId が設定されること");
        assertTrue(principal.isDelegationMode(), "isDelegationMode() が true であること");

        verify(mockDao).selectById("DELEGATEE-001", "ACT09-USER");
    }

    @Test
    @DisplayName("代行成功（DAO なし）: ACT-09 + 代行ヘッダー → 関係チェックスキップ、delegationStaffId がセットされる")
    void delegationSuccess_withoutDao_skipsDaoCheck() throws ServletException, IOException {
        // Arrange: filter without DAO (null)
        AlbOidcAuthFilter noDaoFilter = new AlbOidcAuthFilter(JWT_SECRET);

        Map<String, Object> claims = act09Claims();
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        request.addHeader("X-Delegation-Staff-Id", "DELEGATEE-002");

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        // Act
        noDaoFilter.doFilter(request, response, chain);

        // Assert
        CzPrincipal principal = captured.get();
        assertNotNull(principal, "CzPrincipal が構築されること");
        assertEquals("ACT09-USER", principal.userId());
        assertEquals("DELEGATEE-002", principal.delegationStaffId(),
                "DAO なしでも delegationStaffId が設定されること");
        assertTrue(principal.isDelegationMode(), "isDelegationMode() が true であること");
    }

    @Test
    @DisplayName("代行拒否: 正社員（employmentType=0）が代行ヘッダーを送信 → 403 + CZ-307")
    void delegationDenied_notSubcontract_returns403() throws ServletException, IOException {
        // Arrange: baseClaims has employmentType=0 (OFFICIAL)
        Map<String, Object> claims = baseClaims("OFFICIAL-USER", "正社員ユーザー");
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        request.addHeader("X-Delegation-Staff-Id", "DELEGATEE-003");

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertFalse(chainCalled.get(), "FilterChain が呼ばれないこと（リクエスト中断）");
        assertEquals(403, response.getStatus(), "HTTP 403 が返されること");
        String body = response.getContentAsString();
        assertTrue(body.contains("\"errorCode\":\"CZ-307\""),
                "レスポンスに CZ-307 エラーコードが含まれること");
        assertTrue(body.contains("\"message\":\"代行権限がありません\""),
                "レスポンスに代行権限エラーメッセージが含まれること");
    }

    @Test
    @DisplayName("代行拒否: canDelegate=false の外部契約者 → 403 + CZ-307")
    void delegationDenied_canDelegateFalse_returns403() throws ServletException, IOException {
        // Arrange: ACT-09 ベースだが canDelegate=false に変更
        Map<String, Object> claims = act09Claims();
        claims.put("canDelegate", false);
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        request.addHeader("X-Delegation-Staff-Id", "DELEGATEE-004");

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertFalse(chainCalled.get(), "FilterChain が呼ばれないこと（リクエスト中断）");
        assertEquals(403, response.getStatus(), "HTTP 403 が返されること");
        String body = response.getContentAsString();
        assertTrue(body.contains("\"errorCode\":\"CZ-307\""),
                "レスポンスに CZ-307 エラーコードが含まれること");
        assertTrue(body.contains("\"message\":\"代行権限がありません\""),
                "レスポンスに代行権限エラーメッセージが含まれること");
    }

    @Test
    @DisplayName("代行拒否: DAO が空を返す（代行関係なし） → 403 + CZ-307")
    void delegationDenied_daoReturnsEmpty_returns403() throws ServletException, IOException {
        // Arrange: mock DAO returns empty
        Mcz21KanriTaisyoDao mockDao = mock(Mcz21KanriTaisyoDao.class);
        when(mockDao.selectById("DELEGATEE-005", "ACT09-USER"))
                .thenReturn(Optional.empty());

        AlbOidcAuthFilter daoFilter = new AlbOidcAuthFilter(JWT_SECRET, mockDao);

        Map<String, Object> claims = act09Claims();
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        request.addHeader("X-Delegation-Staff-Id", "DELEGATEE-005");

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        // Act
        daoFilter.doFilter(request, response, chain);

        // Assert
        assertFalse(chainCalled.get(), "FilterChain が呼ばれないこと（リクエスト中断）");
        assertEquals(403, response.getStatus(), "HTTP 403 が返されること");
        String body = response.getContentAsString();
        assertTrue(body.contains("\"errorCode\":\"CZ-307\""),
                "レスポンスに CZ-307 エラーコードが含まれること");
        assertTrue(body.contains("\"message\":\"代行権限がありません\""),
                "レスポンスに代行権限エラーメッセージが含まれること");

        verify(mockDao).selectById("DELEGATEE-005", "ACT09-USER");
    }

    @Test
    @DisplayName("代行ヘッダーなし: ACT-09 JWT でも通常フロー → delegationStaffId=null")
    void noDelegationHeader_normalFlow_noDelegation() throws ServletException, IOException {
        // Arrange: ACT-09 JWT without delegation header
        Map<String, Object> claims = act09Claims();
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        // X-Delegation-Staff-Id ヘッダーを設定しない

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        CzPrincipal principal = captured.get();
        assertNotNull(principal, "CzPrincipal が構築されること");
        assertEquals("ACT09-USER", principal.userId());
        assertNull(principal.delegationStaffId(),
                "delegationStaffId が null であること（通常フロー）");
        assertFalse(principal.isDelegationMode(),
                "isDelegationMode() が false であること（通常フロー）");
    }

    @Test
    @DisplayName("空白の代行ヘッダーは無視: ACT-09 JWT + 空白ヘッダー → 通常フロー")
    void blankDelegationHeader_ignored_normalFlow() throws ServletException, IOException {
        // Arrange: ACT-09 JWT with blank delegation header
        Map<String, Object> claims = act09Claims();
        request.addHeader("X-Amzn-Oidc-Data", buildJwt(claims));
        request.addHeader("X-Delegation-Staff-Id", "   ");

        AtomicReference<CzPrincipal> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(CzSecurityContext.get());

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        CzPrincipal principal = captured.get();
        assertNotNull(principal, "CzPrincipal が構築されること");
        assertEquals("ACT09-USER", principal.userId());
        assertNull(principal.delegationStaffId(),
                "空白ヘッダーの場合、delegationStaffId が null であること");
        assertFalse(principal.isDelegationMode(),
                "空白ヘッダーの場合、isDelegationMode() が false であること");
    }
}

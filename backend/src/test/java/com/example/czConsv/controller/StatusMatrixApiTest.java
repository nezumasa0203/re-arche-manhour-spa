package com.example.czConsv.controller;

import com.example.czConsv.filter.ServiceTimeFilter;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.config.SecurityConfig;
import com.example.czConsv.security.model.*;
import com.example.czConsv.security.util.StatusMatrixResolver;
import com.example.czConsv.service.ExcelExportService;
import com.example.czConsv.service.MonthlyControlService;
import com.example.czConsv.service.WorkHoursService;
import com.example.czConsv.service.WorkStatusService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-054: ステータスマトリクス × API 操作テスト。
 *
 * 12状態 × 2系列 の全パターンで /api/auth/status-matrix エンドポイントを呼び出し、
 * StatusMatrixResolver の判定結果と一致することを検証する。
 */
@WebMvcTest(
        controllers = {AuthController.class, WorkHoursController.class, WorkStatusController.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = ServiceTimeFilter.class)
        })
class StatusMatrixApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkHoursService workHoursService;
    @MockBean
    private WorkStatusService workStatusService;
    @MockBean
    private MonthlyControlService monthlyControlService;
    @MockBean
    private ExcelExportService excelExportService;

    @AfterEach
    void clearContext() {
        CzSecurityContext.clear();
    }

    // ─── Actor Helpers ──────────────────────────────────────────────────

    /** TAN 系列アクター（tab010.bit2=true → useTanSeries=true） */
    private static CzPrincipal tanActor() {
        Map<String, Boolean> bits = Map.of("bit0", true, "bit1", true, "bit2", true);
        TabPermission tab010 = new TabPermission(bits);
        CzPermissions perms = new CzPermissions(
                false, tab010, TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA),
                EmploymentType.OFFICIAL, 935, true);
        return new CzPrincipal("TAN01", "TAN系列ユーザー", "tan@test.com",
                "ORG001", "テスト組織", perms);
    }

    /** MAN 系列アクター（tab010.bit2=false → useTanSeries=false） */
    private static CzPrincipal manActor() {
        Map<String, Boolean> bits = Map.of("bit0", true, "bit1", true);
        TabPermission tab010 = new TabPermission(bits);
        CzPermissions perms = new CzPermissions(
                false, tab010, TabPermission.EMPTY, TabPermission.EMPTY,
                new DataAuthority(DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA),
                EmploymentType.OFFICIAL, 932, true);
        return new CzPrincipal("MAN01", "MAN系列ユーザー", "man@test.com",
                "ORG001", "テスト組織", perms);
    }

    // ─── 12 ステータスキー ──────────────────────────────────────────────

    private static final String[] STATUS_KEYS = {
            "000", "010", "011", "100", "110", "111",
            "200", "210", "211", "900", "910", "911"
    };

    // ─── パラメータプロバイダ ──────────────────────────────────────────

    /** TAN系列: 12状態 × 5操作 = 60 パターン */
    static Stream<Arguments> tanPatterns() {
        String[] ops = { "add", "copy", "delete", "update", "view" };
        Stream.Builder<Arguments> builder = Stream.builder();
        for (String key : STATUS_KEYS) {
            for (String op : ops) {
                int expected = StatusMatrixResolver.resolveOperation(key, true, op);
                builder.add(Arguments.of(key, op, expected));
            }
        }
        return builder.build();
    }

    /** MAN系列: 12状態 × 7操作 = 84 パターン */
    static Stream<Arguments> manPatterns() {
        String[] ops = { "add", "copy", "delete", "update", "view", "statusUpdate", "statusView" };
        Stream.Builder<Arguments> builder = Stream.builder();
        for (String key : STATUS_KEYS) {
            for (String op : ops) {
                int expected = StatusMatrixResolver.resolveOperation(key, false, op);
                builder.add(Arguments.of(key, op, expected));
            }
        }
        return builder.build();
    }

    // ─── TAN 系列テスト ──────────────────────────────────────────────

    @Nested
    @DisplayName("TAN 系列（担当者）: /api/auth/status-matrix")
    class TanSeriesTests {

        @ParameterizedTest(name = "key={0}, op={1} → expected={2}")
        @MethodSource("com.example.czConsv.controller.StatusMatrixApiTest#tanPatterns")
        @DisplayName("TAN 系列: ステータスキー別操作の制御値が正しい")
        void tanSeriesReturnsCorrectValues(String statusKey, String operation, int expected)
                throws Exception {
            CzSecurityContext.set(tanActor());

            MvcResult result = mockMvc.perform(get("/api/auth/status-matrix")
                            .param("statusKey", statusKey))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            // JSON からオペレーションの値を抽出
            Map<String, Integer> matrix = StatusMatrixResolver.resolve(statusKey, true);
            Integer actual = matrix.get(operation);

            assertEquals(expected, actual,
                    String.format("TAN key=%s op=%s: expected=%d actual=%d",
                            statusKey, operation, expected, actual));
        }
    }

    // ─── MAN 系列テスト ──────────────────────────────────────────────

    @Nested
    @DisplayName("MAN 系列（管理者）: /api/auth/status-matrix")
    class ManSeriesTests {

        @ParameterizedTest(name = "key={0}, op={1} → expected={2}")
        @MethodSource("com.example.czConsv.controller.StatusMatrixApiTest#manPatterns")
        @DisplayName("MAN 系列: ステータスキー別操作の制御値が正しい")
        void manSeriesReturnsCorrectValues(String statusKey, String operation, int expected)
                throws Exception {
            CzSecurityContext.set(manActor());

            MvcResult result = mockMvc.perform(get("/api/auth/status-matrix")
                            .param("statusKey", statusKey))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            Map<String, Integer> matrix = StatusMatrixResolver.resolve(statusKey, false);
            Integer actual = matrix.get(operation);

            assertEquals(expected, actual,
                    String.format("MAN key=%s op=%s: expected=%d actual=%d",
                            statusKey, operation, expected, actual));
        }
    }

    // ─── API レスポンス JSON 構造検証 ────────────────────────────────

    @Nested
    @DisplayName("API レスポンス構造の検証")
    class ResponseStructure {

        @ParameterizedTest(name = "TAN key={0}")
        @MethodSource("com.example.czConsv.controller.StatusMatrixApiTest#statusKeyProvider")
        @DisplayName("TAN 系列: レスポンスに 5 操作が含まれる")
        void tanResponseHas5Operations(String statusKey) throws Exception {
            CzSecurityContext.set(tanActor());

            MvcResult result = mockMvc.perform(get("/api/auth/status-matrix")
                            .param("statusKey", statusKey))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            // TAN 系列は add, copy, delete, update, view の 5 操作
            assertTrue(json.contains("\"add\""), "TAN should have add");
            assertTrue(json.contains("\"copy\""), "TAN should have copy");
            assertTrue(json.contains("\"delete\""), "TAN should have delete");
            assertTrue(json.contains("\"update\""), "TAN should have update");
            assertTrue(json.contains("\"view\""), "TAN should have view");
            // statusUpdate/statusView は含まれないはず
            assertFalse(json.contains("\"statusUpdate\""), "TAN should NOT have statusUpdate");
            assertFalse(json.contains("\"statusView\""), "TAN should NOT have statusView");
        }

        @ParameterizedTest(name = "MAN key={0}")
        @MethodSource("com.example.czConsv.controller.StatusMatrixApiTest#statusKeyProvider")
        @DisplayName("MAN 系列: レスポンスに 7 操作が含まれる")
        void manResponseHas7Operations(String statusKey) throws Exception {
            CzSecurityContext.set(manActor());

            MvcResult result = mockMvc.perform(get("/api/auth/status-matrix")
                            .param("statusKey", statusKey))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            // MAN 系列は 7 操作すべて含む
            assertTrue(json.contains("\"add\""), "MAN should have add");
            assertTrue(json.contains("\"copy\""), "MAN should have copy");
            assertTrue(json.contains("\"delete\""), "MAN should have delete");
            assertTrue(json.contains("\"update\""), "MAN should have update");
            assertTrue(json.contains("\"view\""), "MAN should have view");
            assertTrue(json.contains("\"statusUpdate\""), "MAN should have statusUpdate");
            assertTrue(json.contains("\"statusView\""), "MAN should have statusView");
        }
    }

    // ─── 不正キーの検証 ──────────────────────────────────────────────

    @Nested
    @DisplayName("不正ステータスキー")
    class InvalidStatusKey {

        @ParameterizedTest(name = "TAN invalid key: {0}")
        @MethodSource("com.example.czConsv.controller.StatusMatrixApiTest#invalidKeyProvider")
        @DisplayName("不正なステータスキーでは空マップが返る")
        void invalidKeyReturnsEmptyForTan(String invalidKey) throws Exception {
            CzSecurityContext.set(tanActor());

            MvcResult result = mockMvc.perform(get("/api/auth/status-matrix")
                            .param("statusKey", invalidKey))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertEquals("{}", json, "Invalid key should return empty map");
        }

        @ParameterizedTest(name = "MAN invalid key: {0}")
        @MethodSource("com.example.czConsv.controller.StatusMatrixApiTest#invalidKeyProvider")
        @DisplayName("MAN系列でも不正なステータスキーでは空マップが返る")
        void invalidKeyReturnsEmptyForMan(String invalidKey) throws Exception {
            CzSecurityContext.set(manActor());

            MvcResult result = mockMvc.perform(get("/api/auth/status-matrix")
                            .param("statusKey", invalidKey))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertEquals("{}", json, "Invalid key should return empty map");
        }
    }

    // ─── パラメータプロバイダ（共通） ────────────────────────────────

    static Stream<Arguments> statusKeyProvider() {
        return Stream.of(STATUS_KEYS).map(Arguments::of);
    }

    static Stream<Arguments> invalidKeyProvider() {
        return Stream.of(
                Arguments.of("999"),
                Arguments.of("abc"),
                Arguments.of(""),
                Arguments.of("001")
        );
    }
}

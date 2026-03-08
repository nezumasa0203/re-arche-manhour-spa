package com.example.czConsv.security.service;

import com.example.czConsv.security.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CzClaimsMapper 単体テスト。
 *
 * <p>Okta カスタム属性 (JWT claims) から CZ 4層権限モデルへの変換ロジックを検証する。
 * 仕様 4.2 節に基づき、全変換パターンを網羅的にテストする。
 *
 * <p>TDD Red フェーズ: 実装前にテストを先に書く。
 *
 * @see CzClaimsMapper
 * @see CzPermissions
 */
@DisplayName("CzClaimsMapper - Okta JWT claims から CZ 権限モデルへの変換")
class CzClaimsMapperTest {

    private CzClaimsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CzClaimsMapper();
    }

    // ================================================================
    // 1. parseBitString テスト (TAB 010/011/012 変換)
    // ================================================================

    @Nested
    @DisplayName("parseBitString - ビット文字列から TabPermission への変換")
    class ParseBitStringTest {

        @Test
        @DisplayName("TAB 010 ビット文字列 '110000' は bit0=true, bit1=true, bit2=false を返す")
        void tab010BitString110000() {
            TabPermission result = mapper.parseBitString("110000");

            assertAll(
                    () -> assertTrue(result.bit0(), "位置0 = '1' → bit0=true"),
                    () -> assertTrue(result.bit1(), "位置1 = '1' → bit1=true"),
                    () -> assertFalse(result.bit2(), "位置2 = '0' → bit2=false")
            );
        }

        @Test
        @DisplayName("TAB 010 ビット文字列 '001000' は bit0=false, bit1=false, bit2=true を返す")
        void tab010BitString001000() {
            TabPermission result = mapper.parseBitString("001000");

            assertAll(
                    () -> assertFalse(result.bit0(), "位置0 = '0' → bit0=false"),
                    () -> assertFalse(result.bit1(), "位置1 = '0' → bit1=false"),
                    () -> assertTrue(result.bit2(), "位置2 = '1' → bit2=true")
            );
        }

        @Test
        @DisplayName("TAB 010 ビット文字列 '100000' は bit0=true のみ true を返す")
        void tab010BitString100000() {
            TabPermission result = mapper.parseBitString("100000");

            assertAll(
                    () -> assertTrue(result.bit0(), "位置0 = '1' → bit0=true"),
                    () -> assertFalse(result.bit1(), "位置1 = '0' → bit1=false"),
                    () -> assertFalse(result.bit2(), "位置2 = '0' → bit2=false")
            );
        }

        @Test
        @DisplayName("TAB 011 ビット文字列 '11' は bit0=true, bit1=true を返す")
        void tab011BitString11() {
            TabPermission result = mapper.parseBitString("11");

            assertAll(
                    () -> assertTrue(result.bit0(), "位置0 = '1' → bit0=true"),
                    () -> assertTrue(result.bit1(), "位置1 = '1' → bit1=true")
            );
        }

        @Test
        @DisplayName("TAB 012 ビット文字列 '10' は bit0=true, bit1=false を返す")
        void tab012BitString10() {
            TabPermission result = mapper.parseBitString("10");

            assertAll(
                    () -> assertTrue(result.bit0(), "位置0 = '1' → bit0=true"),
                    () -> assertFalse(result.bit1(), "位置1 = '0' → bit1=false")
            );
        }

        @ParameterizedTest(name = "空/null ビット文字列 [{0}] は TabPermission.EMPTY を返す")
        @NullAndEmptySource
        @DisplayName("空文字列または null は TabPermission.EMPTY を返す")
        void emptyOrNullBitStringReturnsEmpty(String input) {
            TabPermission result = mapper.parseBitString(input);

            assertAll(
                    () -> assertNotNull(result, "null ではなく EMPTY を返すべき"),
                    () -> assertFalse(result.bit0(), "EMPTY.bit0() は false"),
                    () -> assertFalse(result.bit1(), "EMPTY.bit1() は false"),
                    () -> assertFalse(result.bit2(), "EMPTY.bit2() は false"),
                    () -> assertEquals(TabPermission.EMPTY, result,
                            "TabPermission.EMPTY と等価であるべき")
            );
        }

        @Test
        @DisplayName("全ビット '1' の文字列 '111000' は bit0/bit1/bit2 すべて true を返す")
        void allBitsSet() {
            TabPermission result = mapper.parseBitString("111000");

            assertAll(
                    () -> assertTrue(result.bit0()),
                    () -> assertTrue(result.bit1()),
                    () -> assertTrue(result.bit2())
            );
        }

        @Test
        @DisplayName("全ビット '0' の文字列 '000000' は全ビット false を返す")
        void noBitsSet() {
            TabPermission result = mapper.parseBitString("000000");

            assertAll(
                    () -> assertFalse(result.bit0()),
                    () -> assertFalse(result.bit1()),
                    () -> assertFalse(result.bit2())
            );
        }
    }

    // ================================================================
    // 2. jinjiMode 変換テスト
    // ================================================================

    @Nested
    @DisplayName("jinjiMode - 文字列 → boolean 変換")
    class JinjiModeTest {

        @Test
        @DisplayName("jinjiMode 'true' は boolean true に変換される")
        void trueStringConvertsToTrue() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("custom:jinjiMode", "true");
            claims.put("custom:tab010", "100000");
            claims.put("custom:tab011", "00");
            claims.put("custom:tab012", "00");
            claims.put("custom:dataAuthRef", "KA");
            claims.put("custom:dataAuthIns", "");
            claims.put("custom:dataAuthUpd", "");
            claims.put("custom:employmentType", "0");
            claims.put("custom:staffRole", "");
            claims.put("custom:canDelegate", "false");
            claims.put("organizationCode", "ORG001");
            claims.put("organizationName", "テスト組織");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertTrue(result.jinjiMode(), "'true' → boolean true");
        }

        @Test
        @DisplayName("jinjiMode 'false' は boolean false に変換される")
        void falseStringConvertsToFalse() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("custom:jinjiMode", "false");
            claims.put("custom:tab010", "100000");
            claims.put("custom:tab011", "00");
            claims.put("custom:tab012", "00");
            claims.put("custom:dataAuthRef", "KA");
            claims.put("custom:dataAuthIns", "");
            claims.put("custom:dataAuthUpd", "");
            claims.put("custom:employmentType", "0");
            claims.put("custom:staffRole", "");
            claims.put("custom:canDelegate", "false");
            claims.put("organizationCode", "ORG001");
            claims.put("organizationName", "テスト組織");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertFalse(result.jinjiMode(), "'false' → boolean false");
        }

        @Test
        @DisplayName("jinjiMode が null のとき boolean false に変換される")
        void nullConvertsToFalse() {
            Map<String, Object> claims = new HashMap<>();
            // custom:jinjiMode を設定しない (null)
            claims.put("custom:tab010", "100000");
            claims.put("custom:tab011", "00");
            claims.put("custom:tab012", "00");
            claims.put("custom:dataAuthRef", "KA");
            claims.put("custom:dataAuthIns", "");
            claims.put("custom:dataAuthUpd", "");
            claims.put("custom:employmentType", "0");
            claims.put("custom:staffRole", "");
            claims.put("custom:canDelegate", "false");
            claims.put("organizationCode", "ORG001");
            claims.put("organizationName", "テスト組織");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertFalse(result.jinjiMode(), "null → boolean false");
        }
    }

    // ================================================================
    // 3. employmentType 変換テスト
    // ================================================================

    @Nested
    @DisplayName("employmentType - 文字列 → EmploymentType 変換")
    class EmploymentTypeConversionTest {

        @ParameterizedTest(name = "employmentType ''{0}'' → {1}")
        @CsvSource({
                "0, OFFICIAL",
                "1, TEMPORARY_1",
                "2, TEMPORARY_2",
                "3, SUBCONTRACT"
        })
        @DisplayName("employmentType 文字列コード → EmploymentType enum に変換される")
        void stringCodeConvertsToEnum(String code, EmploymentType expected) {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:employmentType", code);

            CzPermissions result = mapper.mapFromClaims(claims);
            assertEquals(expected, result.employmentType(),
                    "'" + code + "' → " + expected);
        }
    }

    // ================================================================
    // 4. dataAuthority 変換テスト
    // ================================================================

    @Nested
    @DisplayName("dataAuthority - 文字列 → DataAuthority 変換")
    class DataAuthorityConversionTest {

        @Test
        @DisplayName("dataAuthRef 'HONBU' はそのまま 'HONBU' としてパススルーされる")
        void refPassthrough() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:dataAuthRef", "HONBU");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertEquals("HONBU", result.dataAuthority().ref(),
                    "'HONBU' → そのまま 'HONBU'");
        }

        @Test
        @DisplayName("dataAuthRef 空文字列は null に変換される")
        void emptyRefConvertsToNull() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:dataAuthRef", "");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertNull(result.dataAuthority().ref(),
                    "空文字列 → null");
        }

        @Test
        @DisplayName("dataAuthIns 'KYOKU' はそのまま 'KYOKU' としてパススルーされる")
        void insPassthrough() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:dataAuthIns", "KYOKU");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertEquals("KYOKU", result.dataAuthority().ins(),
                    "'KYOKU' → そのまま 'KYOKU'");
        }

        @Test
        @DisplayName("dataAuthIns 空文字列は null に変換される")
        void emptyInsConvertsToNull() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:dataAuthIns", "");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertNull(result.dataAuthority().ins(),
                    "空文字列 → null");
        }

        @Test
        @DisplayName("dataAuthUpd 'KYOKU' はそのまま 'KYOKU' としてパススルーされる")
        void updPassthrough() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:dataAuthUpd", "KYOKU");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertEquals("KYOKU", result.dataAuthority().upd(),
                    "'KYOKU' → そのまま 'KYOKU'");
        }

        @Test
        @DisplayName("dataAuthUpd 空文字列は null に変換される")
        void emptyUpdConvertsToNull() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:dataAuthUpd", "");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertNull(result.dataAuthority().upd(),
                    "空文字列 → null");
        }
    }

    // ================================================================
    // 5. staffRole 変換テスト
    // ================================================================

    @Nested
    @DisplayName("staffRole - 文字列 → Integer 変換")
    class StaffRoleConversionTest {

        @Test
        @DisplayName("staffRole '931' は Integer 931 に変換される")
        void numericStringConvertsToInteger() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:staffRole", "931");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertEquals(931, result.staffRole(),
                    "'931' → Integer 931");
        }

        @Test
        @DisplayName("staffRole 空文字列は null に変換される")
        void emptyStringConvertsToNull() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:staffRole", "");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertNull(result.staffRole(),
                    "空文字列 → null");
        }

        @Test
        @DisplayName("staffRole が claims に存在しない場合は null を返す")
        void missingClaimReturnsNull() {
            Map<String, Object> claims = minimalClaims();
            claims.remove("custom:staffRole");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertNull(result.staffRole(),
                    "claims に存在しない → null");
        }
    }

    // ================================================================
    // 6. canDelegate 変換テスト
    // ================================================================

    @Nested
    @DisplayName("canDelegate - 文字列 → boolean 変換")
    class CanDelegateConversionTest {

        @Test
        @DisplayName("canDelegate 'true' は boolean true に変換される")
        void trueStringConvertsToTrue() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:canDelegate", "true");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertTrue(result.canDelegate(),
                    "'true' → boolean true");
        }

        @Test
        @DisplayName("canDelegate null は boolean false に変換される")
        void nullConvertsToFalse() {
            Map<String, Object> claims = minimalClaims();
            claims.remove("custom:canDelegate");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertFalse(result.canDelegate(),
                    "null → boolean false");
        }

        @Test
        @DisplayName("canDelegate 'false' は boolean false に変換される")
        void falseStringConvertsToFalse() {
            Map<String, Object> claims = minimalClaims();
            claims.put("custom:canDelegate", "false");

            CzPermissions result = mapper.mapFromClaims(claims);
            assertFalse(result.canDelegate(),
                    "'false' → boolean false");
        }
    }

    // ================================================================
    // 7. 統合テスト: mapFromClaims フルマッピング
    // ================================================================

    @Nested
    @DisplayName("mapFromClaims - 全属性の統合マッピング")
    class FullMappingTest {

        @Test
        @DisplayName("全 Okta 属性を含む claims マップから CzPermissions が正しく構築される")
        void fullClaimsMapToCzPermissions() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("custom:jinjiMode", "true");
            claims.put("custom:tab010", "110000");
            claims.put("custom:tab011", "11");
            claims.put("custom:tab012", "10");
            claims.put("custom:dataAuthRef", "HONBU");
            claims.put("custom:dataAuthIns", "KYOKU");
            claims.put("custom:dataAuthUpd", "");
            claims.put("custom:employmentType", "0");
            claims.put("custom:staffRole", "931");
            claims.put("custom:canDelegate", "true");
            claims.put("organizationCode", "ORG001");
            claims.put("organizationName", "テスト本部");

            CzPermissions result = mapper.mapFromClaims(claims);

            assertAll(
                    // Layer 1: jinjiMode
                    () -> assertTrue(result.jinjiMode(), "jinjiMode = true"),

                    // Layer 2: TAB 010
                    () -> assertTrue(result.tab010().bit0(), "tab010 bit0 = true"),
                    () -> assertTrue(result.tab010().bit1(), "tab010 bit1 = true"),
                    () -> assertFalse(result.tab010().bit2(), "tab010 bit2 = false"),

                    // Layer 2: TAB 011
                    () -> assertTrue(result.tab011().bit0(), "tab011 bit0 = true"),
                    () -> assertTrue(result.tab011().bit1(), "tab011 bit1 = true"),

                    // Layer 2: TAB 012
                    () -> assertTrue(result.tab012().bit0(), "tab012 bit0 = true"),
                    () -> assertFalse(result.tab012().bit1(), "tab012 bit1 = false"),

                    // Layer 3: DataAuthority
                    () -> assertEquals("HONBU", result.dataAuthority().ref(), "ref = HONBU"),
                    () -> assertEquals("KYOKU", result.dataAuthority().ins(), "ins = KYOKU"),
                    () -> assertNull(result.dataAuthority().upd(), "upd = null (空文字列)"),

                    // Layer 4: EmploymentType
                    () -> assertEquals(EmploymentType.OFFICIAL, result.employmentType(),
                            "employmentType = OFFICIAL"),

                    // staffRole
                    () -> assertEquals(931, result.staffRole(), "staffRole = 931"),

                    // canDelegate
                    () -> assertTrue(result.canDelegate(), "canDelegate = true"),

                    // 便利メソッド検証
                    () -> assertTrue(result.canReport(), "tab010.bit0 → canReport"),
                    () -> assertTrue(result.canManageReports(), "tab010.bit1 → canManageReports"),
                    () -> assertFalse(result.canFullManage(), "tab010.bit2=false → canFullManage=false"),
                    () -> assertTrue(result.canOutputMaintenanceHours(), "tab011.bit0 → canOutputMaintenanceHours"),
                    () -> assertTrue(result.canNavigateBetweenForms(), "tab011.bit1 → canNavigateBetweenForms"),
                    () -> assertTrue(result.canInputPeriodCondition(), "tab012.bit0 → canInputPeriodCondition"),
                    () -> assertFalse(result.canAggregatePeriod(), "tab012.bit1=false → canAggregatePeriod=false")
            );
        }
    }

    // ================================================================
    // 8. 15 アクター @ParameterizedTest (02_actor_definition.md 準拠)
    // ================================================================

    @Nested
    @DisplayName("15 アクター Claims マッピング (02_actor_definition.md 準拠)")
    class ActorClaimsMappingTest {

        /**
         * 15 アクターの Okta claims パラメータを提供する。
         *
         * <p>各引数:
         * actorId, jinjiMode, tab010, tab011, tab012,
         * dataAuthRef, dataAuthIns, dataAuthUpd,
         * employmentType, staffRole, canDelegate,
         * 説明
         */
        static Stream<Arguments> actorClaims() {
            return Stream.of(
                    // ACT-01: 報告担当者 - jinjiMode=true, tab010 bit0, 正社員, 課レベル参照のみ
                    Arguments.of("ACT-01", "true",
                            "100000", "00", "00",
                            "KA", "", "",
                            "0", "", "false",
                            "報告担当者 - 報告書作成担当グループ"),

                    // ACT-02: 報告管理者 - tab010 bit1, 部レベル参照+登録
                    Arguments.of("ACT-02", "false",
                            "010000", "00", "00",
                            "BU", "BU", "",
                            "0", "", "false",
                            "報告管理者 - 報告書管理担当グループ"),

                    // ACT-03: 全権管理者 - tab010 bit2, 全社フルアクセス
                    Arguments.of("ACT-03", "false",
                            "001000", "01", "11",
                            "ZENSYA", "ZENSYA", "ZENSYA",
                            "0", "", "false",
                            "全権管理者 - 全管理グループ"),

                    // ACT-04: 管理モードユーザー - jinjiMode=false, tab010 bit0+bit1
                    Arguments.of("ACT-04", "false",
                            "110000", "10", "00",
                            "HONBU", "HONBU", "HONBU",
                            "0", "", "false",
                            "管理モードユーザー"),

                    // ACT-05: 人事モードユーザー - jinjiMode=true, tab010 bit0
                    Arguments.of("ACT-05", "true",
                            "100000", "00", "00",
                            "KA", "KA", "",
                            "0", "", "false",
                            "人事モードユーザー"),

                    // ACT-06: 正社員 - employmentType=0
                    Arguments.of("ACT-06", "false",
                            "100000", "00", "00",
                            "KA", "KA", "KA",
                            "0", "", "false",
                            "正社員 - 全機能アクセス可能"),

                    // ACT-07: 臨時職員1 - employmentType=1
                    Arguments.of("ACT-07", "true",
                            "100000", "00", "00",
                            "KA", "", "",
                            "1", "", "false",
                            "臨時職員1 - 制限付きアクセス"),

                    // ACT-08: 臨時職員2 - employmentType=2
                    Arguments.of("ACT-08", "true",
                            "100000", "00", "00",
                            "KA", "", "",
                            "2", "", "false",
                            "臨時職員2 - 制限付きアクセス"),

                    // ACT-09: 外部契約者 - employmentType=3, canDelegate=true
                    Arguments.of("ACT-09", "true",
                            "100000", "00", "00",
                            "KA", "", "",
                            "3", "", "true",
                            "外部契約者 - 代行モード可能"),

                    // ACT-10: 全社スタッフ - staffRole=931, 全社データアクセス
                    Arguments.of("ACT-10", "false",
                            "110000", "11", "11",
                            "ZENSYA", "ZENSYA", "ZENSYA",
                            "0", "931", "false",
                            "全社スタッフ - 全社横断データアクセス"),

                    // ACT-11: 事業スタッフ - staffRole=932
                    Arguments.of("ACT-11", "false",
                            "110000", "11", "11",
                            "EIGYOSHO", "EIGYOSHO", "EIGYOSHO",
                            "0", "932", "false",
                            "事業スタッフ - 事業本部レベル"),

                    // ACT-12: 本社スタッフ - staffRole=933
                    Arguments.of("ACT-12", "false",
                            "110000", "10", "00",
                            "HONBU", "HONBU", "",
                            "0", "933", "false",
                            "本社スタッフ - 本社レベル"),

                    // ACT-13: 局スタッフ - staffRole=934
                    Arguments.of("ACT-13", "false",
                            "100000", "10", "00",
                            "KYOKU", "KYOKU", "",
                            "0", "934", "false",
                            "局スタッフ - 局レベル"),

                    // ACT-14: 局スタッフ（総務管理者） - staffRole=935
                    Arguments.of("ACT-14", "false",
                            "110000", "11", "11",
                            "KYOKU", "KYOKU", "KYOKU",
                            "0", "935", "false",
                            "局スタッフ（総務管理者） - 局レベル + 総務管理権限"),

                    // ACT-15: 局スタッフ（営業） - staffRole=936
                    Arguments.of("ACT-15", "false",
                            "100000", "00", "00",
                            "EIGYOSHO", "", "",
                            "0", "936", "false",
                            "局スタッフ（営業） - 局レベル + 営業部門権限")
            );
        }

        @ParameterizedTest(name = "{0}: {11}")
        @MethodSource("actorClaims")
        @DisplayName("アクターの Okta claims から CzPermissions が正しく構築される")
        void actorClaimsMapToCzPermissions(
                String actorId,
                String jinjiMode,
                String tab010, String tab011, String tab012,
                String dataAuthRef, String dataAuthIns, String dataAuthUpd,
                String employmentType, String staffRole, String canDelegate,
                String description
        ) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("custom:jinjiMode", jinjiMode);
            claims.put("custom:tab010", tab010);
            claims.put("custom:tab011", tab011);
            claims.put("custom:tab012", tab012);
            claims.put("custom:dataAuthRef", dataAuthRef);
            claims.put("custom:dataAuthIns", dataAuthIns);
            claims.put("custom:dataAuthUpd", dataAuthUpd);
            claims.put("custom:employmentType", employmentType);
            claims.put("custom:staffRole", staffRole);
            claims.put("custom:canDelegate", canDelegate);
            claims.put("organizationCode", "ORG-" + actorId);
            claims.put("organizationName", description);

            CzPermissions result = mapper.mapFromClaims(claims);

            assertNotNull(result, actorId + " の CzPermissions が生成されるべき");

            // jinjiMode 検証
            assertEquals("true".equals(jinjiMode), result.jinjiMode(),
                    actorId + ": jinjiMode");

            // employmentType 検証
            assertEquals(EmploymentType.fromCode(Integer.parseInt(employmentType)),
                    result.employmentType(),
                    actorId + ": employmentType");

            // staffRole 検証
            Integer expectedStaffRole = staffRole.isEmpty() ? null : Integer.valueOf(staffRole);
            assertEquals(expectedStaffRole, result.staffRole(),
                    actorId + ": staffRole");

            // canDelegate 検証
            assertEquals("true".equals(canDelegate), result.canDelegate(),
                    actorId + ": canDelegate");

            // dataAuthority 検証
            String expectedRef = dataAuthRef.isEmpty() ? null : dataAuthRef;
            String expectedIns = dataAuthIns.isEmpty() ? null : dataAuthIns;
            String expectedUpd = dataAuthUpd.isEmpty() ? null : dataAuthUpd;
            assertAll(
                    () -> assertEquals(expectedRef, result.dataAuthority().ref(),
                            actorId + ": dataAuthority.ref"),
                    () -> assertEquals(expectedIns, result.dataAuthority().ins(),
                            actorId + ": dataAuthority.ins"),
                    () -> assertEquals(expectedUpd, result.dataAuthority().upd(),
                            actorId + ": dataAuthority.upd")
            );
        }

        @Test
        @DisplayName("ACT-03: 全権管理者 - useTanSeries()=true, 全便利メソッド検証")
        void act03FullManagerConvenienceMethods() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("custom:jinjiMode", "false");
            claims.put("custom:tab010", "001000");
            claims.put("custom:tab011", "01");
            claims.put("custom:tab012", "11");
            claims.put("custom:dataAuthRef", "ZENSYA");
            claims.put("custom:dataAuthIns", "ZENSYA");
            claims.put("custom:dataAuthUpd", "ZENSYA");
            claims.put("custom:employmentType", "0");
            claims.put("custom:staffRole", "");
            claims.put("custom:canDelegate", "false");
            claims.put("organizationCode", "ORG-ACT03");
            claims.put("organizationName", "全権管理者");

            CzPermissions result = mapper.mapFromClaims(claims);

            assertAll(
                    () -> assertTrue(result.canFullManage(), "全管理グループ (bit2=true)"),
                    () -> assertTrue(result.useTanSeries(), "bit2=true → 担当者系列"),
                    () -> assertTrue(result.canNavigateBetweenForms(), "画面遷移リンク有効 (tab011.bit1)"),
                    () -> assertTrue(result.canInputPeriodCondition(), "期間入力条件有効 (tab012.bit0)"),
                    () -> assertTrue(result.canAggregatePeriod(), "期間集計有効 (tab012.bit1)"),
                    () -> assertTrue(result.dataAuthority().canRef(), "全社参照可"),
                    () -> assertTrue(result.dataAuthority().canIns(), "全社登録可"),
                    () -> assertTrue(result.dataAuthority().canUpd(), "全社更新可")
            );
        }

        @Test
        @DisplayName("ACT-09: 外部契約者 - SUBCONTRACT, canDelegate=true, 登録/更新不可")
        void act09SubcontractorConvenienceMethods() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("custom:jinjiMode", "true");
            claims.put("custom:tab010", "100000");
            claims.put("custom:tab011", "00");
            claims.put("custom:tab012", "00");
            claims.put("custom:dataAuthRef", "KA");
            claims.put("custom:dataAuthIns", "");
            claims.put("custom:dataAuthUpd", "");
            claims.put("custom:employmentType", "3");
            claims.put("custom:staffRole", "");
            claims.put("custom:canDelegate", "true");
            claims.put("organizationCode", "ORG-ACT09");
            claims.put("organizationName", "外部契約者");

            CzPermissions result = mapper.mapFromClaims(claims);

            assertAll(
                    () -> assertTrue(result.jinjiMode(), "人事モード"),
                    () -> assertTrue(result.isSubcontract(), "外部契約者"),
                    () -> assertFalse(result.isOfficial(), "正社員ではない"),
                    () -> assertTrue(result.canDelegate(), "代行モード可能"),
                    () -> assertTrue(result.canReport(), "報告は可能"),
                    () -> assertFalse(result.dataAuthority().canIns(), "登録不可"),
                    () -> assertFalse(result.dataAuthority().canUpd(), "更新不可")
            );
        }
    }

    // ================================================================
    // ヘルパーメソッド
    // ================================================================

    /**
     * 最小構成の claims マップを生成する。
     * テストケースで個別の属性だけを変更する際のベースとして使用する。
     */
    private static Map<String, Object> minimalClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("custom:jinjiMode", "false");
        claims.put("custom:tab010", "100000");
        claims.put("custom:tab011", "00");
        claims.put("custom:tab012", "00");
        claims.put("custom:dataAuthRef", "KA");
        claims.put("custom:dataAuthIns", "");
        claims.put("custom:dataAuthUpd", "");
        claims.put("custom:employmentType", "0");
        claims.put("custom:staffRole", "");
        claims.put("custom:canDelegate", "false");
        claims.put("organizationCode", "ORG001");
        claims.put("organizationName", "テスト組織");
        return claims;
    }
}

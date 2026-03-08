package com.example.czConsv.security.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CZ 4層権限モデル単体テスト。
 *
 * <p>対象クラス:
 * <ul>
 *   <li>{@link TabPermission} - Layer 2 ビットベース機能権限</li>
 *   <li>{@link DataAuthority} - Layer 3 データアクセス権限</li>
 *   <li>{@link EmploymentType} - Layer 4 雇用形態</li>
 *   <li>{@link CzPermissions} - 統合ビュー (便利メソッド)</li>
 *   <li>{@link CzPrincipal} - 認証済みユーザー情報</li>
 * </ul>
 *
 * <p>02_actor_definition.md の 15 アクター定義を網羅的に検証する。
 */
@DisplayName("CZ 4層権限モデル")
class CzPermissionsTest {

    // ================================================================
    // ヘルパーメソッド
    // ================================================================

    /**
     * 指定ビットが true になる TabPermission を生成する。
     */
    private static TabPermission tabWith(int... trueBits) {
        if (trueBits.length == 0) {
            return TabPermission.EMPTY;
        }
        var map = new java.util.HashMap<String, Boolean>();
        for (int bit : trueBits) {
            map.put("bit" + bit, true);
        }
        return new TabPermission(Collections.unmodifiableMap(map));
    }

    /**
     * CzPermissions ビルダー (テスト用)。
     * デフォルト値: jinjiMode=false, 全タブEMPTY, dataAuthority null, OFFICIAL, staffRole=null, canDelegate=false
     */
    private static CzPermissions permissions(
            boolean jinjiMode,
            TabPermission tab010,
            TabPermission tab011,
            TabPermission tab012,
            DataAuthority dataAuthority,
            EmploymentType employmentType,
            Integer staffRole,
            boolean canDelegate
    ) {
        return new CzPermissions(
                jinjiMode, tab010, tab011, tab012,
                dataAuthority, employmentType, staffRole, canDelegate
        );
    }

    /** 最小構成の CzPermissions を返す。 */
    private static CzPermissions minimalPermissions() {
        return permissions(
                false,
                TabPermission.EMPTY,
                TabPermission.EMPTY,
                TabPermission.EMPTY,
                new DataAuthority(null, null, null),
                EmploymentType.OFFICIAL,
                null,
                false
        );
    }

    // ================================================================
    // 1. TabPermission テスト
    // ================================================================

    @Nested
    @DisplayName("TabPermission - ビットベース機能権限")
    class TabPermissionTest {

        @Test
        @DisplayName("EMPTY 定数は空マップを保持し、全ビットが false を返す")
        void emptyConstantReturnsFalseForAllBits() {
            TabPermission empty = TabPermission.EMPTY;

            assertAll(
                    () -> assertNotNull(empty, "EMPTY は null であってはならない"),
                    () -> assertTrue(empty.bits().isEmpty(), "EMPTY の bits マップは空であるべき"),
                    () -> assertFalse(empty.bit0(), "EMPTY.bit0() は false"),
                    () -> assertFalse(empty.bit1(), "EMPTY.bit1() は false"),
                    () -> assertFalse(empty.bit2(), "EMPTY.bit2() は false")
            );
        }

        @Test
        @DisplayName("bit0 のみ true のとき、bit0() = true / bit1() = false / bit2() = false")
        void onlyBit0True() {
            TabPermission tab = tabWith(0);

            assertAll(
                    () -> assertTrue(tab.bit0()),
                    () -> assertFalse(tab.bit1()),
                    () -> assertFalse(tab.bit2())
            );
        }

        @Test
        @DisplayName("bit1 のみ true のとき、bit1() = true / bit0() = false / bit2() = false")
        void onlyBit1True() {
            TabPermission tab = tabWith(1);

            assertAll(
                    () -> assertFalse(tab.bit0()),
                    () -> assertTrue(tab.bit1()),
                    () -> assertFalse(tab.bit2())
            );
        }

        @Test
        @DisplayName("bit2 のみ true のとき、bit2() = true / bit0() = false / bit1() = false")
        void onlyBit2True() {
            TabPermission tab = tabWith(2);

            assertAll(
                    () -> assertFalse(tab.bit0()),
                    () -> assertFalse(tab.bit1()),
                    () -> assertTrue(tab.bit2())
            );
        }

        @Test
        @DisplayName("全ビット true のとき、bit0/bit1/bit2 全て true")
        void allBitsTrue() {
            TabPermission tab = tabWith(0, 1, 2);

            assertAll(
                    () -> assertTrue(tab.bit0()),
                    () -> assertTrue(tab.bit1()),
                    () -> assertTrue(tab.bit2())
            );
        }

        @ParameterizedTest(name = "hasBit({0}) はマップに存在しないインデックスで false を返す")
        @ValueSource(ints = {3, 4, 5, 10, 99})
        @DisplayName("hasBit - マップに存在しないインデックスは false")
        void hasBitReturnsFalseForMissingIndex(int index) {
            TabPermission tab = tabWith(0, 1, 2);
            assertFalse(tab.hasBit(index),
                    "hasBit(" + index + ") はマップに存在しないため false");
        }

        @ParameterizedTest(name = "hasBit({0}) は設定済みインデックスで true を返す")
        @ValueSource(ints = {0, 1, 2})
        @DisplayName("hasBit - 設定済みインデックスは true")
        void hasBitReturnsTrueForSetIndex(int index) {
            TabPermission tab = tabWith(0, 1, 2);
            assertTrue(tab.hasBit(index),
                    "hasBit(" + index + ") は設定済みのため true");
        }

        @Test
        @DisplayName("TAB 010 ロールデータ '110000' のビットパターン再現: bit0=true, bit1=true, bit2=false")
        void roleDataPattern110000() {
            // dummySecurity.xml の例: roledata "110000" → bit0=1, bit1=1, bit2=0
            TabPermission tab = tabWith(0, 1);

            assertAll(
                    () -> assertTrue(tab.bit0(), "ロールデータ位置0 = '1'"),
                    () -> assertTrue(tab.bit1(), "ロールデータ位置1 = '1'"),
                    () -> assertFalse(tab.bit2(), "ロールデータ位置2 = '0'")
            );
        }
    }

    // ================================================================
    // 2. DataAuthority テスト
    // ================================================================

    @Nested
    @DisplayName("DataAuthority - データアクセス権限 (Layer 3)")
    class DataAuthorityTest {

        @Test
        @DisplayName("組織階層レベル定数が正しく定義されている")
        void hierarchyConstantsAreDefined() {
            assertAll(
                    () -> assertEquals("ZENSYA", DataAuthority.ZENSYA),
                    () -> assertEquals("EIGYOSHO", DataAuthority.EIGYOSHO),
                    () -> assertEquals("HONBU", DataAuthority.HONBU),
                    () -> assertEquals("KYOKU", DataAuthority.KYOKU),
                    () -> assertEquals("SHITSU", DataAuthority.SHITSU),
                    () -> assertEquals("BU", DataAuthority.BU),
                    () -> assertEquals("KA", DataAuthority.KA)
            );
        }

        @Test
        @DisplayName("全フィールドが non-null のとき canRef/canIns/canUpd は全て true")
        void allNonNullReturnsTrueForAll() {
            var da = new DataAuthority(DataAuthority.ZENSYA, DataAuthority.HONBU, DataAuthority.KA);

            assertAll(
                    () -> assertTrue(da.canRef(), "ref が non-null → canRef() は true"),
                    () -> assertTrue(da.canIns(), "ins が non-null → canIns() は true"),
                    () -> assertTrue(da.canUpd(), "upd が non-null → canUpd() は true")
            );
        }

        @Test
        @DisplayName("全フィールドが null のとき canRef/canIns/canUpd は全て false")
        void allNullReturnsFalseForAll() {
            var da = new DataAuthority(null, null, null);

            assertAll(
                    () -> assertFalse(da.canRef()),
                    () -> assertFalse(da.canIns()),
                    () -> assertFalse(da.canUpd())
            );
        }

        @Test
        @DisplayName("ref のみ non-null のとき canRef=true, canIns=false, canUpd=false")
        void onlyRefNonNull() {
            var da = new DataAuthority(DataAuthority.KYOKU, null, null);

            assertAll(
                    () -> assertTrue(da.canRef()),
                    () -> assertFalse(da.canIns()),
                    () -> assertFalse(da.canUpd())
            );
        }

        @Test
        @DisplayName("ins のみ non-null のとき canRef=false, canIns=true, canUpd=false")
        void onlyInsNonNull() {
            var da = new DataAuthority(null, DataAuthority.BU, null);

            assertAll(
                    () -> assertFalse(da.canRef()),
                    () -> assertTrue(da.canIns()),
                    () -> assertFalse(da.canUpd())
            );
        }

        @Test
        @DisplayName("upd のみ non-null のとき canRef=false, canIns=false, canUpd=true")
        void onlyUpdNonNull() {
            var da = new DataAuthority(null, null, DataAuthority.EIGYOSHO);

            assertAll(
                    () -> assertFalse(da.canRef()),
                    () -> assertFalse(da.canIns()),
                    () -> assertTrue(da.canUpd())
            );
        }

        @Test
        @DisplayName("record のアクセサで ref/ins/upd の値を取得できる")
        void recordAccessorsReturnCorrectValues() {
            var da = new DataAuthority(DataAuthority.ZENSYA, DataAuthority.HONBU, DataAuthority.KA);

            assertAll(
                    () -> assertEquals(DataAuthority.ZENSYA, da.ref()),
                    () -> assertEquals(DataAuthority.HONBU, da.ins()),
                    () -> assertEquals(DataAuthority.KA, da.upd())
            );
        }
    }

    // ================================================================
    // 3. EmploymentType テスト
    // ================================================================

    @Nested
    @DisplayName("EmploymentType - 雇用形態 (Layer 4)")
    class EmploymentTypeTest {

        @ParameterizedTest(name = "fromCode({0}) = {1}")
        @CsvSource({
                "0, OFFICIAL",
                "1, TEMPORARY_1",
                "2, TEMPORARY_2",
                "3, SUBCONTRACT"
        })
        @DisplayName("fromCode で全4種の雇用形態を正しく取得できる")
        void fromCodeReturnsCorrectType(int code, EmploymentType expected) {
            assertEquals(expected, EmploymentType.fromCode(code));
        }

        @ParameterizedTest(name = "fromCode({0}) は OFFICIAL にフォールバック")
        @ValueSource(ints = {-1, 4, 5, 99, 999, Integer.MAX_VALUE})
        @DisplayName("fromCode - 未知のコードは OFFICIAL にフォールバックする")
        void fromCodeFallsBackToOfficialForUnknownCode(int unknownCode) {
            assertEquals(EmploymentType.OFFICIAL, EmploymentType.fromCode(unknownCode),
                    "未知のコード " + unknownCode + " は OFFICIAL にフォールバック");
        }

        @Test
        @DisplayName("getCode で各 enum の code 値を取得できる")
        void getCodeReturnsAssignedValue() {
            assertAll(
                    () -> assertEquals(0, EmploymentType.OFFICIAL.getCode()),
                    () -> assertEquals(1, EmploymentType.TEMPORARY_1.getCode()),
                    () -> assertEquals(2, EmploymentType.TEMPORARY_2.getCode()),
                    () -> assertEquals(3, EmploymentType.SUBCONTRACT.getCode())
            );
        }

        @ParameterizedTest
        @EnumSource(EmploymentType.class)
        @DisplayName("fromCode(getCode()) で元の enum に戻る (往復変換)")
        void roundTrip(EmploymentType type) {
            assertEquals(type, EmploymentType.fromCode(type.getCode()),
                    type.name() + " の往復変換が一致しない");
        }
    }

    // ================================================================
    // 4. CzPermissions 便利メソッドテスト
    // ================================================================

    @Nested
    @DisplayName("CzPermissions - Layer 2 便利メソッド (TAB 010)")
    class CzPermissionsTab010Test {

        @Test
        @DisplayName("canReport() は tab010.bit0() に委譲する")
        void canReportDelegatesToTab010Bit0() {
            CzPermissions withBit0 = permissions(
                    false, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit0 = permissions(
                    false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertTrue(withBit0.canReport(), "tab010.bit0=true のとき canReport()=true"),
                    () -> assertFalse(withoutBit0.canReport(), "tab010.bit0=false のとき canReport()=false")
            );
        }

        @Test
        @DisplayName("canManageReports() は tab010.bit1() に委譲する")
        void canManageReportsDelegatesToTab010Bit1() {
            CzPermissions withBit1 = permissions(
                    false, tabWith(1), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit1 = minimalPermissions();

            assertAll(
                    () -> assertTrue(withBit1.canManageReports(), "tab010.bit1=true のとき canManageReports()=true"),
                    () -> assertFalse(withoutBit1.canManageReports(), "tab010.bit1=false のとき canManageReports()=false")
            );
        }

        @Test
        @DisplayName("canFullManage() は tab010.bit2() に委譲する")
        void canFullManageDelegatesToTab010Bit2() {
            CzPermissions withBit2 = permissions(
                    false, tabWith(2), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit2 = minimalPermissions();

            assertAll(
                    () -> assertTrue(withBit2.canFullManage(), "tab010.bit2=true のとき canFullManage()=true"),
                    () -> assertFalse(withoutBit2.canFullManage(), "tab010.bit2=false のとき canFullManage()=false")
            );
        }
    }

    @Nested
    @DisplayName("CzPermissions - Layer 2 便利メソッド (TAB 011)")
    class CzPermissionsTab011Test {

        @Test
        @DisplayName("canOutputMaintenanceHours() は tab011.bit0() に委譲する")
        void canOutputMaintenanceHoursDelegatesToTab011Bit0() {
            CzPermissions withBit0 = permissions(
                    false, TabPermission.EMPTY, tabWith(0), TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit0 = minimalPermissions();

            assertAll(
                    () -> assertTrue(withBit0.canOutputMaintenanceHours()),
                    () -> assertFalse(withoutBit0.canOutputMaintenanceHours())
            );
        }

        @Test
        @DisplayName("canNavigateBetweenForms() は tab011.bit1() に委譲する")
        void canNavigateBetweenFormsDelegatesToTab011Bit1() {
            CzPermissions withBit1 = permissions(
                    false, TabPermission.EMPTY, tabWith(1), TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit1 = minimalPermissions();

            assertAll(
                    () -> assertTrue(withBit1.canNavigateBetweenForms()),
                    () -> assertFalse(withoutBit1.canNavigateBetweenForms())
            );
        }
    }

    @Nested
    @DisplayName("CzPermissions - Layer 2 便利メソッド (TAB 012)")
    class CzPermissionsTab012Test {

        @Test
        @DisplayName("canInputPeriodCondition() は tab012.bit0() に委譲する")
        void canInputPeriodConditionDelegatesToTab012Bit0() {
            CzPermissions withBit0 = permissions(
                    false, TabPermission.EMPTY, TabPermission.EMPTY, tabWith(0),
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit0 = minimalPermissions();

            assertAll(
                    () -> assertTrue(withBit0.canInputPeriodCondition()),
                    () -> assertFalse(withoutBit0.canInputPeriodCondition())
            );
        }

        @Test
        @DisplayName("canAggregatePeriod() は tab012.bit1() に委譲する")
        void canAggregatePeriodDelegatesToTab012Bit1() {
            CzPermissions withBit1 = permissions(
                    false, TabPermission.EMPTY, TabPermission.EMPTY, tabWith(1),
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit1 = minimalPermissions();

            assertAll(
                    () -> assertTrue(withBit1.canAggregatePeriod()),
                    () -> assertFalse(withoutBit1.canAggregatePeriod())
            );
        }
    }

    // ================================================================
    // 5. useTanSeries テスト
    // ================================================================

    @Nested
    @DisplayName("CzPermissions - useTanSeries (ステータスマトリクス系列判定)")
    class UseTanSeriesTest {

        @Test
        @DisplayName("tab010.bit2=true のとき useTanSeries()=true (担当者系列)")
        void bit2TrueReturnsTanSeries() {
            CzPermissions perm = permissions(
                    false, tabWith(2), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            assertTrue(perm.useTanSeries(), "bit2=true → 担当者系列 (tan)");
        }

        @Test
        @DisplayName("tab010.bit2=false のとき useTanSeries()=false (管理者系列)")
        void bit2FalseReturnsManSeries() {
            CzPermissions perm = permissions(
                    false, tabWith(0, 1), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            assertFalse(perm.useTanSeries(), "bit2=false → 管理者系列 (man)");
        }

        @Test
        @DisplayName("useTanSeries() と canFullManage() は同じ tab010.bit2() を参照する")
        void useTanSeriesMatchesCanFullManage() {
            CzPermissions withBit2 = permissions(
                    false, tabWith(2), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions withoutBit2 = minimalPermissions();

            assertAll(
                    () -> assertEquals(withBit2.canFullManage(), withBit2.useTanSeries(),
                            "bit2=true: canFullManage と useTanSeries は一致する"),
                    () -> assertEquals(withoutBit2.canFullManage(), withoutBit2.useTanSeries(),
                            "bit2=false: canFullManage と useTanSeries は一致する")
            );
        }
    }

    // ================================================================
    // 6. isOfficial / isSubcontract テスト
    // ================================================================

    @Nested
    @DisplayName("CzPermissions - Layer 4 雇用形態判定")
    class EmploymentTypeCheckTest {

        @Test
        @DisplayName("OFFICIAL のとき isOfficial()=true, isSubcontract()=false")
        void officialEmployee() {
            CzPermissions perm = permissions(
                    false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertTrue(perm.isOfficial()),
                    () -> assertFalse(perm.isSubcontract())
            );
        }

        @Test
        @DisplayName("SUBCONTRACT のとき isSubcontract()=true, isOfficial()=false")
        void subcontractEmployee() {
            CzPermissions perm = permissions(
                    false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.SUBCONTRACT, null, false
            );

            assertAll(
                    () -> assertFalse(perm.isOfficial()),
                    () -> assertTrue(perm.isSubcontract())
            );
        }

        @Test
        @DisplayName("TEMPORARY_1 のとき isOfficial()=false, isSubcontract()=false")
        void temporary1IsNeitherOfficialNorSubcontract() {
            CzPermissions perm = permissions(
                    false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.TEMPORARY_1, null, false
            );

            assertAll(
                    () -> assertFalse(perm.isOfficial()),
                    () -> assertFalse(perm.isSubcontract())
            );
        }

        @Test
        @DisplayName("TEMPORARY_2 のとき isOfficial()=false, isSubcontract()=false")
        void temporary2IsNeitherOfficialNorSubcontract() {
            CzPermissions perm = permissions(
                    false, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.TEMPORARY_2, null, false
            );

            assertAll(
                    () -> assertFalse(perm.isOfficial()),
                    () -> assertFalse(perm.isSubcontract())
            );
        }
    }

    // ================================================================
    // 7. CzPrincipal テスト
    // ================================================================

    @Nested
    @DisplayName("CzPrincipal - 認証済みユーザー情報")
    class CzPrincipalTest {

        @Test
        @DisplayName("Principal インターフェースを実装している")
        void implementsPrincipal() {
            CzPrincipal principal = new CzPrincipal(
                    "U001", "テストユーザー", "test@example.com",
                    "ORG001", "テスト組織", minimalPermissions()
            );
            assertInstanceOf(Principal.class, principal,
                    "CzPrincipal は java.security.Principal を実装すべき");
        }

        @Test
        @DisplayName("getName() は userId を返す")
        void getNameReturnsUserId() {
            String userId = "U12345";
            CzPrincipal principal = new CzPrincipal(
                    userId, "佐藤太郎", "sato@example.com",
                    "ORG100", "総務部", minimalPermissions()
            );
            assertEquals(userId, principal.getName(),
                    "getName() は userId フィールドの値を返すべき");
        }

        @Test
        @DisplayName("record のアクセサで全フィールドを取得できる")
        void recordAccessorsReturnCorrectValues() {
            CzPermissions perm = minimalPermissions();
            CzPrincipal principal = new CzPrincipal(
                    "U001", "山田花子", "yamada@example.com",
                    "ORG200", "人事部", perm
            );

            assertAll(
                    () -> assertEquals("U001", principal.userId()),
                    () -> assertEquals("山田花子", principal.userName()),
                    () -> assertEquals("yamada@example.com", principal.email()),
                    () -> assertEquals("ORG200", principal.organizationCode()),
                    () -> assertEquals("人事部", principal.organizationName()),
                    () -> assertSame(perm, principal.permissions())
            );
        }

        @Test
        @DisplayName("CzPrincipal から CzPermissions の便利メソッドにアクセスできる")
        void canAccessPermissionsThroughPrincipal() {
            CzPermissions perm = permissions(
                    true, tabWith(0, 2), tabWith(0, 1), tabWith(0, 1),
                    new DataAuthority(DataAuthority.ZENSYA, DataAuthority.HONBU, DataAuthority.KA),
                    EmploymentType.OFFICIAL, null, false
            );
            CzPrincipal principal = new CzPrincipal(
                    "U001", "テスト", "test@example.com",
                    "ORG001", "テスト組織", perm
            );

            assertAll(
                    () -> assertTrue(principal.permissions().canReport()),
                    () -> assertTrue(principal.permissions().canFullManage()),
                    () -> assertTrue(principal.permissions().isOfficial())
            );
        }
    }

    // ================================================================
    // 8. 15 アクター構成テスト (02_actor_definition.md 準拠)
    // ================================================================

    @Nested
    @DisplayName("15 アクター構成テスト (02_actor_definition.md 準拠)")
    class ActorConfigurationTest {

        /**
         * 15 アクター構成のパラメータを提供する。
         *
         * 各引数: actorId, jinjiMode,
         *   tab010 bits (カンマ区切り or "EMPTY"),
         *   tab011 bits, tab012 bits,
         *   refAuthority, insAuthority, updAuthority,
         *   employmentType, staffRole, canDelegate,
         *   説明
         */
        static Stream<Arguments> actorConfigurations() {
            return Stream.of(
                    // ACT-01: 報告担当者 - jinjiMode=true (人事モード), tab010 bit0=true, OFFICIAL
                    Arguments.of("ACT-01", true,
                            new int[]{0}, new int[]{}, new int[]{},
                            DataAuthority.KA, null, null,
                            EmploymentType.OFFICIAL, null, false,
                            "報告担当者 - 報告書作成担当グループ"),

                    // ACT-02: 報告管理者 - tab010 bit1=true
                    Arguments.of("ACT-02", false,
                            new int[]{1}, new int[]{}, new int[]{},
                            DataAuthority.BU, DataAuthority.BU, null,
                            EmploymentType.OFFICIAL, null, false,
                            "報告管理者 - 報告書管理担当グループ"),

                    // ACT-03: 全権管理者 - tab010 bit2=true (full manage), OFFICIAL
                    Arguments.of("ACT-03", false,
                            new int[]{2}, new int[]{1}, new int[]{0, 1},
                            DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA,
                            EmploymentType.OFFICIAL, null, false,
                            "全権管理者 - 全管理グループ"),

                    // ACT-04: 管理モードユーザー - jinjiMode=false, tab010 bit0+bit1
                    Arguments.of("ACT-04", false,
                            new int[]{0, 1}, new int[]{0}, new int[]{},
                            DataAuthority.HONBU, DataAuthority.HONBU, DataAuthority.HONBU,
                            EmploymentType.OFFICIAL, null, false,
                            "管理モードユーザー"),

                    // ACT-05: 人事モードユーザー - jinjiMode=true, tab010 bit0
                    Arguments.of("ACT-05", true,
                            new int[]{0}, new int[]{}, new int[]{},
                            DataAuthority.KA, DataAuthority.KA, null,
                            EmploymentType.OFFICIAL, null, false,
                            "人事モードユーザー"),

                    // ACT-06: 正社員 - employmentType=OFFICIAL
                    Arguments.of("ACT-06", false,
                            new int[]{0}, new int[]{}, new int[]{},
                            DataAuthority.KA, DataAuthority.KA, DataAuthority.KA,
                            EmploymentType.OFFICIAL, null, false,
                            "正社員 - 全機能アクセス可能"),

                    // ACT-07: 臨時職員1 - employmentType=TEMPORARY_1
                    Arguments.of("ACT-07", true,
                            new int[]{0}, new int[]{}, new int[]{},
                            DataAuthority.KA, null, null,
                            EmploymentType.TEMPORARY_1, null, false,
                            "臨時職員1 - 制限付きアクセス"),

                    // ACT-08: 臨時職員2 - employmentType=TEMPORARY_2
                    Arguments.of("ACT-08", true,
                            new int[]{0}, new int[]{}, new int[]{},
                            DataAuthority.KA, null, null,
                            EmploymentType.TEMPORARY_2, null, false,
                            "臨時職員2 - 制限付きアクセス"),

                    // ACT-09: 外部契約者 - jinjiMode=true, SUBCONTRACT, canDelegate=true
                    Arguments.of("ACT-09", true,
                            new int[]{0}, new int[]{}, new int[]{},
                            DataAuthority.KA, null, null,
                            EmploymentType.SUBCONTRACT, null, true,
                            "外部契約者 - 代行モード可能"),

                    // ACT-10: 全社スタッフ - staffRole=931
                    Arguments.of("ACT-10", false,
                            new int[]{0, 1}, new int[]{0, 1}, new int[]{0, 1},
                            DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA,
                            EmploymentType.OFFICIAL, 931, false,
                            "全社スタッフ - 全社横断データアクセス"),

                    // ACT-11: 事業スタッフ - staffRole=932
                    Arguments.of("ACT-11", false,
                            new int[]{0, 1}, new int[]{0, 1}, new int[]{0, 1},
                            DataAuthority.EIGYOSHO, DataAuthority.EIGYOSHO, DataAuthority.EIGYOSHO,
                            EmploymentType.OFFICIAL, 932, false,
                            "事業スタッフ - 事業本部レベル"),

                    // ACT-12: 本社スタッフ - staffRole=933
                    Arguments.of("ACT-12", false,
                            new int[]{0, 1}, new int[]{0}, new int[]{},
                            DataAuthority.HONBU, DataAuthority.HONBU, null,
                            EmploymentType.OFFICIAL, 933, false,
                            "本社スタッフ - 本社レベル"),

                    // ACT-13: 局スタッフ - staffRole=934
                    Arguments.of("ACT-13", false,
                            new int[]{0}, new int[]{0}, new int[]{},
                            DataAuthority.KYOKU, DataAuthority.KYOKU, null,
                            EmploymentType.OFFICIAL, 934, false,
                            "局スタッフ - 局レベル"),

                    // ACT-14: 局スタッフ（総務管理者） - staffRole=935
                    Arguments.of("ACT-14", false,
                            new int[]{0, 1}, new int[]{0, 1}, new int[]{0, 1},
                            DataAuthority.KYOKU, DataAuthority.KYOKU, DataAuthority.KYOKU,
                            EmploymentType.OFFICIAL, 935, false,
                            "局スタッフ（総務管理者） - 局レベル + 総務管理権限"),

                    // ACT-15: 局スタッフ（営業） - staffRole=936
                    Arguments.of("ACT-15", false,
                            new int[]{0}, new int[]{}, new int[]{},
                            DataAuthority.EIGYOSHO, null, null,
                            EmploymentType.OFFICIAL, 936, false,
                            "局スタッフ（営業） - 局レベル + 営業部門権限")
            );
        }

        @ParameterizedTest(name = "{0}: {11}")
        @MethodSource("actorConfigurations")
        @DisplayName("アクター構成の権限が正しく構築される")
        void actorPermissionsAreCorrectlyConstructed(
                String actorId,
                boolean jinjiMode,
                int[] tab010Bits, int[] tab011Bits, int[] tab012Bits,
                String refAuth, String insAuth, String updAuth,
                EmploymentType employmentType,
                Integer staffRole,
                boolean canDelegate,
                String description
        ) {
            CzPermissions perm = permissions(
                    jinjiMode,
                    tabWith(tab010Bits),
                    tabWith(tab011Bits),
                    tabWith(tab012Bits),
                    new DataAuthority(refAuth, insAuth, updAuth),
                    employmentType, staffRole, canDelegate
            );

            assertNotNull(perm, actorId + " の CzPermissions が生成されるべき");

            // record フィールドの検証
            assertAll(
                    () -> assertEquals(jinjiMode, perm.jinjiMode(),
                            actorId + ": jinjiMode"),
                    () -> assertEquals(employmentType, perm.employmentType(),
                            actorId + ": employmentType"),
                    () -> assertEquals(staffRole, perm.staffRole(),
                            actorId + ": staffRole"),
                    () -> assertEquals(canDelegate, perm.canDelegate(),
                            actorId + ": canDelegate")
            );
        }

        // ---- 個別アクターの詳細検証 ----

        @Test
        @DisplayName("ACT-01: 報告担当者 - canReport()=true, canManageReports()=false, canFullManage()=false, 正社員")
        void act01ReportWorker() {
            CzPermissions perm = permissions(
                    true, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.KA, null, null),
                    EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertTrue(perm.jinjiMode(), "人事モード"),
                    () -> assertTrue(perm.canReport(), "報告書作成担当グループ"),
                    () -> assertFalse(perm.canManageReports(), "報告書管理は不可"),
                    () -> assertFalse(perm.canFullManage(), "全管理は不可"),
                    () -> assertTrue(perm.isOfficial(), "正社員"),
                    () -> assertFalse(perm.useTanSeries(), "bit2=false → 管理者系列")
            );
        }

        @Test
        @DisplayName("ACT-02: 報告管理者 - canManageReports()=true, canReport()=false")
        void act02ReportManager() {
            CzPermissions perm = permissions(
                    false, tabWith(1), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.BU, DataAuthority.BU, null),
                    EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertFalse(perm.jinjiMode(), "管理モード"),
                    () -> assertFalse(perm.canReport(), "報告書作成は不可"),
                    () -> assertTrue(perm.canManageReports(), "報告書管理担当グループ"),
                    () -> assertFalse(perm.canFullManage(), "全管理は不可"),
                    () -> assertFalse(perm.useTanSeries(), "bit2=false → 管理者系列")
            );
        }

        @Test
        @DisplayName("ACT-03: 全権管理者 - canFullManage()=true, useTanSeries()=true, 全データアクセス")
        void act03FullManager() {
            CzPermissions perm = permissions(
                    false, tabWith(2), tabWith(1), tabWith(0, 1),
                    new DataAuthority(DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA),
                    EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertFalse(perm.jinjiMode(), "管理モード"),
                    () -> assertFalse(perm.canReport(), "bit0 は未設定"),
                    () -> assertFalse(perm.canManageReports(), "bit1 は未設定"),
                    () -> assertTrue(perm.canFullManage(), "全管理グループ"),
                    () -> assertTrue(perm.useTanSeries(), "bit2=true → 担当者系列"),
                    () -> assertTrue(perm.canNavigateBetweenForms(), "画面遷移リンク有効"),
                    () -> assertTrue(perm.canInputPeriodCondition(), "期間入力条件有効"),
                    () -> assertTrue(perm.canAggregatePeriod(), "期間集計有効"),
                    () -> assertTrue(perm.dataAuthority().canRef(), "全社参照可"),
                    () -> assertTrue(perm.dataAuthority().canIns(), "全社登録可"),
                    () -> assertTrue(perm.dataAuthority().canUpd(), "全社更新可"),
                    () -> assertTrue(perm.isOfficial(), "正社員")
            );
        }

        @Test
        @DisplayName("ACT-04: 管理モードユーザー - jinjiMode=false, 報告+管理権限")
        void act04ManagementModeUser() {
            CzPermissions perm = permissions(
                    false, tabWith(0, 1), tabWith(0), TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.HONBU, DataAuthority.HONBU, DataAuthority.HONBU),
                    EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertFalse(perm.jinjiMode(), "管理モード (czMgr)"),
                    () -> assertTrue(perm.canReport(), "報告書作成担当"),
                    () -> assertTrue(perm.canManageReports(), "報告書管理担当"),
                    () -> assertFalse(perm.canFullManage(), "全管理は不可"),
                    () -> assertTrue(perm.canOutputMaintenanceHours(), "保守H時間出力有効"),
                    () -> assertFalse(perm.useTanSeries(), "bit2=false → 管理者系列")
            );
        }

        @Test
        @DisplayName("ACT-05: 人事モードユーザー - jinjiMode=true, 報告登録主体")
        void act05HrModeUser() {
            CzPermissions perm = permissions(
                    true, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.KA, DataAuthority.KA, null),
                    EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertTrue(perm.jinjiMode(), "人事モード (czEnt)"),
                    () -> assertTrue(perm.canReport(), "報告書作成担当"),
                    () -> assertFalse(perm.canManageReports()),
                    () -> assertFalse(perm.canFullManage()),
                    () -> assertTrue(perm.dataAuthority().canRef(), "参照可"),
                    () -> assertTrue(perm.dataAuthority().canIns(), "登録可"),
                    () -> assertFalse(perm.dataAuthority().canUpd(), "更新不可")
            );
        }

        @Test
        @DisplayName("ACT-06: 正社員 - employmentType=OFFICIAL, isOfficial()=true")
        void act06OfficialEmployee() {
            CzPermissions perm = permissions(
                    false, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.KA, DataAuthority.KA, DataAuthority.KA),
                    EmploymentType.OFFICIAL, null, false
            );

            assertAll(
                    () -> assertTrue(perm.isOfficial()),
                    () -> assertFalse(perm.isSubcontract()),
                    () -> assertEquals(EmploymentType.OFFICIAL, perm.employmentType())
            );
        }

        @Test
        @DisplayName("ACT-07: 臨時職員1 - employmentType=TEMPORARY_1, 制限付きアクセス")
        void act07Temporary1() {
            CzPermissions perm = permissions(
                    true, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.KA, null, null),
                    EmploymentType.TEMPORARY_1, null, false
            );

            assertAll(
                    () -> assertFalse(perm.isOfficial(), "正社員ではない"),
                    () -> assertFalse(perm.isSubcontract(), "外部契約者でもない"),
                    () -> assertEquals(EmploymentType.TEMPORARY_1, perm.employmentType()),
                    () -> assertFalse(perm.dataAuthority().canIns(), "登録不可"),
                    () -> assertFalse(perm.dataAuthority().canUpd(), "更新不可")
            );
        }

        @Test
        @DisplayName("ACT-08: 臨時職員2 - employmentType=TEMPORARY_2, 制限付きアクセス")
        void act08Temporary2() {
            CzPermissions perm = permissions(
                    true, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.KA, null, null),
                    EmploymentType.TEMPORARY_2, null, false
            );

            assertAll(
                    () -> assertFalse(perm.isOfficial()),
                    () -> assertFalse(perm.isSubcontract()),
                    () -> assertEquals(EmploymentType.TEMPORARY_2, perm.employmentType())
            );
        }

        @Test
        @DisplayName("ACT-09: 外部契約者 - jinjiMode=true, SUBCONTRACT, canDelegate=true")
        void act09Subcontractor() {
            CzPermissions perm = permissions(
                    true, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.KA, null, null),
                    EmploymentType.SUBCONTRACT, null, true
            );

            assertAll(
                    () -> assertTrue(perm.jinjiMode(), "人事モード"),
                    () -> assertFalse(perm.isOfficial(), "正社員ではない"),
                    () -> assertTrue(perm.isSubcontract(), "外部契約者"),
                    () -> assertTrue(perm.canDelegate(), "代行モード可能"),
                    () -> assertTrue(perm.canReport(), "報告は可能"),
                    () -> assertFalse(perm.dataAuthority().canIns(), "登録不可"),
                    () -> assertFalse(perm.dataAuthority().canUpd(), "更新不可")
            );
        }

        @Test
        @DisplayName("ACT-10: 全社スタッフ - staffRole=931, 全社横断データアクセス")
        void act10CompanyWideStaff() {
            CzPermissions perm = permissions(
                    false, tabWith(0, 1), tabWith(0, 1), tabWith(0, 1),
                    new DataAuthority(DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA),
                    EmploymentType.OFFICIAL, 931, false
            );

            assertAll(
                    () -> assertEquals(931, perm.staffRole(), "全社スタッフロール"),
                    () -> assertTrue(perm.dataAuthority().canRef()),
                    () -> assertTrue(perm.dataAuthority().canIns()),
                    () -> assertTrue(perm.dataAuthority().canUpd()),
                    () -> assertEquals(DataAuthority.ZENSYA, perm.dataAuthority().ref(),
                            "全社スコープ (参照)"),
                    () -> assertTrue(perm.isOfficial()),
                    () -> assertTrue(perm.canReport()),
                    () -> assertTrue(perm.canManageReports()),
                    () -> assertTrue(perm.canOutputMaintenanceHours()),
                    () -> assertTrue(perm.canNavigateBetweenForms()),
                    () -> assertTrue(perm.canInputPeriodCondition()),
                    () -> assertTrue(perm.canAggregatePeriod())
            );
        }

        @Test
        @DisplayName("ACT-11: 事業スタッフ - staffRole=932, 事業本部レベル")
        void act11BusinessStaff() {
            CzPermissions perm = permissions(
                    false, tabWith(0, 1), tabWith(0, 1), tabWith(0, 1),
                    new DataAuthority(DataAuthority.EIGYOSHO, DataAuthority.EIGYOSHO, DataAuthority.EIGYOSHO),
                    EmploymentType.OFFICIAL, 932, false
            );

            assertAll(
                    () -> assertEquals(932, perm.staffRole(), "事業スタッフロール"),
                    () -> assertEquals(DataAuthority.EIGYOSHO, perm.dataAuthority().ref(),
                            "営業所スコープ"),
                    () -> assertTrue(perm.isOfficial())
            );
        }

        @Test
        @DisplayName("ACT-12: 本社スタッフ - staffRole=933, 本社レベル")
        void act12HeadquartersStaff() {
            CzPermissions perm = permissions(
                    false, tabWith(0, 1), tabWith(0), TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.HONBU, DataAuthority.HONBU, null),
                    EmploymentType.OFFICIAL, 933, false
            );

            assertAll(
                    () -> assertEquals(933, perm.staffRole(), "本社スタッフロール"),
                    () -> assertEquals(DataAuthority.HONBU, perm.dataAuthority().ref(),
                            "本部スコープ"),
                    () -> assertTrue(perm.dataAuthority().canRef()),
                    () -> assertTrue(perm.dataAuthority().canIns()),
                    () -> assertFalse(perm.dataAuthority().canUpd(), "更新権限なし"),
                    () -> assertTrue(perm.canOutputMaintenanceHours()),
                    () -> assertFalse(perm.canNavigateBetweenForms(), "画面遷移不可")
            );
        }

        @Test
        @DisplayName("ACT-13: 局スタッフ - staffRole=934, 局レベル")
        void act13BureauStaff() {
            CzPermissions perm = permissions(
                    false, tabWith(0), tabWith(0), TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.KYOKU, DataAuthority.KYOKU, null),
                    EmploymentType.OFFICIAL, 934, false
            );

            assertAll(
                    () -> assertEquals(934, perm.staffRole(), "局スタッフロール"),
                    () -> assertEquals(DataAuthority.KYOKU, perm.dataAuthority().ref(),
                            "局スコープ"),
                    () -> assertTrue(perm.canReport()),
                    () -> assertFalse(perm.canManageReports(), "報告管理は不可"),
                    () -> assertTrue(perm.canOutputMaintenanceHours()),
                    () -> assertFalse(perm.canNavigateBetweenForms())
            );
        }

        @Test
        @DisplayName("ACT-14: 局スタッフ（総務管理者） - staffRole=935, 局レベル + 総務管理権限")
        void act14BureauAdminStaff() {
            CzPermissions perm = permissions(
                    false, tabWith(0, 1), tabWith(0, 1), tabWith(0, 1),
                    new DataAuthority(DataAuthority.KYOKU, DataAuthority.KYOKU, DataAuthority.KYOKU),
                    EmploymentType.OFFICIAL, 935, false
            );

            assertAll(
                    () -> assertEquals(935, perm.staffRole(), "局スタッフ（総務管理者）ロール"),
                    () -> assertEquals(DataAuthority.KYOKU, perm.dataAuthority().ref()),
                    () -> assertTrue(perm.dataAuthority().canRef()),
                    () -> assertTrue(perm.dataAuthority().canIns()),
                    () -> assertTrue(perm.dataAuthority().canUpd(), "総務管理者は更新可"),
                    () -> assertTrue(perm.canReport()),
                    () -> assertTrue(perm.canManageReports()),
                    () -> assertTrue(perm.canOutputMaintenanceHours()),
                    () -> assertTrue(perm.canNavigateBetweenForms()),
                    () -> assertTrue(perm.canInputPeriodCondition()),
                    () -> assertTrue(perm.canAggregatePeriod())
            );
        }

        @Test
        @DisplayName("ACT-15: 局スタッフ（営業） - staffRole=936, 営業部門権限")
        void act15BureauSalesStaff() {
            CzPermissions perm = permissions(
                    false, tabWith(0), TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(DataAuthority.EIGYOSHO, null, null),
                    EmploymentType.OFFICIAL, 936, false
            );

            assertAll(
                    () -> assertEquals(936, perm.staffRole(), "局スタッフ（営業）ロール"),
                    () -> assertEquals(DataAuthority.EIGYOSHO, perm.dataAuthority().ref(),
                            "営業所スコープ (参照のみ)"),
                    () -> assertTrue(perm.dataAuthority().canRef()),
                    () -> assertFalse(perm.dataAuthority().canIns(), "登録不可"),
                    () -> assertFalse(perm.dataAuthority().canUpd(), "更新不可"),
                    () -> assertTrue(perm.canReport()),
                    () -> assertFalse(perm.canManageReports()),
                    () -> assertFalse(perm.canFullManage()),
                    () -> assertFalse(perm.canOutputMaintenanceHours()),
                    () -> assertFalse(perm.canNavigateBetweenForms())
            );
        }
    }

    // ================================================================
    // 9. アクターの JinjiMode 切替テスト (30 パターン)
    // ================================================================

    @Nested
    @DisplayName("JinjiMode 切替 - 同一アクターでモード切替が正しく動作する")
    class JinjiModeSwitchTest {

        @Test
        @DisplayName("全権管理者 (ACT-03) の jinjiMode 切替で便利メソッドの結果は不変")
        void act03JinjiModeSwitchDoesNotAffectTabPermissions() {
            TabPermission tab010 = tabWith(2);
            TabPermission tab011 = tabWith(1);
            TabPermission tab012 = tabWith(0, 1);
            DataAuthority da = new DataAuthority(DataAuthority.ZENSYA, DataAuthority.ZENSYA, DataAuthority.ZENSYA);

            CzPermissions hrMode = permissions(true, tab010, tab011, tab012, da, EmploymentType.OFFICIAL, null, false);
            CzPermissions mgrMode = permissions(false, tab010, tab011, tab012, da, EmploymentType.OFFICIAL, null, false);

            // jinjiMode は異なるが、Tab 権限は同一
            assertAll(
                    () -> assertTrue(hrMode.jinjiMode()),
                    () -> assertFalse(mgrMode.jinjiMode()),
                    () -> assertEquals(hrMode.canFullManage(), mgrMode.canFullManage()),
                    () -> assertEquals(hrMode.canNavigateBetweenForms(), mgrMode.canNavigateBetweenForms()),
                    () -> assertEquals(hrMode.canInputPeriodCondition(), mgrMode.canInputPeriodCondition()),
                    () -> assertEquals(hrMode.canAggregatePeriod(), mgrMode.canAggregatePeriod()),
                    () -> assertEquals(hrMode.useTanSeries(), mgrMode.useTanSeries()),
                    () -> assertEquals(hrMode.isOfficial(), mgrMode.isOfficial())
            );
        }

        @Test
        @DisplayName("外部契約者 (ACT-09) の jinjiMode 切替")
        void act09JinjiModeSwitch() {
            TabPermission tab010 = tabWith(0);
            DataAuthority da = new DataAuthority(DataAuthority.KA, null, null);

            CzPermissions hrMode = permissions(true, tab010, TabPermission.EMPTY, TabPermission.EMPTY,
                    da, EmploymentType.SUBCONTRACT, null, true);
            CzPermissions mgrMode = permissions(false, tab010, TabPermission.EMPTY, TabPermission.EMPTY,
                    da, EmploymentType.SUBCONTRACT, null, true);

            assertAll(
                    () -> assertTrue(hrMode.jinjiMode()),
                    () -> assertFalse(mgrMode.jinjiMode()),
                    () -> assertTrue(hrMode.isSubcontract()),
                    () -> assertTrue(mgrMode.isSubcontract()),
                    () -> assertTrue(hrMode.canDelegate()),
                    () -> assertTrue(mgrMode.canDelegate()),
                    () -> assertEquals(hrMode.canReport(), mgrMode.canReport())
            );
        }
    }

    // ================================================================
    // 10. record 等値性テスト
    // ================================================================

    @Nested
    @DisplayName("record 等値性 - equals/hashCode/toString")
    class RecordEqualityTest {

        @Test
        @DisplayName("同一パラメータの CzPermissions は equals で等しい")
        void sameParametersAreEqual() {
            CzPermissions p1 = minimalPermissions();
            CzPermissions p2 = minimalPermissions();
            assertEquals(p1, p2);
        }

        @Test
        @DisplayName("異なる jinjiMode の CzPermissions は equals で等しくない")
        void differentJinjiModeNotEqual() {
            CzPermissions p1 = permissions(
                    true, TabPermission.EMPTY, TabPermission.EMPTY, TabPermission.EMPTY,
                    new DataAuthority(null, null, null), EmploymentType.OFFICIAL, null, false
            );
            CzPermissions p2 = minimalPermissions(); // jinjiMode=false

            assertNotEquals(p1, p2);
        }

        @Test
        @DisplayName("同一パラメータの TabPermission は equals で等しい")
        void sameTabPermissionsAreEqual() {
            TabPermission t1 = tabWith(0, 1);
            TabPermission t2 = tabWith(0, 1);
            assertEquals(t1, t2);
        }

        @Test
        @DisplayName("同一パラメータの DataAuthority は equals で等しい")
        void sameDataAuthorityAreEqual() {
            var d1 = new DataAuthority(DataAuthority.ZENSYA, null, DataAuthority.KA);
            var d2 = new DataAuthority(DataAuthority.ZENSYA, null, DataAuthority.KA);
            assertEquals(d1, d2);
        }

        @Test
        @DisplayName("同一パラメータの CzPrincipal は equals で等しい")
        void sameCzPrincipalAreEqual() {
            CzPermissions perm = minimalPermissions();
            CzPrincipal p1 = new CzPrincipal("U001", "テスト", "t@t.com", "ORG", "組織", perm);
            CzPrincipal p2 = new CzPrincipal("U001", "テスト", "t@t.com", "ORG", "組織", perm);
            assertEquals(p1, p2);
        }
    }
}

package com.example.czConsv.security.service;

import com.example.czConsv.dao.Mcz12OrgnKrDao;
import com.example.czConsv.entity.Mcz12OrgnKr;
import com.example.czConsv.security.model.DataAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * OrganizationScopeResolver 単体テスト。
 *
 * <p>ユーザーの相対権限レベルと組織コードに基づいて、
 * アクセス可能な組織コードリストを解決するロジックを検証する。
 *
 * <p>Mcz12OrgnKrDao をモックし、階層テーブルへの依存を排除する。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationScopeResolver: 組織スコープ解決テスト")
class OrganizationScopeResolverTest {

    @Mock
    private Mcz12OrgnKrDao mcz12Dao;

    private OrganizationScopeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OrganizationScopeResolver(mcz12Dao);
    }

    // ========================================================================
    // テストデータ生成ヘルパー
    // ========================================================================

    /**
     * 組織階層レコードを生成する。
     *
     * @param sikcd     組織コード（主キー）
     * @param honb      本部コード
     * @param kyk       局コード
     * @param situ      室コード
     * @param bu        部コード
     * @param ka        課コード
     * @param egsyocd   営業所コード
     * @return Mcz12OrgnKr エンティティ
     */
    private static Mcz12OrgnKr createOrg(String sikcd, String honb, String kyk,
                                          String situ, String bu, String ka,
                                          String egsyocd) {
        var org = new Mcz12OrgnKr();
        org.sikcd = sikcd;
        org.sikcdhonb = honb;
        org.sikcdkyk = kyk;
        org.sikcdsitu = situ;
        org.sikcdbu = bu;
        org.sikcdka = ka;
        org.egsyocd = egsyocd;
        return org;
    }

    /**
     * テスト用組織階層データ（7レコード）。
     *
     * <pre>
     * sikcd   honb    kyk     situ    bu      ka      egsyocd
     * ORG0001 HONB001 KYK001  SITU001 BU001   KA001   EGS001   ← ユーザーの所属組織
     * ORG0002 HONB001 KYK001  SITU001 BU001   KA002   EGS001   ← 同部・別課
     * ORG0003 HONB001 KYK001  SITU001 BU002   KA003   EGS001   ← 同室・別部
     * ORG0004 HONB001 KYK001  SITU002 BU003   KA004   EGS001   ← 同局・別室
     * ORG0005 HONB001 KYK002  SITU003 BU004   KA005   EGS001   ← 同本部・別局
     * ORG0006 HONB002 KYK003  SITU004 BU005   KA006   EGS002   ← 別本部
     * ORG0007 HONB001 KYK001  SITU001 BU001   KA007   EGS002   ← 同部・別営業所
     * </pre>
     */
    private List<Mcz12OrgnKr> createTestHierarchy() {
        return List.of(
                createOrg("ORG0001", "HONB001", "KYK001", "SITU001", "BU001", "KA001", "EGS001"),
                createOrg("ORG0002", "HONB001", "KYK001", "SITU001", "BU001", "KA002", "EGS001"),
                createOrg("ORG0003", "HONB001", "KYK001", "SITU001", "BU002", "KA003", "EGS001"),
                createOrg("ORG0004", "HONB001", "KYK001", "SITU002", "BU003", "KA004", "EGS001"),
                createOrg("ORG0005", "HONB001", "KYK002", "SITU003", "BU004", "KA005", "EGS001"),
                createOrg("ORG0006", "HONB002", "KYK003", "SITU004", "BU005", "KA006", "EGS002"),
                createOrg("ORG0007", "HONB001", "KYK001", "SITU001", "BU001", "KA007", "EGS002")
        );
    }

    /**
     * モックを設定する。ユーザー組織の検索と全件取得を設定する。
     */
    private void setupMocks(String userOrgCode) {
        List<Mcz12OrgnKr> allOrgs = createTestHierarchy();

        // selectById: ユーザーの組織レコードを返す
        Optional<Mcz12OrgnKr> userOrg = allOrgs.stream()
                .filter(o -> o.sikcd.equals(userOrgCode))
                .findFirst();
        when(mcz12Dao.selectById(userOrgCode)).thenReturn(userOrg);

        // selectAll: 全組織レコードを返す
        when(mcz12Dao.selectAll()).thenReturn(allOrgs);
    }

    // ========================================================================
    // ZENSYA（全社）レベル
    // ========================================================================

    @Nested
    @DisplayName("ZENSYA（全社）レベルの検証")
    class ZensyaTest {

        @Test
        @DisplayName("ZENSYA権限はnullを返す（フィルタなし＝全データアクセス可能）")
        void zensya_returnsNull_noFilter() {
            List<String> result = resolver.resolve(DataAuthority.ZENSYA, "ORG0001");

            assertNull(result, "ZENSYAはnull（フィルタなし）を返すべき");
            // ZENSYA の場合 DAO は呼ばれない
            verifyNoInteractions(mcz12Dao);
        }
    }

    // ========================================================================
    // KA（課）レベル
    // ========================================================================

    @Nested
    @DisplayName("KA（課）レベルの検証")
    class KaTest {

        @Test
        @DisplayName("KA権限はユーザー自身の組織コードのみを返す")
        void ka_returnsOnlyUserOrgCode() {
            // KA は selectAll を呼ばないため selectById のみモック
            when(mcz12Dao.selectById("ORG0001"))
                    .thenReturn(Optional.of(createOrg("ORG0001", "HONB001", "KYK001",
                            "SITU001", "BU001", "KA001", "EGS001")));

            List<String> result = resolver.resolve(DataAuthority.KA, "ORG0001");

            assertNotNull(result);
            assertEquals(1, result.size(), "KAレベルは1件のみ");
            assertTrue(result.contains("ORG0001"), "ユーザー自身の組織コードが含まれる");
        }
    }

    // ========================================================================
    // BU（部）レベル
    // ========================================================================

    @Nested
    @DisplayName("BU（部）レベルの検証")
    class BuTest {

        @Test
        @DisplayName("BU権限はユーザーの組織＋同部の子組織コードを返す")
        void bu_returnsUserOrgAndChildOrgs() {
            setupMocks("ORG0001");

            List<String> result = resolver.resolve(DataAuthority.BU, "ORG0001");

            assertNotNull(result);
            // BU001 に属する組織: ORG0001(KA001), ORG0002(KA002), ORG0007(KA007)
            assertEquals(3, result.size(), "BUレベルは同部の3件を返す");
            assertTrue(result.contains("ORG0001"), "ユーザー自身の組織");
            assertTrue(result.contains("ORG0002"), "同部・別課 KA002");
            assertTrue(result.contains("ORG0007"), "同部・別営業所 KA007");
            assertFalse(result.contains("ORG0003"), "別部 BU002 は含まれない");
        }
    }

    // ========================================================================
    // KYOKU（局）レベル
    // ========================================================================

    @Nested
    @DisplayName("KYOKU（局）レベルの検証")
    class KyokuTest {

        @Test
        @DisplayName("KYOKU権限はユーザーの局配下の組織コードを返す")
        void kyoku_returnsBroaderSetOfOrgCodes() {
            setupMocks("ORG0001");

            List<String> result = resolver.resolve(DataAuthority.KYOKU, "ORG0001");

            assertNotNull(result);
            // KYK001 に属する組織: ORG0001, ORG0002, ORG0003, ORG0004, ORG0007
            assertEquals(5, result.size(), "KYOKUレベルは同局の5件を返す");
            assertTrue(result.contains("ORG0001"), "ユーザー自身の組織");
            assertTrue(result.contains("ORG0002"), "同局・同部・別課");
            assertTrue(result.contains("ORG0003"), "同局・別部 BU002");
            assertTrue(result.contains("ORG0004"), "同局・別室 SITU002");
            assertTrue(result.contains("ORG0007"), "同局・同部・別営業所");
            assertFalse(result.contains("ORG0005"), "別局 KYK002 は含まれない");
            assertFalse(result.contains("ORG0006"), "別本部 HONB002 は含まれない");
        }
    }

    // ========================================================================
    // SHITSU（室）レベル
    // ========================================================================

    @Nested
    @DisplayName("SHITSU（室）レベルの検証")
    class ShitsuTest {

        @Test
        @DisplayName("SHITSU権限はユーザーの室配下の組織コードを返す")
        void shitsu_returnsOrgCodesWithinShitsu() {
            setupMocks("ORG0001");

            List<String> result = resolver.resolve(DataAuthority.SHITSU, "ORG0001");

            assertNotNull(result);
            // SITU001 に属する組織: ORG0001, ORG0002, ORG0003, ORG0007
            assertEquals(4, result.size(), "SHITSUレベルは同室の4件を返す");
            assertTrue(result.contains("ORG0001"), "ユーザー自身の組織");
            assertTrue(result.contains("ORG0002"), "同室・同部・別課");
            assertTrue(result.contains("ORG0003"), "同室・別部 BU002");
            assertTrue(result.contains("ORG0007"), "同室・同部・別営業所");
            assertFalse(result.contains("ORG0004"), "別室 SITU002 は含まれない");
        }
    }

    // ========================================================================
    // HONBU（本部）レベル
    // ========================================================================

    @Nested
    @DisplayName("HONBU（本部）レベルの検証")
    class HonbuTest {

        @Test
        @DisplayName("HONBU権限はユーザーの本部配下の全組織コードを返す")
        void honbu_returnsEvenBroaderSetOfOrgCodes() {
            setupMocks("ORG0001");

            List<String> result = resolver.resolve(DataAuthority.HONBU, "ORG0001");

            assertNotNull(result);
            // HONB001 に属する組織: ORG0001, ORG0002, ORG0003, ORG0004, ORG0005, ORG0007
            assertEquals(6, result.size(), "HONBUレベルは同本部の6件を返す");
            assertTrue(result.contains("ORG0001"), "ユーザー自身の組織");
            assertTrue(result.contains("ORG0002"), "同本部・同局");
            assertTrue(result.contains("ORG0003"), "同本部・同局・別部");
            assertTrue(result.contains("ORG0004"), "同本部・同局・別室");
            assertTrue(result.contains("ORG0005"), "同本部・別局 KYK002");
            assertTrue(result.contains("ORG0007"), "同本部・同局・同部・別営業所");
            assertFalse(result.contains("ORG0006"), "別本部 HONB002 は含まれない");
        }
    }

    // ========================================================================
    // EIGYOSHO（営業所）レベル
    // ========================================================================

    @Nested
    @DisplayName("EIGYOSHO（営業所）レベルの検証")
    class EigyoshoTest {

        @Test
        @DisplayName("EIGYOSHO権限はユーザーの営業所配下の組織コードを返す")
        void eigyosho_returnsAllWithinEigyosho() {
            setupMocks("ORG0001");

            List<String> result = resolver.resolve(DataAuthority.EIGYOSHO, "ORG0001");

            assertNotNull(result);
            // EGS001 に属する組織: ORG0001, ORG0002, ORG0003, ORG0004, ORG0005
            assertEquals(5, result.size(), "EIGYOSHOレベルは同営業所の5件を返す");
            assertTrue(result.contains("ORG0001"), "ユーザー自身の組織");
            assertTrue(result.contains("ORG0002"), "同営業所 EGS001");
            assertTrue(result.contains("ORG0003"), "同営業所 EGS001");
            assertTrue(result.contains("ORG0004"), "同営業所 EGS001");
            assertTrue(result.contains("ORG0005"), "同営業所 EGS001");
            assertFalse(result.contains("ORG0006"), "別営業所 EGS002 は含まれない");
            assertFalse(result.contains("ORG0007"), "別営業所 EGS002 は含まれない");
        }
    }

    // ========================================================================
    // null 権限レベル
    // ========================================================================

    @Nested
    @DisplayName("null権限レベルの検証")
    class NullAuthorityTest {

        @Test
        @DisplayName("null権限レベルは空リストを返す（アクセス不可）")
        void nullAuthority_returnsEmptyList() {
            List<String> result = resolver.resolve(null, "ORG0001");

            assertNotNull(result, "結果はnullではなく空リスト");
            assertTrue(result.isEmpty(), "null権限は空リスト（アクセス不可）");
            verifyNoInteractions(mcz12Dao);
        }
    }

    // ========================================================================
    // 不明な権限レベル
    // ========================================================================

    @Nested
    @DisplayName("不明な権限レベルの検証")
    class UnknownAuthorityTest {

        @Test
        @DisplayName("不明な権限レベルは空リストを返す（アクセス不可）")
        void unknownAuthority_returnsEmptyList() {
            // 不明レベルは selectById の後に resolveParentCodeExtractor → null で終了
            // selectAll は呼ばれないため selectById のみモック
            when(mcz12Dao.selectById("ORG0001"))
                    .thenReturn(Optional.of(createOrg("ORG0001", "HONB001", "KYK001",
                            "SITU001", "BU001", "KA001", "EGS001")));

            List<String> result = resolver.resolve("UNKNOWN_LEVEL", "ORG0001");

            assertNotNull(result, "結果はnullではなく空リスト");
            assertTrue(result.isEmpty(), "不明な権限レベルは空リスト（アクセス不可）");
        }
    }

    // ========================================================================
    // ユーザー組織コードが見つからない場合
    // ========================================================================

    @Nested
    @DisplayName("ユーザー組織コード未発見の検証")
    class OrgCodeNotFoundTest {

        @Test
        @DisplayName("テーブルに存在しない組織コードは空リストを返す")
        void orgCodeNotFound_returnsEmptyList() {
            when(mcz12Dao.selectById("UNKNOWN_ORG")).thenReturn(Optional.empty());

            List<String> result = resolver.resolve(DataAuthority.BU, "UNKNOWN_ORG");

            assertNotNull(result, "結果はnullではなく空リスト");
            assertTrue(result.isEmpty(), "存在しない組織コードは空リスト");
            verify(mcz12Dao).selectById("UNKNOWN_ORG");
            verify(mcz12Dao, never()).selectAll();
        }

        @Test
        @DisplayName("null組織コードは空リストを返す")
        void nullOrgCode_returnsEmptyList() {
            List<String> result = resolver.resolve(DataAuthority.BU, null);

            assertNotNull(result, "結果はnullではなく空リスト");
            assertTrue(result.isEmpty(), "null組織コードは空リスト");
            verifyNoInteractions(mcz12Dao);
        }
    }

    // ========================================================================
    // 階層フィルタリングの正確性
    // ========================================================================

    @Nested
    @DisplayName("階層フィルタリングの境界検証")
    class HierarchyBoundaryTest {

        @Test
        @DisplayName("BUレベルで別部の組織は含まれないこと")
        void bu_excludesDifferentBu() {
            setupMocks("ORG0001");

            List<String> result = resolver.resolve(DataAuthority.BU, "ORG0001");

            assertFalse(result.contains("ORG0003"), "BU002（別部）は含まれない");
            assertFalse(result.contains("ORG0004"), "BU003（別部）は含まれない");
            assertFalse(result.contains("ORG0005"), "BU004（別部）は含まれない");
            assertFalse(result.contains("ORG0006"), "BU005（別部）は含まれない");
        }

        @Test
        @DisplayName("KYOKUレベルで別局の組織は含まれないこと")
        void kyoku_excludesDifferentKyoku() {
            setupMocks("ORG0001");

            List<String> result = resolver.resolve(DataAuthority.KYOKU, "ORG0001");

            assertFalse(result.contains("ORG0005"), "KYK002（別局）は含まれない");
            assertFalse(result.contains("ORG0006"), "KYK003（別局）は含まれない");
        }

        @Test
        @DisplayName("ZENSYAはDAOを呼び出さないこと")
        void zensya_doesNotCallDao() {
            resolver.resolve(DataAuthority.ZENSYA, "ORG0001");

            verifyNoInteractions(mcz12Dao);
        }

        @Test
        @DisplayName("KAレベルはselectAllを呼び出さないこと（自身の組織のみ）")
        void ka_doesNotCallSelectAll() {
            when(mcz12Dao.selectById("ORG0001"))
                    .thenReturn(Optional.of(createOrg("ORG0001", "HONB001", "KYK001",
                            "SITU001", "BU001", "KA001", "EGS001")));

            resolver.resolve(DataAuthority.KA, "ORG0001");

            verify(mcz12Dao).selectById("ORG0001");
            verify(mcz12Dao, never()).selectAll();
        }
    }
}

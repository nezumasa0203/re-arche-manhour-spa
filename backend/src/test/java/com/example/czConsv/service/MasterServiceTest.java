package com.example.czConsv.service;

import com.example.czConsv.dao.Mav01SysDao;
import com.example.czConsv.dao.Mav03SubsysDao;
import com.example.czConsv.dao.Mcz02HosyuKategoriDao;
import com.example.czConsv.dao.Mcz12OrgnKrDao;
import com.example.czConsv.dao.Tcz16TntBusyoRirekiDao;
import com.example.czConsv.entity.Mav01Sys;
import com.example.czConsv.entity.Mav03Subsys;
import com.example.czConsv.entity.Mcz02HosyuKategori;
import com.example.czConsv.entity.Mcz12OrgnKr;
import com.example.czConsv.entity.Tcz16TntBusyoRireki;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * MasterService の単体テスト。
 *
 * <p>マスタデータ参照のフィルタリングロジックを検証する。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MasterService: マスタデータ参照")
class MasterServiceTest {

    @Mock
    private Mcz12OrgnKrDao organizationDao;

    @Mock
    private Mav01SysDao systemDao;

    @Mock
    private Mav03SubsysDao subsystemDao;

    @Mock
    private Mcz02HosyuKategoriDao categoryDao;

    @Mock
    private Tcz16TntBusyoRirekiDao staffDao;

    @InjectMocks
    private MasterService service;

    // ========================================================================
    // getOrganizations テスト
    // ========================================================================

    @Nested
    @DisplayName("getOrganizations: 組織一覧取得")
    class GetOrganizationsTests {

        @Test
        @DisplayName("全レコードを返す（delflgフィルタなし）")
        void returnsAllRecords() {
            Mcz12OrgnKr org1 = createOrganization("001", "本部A");
            Mcz12OrgnKr org2 = createOrganization("002", "本部B");

            when(organizationDao.selectAll())
                    .thenReturn(List.of(org1, org2));

            List<Mcz12OrgnKr> result = service.getOrganizations();

            assertEquals(2, result.size());
            assertEquals("001", result.get(0).sikcd);
            assertEquals("002", result.get(1).sikcd);
        }
    }

    // ========================================================================
    // getSystems テスト
    // ========================================================================

    @Nested
    @DisplayName("getSystems: システム一覧取得")
    class GetSystemsTests {

        @Test
        @DisplayName("未削除レコードのみ返す")
        void returnsNonDeletedRecords() {
            Mav01Sys active1 = createSystem("01", "SYS001", "システムA", "0");
            Mav01Sys active2 = createSystem("01", "SYS002", "システムB", "0");
            Mav01Sys deleted = createSystem("01", "SYS003", "システムC", "1");

            when(systemDao.selectAll())
                    .thenReturn(List.of(active1, active2, deleted));

            List<Mav01Sys> result = service.getSystems();

            assertEquals(2, result.size());
            assertEquals("SYS001", result.get(0).sknno);
            assertEquals("SYS002", result.get(1).sknno);
        }
    }

    // ========================================================================
    // getSubsystems テスト
    // ========================================================================

    @Nested
    @DisplayName("getSubsystems: サブシステム一覧取得")
    class GetSubsystemsTests {

        @Test
        @DisplayName("キーワード指定時にサブシステム名で部分一致フィルタする")
        void filtersWithKeyword() {
            Mav03Subsys match = createSubsystem("SUB001", "会計サブシステム",
                    "カイケイサブシステム", "0");
            Mav03Subsys noMatch = createSubsystem("SUB002", "人事サブシステム",
                    "ジンジサブシステム", "0");
            Mav03Subsys deleted = createSubsystem("SUB003", "会計バッチ",
                    "カイケイバッチ", "1");

            when(subsystemDao.selectAll())
                    .thenReturn(List.of(match, noMatch, deleted));

            List<Mav03Subsys> result = service.getSubsystems("会計");

            assertEquals(1, result.size());
            assertEquals("SUB001", result.get(0).subsysno);
        }

        @Test
        @DisplayName("キーワードなしの場合は未削除の全件返す")
        void returnsAllWithoutKeyword() {
            Mav03Subsys sub1 = createSubsystem("SUB001", "会計サブシステム",
                    "カイケイサブシステム", "0");
            Mav03Subsys sub2 = createSubsystem("SUB002", "人事サブシステム",
                    "ジンジサブシステム", "0");
            Mav03Subsys deleted = createSubsystem("SUB003", "削除済み",
                    "サクジョズミ", "1");

            when(subsystemDao.selectAll())
                    .thenReturn(List.of(sub1, sub2, deleted));

            List<Mav03Subsys> result = service.getSubsystems(null);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("カナ名での部分一致でもフィルタされる")
        void filtersWithKanaKeyword() {
            Mav03Subsys sub = createSubsystem("SUB001", "会計サブシステム",
                    "カイケイサブシステム", "0");

            when(subsystemDao.selectAll()).thenReturn(List.of(sub));

            List<Mav03Subsys> result = service.getSubsystems("カイケイ");

            assertEquals(1, result.size());
        }
    }

    // ========================================================================
    // getCategories テスト
    // ========================================================================

    @Nested
    @DisplayName("getCategories: 保守カテゴリ一覧取得")
    class GetCategoriesTests {

        @Test
        @DisplayName("指定年度の期間と有効期間が重なるカテゴリを返す")
        void filtersByFiscalYear() {
            // FY2024: 2024-04-01 ~ 2025-03-31
            Mcz02HosyuKategori inRange = createCategory("CAT01",
                    LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31), "0");
            Mcz02HosyuKategori outOfRange = createCategory("CAT02",
                    LocalDate.of(2022, 1, 1), LocalDate.of(2023, 12, 31), "0");
            Mcz02HosyuKategori deleted = createCategory("CAT03",
                    LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31), "1");

            when(categoryDao.selectAll())
                    .thenReturn(List.of(inRange, outOfRange, deleted));

            List<Mcz02HosyuKategori> result = service.getCategories(2024);

            assertEquals(1, result.size());
            assertEquals("CAT01", result.get(0).hsKategori);
        }

        @Test
        @DisplayName("有効期間が年度末にちょうど開始するカテゴリも含む")
        void includesEdgeCaseStart() {
            // FY2024: 2024-04-01 ~ 2025-03-31
            Mcz02HosyuKategori edgeCase = createCategory("CAT01",
                    LocalDate.of(2025, 3, 31), LocalDate.of(2026, 3, 31), "0");

            when(categoryDao.selectAll()).thenReturn(List.of(edgeCase));

            List<Mcz02HosyuKategori> result = service.getCategories(2024);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("有効期間が年度開始にちょうど終了するカテゴリも含む")
        void includesEdgeCaseEnd() {
            // FY2024: 2024-04-01 ~ 2025-03-31
            Mcz02HosyuKategori edgeCase = createCategory("CAT01",
                    LocalDate.of(2023, 1, 1), LocalDate.of(2024, 4, 1), "0");

            when(categoryDao.selectAll()).thenReturn(List.of(edgeCase));

            List<Mcz02HosyuKategori> result = service.getCategories(2024);

            assertEquals(1, result.size());
        }
    }

    // ========================================================================
    // searchStaff テスト
    // ========================================================================

    @Nested
    @DisplayName("searchStaff: 担当者検索")
    class SearchStaffTests {

        @Test
        @DisplayName("担当部署で部分一致検索する")
        void searchByDepartment() {
            Tcz16TntBusyoRireki match = createStaff("0", "EMP001",
                    "DEPT01", "0");
            Tcz16TntBusyoRireki noMatch = createStaff("0", "EMP002",
                    "DEPT99", "0");
            Tcz16TntBusyoRireki deleted = createStaff("0", "EMP003",
                    "DEPT01", "1");

            when(staffDao.selectAll())
                    .thenReturn(List.of(match, noMatch, deleted));

            List<Tcz16TntBusyoRireki> result = service.searchStaff("DEPT01");

            assertEquals(1, result.size());
            assertEquals("EMP001", result.get(0).sknno);
        }

        @Test
        @DisplayName("社員番号でも部分一致検索できる")
        void searchBySknno() {
            Tcz16TntBusyoRireki staff = createStaff("0", "EMP001",
                    "DEPT01", "0");

            when(staffDao.selectAll()).thenReturn(List.of(staff));

            List<Tcz16TntBusyoRireki> result = service.searchStaff("EMP001");

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("空文字列の場合は未削除の全件返す")
        void emptyNameReturnsAll() {
            Tcz16TntBusyoRireki staff1 = createStaff("0", "EMP001",
                    "DEPT01", "0");
            Tcz16TntBusyoRireki staff2 = createStaff("0", "EMP002",
                    "DEPT02", "0");
            Tcz16TntBusyoRireki deleted = createStaff("0", "EMP003",
                    "DEPT03", "1");

            when(staffDao.selectAll())
                    .thenReturn(List.of(staff1, staff2, deleted));

            List<Tcz16TntBusyoRireki> result = service.searchStaff("");

            assertEquals(2, result.size());
        }
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    private Mcz12OrgnKr createOrganization(String sikcd, String hyojikj) {
        Mcz12OrgnKr entity = new Mcz12OrgnKr();
        entity.sikcd = sikcd;
        entity.hyojikj = hyojikj;
        return entity;
    }

    private Mav01Sys createSystem(String skbtcd, String sknno,
                                  String sysMei, String delflg) {
        Mav01Sys entity = new Mav01Sys();
        entity.skbtcd = skbtcd;
        entity.sknno = sknno;
        entity.sysMei = sysMei;
        entity.delflg = delflg;
        return entity;
    }

    private Mav03Subsys createSubsystem(String subsysno, String subsysMei,
                                         String subsysMeiKn, String delflg) {
        Mav03Subsys entity = new Mav03Subsys();
        entity.skbtcd = "01";
        entity.sknno = "SYS001";
        entity.subsysno = subsysno;
        entity.subsysMei = subsysMei;
        entity.subsysMeiKn = subsysMeiKn;
        entity.delflg = delflg;
        return entity;
    }

    private Mcz02HosyuKategori createCategory(String hsKategori,
                                               LocalDate start,
                                               LocalDate end,
                                               String delflg) {
        Mcz02HosyuKategori entity = new Mcz02HosyuKategori();
        entity.hsKategori = hsKategori;
        entity.yukouKaishiki = start;
        entity.yukouSyuryoki = end;
        entity.delflg = delflg;
        return entity;
    }

    private Tcz16TntBusyoRireki createStaff(String tntKubun, String sknno,
                                             String tntBusyo, String delflg) {
        Tcz16TntBusyoRireki entity = new Tcz16TntBusyoRireki();
        entity.tntKubun = tntKubun;
        entity.sknno = sknno;
        entity.tntBusyo = tntBusyo;
        entity.delflg = delflg;
        return entity;
    }
}

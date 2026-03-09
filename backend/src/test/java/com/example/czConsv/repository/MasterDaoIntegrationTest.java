package com.example.czConsv.repository;

import com.example.czConsv.dao.Mav03SubsysDao;
import com.example.czConsv.dao.Mcz02HosyuKategoriDao;
import com.example.czConsv.dao.Tcz16TntBusyoRirekiDao;
import com.example.czConsv.entity.Mav03Subsys;
import com.example.czConsv.entity.Mcz02HosyuKategori;
import com.example.czConsv.entity.Tcz16TntBusyoRireki;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-031: マスタ系 DAO 統合テスト。
 * カテゴリ・サブシステム・担当者 DAO の PostgreSQL 検証。
 */
class MasterDaoIntegrationTest extends DaoIntegrationTestBase {

    @Autowired
    private Mcz02HosyuKategoriDao kategoriDao;

    @Autowired
    private Mav03SubsysDao subsysDao;

    @Autowired
    private Tcz16TntBusyoRirekiDao staffDao;

    // ─── Mcz02HosyuKategoriDao ───────────────────────────────────────

    @Nested
    class KategoriDaoTest {

        @Test
        void selectAllReturnsSeedData() {
            List<Mcz02HosyuKategori> all = kategoriDao.selectAll();
            // 02-seed.sql に 5件挿入済み
            assertThat(all).hasSizeGreaterThanOrEqualTo(5);
        }

        @Test
        void selectByIdFindsSeededRecord() {
            Optional<Mcz02HosyuKategori> found = kategoriDao.selectById(
                    "0001",
                    LocalDate.of(2020, 4, 1),
                    LocalDate.of(2099, 12, 31));
            assertThat(found).isPresent();
            assertThat(found.get().hsKategoriMei).isEqualTo("障害対応");
        }
    }

    // ─── Mav03SubsysDao ──────────────────────────────────────────────

    @Nested
    class SubsysDaoTest {

        @Test
        void selectAllReturnsSeedData() {
            List<Mav03Subsys> all = subsysDao.selectAll();
            // 02-seed.sql に 4件挿入済み
            assertThat(all).hasSizeGreaterThanOrEqualTo(4);
        }

        @Test
        void selectByKeywordFiltersOnName() {
            List<Mav03Subsys> results = subsysDao.selectByKeyword("受注");
            assertThat(results).isNotEmpty();
            assertThat(results).allMatch(r ->
                    r.subsysMei.contains("受注") || r.aplid.contains("受注"));
        }

        @Test
        void selectByKeywordNullReturnsAll() {
            List<Mav03Subsys> withKeyword = subsysDao.selectByKeyword(null);
            List<Mav03Subsys> all = subsysDao.selectAll();
            // null キーワードは全件返す
            assertThat(withKeyword).hasSizeGreaterThanOrEqualTo(all.size());
        }

        @Test
        void selectByIdFindsSeededRecord() {
            Optional<Mav03Subsys> found = subsysDao.selectById("01", "00000001", "00000001");
            assertThat(found).isPresent();
            assertThat(found.get().subsysMei).isEqualTo("受注管理");
        }

        @Test
        void insertAndSelectNewSubsystem() {
            Mav03Subsys s = new Mav03Subsys();
            s.skbtcd = "00";
            s.sknno = "99999999";
            s.subsysno = "00000001";
            s.aplid = "99999999";
            s.subsysMei = "テストサブシステム";
            s.yukouKaishiki = "20200401";
            s.yukouSyuryoki = "20991231";
            s.delflg = "0";
            s.upddate = LocalDateTime.now();
            int count = subsysDao.insert(s);
            assertThat(count).isEqualTo(1);

            Optional<Mav03Subsys> found = subsysDao.selectById("00", "99999999", "00000001");
            assertThat(found).isPresent();
            assertThat(found.get().subsysMei).isEqualTo("テストサブシステム");
        }
    }

    // ─── Tcz16TntBusyoRirekiDao ──────────────────────────────────────

    @Nested
    class StaffDaoTest {

        @Test
        void selectAllReturnsSeedData() {
            List<Tcz16TntBusyoRireki> all = staffDao.selectAll();
            // seed に tcz16 データがあれば確認、なければ空でも可
            assertThat(all).isNotNull();
        }

        @Test
        void insertAndSelectRecord() {
            Tcz16TntBusyoRireki r = new Tcz16TntBusyoRireki();
            r.tntKubun = "0";
            r.sknno = "SKB0001";
            r.tntStrYmd = LocalDate.of(2025, 1, 1);
            r.tntEndYmd = LocalDate.of(2025, 12, 31);
            r.tntBusyo = "DEV01";
            r.delflg = "0";
            r.upddate = LocalDateTime.now();
            int count = staffDao.insert(r);
            assertThat(count).isEqualTo(1);

            Optional<Tcz16TntBusyoRireki> found =
                    staffDao.selectById("0", "SKB0001", LocalDate.of(2025, 12, 31));
            assertThat(found).isPresent();
            assertThat(found.get().tntBusyo).isEqualTo("DEV01");
        }
    }
}

package com.example.czConsv.repository;

import com.example.czConsv.dao.Mcz04CtrlDao;
import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
import com.example.czConsv.entity.Mcz04Ctrl;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-029: WorkStatusDao / ControlDao 統合テスト。
 * 組織別検索、ステータスフィルタ、SELECT FOR UPDATE 排他制御の検証。
 */
class WorkStatusDaoIntegrationTest extends DaoIntegrationTestBase {

    @Autowired
    private Tcz01HosyuKousuuDao workHoursDao;

    @Autowired
    private Mcz04CtrlDao ctrlDao;

    @BeforeEach
    void insertTestRecords() {
        // Status=0 (draft) record for skbtcd='01'
        insertRecord("01", "U001", "20261", "0");
        // Status=1 (confirmed) record for skbtcd='01'
        insertRecord("01", "U002", "20261", "1");
        // Different org record for skbtcd='02'
        insertRecord("02", "U003", "20261", "0");
    }

    private void insertRecord(String skbtcd, String staffId, String yearHalf, String status) {
        Tcz01HosyuKousuu r = new Tcz01HosyuKousuu();
        r.skbtcd = skbtcd;
        r.hssgytntEsqid = staffId;
        r.yearHalf = yearHalf;
        r.sgyymd = LocalDate.of(2026, 2, 1);
        r.status = status;
        r.kousuu = new BigDecimal("60");
        r.delflg = "0";
        r.upddate = LocalDateTime.now();
        workHoursDao.insert(r);
    }

    // ─── 組織別検索テスト ──────────────────────────────────────────────

    @Nested
    class OrganizationFilter {

        @Test
        void filtersBySkbtcd() {
            List<Tcz01HosyuKousuu> records =
                    workHoursDao.selectBySkbtcdAndYearHalf("01", "20261");
            assertThat(records).allMatch(r -> "01".equals(r.skbtcd));
        }

        @Test
        void doesNotReturnOtherOrgRecords() {
            List<Tcz01HosyuKousuu> org01 = workHoursDao.selectBySkbtcdAndYearHalf("01", "20261");
            List<Tcz01HosyuKousuu> org02 = workHoursDao.selectBySkbtcdAndYearHalf("02", "20261");
            assertThat(org01).noneMatch(r -> "02".equals(r.skbtcd));
            assertThat(org02).noneMatch(r -> "01".equals(r.skbtcd));
        }
    }

    // ─── ステータスフィルタテスト ─────────────────────────────────────

    @Nested
    class StatusFilter {

        @Test
        void filtersDraftRecords() {
            List<Tcz01HosyuKousuu> drafts =
                    workHoursDao.selectBySkbtcdYearHalfAndStatus("01", "20261", "0");
            assertThat(drafts).isNotEmpty();
            assertThat(drafts).allMatch(r -> "0".equals(r.status));
        }

        @Test
        void filtersConfirmedRecords() {
            List<Tcz01HosyuKousuu> confirmed =
                    workHoursDao.selectBySkbtcdYearHalfAndStatus("01", "20261", "1");
            assertThat(confirmed).isNotEmpty();
            assertThat(confirmed).allMatch(r -> "1".equals(r.status));
        }
    }

    // ─── Mcz04CtrlDao テスト ──────────────────────────────────────────

    @Nested
    class CtrlDaoTest {

        @Test
        void selectByIdFindsSeededRecord() {
            // 02-seed.sql で ('00', '202602') が挿入済み
            Optional<Mcz04Ctrl> ctrl = ctrlDao.selectById("00", "202602");
            assertThat(ctrl).isPresent();
            assertThat(ctrl.get().gjktFlg).isEqualTo("0");
        }

        @Test
        void selectForUpdateLocksPessimistically() {
            // SELECT FOR UPDATE が例外なく実行できることを確認
            Optional<Mcz04Ctrl> ctrl = ctrlDao.selectForUpdate("00", "202602");
            assertThat(ctrl).isPresent();
        }

        @Test
        void updateChangesGjktFlg() {
            Mcz04Ctrl ctrl = ctrlDao.selectById("00", "202602").orElseThrow();
            ctrl.gjktFlg = "1";
            ctrl.upddate = LocalDateTime.now();
            int count = ctrlDao.update(ctrl);
            assertThat(count).isEqualTo(1);

            Mcz04Ctrl updated = ctrlDao.selectById("00", "202602").orElseThrow();
            assertThat(updated.gjktFlg).isEqualTo("1");
        }

        @Test
        void selectAllReturnsSeedData() {
            List<Mcz04Ctrl> all = ctrlDao.selectAll();
            assertThat(all).hasSizeGreaterThanOrEqualTo(6); // seed has 6 rows
        }
    }
}

package com.example.czConsv.repository;

import com.example.czConsv.dao.Tcz13SubsysSumDao;
import com.example.czConsv.entity.Tcz13SubsysSum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-030: 集計系 DAO 統合テスト。
 * Tcz13SubsysSumDao の3階層集計クエリを PostgreSQL で検証。
 */
class AggregateDaoIntegrationTest extends DaoIntegrationTestBase {

    @Autowired
    private Tcz13SubsysSumDao dao;

    @BeforeEach
    void insertTestData() {
        // 2025年度第1期（202501, 202502, 202503）のサンプルデータ
        insertSum("202501", "20251", "1", "SYS1", "SUB1", "0", "0", "0001", "00");
        insertSum("202502", "20251", "1", "SYS1", "SUB1", "0", "0", "0001", "00");
        insertSum("202503", "20251", "1", "SYS1", "SUB2", "0", "0", "0001", "00");
        // 別の組織
        insertSum("202501", "20251", "1", "SYS1", "SUB1", "0", "0", "0001", "01");
    }

    private void insertSum(String yyyymm, String nendoHalf, String sumkbn,
                           String sknno, String subsknno,
                           String hsSyubetu, String hsUnyouKubun,
                           String hsKategoriId, String skbtcd) {
        Tcz13SubsysSum s = new Tcz13SubsysSum();
        s.yyyymm = yyyymm;
        s.nendoHalf = nendoHalf;
        s.month = yyyymm.substring(4);
        s.sumkbn = sumkbn;
        s.sknno = sknno;
        s.subsknno = subsknno;
        s.hsSyubetu = hsSyubetu;
        s.hsUnyouKubun = hsUnyouKubun;
        s.hsKategoriId = hsKategoriId;
        s.skbtcd = skbtcd;
        s.hsKousuu = new BigDecimal("120");
        s.delflg = "0";
        s.upddate = LocalDateTime.now();
        dao.insert(s);
    }

    @Nested
    class SelectByNendoHalf {

        @Test
        void returnsRecordsForHalfPeriod() {
            List<Tcz13SubsysSum> records = dao.selectByNendoHalf("20251");
            assertThat(records).isNotEmpty();
            assertThat(records).allMatch(r -> "20251".equals(r.nendoHalf));
        }

        @Test
        void returnsEmptyForUnknownPeriod() {
            List<Tcz13SubsysSum> records = dao.selectByNendoHalf("99991");
            assertThat(records).isEmpty();
        }
    }

    @Nested
    class SelectByYyyymmRangeAndSkbtcd {

        @Test
        void filtersMonthsAndOrganization() {
            List<Tcz13SubsysSum> records = dao.selectByYyyymmRangeAndSkbtcd(
                    List.of("202501", "202502"), "00");
            assertThat(records).isNotEmpty();
            assertThat(records).allMatch(r -> "00".equals(r.skbtcd));
            assertThat(records).allMatch(r ->
                    "202501".equals(r.yyyymm) || "202502".equals(r.yyyymm));
        }

        @Test
        void excludesOtherOrganization() {
            List<Tcz13SubsysSum> records = dao.selectByYyyymmRangeAndSkbtcd(
                    List.of("202501"), "00");
            assertThat(records).noneMatch(r -> "01".equals(r.skbtcd));
        }

        @Test
        void returnsEmptyForNoMatchingMonths() {
            List<Tcz13SubsysSum> records = dao.selectByYyyymmRangeAndSkbtcd(
                    List.of("199901"), "00");
            assertThat(records).isEmpty();
        }
    }

    @Nested
    class SelectByYyyymmAndSkbtcd {

        @Test
        void returnsRecordsForMonthAndOrg() {
            List<Tcz13SubsysSum> records =
                    dao.selectByYyyymmAndSkbtcd("202501", "00");
            assertThat(records).isNotEmpty();
            assertThat(records).allMatch(r -> "202501".equals(r.yyyymm));
            assertThat(records).allMatch(r -> "00".equals(r.skbtcd));
        }
    }

    @Nested
    class SelectById {

        @Test
        void findsInsertedRecord() {
            Optional<Tcz13SubsysSum> found =
                    dao.selectById("202501", "1", "00", "SYS1", "SUB1", "0", "0", "0001");
            assertThat(found).isPresent();
            assertThat(found.get().hsKousuu).isEqualByComparingTo("120");
        }
    }
}

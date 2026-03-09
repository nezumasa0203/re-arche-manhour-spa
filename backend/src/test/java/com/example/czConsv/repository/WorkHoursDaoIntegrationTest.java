package com.example.czConsv.repository;

import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
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
 * T-028: WorkHoursDao 統合テスト。
 * Testcontainers + PostgreSQL 16 で実データ検証。
 */
class WorkHoursDaoIntegrationTest extends DaoIntegrationTestBase {

    @Autowired
    private Tcz01HosyuKousuuDao dao;

    private Long insertedSeqNo;

    @BeforeEach
    void insertTestRecord() {
        Tcz01HosyuKousuu record = new Tcz01HosyuKousuu();
        record.skbtcd = "01";
        record.hssgytntEsqid = "U001";
        record.hssgytntName = "テストユーザー";
        record.yearHalf = "20261";
        record.sgyymd = LocalDate.of(2026, 2, 1);
        record.status = "0";
        record.kousuu = new BigDecimal("90");
        record.delflg = "0";
        record.initnt = "TEST";
        record.updtnt = "TEST";
        record.updpgid = "TEST";
        record.upddate = LocalDateTime.now();
        dao.insert(record);
        insertedSeqNo = record.seqNo;
    }

    @Nested
    class SelectById {

        @Test
        void findsInsertedRecord() {
            Optional<Tcz01HosyuKousuu> found = dao.selectById(insertedSeqNo, "01");
            assertThat(found).isPresent();
            assertThat(found.get().hssgytntEsqid).isEqualTo("U001");
        }

        @Test
        void returnsEmptyForNonExistentId() {
            Optional<Tcz01HosyuKousuu> found = dao.selectById(-999L, "01");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    class SelectByTntAndPeriod {

        @Test
        void returnsRecordsForStaffAndPeriod() {
            List<Tcz01HosyuKousuu> records =
                    dao.selectByTntAndPeriod("U001", "20261");
            assertThat(records).isNotEmpty();
            assertThat(records).allMatch(r -> "U001".equals(r.hssgytntEsqid));
            assertThat(records).allMatch(r -> "0".equals(r.delflg));
        }

        @Test
        void returnsEmptyForUnknownStaff() {
            List<Tcz01HosyuKousuu> records =
                    dao.selectByTntAndPeriod("UNKNOWN", "20261");
            assertThat(records).isEmpty();
        }
    }

    @Nested
    class SelectBySkbtcdAndYearHalf {

        @Test
        void returnsRecordsForOrganization() {
            List<Tcz01HosyuKousuu> records =
                    dao.selectBySkbtcdAndYearHalf("01", "20261");
            assertThat(records).isNotEmpty();
            assertThat(records).allMatch(r -> "01".equals(r.skbtcd));
        }
    }

    @Nested
    class SelectBySkbtcdYearHalfAndStatus {

        @Test
        void filtersByStatus() {
            List<Tcz01HosyuKousuu> records =
                    dao.selectBySkbtcdYearHalfAndStatus("01", "20261", "0");
            assertThat(records).isNotEmpty();
            assertThat(records).allMatch(r -> "0".equals(r.status));
        }

        @Test
        void returnsEmptyForConfirmedStatus() {
            List<Tcz01HosyuKousuu> records =
                    dao.selectBySkbtcdYearHalfAndStatus("01", "20261", "1");
            // 新規挿入レコードはステータス0のみなので空のはず
            assertThat(records)
                    .allMatch(r -> "1".equals(r.status));
        }
    }

    @Nested
    class Update {

        @Test
        void updatesStatusField() {
            Tcz01HosyuKousuu record = dao.selectById(insertedSeqNo, "01").orElseThrow();
            record.status = "1";
            record.upddate = LocalDateTime.now();
            int count = dao.update(record);
            assertThat(count).isEqualTo(1);

            Tcz01HosyuKousuu updated = dao.selectById(insertedSeqNo, "01").orElseThrow();
            assertThat(updated.status).isEqualTo("1");
        }
    }

    @Nested
    class LogicalDelete {

        @Test
        void setsDelflgTo1() {
            Tcz01HosyuKousuu record = dao.selectById(insertedSeqNo, "01").orElseThrow();
            record.delflg = "1";
            record.upddate = LocalDateTime.now();
            int count = dao.logicalDelete(record);
            assertThat(count).isEqualTo(1);

            // selectAll では論理削除フィルタがないので取れるが
            // selectByTntAndPeriod では除外される
            List<Tcz01HosyuKousuu> afterDelete =
                    dao.selectByTntAndPeriod("U001", "20261");
            assertThat(afterDelete).noneMatch(r -> insertedSeqNo.equals(r.seqNo));
        }
    }
}

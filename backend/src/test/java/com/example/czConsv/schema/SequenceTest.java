package com.example.czConsv.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-014: BIGSERIAL 連番生成テスト
 * tcz01_hosyu_kousuu.seqno が BIGSERIAL により自動採番されることを検証。
 */
class SequenceTest extends PostgresTestBase {

    @Test
    @DisplayName("BIGSERIAL により seqno が自動採番・単調増加する")
    void autoIncrement() throws Exception {
        List<Long> seqnos = new ArrayList<>();
        try (Connection conn = getConnection()) {
            for (int i = 0; i < 5; i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd) " +
                        "VALUES ('01', 'ESEQ01', '20262', '2026-02-01') RETURNING seqno")) {
                    ResultSet rs = ps.executeQuery();
                    assertTrue(rs.next());
                    seqnos.add(rs.getLong(1));
                }
            }
        }

        assertEquals(5, seqnos.size());
        for (int i = 1; i < seqnos.size(); i++) {
            assertTrue(seqnos.get(i) > seqnos.get(i - 1),
                    "seqno は単調増加であるべき: " + seqnos);
        }
        assertEquals(seqnos.size(), seqnos.stream().distinct().count(),
                "seqno に重複がある: " + seqnos);
    }

    @Test
    @DisplayName("seqno は BIGINT 型である")
    void seqnoIsBigint() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT data_type FROM information_schema.columns " +
                     "WHERE table_name = 'tcz01_hosyu_kousuu' AND column_name = 'seqno'")) {
            assertTrue(rs.next());
            assertEquals("bigint", rs.getString("data_type"));
        }
    }

    @Test
    @DisplayName("setval で SEQUENCE 初期値を設定できる")
    void setvalWorks() throws Exception {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT pg_get_serial_sequence('tcz01_hosyu_kousuu', 'seqno')");
                assertTrue(rs.next());
                String seqName = rs.getString(1);
                assertNotNull(seqName, "SEQUENCE が関連付けられていない");

                stmt.execute("SELECT setval('" + seqName + "', 90000)");

                rs = stmt.executeQuery(
                        "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd) " +
                        "VALUES ('01', 'ESEQ02', '20262', '2026-02-01') RETURNING seqno");
                assertTrue(rs.next());
                assertEquals(90001, rs.getLong(1), "setval 後の次の値は 90001 であるべき");
            } finally {
                conn.rollback();
                // シーケンスはトランザクション外なので明示的にリセット
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT setval(pg_get_serial_sequence('tcz01_hosyu_kousuu', 'seqno'), " +
                            "(SELECT COALESCE(MAX(seqno), 0) + 1 FROM tcz01_hosyu_kousuu), false)");
                }
                conn.setAutoCommit(true);
            }
        }
    }

    @Test
    @DisplayName("batch_execution_log.id も BIGSERIAL で自動採番される")
    void batchLogAutoIncrement() throws Exception {
        List<Long> ids = new ArrayList<>();
        try (Connection conn = getConnection()) {
            for (int i = 0; i < 3; i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO batch_execution_log (batch_name, started_at, status) " +
                        "VALUES ('seq-test', CURRENT_TIMESTAMP, 'RUNNING') RETURNING id")) {
                    ResultSet rs = ps.executeQuery();
                    assertTrue(rs.next());
                    ids.add(rs.getLong(1));
                }
            }
        }

        assertEquals(3, ids.size());
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i) > ids.get(i - 1),
                    "batch_execution_log.id は単調増加であるべき: " + ids);
        }
    }
}

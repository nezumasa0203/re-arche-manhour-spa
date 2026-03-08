package com.example.czConsv.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-011: CHECK 制約テスト
 * 全 CHECK 制約の正常値・異常値テスト。
 */
class CheckConstraintTest extends PostgresTestBase {

    // --- tcz01_hosyu_kousuu.status ---

    @ParameterizedTest
    @DisplayName("tcz01 status: 正常値")
    @ValueSource(strings = {"0", "1", "2", "9"})
    void tcz01StatusValid(String status) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, status) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, status);
            assertDoesNotThrow(() -> ps.executeUpdate());
        }
    }

    @ParameterizedTest
    @DisplayName("tcz01 status: 異常値は拒否")
    @ValueSource(strings = {"3", "5", "A", ""})
    void tcz01StatusInvalid(String status) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, status) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, status);
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    // --- tcz01_hosyu_kousuu.skbtcd ---

    @ParameterizedTest
    @DisplayName("tcz01 skbtcd: 正常値")
    @ValueSource(strings = {"01", "02"})
    void tcz01SkbtcdValid(String skbtcd) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd) " +
                     "VALUES (?, 'E00001', '20262', '2026-02-01')")) {
            ps.setString(1, skbtcd);
            assertDoesNotThrow(() -> ps.executeUpdate());
        }
    }

    @ParameterizedTest
    @DisplayName("tcz01 skbtcd: 異常値は拒否")
    @ValueSource(strings = {"03", "00", "1", "AB"})
    void tcz01SkbtcdInvalid(String skbtcd) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd) " +
                     "VALUES (?, 'E00001', '20262', '2026-02-01')")) {
            ps.setString(1, skbtcd);
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    // --- tcz01_hosyu_kousuu.delflg ---

    @ParameterizedTest
    @DisplayName("tcz01 delflg: 正常値")
    @ValueSource(strings = {"0", "1"})
    void tcz01DelflgValid(String delflg) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, delflg) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, delflg);
            assertDoesNotThrow(() -> ps.executeUpdate());
        }
    }

    @ParameterizedTest
    @DisplayName("tcz01 delflg: 異常値は拒否")
    @ValueSource(strings = {"2", "9", "X"})
    void tcz01DelflgInvalid(String delflg) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, delflg) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, delflg);
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    // --- tcz01_hosyu_kousuu.kenmei バイト長チェック ---

    @Test
    @DisplayName("tcz01 kenmei: 128バイト以内は許可（ASCII 128文字）")
    void tcz01KenmeiValidAscii() throws Exception {
        String kenmei = "a".repeat(128);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, kenmei) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, kenmei);
            assertDoesNotThrow(() -> ps.executeUpdate());
        }
    }

    @Test
    @DisplayName("tcz01 kenmei: 日本語42文字(126バイト)は許可")
    void tcz01KenmeiValidJapanese() throws Exception {
        String kenmei = "あ".repeat(42);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, kenmei) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, kenmei);
            assertDoesNotThrow(() -> ps.executeUpdate());
        }
    }

    @Test
    @DisplayName("tcz01 kenmei: 129バイト以上は拒否")
    void tcz01KenmeiTooLongAscii() throws Exception {
        String kenmei = "a".repeat(129);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, kenmei) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, kenmei);
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    @Test
    @DisplayName("tcz01 kenmei: 日本語43文字(129バイト)は拒否")
    void tcz01KenmeiTooLongJapanese() throws Exception {
        String kenmei = "あ".repeat(43);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, year_half, sgyymd, kenmei) " +
                     "VALUES ('01', 'E00001', '20262', '2026-02-01', ?)")) {
            ps.setString(1, kenmei);
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    // --- mcz04_ctrl フラグ制約 ---

    @Test
    @DisplayName("mcz04 online_flg: 異常値は拒否")
    void mcz04OnlineFlgInvalid() throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO mcz04_ctrl (sysid, yyyymm, online_flg) VALUES ('99', '209901', '2')")) {
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    @Test
    @DisplayName("mcz04 gjkt_flg: 異常値は拒否")
    void mcz04GjktFlgInvalid() throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO mcz04_ctrl (sysid, yyyymm, gjkt_flg) VALUES ('99', '209902', '9')")) {
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    @Test
    @DisplayName("mcz04 data_sk_flg: 異常値は拒否")
    void mcz04DataSkFlgInvalid() throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO mcz04_ctrl (sysid, yyyymm, data_sk_flg) VALUES ('99', '209903', 'X')")) {
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    // --- batch_execution_log.status ---

    @ParameterizedTest
    @DisplayName("batch_execution_log status: 正常値")
    @ValueSource(strings = {"RUNNING", "SUCCESS", "FAILED"})
    void batchStatusValid(String status) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO batch_execution_log (batch_name, started_at, status) " +
                     "VALUES ('test', CURRENT_TIMESTAMP, ?)")) {
            ps.setString(1, status);
            assertDoesNotThrow(() -> ps.executeUpdate());
        }
    }

    @ParameterizedTest
    @DisplayName("batch_execution_log status: 異常値は拒否")
    @ValueSource(strings = {"UNKNOWN", "PENDING", "ERROR", ""})
    void batchStatusInvalid(String status) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO batch_execution_log (batch_name, started_at, status) " +
                     "VALUES ('test', CURRENT_TIMESTAMP, ?)")) {
            ps.setString(1, status);
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }
}

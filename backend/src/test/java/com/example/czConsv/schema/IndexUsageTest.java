package com.example.czConsv.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-012: インデックス利用テスト
 * インデックスの存在・構造・カラム構成を検証する。
 */
class IndexUsageTest extends PostgresTestBase {

    @Test
    @DisplayName("idx_tcz01_tnt_yyyymm が存在する")
    void indexTntYyyymmExists() throws Exception {
        assertIndexExists("idx_tcz01_tnt_yyyymm");
    }

    @Test
    @DisplayName("idx_tcz01_sgyymd が存在する")
    void indexSgyymdExists() throws Exception {
        assertIndexExists("idx_tcz01_sgyymd");
    }

    @Test
    @DisplayName("idx_tcz01_taisyo が存在する")
    void indexTaisyoExists() throws Exception {
        assertIndexExists("idx_tcz01_taisyo");
    }

    @Test
    @DisplayName("idx_tcz01_status が存在する")
    void indexStatusExists() throws Exception {
        assertIndexExists("idx_tcz01_status");
    }

    @Test
    @DisplayName("idx_tcz13_yyyymm が存在する")
    void indexTcz13YyyymmExists() throws Exception {
        assertIndexExists("idx_tcz13_yyyymm");
    }

    @Test
    @DisplayName("idx_mav03_search が存在する")
    void indexMav03SearchExists() throws Exception {
        assertIndexExists("idx_mav03_search");
    }

    @Test
    @DisplayName("idx_tcz01_tnt_yyyymm は (hssgytnt_esqid, year_half) の複合インデックス")
    void indexTntYyyymmColumns() throws Exception {
        String def = getIndexDef("idx_tcz01_tnt_yyyymm");
        assertTrue(def.contains("hssgytnt_esqid"), "hssgytnt_esqid が含まれるべき");
        assertTrue(def.contains("year_half"), "year_half が含まれるべき");
    }

    @Test
    @DisplayName("idx_tcz01_status は (status, delflg) の複合インデックス")
    void indexStatusColumns() throws Exception {
        String def = getIndexDef("idx_tcz01_status");
        assertTrue(def.contains("status"), "status が含まれるべき");
        assertTrue(def.contains("delflg"), "delflg が含まれるべき");
    }

    @Test
    @DisplayName("idx_tcz01_taisyo は (taisyo_sknno, taisyo_subsysno) の複合インデックス")
    void indexTaisyoColumns() throws Exception {
        String def = getIndexDef("idx_tcz01_taisyo");
        assertTrue(def.contains("taisyo_sknno"), "taisyo_sknno が含まれるべき");
        assertTrue(def.contains("taisyo_subsysno"), "taisyo_subsysno が含まれるべき");
    }

    @Test
    @DisplayName("idx_tcz13_yyyymm は (yyyymm, skbtcd) の複合インデックス")
    void indexTcz13Columns() throws Exception {
        String def = getIndexDef("idx_tcz13_yyyymm");
        assertTrue(def.contains("yyyymm"), "yyyymm が含まれるべき");
        assertTrue(def.contains("skbtcd"), "skbtcd が含まれるべき");
    }

    @Test
    @DisplayName("idx_mav03_search は (skbtcd, sknno) の複合インデックス")
    void indexMav03Columns() throws Exception {
        String def = getIndexDef("idx_mav03_search");
        assertTrue(def.contains("skbtcd"), "skbtcd が含まれるべき");
        assertTrue(def.contains("sknno"), "sknno が含まれるべき");
    }

    @Test
    @DisplayName("EXPLAIN で担当者x期間検索のプランが取得できる")
    void explainTntSearch() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "EXPLAIN SELECT * FROM tcz01_hosyu_kousuu " +
                     "WHERE hssgytnt_esqid = 'E00001' AND year_half = '20262' AND delflg = '0'")) {
            StringBuilder plan = new StringBuilder();
            while (rs.next()) {
                plan.append(rs.getString(1)).append("\n");
            }
            assertFalse(plan.toString().isEmpty(), "EXPLAIN プランが空");
        }
    }

    private void assertIndexExists(String indexName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?")) {
            ps.setString(1, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "インデックス " + indexName + " が存在しない");
            }
        }
    }

    private String getIndexDef(String indexName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT indexdef FROM pg_indexes WHERE indexname = ?")) {
            ps.setString(1, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString("indexdef");
            }
        }
    }
}

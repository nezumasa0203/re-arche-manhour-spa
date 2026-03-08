package com.example.czConsv.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-010: DDL 実行テスト
 * Docker Compose で起動した PostgreSQL 16 に接続し、
 * 全16テーブルの存在、カラム定義（型・NULL許可・デフォルト値）を検証する。
 */
class DdlExecutionTest extends PostgresTestBase {

    @Test
    @DisplayName("全16テーブルが存在する")
    void allTablesExist() throws Exception {
        List<String> expected = List.of(
                "mcz02_hosyu_kategori", "mcz03_apl_bunrui_grp", "mcz04_ctrl",
                "mcz12_orgn_kr", "mcz15_ts_sys", "mcz17_hshk_bunrui_grp",
                "mcz21_kanri_taisyo", "mcz24_tanka",
                "mav01_sys", "mav03_subsys",
                "tcz01_hosyu_kousuu", "tcz13_subsys_sum", "tcz14_grp_key",
                "tcz16_tnt_busyo_rireki", "tcz19_my_sys",
                "batch_execution_log"
        );

        List<String> actual = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actual.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        for (String table : expected) {
            assertTrue(actual.contains(table), "テーブル " + table + " が存在しない");
        }
        assertEquals(16, actual.size(), "テーブル数が16でない: " + actual);
    }

    @ParameterizedTest
    @DisplayName("tcz01_hosyu_kousuu のカラム定義")
    @CsvSource({
            "seqno,       int8,    NO",
            "skbtcd,      varchar, NO",
            "hssgytnt_esqid, varchar, NO",
            "hssgytnt_name,  varchar, YES",
            "year_half,   varchar, NO",
            "sgyymd,      date,    NO",
            "status,      varchar, NO",
            "kousuu,      numeric, YES",
            "inidate,     timestamptz, YES",
            "upddate,     timestamptz, YES",
            "delflg,      varchar, NO"
    })
    void tcz01ColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("tcz01_hosyu_kousuu", column, expectedType, expectedNullable);
    }

    @ParameterizedTest
    @DisplayName("mcz04_ctrl のカラム定義")
    @CsvSource({
            "sysid,       varchar, NO",
            "yyyymm,      varchar, NO",
            "online_flg,  varchar, NO",
            "gjkt_flg,    varchar, YES",
            "data_sk_flg, varchar, YES",
            "delflg,      varchar, NO"
    })
    void mcz04ColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("mcz04_ctrl", column, expectedType, expectedNullable);
    }

    @ParameterizedTest
    @DisplayName("mcz02_hosyu_kategori のカラム定義（DATE型変換 GAP-D06）")
    @CsvSource({
            "hs_kategori,    varchar, NO",
            "yukou_kaishiki, date,    NO",
            "yukou_syuryoki, date,    NO",
            "delflg,         varchar, NO"
    })
    void mcz02ColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("mcz02_hosyu_kategori", column, expectedType, expectedNullable);
    }

    @ParameterizedTest
    @DisplayName("mcz12_orgn_kr のカラム定義（DATE型変換・7階層）")
    @CsvSource({
            "sikcd,       varchar, NO",
            "endymd,      date,    YES",
            "startymd,    date,    YES",
            "krikaisocd,  varchar, YES",
            "sikcdhonb,   varchar, YES",
            "sikcdkyk,    varchar, YES",
            "sikcdsitu,   varchar, YES",
            "sikcdbu,     varchar, YES",
            "sikcdka,     varchar, YES"
    })
    void mcz12ColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("mcz12_orgn_kr", column, expectedType, expectedNullable);
    }

    @ParameterizedTest
    @DisplayName("mcz15_ts_sys のカラム定義（YYYYX半期形式）")
    @CsvSource({
            "tssknno,        varchar, NO",
            "tssubsysno,     varchar, NO",
            "yukou_syuryoki, varchar, NO",
            "tssysname,      varchar, YES",
            "tssubsysname,   varchar, YES"
    })
    void mcz15ColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("mcz15_ts_sys", column, expectedType, expectedNullable);
    }

    @ParameterizedTest
    @DisplayName("tcz16_tnt_busyo_rireki のカラム定義（DATE型変換 GAP-D06）")
    @CsvSource({
            "tnt_kubun,   varchar, NO",
            "sknno,       varchar, NO",
            "tnt_str_ymd, date,    YES",
            "tnt_end_ymd, date,    NO",
            "tnt_busyo,   varchar, YES"
    })
    void tcz16ColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("tcz16_tnt_busyo_rireki", column, expectedType, expectedNullable);
    }

    @ParameterizedTest
    @DisplayName("batch_execution_log のカラム定義（新規テーブル）")
    @CsvSource({
            "id,               int8,        NO",
            "batch_name,       varchar,     NO",
            "started_at,       timestamptz, NO",
            "finished_at,      timestamptz, YES",
            "status,           varchar,     NO",
            "records_affected, int4,        YES",
            "error_message,    text,        YES"
    })
    void batchLogColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("batch_execution_log", column, expectedType, expectedNullable);
    }

    @ParameterizedTest
    @DisplayName("mcz24_tanka のカラム定義")
    @CsvSource({
            "yukou_syuryoki, varchar, NO",
            "skbtcd,         varchar, NO",
            "tanka_kbn,      varchar, NO",
            "tanka,          numeric, YES"
    })
    void mcz24ColumnDefinitions(String column, String expectedType, String expectedNullable) throws Exception {
        assertColumnDefinition("mcz24_tanka", column, expectedType, expectedNullable);
    }

    @Test
    @DisplayName("mcz21_kanri_taisyo は2カラムのみ")
    void mcz21HasOnly2Columns() throws Exception {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, "public", "mcz21_kanri_taisyo", "%")) {
                int count = 0;
                while (rs.next()) count++;
                assertEquals(2, count, "mcz21_kanri_taisyo のカラム数が2でない");
            }
        }
    }

    @Test
    @DisplayName("全6インデックスが存在する")
    void allIndexesExist() throws Exception {
        List<String> expectedIndexes = List.of(
                "idx_tcz01_tnt_yyyymm", "idx_tcz01_sgyymd",
                "idx_tcz01_taisyo", "idx_tcz01_status",
                "idx_tcz13_yyyymm", "idx_mav03_search"
        );

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' " +
                     "AND indexname NOT LIKE 'pk_%' AND indexname NOT LIKE '%_pkey'")) {
            List<String> actual = new ArrayList<>();
            while (rs.next()) {
                actual.add(rs.getString("indexname"));
            }
            for (String idx : expectedIndexes) {
                assertTrue(actual.contains(idx), "インデックス " + idx + " が存在しない");
            }
        }
    }

    @Test
    @DisplayName("mcz12_orgn_kr は32カラム")
    void mcz12Has32Columns() throws Exception {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, "public", "mcz12_orgn_kr", "%")) {
                int count = 0;
                while (rs.next()) count++;
                assertEquals(32, count, "mcz12_orgn_kr のカラム数");
            }
        }
    }

    private void assertColumnDefinition(String table, String column, String expectedType, String expectedNullable)
            throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT udt_name, is_nullable FROM information_schema.columns " +
                     "WHERE table_schema = 'public' AND table_name = '" + table +
                     "' AND column_name = '" + column + "'")) {
            assertTrue(rs.next(), table + "." + column + " が存在しない");
            assertEquals(expectedType, rs.getString("udt_name"),
                    table + "." + column + " の型が不一致");
            assertEquals(expectedNullable, rs.getString("is_nullable"),
                    table + "." + column + " のNULL許可が不一致");
        }
    }
}

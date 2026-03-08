package com.example.czConsv.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-013: UTF-8 エンコーディングテスト
 * 日本語データの INSERT/SELECT テスト。
 * Windows-31J 固有文字が UTF-8 で正常に格納・取得できることを検証（GAP-N08）。
 */
class EncodingTest extends PostgresTestBase {

    @Test
    @DisplayName("全角ひらがな・カタカナ・漢字が正常に格納・取得できる")
    void basicJapaneseCharacters() throws Exception {
        insertAndVerify("山田太郎", "テスト障害対応");
    }

    @Test
    @DisplayName("Windows-31J 固有文字: 丸囲み数字 ①②③")
    void circledNumbers() throws Exception {
        insertAndVerify("担当者①", "作業①②③対応");
    }

    @Test
    @DisplayName("Windows-31J 固有文字: 旧字体 髙﨑")
    void oldKanji() throws Exception {
        insertAndVerify("髙﨑太郎", "髙﨑案件対応");
    }

    @Test
    @DisplayName("Windows-31J 固有文字: ローマ数字 ⅠⅡⅢ")
    void romanNumerals() throws Exception {
        insertAndVerify("担当Ⅰ", "フェーズⅠⅡⅢ");
    }

    @Test
    @DisplayName("全角英数字と記号の混在")
    void fullWidthAlphanumeric() throws Exception {
        insertAndVerify("Ｔ太郎", "Ａ社システム１号機＃２");
    }

    @Test
    @DisplayName("組織構造マスタに日本語階層名を格納・取得")
    void organizationHierarchyJapanese() throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO mcz12_orgn_kr (sikcd, hyojikn, hyojikj, hyojiryaku) " +
                     "VALUES (?, ?, ?, ?) ON CONFLICT (sikcd) DO UPDATE SET hyojikn = EXCLUDED.hyojikn")) {
            ps.setString(1, "9999999");
            ps.setString(2, "ジョウホウシステムホンブ");
            ps.setString(3, "情報システム本部");
            ps.setString(4, "情シス本部");
            ps.executeUpdate();
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT hyojikn, hyojikj, hyojiryaku FROM mcz12_orgn_kr WHERE sikcd = '9999999'")) {
            assertTrue(rs.next());
            assertEquals("ジョウホウシステムホンブ", rs.getString("hyojikn"));
            assertEquals("情報システム本部", rs.getString("hyojikj"));
            assertEquals("情シス本部", rs.getString("hyojiryaku"));
        }
    }

    private void insertAndVerify(String name, String kenmei) throws Exception {
        long seqno;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, hssgytnt_name, " +
                     "year_half, sgyymd, kenmei) VALUES ('01', 'EENC01', ?, '20262', '2026-02-01', ?) " +
                     "RETURNING seqno")) {
            ps.setString(1, name);
            ps.setString(2, kenmei);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            seqno = rs.getLong(1);
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT hssgytnt_name, kenmei FROM tcz01_hosyu_kousuu WHERE seqno = ? AND skbtcd = '01'")) {
            ps.setLong(1, seqno);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(name, rs.getString("hssgytnt_name"));
                assertEquals(kenmei, rs.getString("kenmei"));
            }
        }
    }
}

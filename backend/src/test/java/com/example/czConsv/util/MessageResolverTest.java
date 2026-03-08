package com.example.czConsv.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageResolver のユニットテスト。
 */
class MessageResolverTest {

    // ─── YAML ロードテスト ──────────────────────────────────

    @Test
    void loadMessages_loadsFromClasspath() {
        MessageResolver resolver = new MessageResolver();
        assertTrue(resolver.size() > 0, "messages.yml から読み込めること");
    }

    @Test
    void loadMessages_contains75Messages() {
        MessageResolver resolver = new MessageResolver();
        assertEquals(75, resolver.size());
    }

    // ─── 成功メッセージ (CZ-000〜099) ──────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"CZ-000", "CZ-001", "CZ-002", "CZ-003"})
    void successMessages_exist(String code) {
        MessageResolver resolver = new MessageResolver();
        assertTrue(resolver.hasMessage(code));
    }

    @Test
    void CZ000_returns_成功完了() {
        MessageResolver resolver = new MessageResolver();
        assertEquals("成功完了", resolver.resolve("CZ-000"));
    }

    // ─── パラメータ展開テスト ──────────────────────────────

    @Test
    void resolve_singleParam_expandsPlaceholder() {
        MessageResolver resolver = new MessageResolver();
        String result = resolver.resolve("CZ-126", List.of("作業日"));
        assertEquals("作業日は必須入力です", result);
    }

    @Test
    void resolve_differentParam_expandsCorrectly() {
        MessageResolver resolver = new MessageResolver();
        String result = resolver.resolve("CZ-126", List.of("件名"));
        assertEquals("件名は必須入力です", result);
    }

    @Test
    void resolve_CZ125_withParam() {
        MessageResolver resolver = new MessageResolver();
        String result = resolver.resolve("CZ-125", List.of("工数"));
        assertEquals("工数はHH:MM形式で入力してください", result);
    }

    @Test
    void resolve_CZ141_forbiddenWord() {
        MessageResolver resolver = new MessageResolver();
        String result = resolver.resolve("CZ-141", List.of("カ層"));
        assertEquals("カ層は業務概要としても定義されているため記述できません", result);
    }

    @Test
    void resolve_CZ328_statusChange() {
        MessageResolver resolver = new MessageResolver();
        String result = resolver.resolve("CZ-328", List.of("承認"));
        assertEquals("ステータスを承認に変更できませんでした", result);
    }

    @Test
    void resolve_noParam_returnsTemplate() {
        MessageResolver resolver = new MessageResolver();
        assertEquals("工数は最小0:15以上で入力してください", resolver.resolve("CZ-129"));
    }

    @Test
    void resolve_nullParams_returnsTemplate() {
        MessageResolver resolver = new MessageResolver();
        assertEquals("工数は最小0:15以上で入力してください", resolver.resolve("CZ-129", null));
    }

    @Test
    void resolve_emptyParams_returnsTemplate() {
        MessageResolver resolver = new MessageResolver();
        String result = resolver.resolve("CZ-126", List.of());
        assertEquals("{0}は必須入力です", result);
    }

    // ─── 未定義コード ──────────────────────────────────────

    @Test
    void resolve_unknownCode_returnsFallback() {
        MessageResolver resolver = new MessageResolver();
        assertEquals("不明なエラーが発生しました (CZ-999)", resolver.resolve("CZ-999"));
    }

    @Test
    void hasMessage_unknownCode_returnsFalse() {
        MessageResolver resolver = new MessageResolver();
        assertFalse(resolver.hasMessage("CZ-999"));
    }

    // ─── テスト用コンストラクタ ─────────────────────────────

    @Test
    void testConstructor_usesProvidedMap() {
        Map<String, String> custom = Map.of("TEST-001", "テスト{0}メッセージ");
        MessageResolver resolver = new MessageResolver(custom);

        assertEquals("テスト値メッセージ", resolver.resolve("TEST-001", List.of("値")));
        assertEquals(1, resolver.size());
    }

    // ─── 主要メッセージ存在確認 ─────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "CZ-102", "CZ-126", "CZ-129", "CZ-138", "CZ-144", "CZ-146", "CZ-147",
            "CZ-300", "CZ-500", "CZ-800"
    })
    void keyMessages_exist(String code) {
        MessageResolver resolver = new MessageResolver();
        assertTrue(resolver.hasMessage(code), code + " should exist");
    }
}

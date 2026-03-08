package com.example.czConsv.util;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * CZ メッセージリゾルバ。
 *
 * <p>{@code classpath:messages.yml} の {@code cz.messages} から読み込んだ
 * メッセージテンプレートをコードで引き、{0}, {1}, ... をパラメータで展開する。
 */
@Component
public class MessageResolver {

    private final Map<String, String> messages;

    public MessageResolver() {
        this.messages = loadMessages();
    }

    /**
     * テスト用コンストラクタ。
     */
    MessageResolver(Map<String, String> messages) {
        this.messages = messages;
    }

    /**
     * メッセージコードからメッセージ文字列を解決する。
     *
     * @param code   CZ エラーコード (e.g., "CZ-126")
     * @param params パラメータ (nullable)
     * @return 展開済みメッセージ文字列
     */
    public String resolve(String code, List<String> params) {
        String template = messages.get(code);
        if (template == null) {
            return "不明なエラーが発生しました (" + code + ")";
        }
        if (params == null || params.isEmpty()) {
            return template;
        }
        String result = template;
        for (int i = 0; i < params.size(); i++) {
            result = result.replace("{" + i + "}", params.get(i));
        }
        return result;
    }

    /**
     * パラメータなしでメッセージを解決する。
     *
     * @param code CZ エラーコード
     * @return メッセージ文字列
     */
    public String resolve(String code) {
        return resolve(code, null);
    }

    /**
     * 指定コードのテンプレートが存在するかチェックする。
     *
     * @param code CZ エラーコード
     * @return テンプレートが存在する場合 true
     */
    public boolean hasMessage(String code) {
        return messages.containsKey(code);
    }

    /**
     * 登録済みメッセージ件数を返す。
     */
    public int size() {
        return messages.size();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadMessages() {
        try (InputStream is = MessageResolver.class.getClassLoader()
                .getResourceAsStream("messages.yml")) {
            if (is == null) {
                throw new IllegalStateException("messages.yml not found on classpath");
            }
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            Map<String, Object> cz = (Map<String, Object>) root.get("cz");
            return (Map<String, String>) cz.get("messages");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

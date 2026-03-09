package com.example.czConsv.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ソートパラメータパーサー。
 *
 * <p>フロントエンドから受け取った {@code "column:direction"} 形式の sort パラメータを
 * SQL ORDER BY 句に変換する。ホワイトリスト方式で SQL インジェクションを防止する。
 *
 * <p>変換例:
 * <pre>
 *   "workDate:asc,status:desc"
 *   → " ORDER BY work_date ASC, status DESC"
 * </pre>
 *
 * <p>使用方法:
 * <pre>
 *   Set&lt;String&gt; allowed = Set.of("workDate", "status", "hours");
 *   String orderBy = SortParser.parse(sortParam, allowed);
 * </pre>
 */
public final class SortParser {

    private SortParser() {
    }

    /**
     * sort パラメータを ORDER BY 句に変換する。
     *
     * @param sortParam  "{@code column:direction}" 形式の文字列。複数指定はカンマ区切り。
     *                   null または空文字列の場合は空文字列を返す。
     * @param allowedColumns 許可されたカラム名のセット（camelCase）
     * @return ORDER BY 句（先頭にスペースを含む）。例: {@code " ORDER BY work_date ASC"}
     * @throws IllegalArgumentException ホワイトリスト外のカラムまたは無効な方向が指定された場合
     */
    public static String parse(String sortParam, Set<String> allowedColumns) {
        if (sortParam == null || sortParam.isBlank()) {
            return "";
        }

        String[] tokens = sortParam.split(",");
        List<String> clauses = new ArrayList<>(tokens.length);

        for (String token : tokens) {
            String trimmed = token.trim();
            String[] parts = trimmed.split(":", 2);
            String column = parts[0].trim();
            String direction = parts.length > 1 ? parts[1].trim().toUpperCase() : "ASC";

            // カラム名ホワイトリスト検証
            if (!allowedColumns.contains(column)) {
                throw new IllegalArgumentException(
                        "ソートカラム '%s' は許可されていません".formatted(column));
            }

            // 方向検証
            if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
                throw new IllegalArgumentException(
                        "ソート方向 '%s' は無効です（ASC または DESC を指定してください）".formatted(direction));
            }

            clauses.add(toSnakeCase(column) + " " + direction);
        }

        return " ORDER BY " + String.join(", ", clauses);
    }

    /**
     * camelCase 文字列を snake_case に変換する。
     *
     * <p>例: {@code "workDate"} → {@code "work_date"}
     *
     * @param camel camelCase 文字列
     * @return snake_case 文字列
     */
    public static String toSnakeCase(String camel) {
        StringBuilder sb = new StringBuilder(camel.length() + 4);
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}

package com.example.czConsv.service;

import java.util.List;

/**
 * バリデーションエラーを表すレコード。
 *
 * <p>CZ エラーコード体系（CZ-000〜CZ-999）に準拠したバリデーション結果を格納する。
 * ValidationService が返すエラー情報の単位として使用される。
 *
 * @param code    CZ エラーコード（例: CZ-126）
 * @param message エラーメッセージ
 * @param field   エラー対象フィールド名
 * @param params  メッセージ埋め込みパラメータ（nullable）
 */
public record ValidationError(
        String code,
        String message,
        String field,
        List<String> params
) {

    /**
     * パラメータなしの簡易コンストラクタ。
     */
    public ValidationError(String code, String message, String field) {
        this(code, message, field, null);
    }
}

package com.example.czConsv.dto.response;

import java.util.List;

/**
 * エラーレスポンス DTO。
 *
 * <p>CZ エラーコード体系（CZ-000〜CZ-999）に準拠。
 * <ul>
 *   <li>CZ-000〜099: 成功メッセージ</li>
 *   <li>CZ-100〜299: 警告メッセージ</li>
 *   <li>CZ-300〜499: システムエラー</li>
 *   <li>CZ-500〜799: 確認ダイアログ</li>
 *   <li>CZ-800〜999: 情報メッセージ</li>
 * </ul>
 *
 * @param code     CZ エラーコード（例: CZ-101）
 * @param message  エラーメッセージ
 * @param field    バリデーションエラー対象フィールド（nullable）
 * @param params   メッセージ埋め込みパラメータ（nullable）
 * @param recordId バッチ操作時のレコード ID（nullable）
 */
public record ErrorResponse(
        String code,
        String message,
        String field,
        List<String> params,
        Long recordId
) {

    /**
     * フィールド指定なし・パラメータなし・レコード ID なしの簡易コンストラクタ。
     */
    public ErrorResponse(String code, String message) {
        this(code, message, null, null, null);
    }

    /**
     * フィールド指定ありの簡易コンストラクタ。
     */
    public ErrorResponse(String code, String message, String field) {
        this(code, message, field, null, null);
    }
}

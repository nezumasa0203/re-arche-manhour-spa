package com.example.czConsv.dto.response;

import java.util.List;

/**
 * マスタ一覧汎用レスポンス DTO。
 *
 * <p>ページネーション付きのマスタデータ一覧に使用する。
 * 型パラメータを使用せず {@code List<Object>} とすることで
 * JSON シリアライズ時の柔軟性を確保する。
 */
public record MasterListResponse(
        List<Object> items,
        int totalCount,
        int page,
        int pageSize
) {
}

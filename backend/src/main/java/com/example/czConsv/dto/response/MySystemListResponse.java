package com.example.czConsv.dto.response;

import java.util.List;

/**
 * GET /my-systems レスポンス DTO。
 *
 * <p>マイシステム一覧を返す。
 */
public record MySystemListResponse(
        List<MySystemItem> systems
) {

    /**
     * マイシステム 1 件。
     */
    public record MySystemItem(
            String systemNo,
            String systemName,
            int subsystemCount
    ) {
    }
}

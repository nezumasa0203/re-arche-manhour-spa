package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /my-systems リクエスト DTO。
 *
 * <p>マイシステム登録用。
 */
public record MySystemCreateRequest(

        @NotBlank
        String systemNo
) {
}

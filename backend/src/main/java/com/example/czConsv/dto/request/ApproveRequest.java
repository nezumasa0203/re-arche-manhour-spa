package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * POST /work-status/approve リクエスト DTO。
 *
 * <p>選択した工数レコードを管理者承認する。
 */
public record ApproveRequest(

        @NotEmpty
        List<Long> ids
) {
}

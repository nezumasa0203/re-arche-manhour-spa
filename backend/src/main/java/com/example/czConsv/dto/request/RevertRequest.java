package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * POST /work-status/revert リクエスト DTO。
 *
 * <p>承認済みレコードを差戻す。
 */
public record RevertRequest(

        @NotEmpty
        List<Long> ids
) {
}

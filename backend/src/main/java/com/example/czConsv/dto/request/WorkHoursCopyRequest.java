package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * POST /work-hours/copy リクエスト DTO。
 *
 * <p>選択した工数レコードを複製する。
 */
public record WorkHoursCopyRequest(

        @NotEmpty
        List<Long> ids
) {
}

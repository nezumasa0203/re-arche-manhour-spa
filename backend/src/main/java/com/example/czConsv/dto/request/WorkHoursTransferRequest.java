package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * POST /work-hours/transfer-next-month リクエスト DTO。
 *
 * <p>選択した工数レコードを指定月へ繰越す。
 */
public record WorkHoursTransferRequest(

        @NotEmpty
        List<Long> ids,

        @NotEmpty
        List<@Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])",
                message = "targetMonths の各要素は YYYY-MM 形式で指定してください") String> targetMonths
) {
}

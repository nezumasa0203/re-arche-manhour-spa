package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST /work-hours リクエスト DTO。
 *
 * <p>下書きモード（ドラフト）対応のため、yearMonth 以外のフィールドは nullable。
 */
public record WorkHoursCreateRequest(

        @NotBlank
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])",
                message = "yearMonth は YYYY-MM 形式で指定してください")
        String yearMonth,

        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])",
                message = "workDate は YYYY-MM-DD 形式で指定してください")
        String workDate,

        String targetSubsystemNo,

        String causeSubsystemNo,

        String categoryCode,

        String subject,

        @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d",
                message = "hours は HH:mm 形式で指定してください")
        String hours,

        String tmrNo,

        String workRequestNo,

        String workRequesterName
) {
}

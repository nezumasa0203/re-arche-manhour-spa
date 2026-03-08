package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST /work-hours/batch-confirm リクエスト DTO。
 *
 * <p>指定月の全下書きレコードを一括確定する。
 */
public record BatchConfirmRequest(

        @NotBlank
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])",
                message = "yearMonth は YYYY-MM 形式で指定してください")
        String yearMonth
) {
}

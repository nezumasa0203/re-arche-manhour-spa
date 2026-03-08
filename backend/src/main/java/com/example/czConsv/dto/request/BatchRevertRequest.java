package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST /work-hours/batch-revert リクエスト DTO。
 *
 * <p>指定月の確定済みレコードを一括で下書きに戻す。
 */
public record BatchRevertRequest(

        @NotBlank
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])",
                message = "yearMonth は YYYY-MM 形式で指定してください")
        String yearMonth
) {
}

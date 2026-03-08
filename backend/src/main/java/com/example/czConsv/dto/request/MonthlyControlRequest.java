package com.example.czConsv.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 月次管理操作リクエスト DTO。
 *
 * <p>以下のエンドポイントで共用:
 * <ul>
 *   <li>POST /work-status/monthly-confirm — 月次確定</li>
 *   <li>POST /work-status/monthly-aggregate — 月次集計</li>
 *   <li>POST /work-status/monthly-unconfirm — 月次確定解除</li>
 * </ul>
 */
public record MonthlyControlRequest(

        @NotBlank
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])",
                message = "yearMonth は YYYY-MM 形式で指定してください")
        String yearMonth,

        @NotBlank
        String organizationCode
) {
}

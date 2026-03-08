package com.example.czConsv.dto.response;

/**
 * PATCH /work-hours/{id} レスポンス DTO。
 *
 * <p>セル単位編集の結果を返す。
 * summary にはサマリ更新値を含む。
 */
public record WorkHoursUpdateResponse(
        Long id,
        String field,
        String oldValue,
        String newValue,
        WorkHoursListResponse.WorkHoursSummary summary
) {
}

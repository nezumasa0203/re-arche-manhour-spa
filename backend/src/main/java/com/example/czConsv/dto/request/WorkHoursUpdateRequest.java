package com.example.czConsv.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * PATCH /work-hours/{id} リクエスト DTO。
 *
 * <p>セル単位のインライン編集を想定。
 * 楽観ロック用に updatedAt を必須とする。
 */
public record WorkHoursUpdateRequest(

        @NotBlank
        String field,

        @NotBlank
        String value,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
}

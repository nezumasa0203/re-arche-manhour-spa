package com.example.czConsv.dto.request;

/**
 * POST /delegation/switch リクエスト DTO。
 *
 * <p>代行モードの切替に使用する。
 * targetStaffId が null の場合は代行解除を意味する。
 */
public record DelegationSwitchRequest(

        String targetStaffId
) {
}

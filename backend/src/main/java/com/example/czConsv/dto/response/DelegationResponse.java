package com.example.czConsv.dto.response;

/**
 * POST /delegation/switch レスポンス DTO。
 *
 * <p>代行モード切替結果を返す。
 * isDaiko が true の場合、delegationStaffId/Name に代行先の情報が入る。
 */
public record DelegationResponse(
        String delegationStaffId,
        String delegationStaffName,
        boolean isDaiko
) {

    /**
     * 代行解除用の簡易コンストラクタ。
     */
    public static DelegationResponse cancelled() {
        return new DelegationResponse(null, null, false);
    }
}

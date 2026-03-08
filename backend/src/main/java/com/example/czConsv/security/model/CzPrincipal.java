package com.example.czConsv.security.model;

import java.security.Principal;

/**
 * CZ システム認証済みユーザー情報。
 * JWT クレームから構築される。
 *
 * <p>{@code delegationStaffId} は代行モード時に設定される。
 * X-Delegation-Staff-Id ヘッダーが指定され、かつ代行権限の検証に
 * 合格した場合のみ非 null となる。通常リクエストでは null。
 */
public record CzPrincipal(
        String userId,
        String userName,
        String email,
        String organizationCode,
        String organizationName,
        CzPermissions permissions,
        String delegationStaffId
) implements Principal {

    /**
     * 代行モードなし（delegationStaffId = null）の簡易コンストラクタ。
     * 既存コードとの後方互換性を維持する。
     */
    public CzPrincipal(
            String userId, String userName, String email,
            String organizationCode, String organizationName,
            CzPermissions permissions) {
        this(userId, userName, email, organizationCode, organizationName,
                permissions, null);
    }

    @Override
    public String getName() {
        return userId;
    }

    /**
     * 代行モードが有効かどうかを返す。
     */
    public boolean isDelegationMode() {
        return delegationStaffId != null;
    }
}

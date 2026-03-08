package com.example.czConsv.security.model;

import java.util.Collections;
import java.util.Map;

/**
 * Layer 2: TAB ビットベース機能権限。
 * 移行元: SecurityRoleInfo.isAvailableFunction(category, funcCode)
 *
 * ロールデータ文字列の各位置が '1' であるかを判定する。
 * 例: TAB 010 の roledata "110000" → bit0=true, bit1=true, bit2=false
 */
public record TabPermission(Map<String, Boolean> bits) {

    public static final TabPermission EMPTY = new TabPermission(Collections.emptyMap());

    public boolean hasBit(int index) {
        return bits.getOrDefault("bit" + index, false);
    }

    /**
     * TAB 010 bit0: 報告書作成担当グループ
     * 移行元: canUseSbt010_0bit()
     */
    public boolean bit0() {
        return hasBit(0);
    }

    /**
     * TAB 010 bit1: 報告書管理担当グループ
     * 移行元: canUseSbt010_1bit()
     */
    public boolean bit1() {
        return hasBit(1);
    }

    /**
     * TAB 010 bit2: 全管理グループ
     * 移行元: canUseSbt010_2bit()
     */
    public boolean bit2() {
        return hasBit(2);
    }
}

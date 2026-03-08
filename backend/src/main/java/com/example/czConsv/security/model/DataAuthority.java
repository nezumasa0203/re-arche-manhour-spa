package com.example.czConsv.security.model;

/**
 * Layer 3: データアクセス権限（相対権限モデル）。
 * 移行元: SecurityRoleInfo.getRelativeAuthority()
 *
 * @param ref 相対権限（参照）— ロールコード 201
 * @param ins 相対権限（登録）— ロールコード 202
 * @param upd 相対権限（更新）— ロールコード 211
 */
public record DataAuthority(String ref, String ins, String upd) {

    /** 組織階層レベル定数 */
    public static final String ZENSYA = "ZENSYA";
    public static final String EIGYOSHO = "EIGYOSHO";
    public static final String HONBU = "HONBU";
    public static final String KYOKU = "KYOKU";
    public static final String SHITSU = "SHITSU";
    public static final String BU = "BU";
    public static final String KA = "KA";

    public boolean canRef() {
        return ref != null;
    }

    public boolean canIns() {
        return ins != null;
    }

    public boolean canUpd() {
        return upd != null;
    }
}

package com.example.czConsv.security.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 12状態ステータスマトリクス解決。
 * 移行元: ApParameter.xml の sts_base_key + InsertListJspBean の系列切替
 *
 * ステータスキー (000-911) と TAB 010 bit2 の値から、
 * 各操作ボタンの表示状態 (1=有効, 0=無効, 9=非表示) を返す。
 */
public final class StatusMatrixResolver {

    /** 操作名定数 */
    public static final String OP_ADD = "add";
    public static final String OP_COPY = "copy";
    public static final String OP_DELETE = "delete";
    public static final String OP_UPDATE = "update";
    public static final String OP_VIEW = "view";
    public static final String OP_STATUS_UPDATE = "statusUpdate";
    public static final String OP_STATUS_VIEW = "statusView";

    /** 状態値: 有効 */
    public static final int ENABLED = 1;
    /** 状態値: 無効 */
    public static final int DISABLED = 0;
    /** 状態値: 非表示 */
    public static final int HIDDEN = 9;

    // ステータスキーのインデックス
    private static final String[] STATUS_KEYS = {
            "000", "010", "011", "100", "110", "111",
            "200", "210", "211", "900", "910", "911"
    };

    // 担当者系列 (tan) — canUseSbt010_2bit() = true
    // 行: add, copy, delete, update, view
    private static final int[][] TAN_MATRIX = {
            // 000 010 011 100 110 111 200 210 211 900 910 911
            {  9,  9,  9,  9,  9,  9,  9,  9,  9,  1,  0,  0 }, // add
            {  1,  0,  0,  1,  0,  0,  1,  0,  0,  9,  9,  9 }, // copy
            {  1,  0,  0,  1,  0,  0,  0,  0,  0,  9,  9,  9 }, // delete
            {  1,  0,  0,  1,  0,  0,  9,  9,  9,  9,  9,  9 }, // update
            {  1,  1,  1,  1,  1,  1,  1,  1,  1,  9,  9,  9 }, // view
    };

    // 管理者系列 (man) — canUseSbt010_2bit() = false
    // 行: add, copy, delete, update, view, statusUpdate, statusView
    private static final int[][] MAN_MATRIX = {
            // 000 010 011 100 110 111 200 210 211 900 910 911
            {  9,  9,  9,  9,  9,  9,  9,  9,  9,  1,  1,  1 }, // add
            {  1,  1,  1,  1,  1,  1,  1,  1,  1,  9,  9,  9 }, // copy
            {  1,  1,  1,  1,  1,  1,  0,  0,  0,  9,  9,  9 }, // delete
            {  1,  1,  1,  1,  1,  1,  1,  1,  1,  9,  9,  9 }, // update
            {  1,  1,  1,  1,  1,  1,  1,  1,  1,  9,  9,  9 }, // view
            {  0,  0,  0,  1,  1,  1,  1,  1,  1,  9,  9,  9 }, // statusUpdate
            {  1,  1,  1,  1,  1,  1,  1,  1,  1,  9,  9,  9 }, // statusView
    };

    private static final String[] TAN_OPS = { OP_ADD, OP_COPY, OP_DELETE, OP_UPDATE, OP_VIEW };
    private static final String[] MAN_OPS = { OP_ADD, OP_COPY, OP_DELETE, OP_UPDATE, OP_VIEW,
            OP_STATUS_UPDATE, OP_STATUS_VIEW };

    private StatusMatrixResolver() {
    }

    /**
     * 指定ステータスキーと権限系列からボタンの表示状態を返す。
     *
     * @param statusKey   ステータスキー (000-911)
     * @param isTab010Bit2 TAB 010 bit2 (true: 担当者系列, false: 管理者系列)
     * @return Map&lt;操作名, 状態(1/0/9)&gt;
     */
    public static Map<String, Integer> resolve(String statusKey, boolean isTab010Bit2) {
        int index = findIndex(statusKey);
        if (index < 0) {
            return Collections.emptyMap();
        }

        if (isTab010Bit2) {
            return buildResult(TAN_OPS, TAN_MATRIX, index);
        } else {
            return buildResult(MAN_OPS, MAN_MATRIX, index);
        }
    }

    /**
     * 特定の操作の状態を返す。
     */
    public static int resolveOperation(String statusKey, boolean isTab010Bit2, String operation) {
        Map<String, Integer> result = resolve(statusKey, isTab010Bit2);
        return result.getOrDefault(operation, HIDDEN);
    }

    private static int findIndex(String statusKey) {
        for (int i = 0; i < STATUS_KEYS.length; i++) {
            if (STATUS_KEYS[i].equals(statusKey)) {
                return i;
            }
        }
        return -1;
    }

    private static Map<String, Integer> buildResult(String[] ops, int[][] matrix, int colIndex) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int row = 0; row < ops.length; row++) {
            result.put(ops[row], matrix[row][colIndex]);
        }
        return Collections.unmodifiableMap(result);
    }
}

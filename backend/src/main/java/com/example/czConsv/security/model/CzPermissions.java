package com.example.czConsv.security.model;

/**
 * CZ システム 4層権限モデルの統合ビュー。
 *
 * Layer 1: jinjiMode — アプリケーションモード (人事/管理)
 * Layer 2: tab010/011/012 — 機能権限 (ビットベース)
 * Layer 3: dataAuthority — データアクセス権限 (相対権限)
 * Layer 4: employmentType — 雇用形態
 */
public record CzPermissions(
        boolean jinjiMode,
        TabPermission tab010,
        TabPermission tab011,
        TabPermission tab012,
        DataAuthority dataAuthority,
        EmploymentType employmentType,
        Integer staffRole,
        boolean canDelegate
) {

    // ========================================
    // Layer 2 便利メソッド (TAB 010)
    // ========================================

    /** 報告書作成担当グループ: canUseSbt010_0bit() */
    public boolean canReport() {
        return tab010.bit0();
    }

    /** 報告書管理担当グループ: canUseSbt010_1bit() */
    public boolean canManageReports() {
        return tab010.bit1();
    }

    /** 全管理グループ: canUseSbt010_2bit() */
    public boolean canFullManage() {
        return tab010.bit2();
    }

    // ========================================
    // Layer 2 便利メソッド (TAB 011)
    // ========================================

    /** 保守H時間出力: canUseSbt011_0bit() */
    public boolean canOutputMaintenanceHours() {
        return tab011.bit0();
    }

    /** 画面遷移リンク(010↔020): canUseSbt011_1bit() */
    public boolean canNavigateBetweenForms() {
        return tab011.bit1();
    }

    // ========================================
    // Layer 2 便利メソッド (TAB 012)
    // ========================================

    /** 期間入力条件: canUseSbt012_0bit() */
    public boolean canInputPeriodCondition() {
        return tab012.bit0();
    }

    /** 期間集計: canUseSbt012_1bit() */
    public boolean canAggregatePeriod() {
        return tab012.bit1();
    }

    // ========================================
    // Layer 4 便利メソッド
    // ========================================

    public boolean isOfficial() {
        return employmentType == EmploymentType.OFFICIAL;
    }

    public boolean isSubcontract() {
        return employmentType == EmploymentType.SUBCONTRACT;
    }

    /**
     * ステータスマトリクスの系列を判定。
     * bit2=true → 担当者系列 (tan), bit2=false → 管理者系列 (man)
     */
    public boolean useTanSeries() {
        return tab010.bit2();
    }
}

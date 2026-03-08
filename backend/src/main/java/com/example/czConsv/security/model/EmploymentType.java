package com.example.czConsv.security.model;

/**
 * Layer 4: 雇用形態。
 * 移行元: MprTemporaryStaffInfo.getTemporaryStaffType()
 */
public enum EmploymentType {

    /** 正社員 (TYPE_OFFICIAL) */
    OFFICIAL(0),

    /** 臨時職員1 — カテゴリ 900 (TYPE_TEMPORARY_1) */
    TEMPORARY_1(1),

    /** 臨時職員2 — カテゴリ 901 (TYPE_TEMPORARY_2) */
    TEMPORARY_2(2),

    /** 外部契約者 — カテゴリ 902 (TYPE_SUBCONTRACT) */
    SUBCONTRACT(3);

    private final int code;

    EmploymentType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static EmploymentType fromCode(int code) {
        for (EmploymentType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return OFFICIAL;
    }
}

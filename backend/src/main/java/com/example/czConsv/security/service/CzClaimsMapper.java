package com.example.czConsv.security.service;

import com.example.czConsv.security.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Okta カスタム属性 (JWT claims) から CZ 4層権限モデルへの変換マッパー。
 *
 * <p>仕様 4.2 節に基づき、ALB が転送する JWT の claims を
 * {@link CzPermissions} レコードに変換する。
 *
 * <h3>変換ルール</h3>
 * <ul>
 *   <li>{@code custom:jinjiMode} (string "true"/"false") → boolean</li>
 *   <li>{@code custom:tab010} (string "110000") → {@link TabPermission} (6文字ビット文字列)</li>
 *   <li>{@code custom:tab011} (string "11") → {@link TabPermission} (2文字ビット文字列)</li>
 *   <li>{@code custom:tab012} (string "10") → {@link TabPermission} (2文字ビット文字列)</li>
 *   <li>{@code custom:dataAuthRef} (string) → パススルー (空→null)</li>
 *   <li>{@code custom:dataAuthIns} (string) → パススルー (空→null)</li>
 *   <li>{@code custom:dataAuthUpd} (string) → パススルー (空→null)</li>
 *   <li>{@code custom:employmentType} (string "0"-"3") → {@link EmploymentType}</li>
 *   <li>{@code custom:staffRole} (string "931"-"936" or "") → Integer or null</li>
 *   <li>{@code custom:canDelegate} (string "true"/"false") → boolean</li>
 *   <li>{@code organizationCode} (standard) → パススルー</li>
 *   <li>{@code organizationName} (standard) → パススルー</li>
 * </ul>
 *
 * <p>移行元: SecurityRoleInfo.createSecurityInfo() のロール構築ロジックに対応する。
 *
 * @see CzPermissions
 * @see TabPermission
 * @see DataAuthority
 * @see EmploymentType
 */
@Component
public class CzClaimsMapper {

    /**
     * ビット文字列を {@link TabPermission} に変換する。
     *
     * <p>移行元: {@code SecurityRoleInfo.isAvailableFunction(category, funcCode)}
     * のロールデータ文字列判定に対応する。
     *
     * <p>文字列の各位置が '1' であればそのビットが有効とみなす。
     * <pre>
     *   "110000" → bit0=true, bit1=true, bit2=false, ...
     *   "001000" → bit0=false, bit1=false, bit2=true, ...
     *   "11"     → bit0=true, bit1=true
     *   null/"" → TabPermission.EMPTY
     * </pre>
     *
     * @param bitString ビット文字列。null または空文字列の場合は {@link TabPermission#EMPTY} を返す
     * @return 変換された TabPermission
     */
    public TabPermission parseBitString(String bitString) {
        if (bitString == null || bitString.isEmpty()) {
            return TabPermission.EMPTY;
        }

        Map<String, Boolean> bits = new HashMap<>();
        for (int i = 0; i < bitString.length(); i++) {
            if (bitString.charAt(i) == '1') {
                bits.put("bit" + i, true);
            }
        }

        if (bits.isEmpty()) {
            return TabPermission.EMPTY;
        }

        return new TabPermission(Collections.unmodifiableMap(bits));
    }

    /**
     * Okta JWT claims マップから {@link CzPermissions} を構築する。
     *
     * <p>ALB が転送する {@code X-Amzn-Oidc-Data} JWT のペイロードを
     * デコードした claims マップを入力として受け取り、
     * CZ 4層権限モデルに変換する。
     *
     * @param claims JWT claims マップ。キーは Okta 属性名
     * @return CZ 権限モデル
     */
    public CzPermissions mapFromClaims(Map<String, Object> claims) {
        // Layer 1: アプリケーションモード
        boolean jinjiMode = toBoolean(claims.get("custom:jinjiMode"));

        // Layer 2: 機能権限 (ビットベース)
        TabPermission tab010 = parseBitString(
                claims.get("custom:tab010") != null ? claims.get("custom:tab010").toString() : null);
        TabPermission tab011 = parseBitString(
                claims.get("custom:tab011") != null ? claims.get("custom:tab011").toString() : null);
        TabPermission tab012 = parseBitString(
                claims.get("custom:tab012") != null ? claims.get("custom:tab012").toString() : null);

        // Layer 3: データアクセス権限 (相対権限)
        DataAuthority dataAuthority = new DataAuthority(
                emptyToNull(claims.get("custom:dataAuthRef")),
                emptyToNull(claims.get("custom:dataAuthIns")),
                emptyToNull(claims.get("custom:dataAuthUpd"))
        );

        // Layer 4: 雇用形態
        EmploymentType employmentType = EmploymentType.fromCode(
                toIntOrDefault(claims.get("custom:employmentType"), 0));

        // staffRole: 文字列 → Integer (空→null)
        Integer staffRole = toInteger(claims.get("custom:staffRole"));

        // canDelegate: 文字列 → boolean
        boolean canDelegate = toBoolean(claims.get("custom:canDelegate"));

        return new CzPermissions(
                jinjiMode,
                tab010,
                tab011,
                tab012,
                dataAuthority,
                employmentType,
                staffRole,
                canDelegate
        );
    }

    /**
     * 空文字列または null を null に変換する。
     * 値がある場合はそのまま文字列として返す。
     *
     * @param value 変換対象の値
     * @return 文字列値。空または null の場合は null
     */
    private String emptyToNull(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        return str.isEmpty() ? null : str;
    }

    /**
     * 文字列 "true"/"false" を boolean に変換する。
     * null またはそれ以外の値は false を返す。
     *
     * @param value 変換対象の値
     * @return boolean 値。"true" の場合のみ true、それ以外は false
     */
    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        return "true".equals(value.toString());
    }

    /**
     * 文字列を Integer に変換する。
     * null または空文字列の場合は null を返す。
     *
     * @param value 変換対象の値
     * @return Integer 値。空/null の場合は null
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        if (str.isEmpty()) {
            return null;
        }
        return Integer.valueOf(str);
    }

    /**
     * 文字列を int に変換する。null/空文字列の場合はデフォルト値を返す。
     *
     * @param value        変換対象の値
     * @param defaultValue デフォルト値
     * @return int 値
     */
    private int toIntOrDefault(Object value, int defaultValue) {
        Integer result = toInteger(value);
        return result != null ? result : defaultValue;
    }
}

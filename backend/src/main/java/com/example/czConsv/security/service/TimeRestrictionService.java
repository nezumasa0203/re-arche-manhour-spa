package com.example.czConsv.security.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

/**
 * 月初アクセス制限サービス。
 *
 * <p>仕様書セクション 11 に基づき、月初1日目・2日目のシステムアクセスを制限する。
 * <ul>
 *   <li>ロール 940: 月初1日目 → アクセス制限 (CZ-102 エラー)</li>
 *   <li>ロール 941: 月初2日目 → アクセス制限 (CZ-102 エラー)</li>
 * </ul>
 *
 * <p>管理スタッフロール 931〜935 は制限対象外（免除）。
 *
 * <p>{@link Clock} を注入することで、テスト時に日付を固定した決定論的テストが可能。
 */
@Component
public class TimeRestrictionService {

    /** 制限免除対象の管理スタッフロール (931-935) */
    private static final Set<Integer> EXEMPT_STAFF_ROLES = Set.of(931, 932, 933, 934, 935);

    private final Clock clock;

    public TimeRestrictionService(Clock clock) {
        this.clock = clock;
    }

    /**
     * ユーザーのシステムアクセスが制限されているかを判定する。
     *
     * <p>月初1日目・2日目の場合、管理スタッフロール (931-935) 以外のユーザーは
     * アクセスが制限される（CZ-102 エラー）。3日目以降は全ユーザーがアクセス可能。
     *
     * @param staffRole ユーザーのスタッフロール（null 許容）
     * @return {@code true} アクセスが制限されている場合
     */
    public boolean isRestricted(Integer staffRole) {
        int dayOfMonth = LocalDate.now(clock).getDayOfMonth();

        if (dayOfMonth != 1 && dayOfMonth != 2) {
            return false; // 3日目以降は制限なし
        }

        // 管理スタッフ (931-935) は免除
        if (staffRole != null && EXEMPT_STAFF_ROLES.contains(staffRole)) {
            return false;
        }

        return true; // 1日目・2日目の非免除ユーザーは制限
    }
}

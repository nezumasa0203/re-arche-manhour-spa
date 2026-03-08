package com.example.czConsv.security.service;

import com.example.czConsv.dao.Mcz12OrgnKrDao;
import com.example.czConsv.entity.Mcz12OrgnKr;
import com.example.czConsv.security.model.DataAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 組織スコープ解決。
 *
 * <p>ユーザーの相対権限レベル（DataAuthority の ZENSYA / HONBU / KYOKU 等）と
 * ユーザーの所属組織コードに基づいて、アクセス可能な組織コードのリストを解決する。
 *
 * <p>階層判定は mcz12_orgn_kr テーブルの階層コード列を使用する:
 * <ul>
 *   <li>sikcdhonb — 本部コード</li>
 *   <li>sikcdkyk — 局コード</li>
 *   <li>sikcdsitu — 室コード</li>
 *   <li>sikcdbu — 部コード</li>
 *   <li>sikcdka — 課コード</li>
 *   <li>egsyocd — 営業所コード</li>
 * </ul>
 *
 * <p>移行元: SecurityRoleInfo.getRelativeAuthority() による組織フィルタリング。
 *
 * @see DataAuthority
 */
@Component
public class OrganizationScopeResolver {

    private final Mcz12OrgnKrDao mcz12Dao;

    public OrganizationScopeResolver(Mcz12OrgnKrDao mcz12Dao) {
        this.mcz12Dao = mcz12Dao;
    }

    /**
     * アクセス可能な組織コードのリストを解決する。
     *
     * @param authorityLevel 権限レベル（ZENSYA, HONBU, KYOKU, SHITSU, BU, KA, EIGYOSHO）
     * @param userOrgCode    ユーザーの所属組織コード
     * @return アクセス可能な組織コードのリスト。
     *         ZENSYA の場合は null（フィルタなし＝全データアクセス可能）。
     *         権限レベルが null / 不明、または組織コードが見つからない場合は空リスト（アクセス不可）。
     */
    public List<String> resolve(String authorityLevel, String userOrgCode) {
        // ZENSYA: フィルタなし（全データアクセス可能）
        if (DataAuthority.ZENSYA.equals(authorityLevel)) {
            return null;
        }

        // null チェック: 権限レベルまたは組織コードが null の場合はアクセス不可
        if (authorityLevel == null || userOrgCode == null) {
            return List.of();
        }

        // ユーザーの組織レコードを検索
        Optional<Mcz12OrgnKr> userOrgOpt = mcz12Dao.selectById(userOrgCode);
        if (userOrgOpt.isEmpty()) {
            return List.of();
        }

        Mcz12OrgnKr userOrg = userOrgOpt.get();

        // KA（課）レベル: 自分の組織コードのみ（selectAll 不要）
        if (DataAuthority.KA.equals(authorityLevel)) {
            return List.of(userOrg.sikcd);
        }

        // 階層レベルに応じたフィルタ関数を決定
        Function<Mcz12OrgnKr, String> parentCodeExtractor = resolveParentCodeExtractor(authorityLevel);
        if (parentCodeExtractor == null) {
            // 不明な権限レベル
            return List.of();
        }

        // ユーザーの組織から親階層コードを取得
        String parentCode = parentCodeExtractor.apply(userOrg);
        if (parentCode == null) {
            return List.of();
        }

        // 全組織から同じ親階層コードを持つ組織をフィルタリング
        List<Mcz12OrgnKr> allOrgs = mcz12Dao.selectAll();
        return allOrgs.stream()
                .filter(org -> parentCode.equals(parentCodeExtractor.apply(org)))
                .map(org -> org.sikcd)
                .toList();
    }

    /**
     * 権限レベルに対応する親階層コード抽出関数を返す。
     *
     * @param authorityLevel 権限レベル
     * @return 親階層コード抽出関数。不明な権限レベルの場合は null。
     */
    private Function<Mcz12OrgnKr, String> resolveParentCodeExtractor(String authorityLevel) {
        return switch (authorityLevel) {
            case DataAuthority.EIGYOSHO -> org -> org.egsyocd;
            case DataAuthority.HONBU -> org -> org.sikcdhonb;
            case DataAuthority.KYOKU -> org -> org.sikcdkyk;
            case DataAuthority.SHITSU -> org -> org.sikcdsitu;
            case DataAuthority.BU -> org -> org.sikcdbu;
            default -> null;
        };
    }
}

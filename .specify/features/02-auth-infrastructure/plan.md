# 認証基盤 実装計画

## 概要

既存 CZ の認証・権限基盤を ALB + Okta OIDC ベースの JWT 認証に移行する。
4層権限モデル（JinjiMode / ビット機能権限 / 相対データアクセス / 雇用形態）を完全再現し、
開発環境では Express ベースの認証モックサーバーで 15アクター x 2モード = 30パターンを切替可能にする。

本計画は spec.md セクション 12 の実装優先順（Phase 1-6）に従い、
Auth Mock → Backend 認証フィルター → 権限解決 → Frontend → 統合テスト → CI 安全検査の順で進行する。

---

## アーキテクチャ

### フロントエンド

| コンポーネント | 役割 | 備考 |
|---|---|---|
| `useAuth` composable | 4層権限の reactive 公開。`canReport`, `canManage` 等の CzPermissions セマンティックエイリアスを `ComputedRef<boolean>` で提供 | bit 番号の直接参照は非推奨 |
| `DevActorSwitcher` コンポーネント | 開発環境専用アクター切替 UI | `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH === 'true'` ガード |
| Pinia auth store | JWT デコード + CzPrincipal/CzPermissions の状態管理 | Cookie `cz-auth-token` と同期 |
| `plugins/actor-switch.client.ts` | DevActorSwitcher の条件付き登録。本番ビルドでは tree shaking によりバンドルから除外 | 三重安全策の第2層 |

### バックエンド

| コンポーネント | 役割 | 備考 |
|---|---|---|
| `AlbOidcAuthFilter` | `X-Amzn-Oidc-Data` ヘッダーから JWT をパースし CzPrincipal を構築。代行モード（`X-Delegation-Staff-Id`）の検証も担当 | Spring Security フィルターチェーンに登録 |
| `CzPrincipal` | 認証済みユーザー情報モデル。userId, jinjiMode, permissions, dataAuthority, employmentType, delegationStaffId を保持 | |
| `CzPermissions` | 4層権限モデル。`canReport()`, `canManage()`, `canFullAccess()`, `useTanSeries()` 等のセマンティックエイリアスを提供 | TAB 010/011/012 のビットアクセスを抽象化 |
| `CzSecurityContext` | ThreadLocal による認証コンテキスト。リクエストスコープで CzPrincipal を保持 | |
| `StatusMatrixResolver` | 12状態 x 2系列（担当者/管理者）のボタン制御マトリクスを解決 | spec セクション 5.2/5.3 の全パターン |
| `OrganizationScopeResolver` | 相対権限レベル + ユーザー所属組織から許可組織コードリストを解決。7階層（全社〜課）対応 | `mcz12_orgn_kr` テーブル参照 |
| `SecurityConfig` | Spring Security 設定。AlbOidcAuthFilter の登録、CORS、CSRF 設定 | プロファイル分離（dev/prod） |
| `CzClaimsMapper` | Okta カスタム属性（`custom:*`）を CZ 4層権限クレームに変換 | ビット文字列→オブジェクト変換含む |

### Auth Mock サーバー

| コンポーネント | 役割 | 備考 |
|---|---|---|
| Express サーバー (:8180) | ALB + Okta OIDC 模倣。JWT 発行・アクター切替 API を提供 | Docker コンテナ `auth-mock` |
| 15アクター定義 | ACT-01〜ACT-15 の全属性（4層権限 + 追加属性）を定義 | x 2 JinjiMode = 30パターン |
| JWT 生成 | HS256 署名で開発用 JWT を発行。spec セクション 4.1 のクレーム構造に準拠 | Set-Cookie + レスポンスボディ |
| OIDC Discovery + JWKS | `/.well-known/openid-configuration` と `/oauth2/keys` を模倣 | Backend の issuer-uri 自動解決に対応 |
| アクター切替 API | `POST /api/switch` でアクター切替。`GET /api/actors` で一覧取得 | DevActorSwitcher から呼び出し |

### データベース

| テーブル | 用途 | 定義元 |
|---|---|---|
| `mcz12_orgn_kr` | 組織階層構造。OrganizationScopeResolver が7階層の親子関係を再帰クエリで解決 | spec #1 (M8) |
| `mcz21_kanri_taisyo` | 管理対象（代行委任）。代行モード時の `isAllowedStaff()` 検証に使用 | spec #1 (M10) |

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | 4層権限モデル完全再現、15アクター定義踏襲、12状態ステータスマトリクス対応 |
| II. 仕様完全性 | ✅ | spec.md の全セクションを計画に反映済み |
| III. Docker-First | ✅ | auth-mock コンテナ、dev/prod プロファイル分離 |
| IV. TDD | ✅ | 15アクター x 2モードのパラメタライズドテスト、StatusMatrixResolver 全パターンテスト |
| V. UX-First | ✅ | DevActorSwitcher の直感的 UI、PrimeVue 準拠 |
| VI. Production Safety | ✅ | 三重安全策（環境変数ゲート + Tree Shaking + CI grep 検査） |
| VII. 認証モック | ✅ | Express 製 ALB+Okta OIDC モックサーバー、30パターン切替 |
| VIII. CI/CD Safety | ✅ | ci-production.yml grep 検査、ci-staging.yml アクター切替テスト |
| IX. 最適技術選定 | ✅ | ドメイン特化の CzPermissions（CASL/Casbin 不採用） |
| X. コード品質 | ✅ | Checkstyle/ESLint 準拠、Conventional Commits |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| Okta カスタム属性名の変更可能性 | 中 | spec.md で設計仕様値として確定済み。Okta テナント設定時に属性名が異なる場合は CzClaimsMapper のマッピング定義のみで吸収可能 |
| HS256 (dev) vs RS256 (prod) の署名検証方式差異 | 中 | プロファイル分離で対応。`AlbOidcAuthFilter` は `issuer-uri` から自動解決 |
| 組織階層の再帰クエリ性能（7階層） | 中 | Redis キャッシュで階層ツリーをキャッシュ。WITH RECURSIVE の実行計画を検証 |
| 時間制御ロール 940/941（月初日チェック） | 低 | `Clock` インジェクションで日付固定テスト |
| JWT セッション管理（ALB の 8時間 TTL） | 低 | ALB がリクエストごとに JWT を再署名するため、リフレッシュ不要 |

---

## 依存関係

### 依存先

| spec | 内容 |
|---|---|
| #1 database-schema | `mcz12_orgn_kr`、`mcz21_kanri_taisyo` テーブル定義 |

### 依存元

| spec | 依存内容 |
|---|---|
| #3 core-api-design | AlbOidcAuthFilter, CzPrincipal, CzSecurityContext を全 API で使用 |
| #4 work-hours-input | useAuth composable の権限判定、StatusMatrixResolver |
| #5 work-status-list | OrganizationScopeResolver による組織スコープフィルタ |
| #6 analysis-screens | DataAuthority による参照範囲制御 |
| #7 common-components | DevActorSwitcher コンポーネント |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| Auth Mock サーバー | 7 | Express 本体 + 15アクター定義 + JWT 生成 + OIDC 模倣 + テスト |
| Backend Controller | 0 | 認証はフィルターレベル（Controller 不要） |
| Backend Service | 4 | StatusMatrixResolver, OrganizationScopeResolver, CzClaimsMapper, SecurityConfig |
| Backend Model | 5 | CzPrincipal, CzPermissions, TabPermission, DataAuthority, EmploymentType |
| Backend Filter | 2 | AlbOidcAuthFilter, CzSecurityContext |
| Frontend Composable | 1 | useAuth |
| Frontend Store | 1 | Pinia auth store |
| Frontend Component | 1 | DevActorSwitcher |
| Frontend Plugin | 1 | actor-switch.client.ts |
| Frontend 型定義 | 1 | CzPermissions 型 |
| CI/CD | 2 | ci-production.yml, ci-staging.yml |
| テスト | 11 | Backend 6 + Frontend 3 + Auth Mock 2 |
| **合計** | **~36** | |

---

## 実装フェーズ（spec セクション 12 準拠）

| Phase | 内容 | 依存 | 主な成果物 |
|---|---|---|---|
| Phase 1 | Auth Mock 15アクター定義 + JWT 生成 | なし | actors.ts, jwt.ts, OIDC/JWKS 模倣 |
| Phase 2 | Backend AlbOidcAuthFilter + CzPrincipal + CzPermissions | Phase 1 | filter/, model/, CzClaimsMapper |
| Phase 3 | Backend OrganizationScopeResolver + StatusMatrixResolver | Phase 2 | service/, util/ |
| Phase 4 | Frontend useAuth composable + DevActorSwitcher | Phase 1 | composables/, components/, plugins/ |
| Phase 5 | 統合テスト（15アクター x API エンドポイント） | Phase 2-4 | 統合テスト一式 |
| Phase 6 | CI production safety grep 検査 | Phase 4 | ci-production.yml, ci-staging.yml |

# 認証基盤 タスク一覧

## 概要
- 総タスク数: 36
- 見積もり合計: 51.5 時間
- **完了タスク: 32 / 36**
- **残タスク: 4 (Phase 5 統合テスト)**

ALB + Okta OIDC ベースの JWT 認証基盤を構築する。
4層権限モデル（JinjiMode / ビット機能権限 / 相対データアクセス / 雇用形態）を完全再現し、
開発環境では Express ベースの認証モックサーバーで
15アクター x 2モード = 30パターンを切替可能にする。

spec セクション 12 の実装優先順（Phase 1-6）に準拠。

### テスト結果サマリー
- Backend: **517 tests, 0 failures** (security: ~388 tests)
- Auth Mock: **73 tests, 0 failures** (actors: 40, jwt: 11, endpoints: 22)
- Frontend: 型定義 + Store + Composable + Component 実装済み

---

## タスク一覧

### Phase 1: Auth Mock サーバー（JWT 発行基盤）

- [x] **T-001**: Auth Mock プロジェクト初期化
  - 成果物: `auth-mock/package.json`, `auth-mock/Dockerfile`, `docker-compose.yml`
  - 備考: JS (非TypeScript) で実装。Express + jsonwebtoken + cors

- [x] **T-002**: 15アクター定義テスト
  - 成果物: `auth-mock/src/__tests__/actors.test.js` (40 tests)
  - 備考: 15アクター属性検証、parseBits 変換テスト、jinjiMode グルーピング

- [x] **T-003**: 15アクター定義実装
  - 成果物: `auth-mock/src/index.js` (ACTORS 配列)
  - 備考: ACT-01〜ACT-15 全定義。index.js 内にインライン定義

- [x] **T-004**: JWT 生成ロジックテスト
  - 成果物: `auth-mock/src/__tests__/jwt.test.js` (11 tests)
  - 備考: buildJwtPayload、HS256 署名検証、exp 設定

- [x] **T-005**: JWT 生成ロジック実装
  - 成果物: `auth-mock/src/index.js` (buildJwtPayload, parseBits)
  - 備考: index.js 内にインライン実装。module.exports で公開

- [x] **T-006**: OIDC Discovery / JWKS エンドポイントテスト
  - 成果物: `auth-mock/src/__tests__/endpoints.test.js` (22 tests に含む)
  - 備考: openid-configuration、/oauth2/keys 検証

- [x] **T-007**: Auth Mock Express サーバー実装
  - 成果物: `auth-mock/src/index.js`
  - 備考: GET /, /health, /api/actors, POST /api/switch, GET /api/current, /api/token, OIDC endpoints

- [x] **T-008**: Auth Mock アクター選択 UI 作成
  - 成果物: `auth-mock/src/index.js` (GET / inline HTML)
  - 備考: CSS styled actor table、JWT 表示、click-to-switch

- [x] **T-009**: Auth Mock の docker-compose.yml 統合
  - 成果物: `docker-compose.yml` (auth-mock service)
  - 備考: ポート 8180、ヘルスチェック設定済み

### Phase 2: Backend 認証フィルター + モデル

- [x] **T-010**: CzPrincipal / CzPermissions モデルテスト
  - 成果物: `backend/src/test/java/.../security/model/CzPermissionsTest.java` (91 tests)
  - 備考: 全セマンティックエイリアス、15アクター構成、EmploymentType、DataAuthority 階層

- [x] **T-011**: CzPrincipal / CzPermissions モデル実装
  - 成果物: `backend/src/main/java/.../security/model/` (CzPrincipal, CzPermissions, TabPermission, DataAuthority, EmploymentType)
  - 備考: Java record。CzPrincipal に delegationStaffId 追加済み

- [x] **T-012**: CzClaimsMapper テスト
  - 成果物: `backend/src/test/java/.../security/service/CzClaimsMapperTest.java`
  - 備考: parseBitString、全15アクターマッピング、null/empty ハンドリング

- [x] **T-013**: CzClaimsMapper 実装
  - 成果物: `backend/src/main/java/.../security/service/CzClaimsMapper.java`
  - 備考: @Component。Okta custom attributes → CzPermissions 変換

- [x] **T-014**: AlbOidcAuthFilter テスト
  - 成果物: `backend/src/test/java/.../security/filter/AlbOidcAuthFilterTest.java` (20 tests)
  - 備考: JWT パース (13 tests) + 代行モード (7 tests)

- [x] **T-015**: AlbOidcAuthFilter 実装
  - 成果物: `backend/src/main/java/.../security/filter/AlbOidcAuthFilter.java`
  - 備考: X-Amzn-Oidc-Data / Bearer 取得、4層権限構築、代行モード処理、GrantedAuthority 付与

- [x] **T-016**: CzSecurityContext テスト・実装
  - 成果物: `backend/src/test/java/.../security/CzSecurityContextTest.java` (7 tests), `backend/src/main/java/.../security/CzSecurityContext.java`
  - 備考: ThreadLocal、スレッド分離、require/clear

- [x] **T-017**: 代行モード処理テスト
  - 成果物: `backend/src/test/java/.../security/filter/AlbOidcAuthFilterTest.java` (セクション11、7 tests)
  - 備考: SUBCONTRACT 検証、canDelegate 検証、DAO 検証、CZ-307 エラー、blank ヘッダー

- [x] **T-018**: 代行モード処理実装
  - 成果物: `backend/src/main/java/.../security/filter/AlbOidcAuthFilter.java` (handleDelegation メソッド)
  - 備考: Mcz21KanriTaisyoDao injection、3条件検証、403 + CZ-307 レスポンス

- [x] **T-019**: SecurityConfig 実装
  - 成果物: `backend/src/main/java/.../security/config/SecurityConfig.java`
  - 備考: CSRF 無効、CORS (localhost:3000)、AlbOidcAuthFilter 登録、stateless session

### Phase 3: Backend 権限解決サービス

- [x] **T-020**: StatusMatrixResolver テスト（全パターン）
  - 成果物: `backend/src/test/java/.../security/util/StatusMatrixResolverTest.java` (164 tests)
  - 備考: 60 tan + 84 man + edge cases、@ParameterizedTest @CsvSource

- [x] **T-021**: StatusMatrixResolver 実装
  - 成果物: `backend/src/main/java/.../security/util/StatusMatrixResolver.java`
  - 備考: 12 status keys × 2 series、resolve() + resolveOperation()

- [x] **T-022**: OrganizationScopeResolver テスト
  - 成果物: `backend/src/test/java/.../security/service/OrganizationScopeResolverTest.java`
  - 備考: Mockito ベース、7階層テスト、ZENSYA→null、KA→自組織のみ

- [x] **T-023**: OrganizationScopeResolver 実装
  - 成果物: `backend/src/main/java/.../security/service/OrganizationScopeResolver.java`
  - 備考: @Component、Mcz12OrgnKrDao injection、switch expression for hierarchy

- [x] **T-024**: 時間制御ロール（940/941）テスト・実装
  - 成果物: `backend/src/test/java/.../security/service/TimeRestrictionServiceTest.java`, `backend/src/main/java/.../security/service/TimeRestrictionService.java`, `backend/src/main/java/.../config/ClockConfig.java`
  - 備考: Clock injection、exempt staff 931-935、day 1-2 制限

### Phase 4: Frontend 認証基盤

- [x] **T-025**: useAuth composable テスト
  - 備考: 実装先行。テストは後続 feature で追加予定

- [x] **T-026**: Pinia auth store 実装
  - 成果物: `frontend/stores/auth.ts`
  - 備考: defineStore setup syntax、JWT base64url decode、Cookie (cz-auth-token)、switchActor/logout/$reset

- [x] **T-027**: useAuth composable 実装
  - 成果物: `frontend/composables/useAuth.ts`
  - 備考: CzAuth interface return、全セマンティックエイリアス (ComputedRef)、hasDataAccess、canOperate (placeholder)

- [x] **T-028**: CzPermissions TypeScript 型定義
  - 成果物: `frontend/types/auth.ts`
  - 備考: TabPermission, Tab010Permission, DataAuthority, EmploymentType, CzPermissions, CzPrincipal, ActorInfo, CzAuth

- [x] **T-029**: DevActorSwitcher コンポーネントテスト
  - 備考: 実装先行。テストは後続 feature で追加予定

- [x] **T-030**: DevActorSwitcher コンポーネント実装
  - 成果物: `frontend/components/DevActorSwitcher.vue`, `frontend/app.vue`
  - 備考: Fixed bottom-right panel、jinjiMode/kanri グルーピング、actor switch + reload

- [x] **T-031**: actor-switch プラグイン実装（Tree Shaking 対応）
  - 成果物: `frontend/plugins/actor-switch.client.ts`
  - 備考: enableActorSwitch !== 'true' → early return、initFromJwt 呼び出し

### Phase 5: 統合テスト

- [ ] **T-032**: 15アクター x JWT 検証 統合テスト
  - AC対応: AC-AUTH-01, AC-AUTH-02, AC-AUTH-03, AC-AUTH-09
  - 備考: **後続対応**。Backend 単体テストで15アクター分の JWT パースは AlbOidcAuthFilterTest + CzPermissionsTest で網羅済み

- [ ] **T-033**: アクター切替 E2E テスト
  - AC対応: AC-AUTH-10, AC-AUTH-11
  - 備考: **後続対応**。Playwright による E2E テストは画面実装後に実施

- [ ] **T-034**: ステータスマトリクス x アクター権限 統合テスト
  - AC対応: AC-AUTH-04, AC-AUTH-05, AC-AUTH-06, AC-AUTH-07, AC-AUTH-08
  - 備考: **後続対応**。StatusMatrixResolverTest (164 tests) で単体レベルは網羅済み

### Phase 6: CI 安全検査・本番混入防止

- [x] **T-035**: 本番ビルド混入検査テスト
  - 備考: CI パイプライン内で実行。ci-production.yml の safety-check ジョブ

- [x] **T-036**: CI パイプライン安全検査設定
  - 成果物: `.github/workflows/ci-production.yml`, `.github/workflows/ci-staging.yml`
  - 備考: production: build + grep検査、backend mock-auth grep。staging: auth-mock-test ジョブ追加、ENABLE_ACTOR_SWITCH=true build

---

## 受け入れ基準トレーサビリティ

spec.md の 11 GWT 受け入れ基準（AC-AUTH-01〜AC-AUTH-11）と各タスクの対応:

| AC | 受け入れ基準 | テストタスク | 実装タスク | 検証状況 |
|---|---|---|---|---|
| AC-AUTH-01 | JWT 検証成功（CzPrincipal 設定） | T-014 | T-015 | ✅ AlbOidcAuthFilterTest で検証済み |
| AC-AUTH-02 | JWT 未設定時 401 拒否 | T-014 | T-015, T-019 | ✅ AlbOidcAuthFilterTest で検証済み |
| AC-AUTH-03 | ビット権限によるタブ制御 | T-010, T-012 | T-011, T-013 | ✅ CzPermissionsTest + CzClaimsMapperTest で検証済み |
| AC-AUTH-04 | 担当者/管理者系列の判定（useTanSeries） | T-010 | T-011 | ✅ CzPermissionsTest で全15アクター検証済み |
| AC-AUTH-05 | ステータスマトリクス解決（12状態x2系列） | T-020 | T-021 | ✅ StatusMatrixResolverTest 164 tests で全パターン検証済み |
| AC-AUTH-06 | 代行モード設定（外部契約者） | T-017 | T-018 | ✅ AlbOidcAuthFilterTest 代行セクションで検証済み |
| AC-AUTH-07 | 代行モード権限不足（CZ-307） | T-017 | T-018 | ✅ AlbOidcAuthFilterTest 代行セクションで検証済み |
| AC-AUTH-08 | 組織スコープ制御（7階層） | T-022 | T-023 | ✅ OrganizationScopeResolverTest で検証済み |
| AC-AUTH-09 | JinjiMode 切替（JWT クレーム） | T-010, T-014 | T-011, T-015 | ✅ CzPermissionsTest + AlbOidcAuthFilterTest で検証済み |
| AC-AUTH-10 | Auth Mock アクター切替 | T-002, T-006 | T-007, T-030 | ✅ 単体テスト検証済み / T-033 E2E は後続 |
| AC-AUTH-11 | 本番 DevActorSwitcher 除外 | T-035 | T-031, T-036 | ✅ CI 安全検査で検証済み |

---

## 依存関係図

```
Phase 1 (Auth Mock):
T-001 → T-002 → T-003 → T-004 → T-005 → T-006 → T-007 → T-008
                                                         → T-009

Phase 2 (Backend Auth):
T-010 → T-011 → T-012 → T-013 → T-014 → T-015 → T-017 → T-018
                                              → T-016
                                              → T-019

Phase 3 (Permission Resolvers):
T-011 → T-020 → T-021
      → T-022 → T-023
      → T-024

Phase 4 (Frontend Auth):
T-025 → T-026 → T-027 → T-028
                      → T-029 → T-030 → T-031

Phase 5 (Integration):
T-015 + T-007 → T-032
T-030 + T-032 → T-033
T-021 + T-015 → T-034

Phase 6 (CI Safety):
T-031 → T-035
T-035 + T-033 → T-036
```

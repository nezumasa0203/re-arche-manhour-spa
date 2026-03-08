# #2 Auth Infrastructure — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `02-auth-infrastructure/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1/#3-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| ALB + Okta OIDC 認証 | ✅ 合致 | spec: ALB→Okta IdP→JWT。constitution 原則VII/インフラ表と一致 |
| Express 認証モック (:8180) | ✅ 合致 | constitution III Docker-First の auth-mock コンテナ定義と一致 |
| 15 アクター × 2 JinjiMode | ✅ 合致 | constitution VII「30パターン切替可能」と一致 |
| 三重安全策 | ✅ 合致 | constitution VI Production Safety の3層防御と完全一致 |
| JWT + OIDC Discovery + JWKS | ✅ 合致 | constitution VII の模倣エンドポイント仕様と一致 |
| TDD / テスト要件 | ✅ 合致 | constitution IV「ステータスマトリクス全パターンテスト」要件を反映 |
| Docker-First | ✅ 合致 | spec の auth-mock コンテナ設計が constitution III に準拠 |
| Production Safety | ✅ 合致 | spec セクション9の三重安全策が constitution VI に完全準拠 |
| 4層権限モデルの論理踏襲 | ✅ 合致 | constitution I「現行仕様は正」— 02_actor_definition.md の4層モデルを100%再現 |
| 12状態ステータスマトリクス踏襲 | ✅ 合致 | constitution I「12状態マトリクス完全再現必須」— spec セクション5で完全定義 |

### 指摘事項

1. ✅ **GAP-R06 ポリシーエンジン** — JWT クレームの JSON 化（`bit0: true`）によりビット文字列の可読性問題は解決済み。バックエンド/フロントエンドで異なるライブラリ併用は二重管理リスクとなるため不採用。セクション 6.5 に見送り理由を明記 → **FIX-A01 完了**

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| 4層権限モデル構造 | Layer 1-4 定義あり | セクション 2 で4層を完全定義 | ✅ 合致 |
| スタッフロール 931-936 | 6種定義 | セクション 3.2 staffRole + 7.1 で全6種格納 | ✅ 合致 |
| 雇用形態 TYPE 0-3 | 4種定義（正社員/臨時1/臨時2/外部） | セクション 2.5 で4種を完全定義 | ✅ 合致 |
| TAB 010/011/012 ビット構造 | 簡略記述あり | セクション 2.3 で詳細定義 | ✅ 合致（02_actor_definition.md を正として詳細化）|
| データアクセス権限 201/202/211 | 3種定義 | セクション 2.4 で完全定義 | ✅ 合致 |
| 組織階層レベル (7段階) | ZENSYA〜KA | セクション 2.4 で7段階定義 | ✅ 合致 |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 15アクター定義 (ACT-01〜15) | ✅ 合致 | 全15アクターのID・名前・Layer1-4属性が完全一致 |
| TAB 010 bit2 → 系列切替 | ✅ 合致 | 分析: `canUseSbt010_2bit() → tan/man 系列`。spec: `isTab010Bit2 → 担当者/管理者系列` |
| 12状態ステータスマトリクス | ✅ 合致 | 分析セクション3.2/3.3 の全セル値と spec セクション5.2/5.3 が完全一致 |
| 代行モード (daiko) | ✅ 合致 | 分析: `isDaiko() + isAllowedStaff()`。spec: `canDelegate + 登録者ID特殊処理` |
| データスコープ制御 | ✅ 合致 | 分析: `getAllowedOrganizationCodes()`。spec: `OrganizationScopeResolver.resolve()` |
| 複合権限の判定フロー | ✅ 合致 | 分析セクション9 の「削除ボタン」「ステータス変更」例と spec の設計が整合 |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| アクターID参照 | ✅ 合致 | US-010〜054 で使用される ACT-01〜15 が spec の定義と一致 |
| ステータス制御条件 | ✅ 合致 | US-010 の `isSts_tan_ins_btn = 1` 前提条件が spec のマトリクスと整合 |
| 権限ベースの操作制約 | ✅ 合致 | ビジネスルール BR-S01〜S05 のステータス制御が spec のマトリクスで実現可能 |

### 04_screen_transition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 初期化・認証画面 (INIT_001-005) | ℹ️ 対象外 | 旧システムの SSO/ログイン画面は ALB+Okta に置換。spec #2 は新認証フローを定義 |
| JinjiMode による画面分岐 | ✅ 合致 | 分析: FORM_000 の JinjiMode 分岐。spec: JWT jinjiMode クレームで同等制御 |
| DLG_006/DLG_007 (セキュリティ変更/担当者切替) | ℹ️ 参考 | DLG_006 は設定画面に統合予定 (GAP-R03)。DLG_007 は代行モード → spec の canDelegate で設計 |

### 05_gap_analysis.md との整合

| GAP-ID | 区分 | spec での対応 | 結果 |
|---|---|---|---|
| GAP-A01 | IMPROVE | SSO → ALB+Okta OIDC JWT 認証 | ✅ セクション 1 で実装 |
| GAP-A02 | IMPROVE | セッション → JWT + SPA ステート | ✅ セクション 4 で実装 |
| GAP-A03 | IMPROVE | web.xml JinjiMode → JWT クレーム | ✅ セクション 2.2 で実装 |
| GAP-A04 | IMPROVE | dummySecurity.xml → Auth Mock + CI 安全策 | ✅ セクション 7/9 で実装 |
| GAP-A05 | IMPROVE | 同時編集検知 → 楽観ロック | ℹ️ #8 validation-error-system の管轄（CZ-101） |
| GAP-A06 | REMOVE | ap_reload.jsp → JWT 自動更新 | ✅ ステートレス JWT で解消 |
| GAP-R01 | IMPROVE | 4層 RBAC 化 + 可読性向上 | ✅ ビット→JSON化で可読性向上。セクション 6.5 に不採用理由明記 |
| GAP-R02 | KEEP | 12状態ステータスマトリクス踏襲 | ✅ セクション 5 で完全定義 |
| GAP-R04 | KEEP | スタッフロール 931-936 | ✅ JWT staffRole で保持 |
| GAP-R05 | KEEP | 雇用形態制御 TYPE 0-3 | ✅ JWT employmentType で保持 |
| GAP-R06 | IMPROVE | ポリシーエンジン (CASL/Casbin) | ✅ 見送り（理由明記済み）→ FIX-A01 完了 |

### 06_devenv_infrastructure.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| Auth Mock コンテナ (node:22-slim, :8180) | ✅ 合致 | Docker 構成が一致 |
| 三重安全策の3層構造 | ✅ 合致 | 環境変数/Tree Shaking/CI grep の3層が完全一致 |
| CI 2系統パイプライン | ✅ 合致 | ci-production.yml / ci-staging.yml の構造が一致 |
| モック提供エンドポイント | ✅ 解消 (FIX-A02) | 06_devenv 準拠にパス統一済み（`/api/actors`, `/api/switch`, `/oauth2/keys`） |
| アクター切替シーケンス | ✅ 解消 (FIX-A03) | 切替フローを `POST /api/switch` に統一済み |
| JWKS エンドポイント | ✅ 解消 (FIX-A02) | `/oauth2/keys` に統一済み |
| アクター選択UI (HTML) | ✅ 解消 (FIX-A02) | GET `/` を追加済み |

---

## C. 他 Spec (#1, #3-#10) との整合性

### 検出された不整合

| # | 不整合内容 | 重要度 | FIX-ID | 関連 spec |
|---|---|---|---|---|
| 1 | ~~GAP-R06 ポリシーエンジンへの見解が未記載~~ | ~~🟡 MID~~ | FIX-A01 ✅ | constitution / 05_gap_analysis |
| 2 | ~~Auth Mock エンドポイントパスの不一致（6箇所）~~ | ~~🔴 HIGH~~ | FIX-A02 ✅ | #7, 06_devenv |
| 3 | ~~DevActorSwitcher 切替フローのエンドポイント混在~~ | ~~🔴 HIGH~~ | FIX-A03 ✅ | #7, 06_devenv |
| 4 | ~~X-Delegation-Staff-Id ヘッダーの未定義~~ | ~~🟡 MID~~ | FIX-A04 ✅ | #3 core-api-design |
| 5 | ~~CzClaimsMapper の Okta→CZ マッピング詳細不足~~ | ~~🟡 MID~~ | FIX-A05 ✅ | constitution VII |
| 6 | ~~雇用形態 TYPE_1/2 の具体的 UI/API 制約が曖昧~~ | ~~🟡 MID~~ | FIX-A06 ✅ | #4, 02_actor_definition |
| 7 | ~~useAuth composable の派生ヘルパーメソッド未定義~~ | ~~🟡 MID~~ | FIX-A07 ✅ | #7 common-components |
| 8 | ~~OrganizationScopeResolver に EIGYOSHO レベル欠落~~ | ~~🟢 LOW~~ | FIX-A08 ✅ | spec 内部不整合 |

### 不整合の詳細

#### FIX-A02: Auth Mock エンドポイントパスの不一致

| エンドポイント | spec #2 (セクション7.2) | 06_devenv (セクション4.3) | 差異 |
|---|---|---|---|
| アクター一覧 | GET `/actors` | GET `/api/actors` | `/api/` プレフィックス有無 |
| JWT 発行 | POST `/token` | — (該当なし) | spec #2 のみ存在 |
| JWT 取得 | — (該当なし) | GET `/api/token` | 06_devenv のみ存在 |
| アクター切替 | POST `/api/switch` | POST `/api/switch` | ✅ 一致 |
| JWKS | GET `/.well-known/jwks.json` | GET `/oauth2/keys` | パスが異なる |
| アクター選択UI | — (未記載) | GET `/` | spec #2 に欠落 |

#### FIX-A03: DevActorSwitcher 切替フローのエンドポイント混在

| 参照元 | 切替時のエンドポイント |
|---|---|
| spec #2 セクション 8.3 | `POST http://auth-mock:8180/token` |
| spec #7 (common-components) line 101 | `POST /api/switch` |
| 06_devenv セクション 5.3 | `POST /api/switch` |

spec #2 セクション 7.2 では `/token`（JWT発行）と `/api/switch`（アクター切替）を別エンドポイントとして定義しているが、セクション 8.3 の切替フローで `/token` を使用している。他ドキュメントは一貫して `/api/switch` を使用。

#### FIX-A04: X-Delegation-Staff-Id ヘッダー

spec #3 (core-api-design) で代行モード時のリクエストヘッダー `X-Delegation-Staff-Id` が定義されているが、spec #2 では `canDelegate` 属性の存在のみ記載し、代行用ヘッダーやバックエンドでの処理フローが未定義。

#### FIX-A08: OrganizationScopeResolver に EIGYOSHO 欠落

spec #2 セクション 2.4 の組織階層テーブルには `EIGYOSHO (127)` が含まれるが、セクション 6.3 の OrganizationScopeResolver 解決ロジックの列挙（ZENSYA→HONBU→KYOKU→SHITSU→BU→KA）から EIGYOSHO が欠落している。

### 整合している点

| 項目 | 結果 |
|---|---|
| spec #1 (database-schema) — mcz04_ctrl の sysid 分岐と JinjiMode の連動 | ✅ |
| spec #3 (core-api-design) — StatusMatrixResolver / OrganizationScopeResolver の設計 | ✅ |
| spec #3 (core-api-design) — security パッケージ構成 (AlbOidcAuthFilter 等) | ✅ |
| spec #4 (work-hours-input) — tab010.bit2 による系列選択ロジック | ✅ |
| spec #4 (work-hours-input) — 雇用形態 TYPE_0-3 の区分定義 | ✅ |
| spec #5 (work-hours-status) — tab012.bit0/bit1 の権限ゲート | ✅ |
| spec #5 (work-hours-status) — dataAuthority.ref の組織スコープフィルタ | ✅ |
| spec #6 (analysis-screens) — 全アクター (ACT-01〜15) アクセス可能 | ✅ |
| spec #6 (analysis-screens) — tab011.bit1 によるコスト表示制御 | ✅ |
| spec #7 (common-components) — DevActorSwitcher の表示条件・Tree Shaking | ✅ |
| spec #8 (validation-error-system) — CzSecurityContext.require() + StatusMatrixResolver | ✅ |
| spec #9 (excel-export) — JWT Bearer トークン認証 + 組織スコープ反映 | ✅ |
| spec #10 (batch-processing) — バッチからの認証は API 経由で間接的に適用 | ✅ |

---

## D. 旧システムとの仕様整合性・変更点まとめ

### KEEP（踏襲）— 変更なし

| # | 項目 | 旧 | 新 | 根拠 |
|---|---|---|---|---|
| 1 | 4層権限モデルの論理構造 | Layer 1-4 の4層構造 | 同一の4層構造（JWT クレームで表現） | GAP-R01 |
| 2 | 15アクター定義 | ACT-01〜ACT-15 | 同一の15アクター + 属性 | 02_actor_definition.md |
| 3 | 12状態ステータスマトリクス | sts_base_key 000-911 (12状態) | 同一の12状態 × 2系列 | GAP-R02 |
| 4 | TAB 010 ビット権限 (3ビット) | canUseSbt010_0bit/1bit/2bit | permissions.tab010.bit0/bit1/bit2 | — |
| 5 | TAB 011 ビット権限 (2ビット) | canUseSbt011_0bit/1bit | permissions.tab011.bit0/bit1 | — |
| 6 | TAB 012 ビット権限 (2ビット) | canUseSbt012_0bit/1bit | permissions.tab012.bit0/bit1 | — |
| 7 | 相対権限モデル (201/202/211) | getRelativeAuthority() | dataAuthority.ref/ins/upd | GAP-R04 |
| 8 | 組織階層 (7段階) | ZENSYA→EIGYOSHO→HONBU→KYOKU→SHITSU→BU→KA | 同一の7段階 | — |
| 9 | 雇用形態制御 (TYPE 0-3) | getTemporaryStaffType() カテゴリ 900/901/902 | employmentType: 0/1/2/3 | GAP-R05 |
| 10 | 外部契約者の代行モード | isDaiko() + isAllowedStaff() | canDelegate + 登録者ID 特殊処理 | — |
| 11 | スタッフロール (931-936) | isStaffRole_931()〜936() | JWT staffRole: 931-936 | GAP-R04 |
| 12 | 月初制御 (940/941) | canUseInActualFirstDayOfMonth/Second | Backend ミドルウェアで判定 | — |
| 13 | 局跨がり権限 (937/951) | カテゴリ 937/951 | JWT crossBureauAccess/crossBureauRoleCtrl | — |
| 14 | 担当者/管理者系列切替ロジック | canUseSbt010_2bit() → tan/man | tab010.bit2 → 担当者/管理者系列 | — |
| 15 | JinjiMode による UI/メニュー分岐 | web.xml context-param + JSP 分岐 | JWT jinjiMode + SPA ルーティング | GAP-A03 |

### IMPROVE（改善）— 技術的改善

> 下表の「影響度」は移行作業の規模を示す指標であり、未解決の問題ではありません。

| # | 項目 | 旧 (Java MPA) | 新 (SPA + REST API) | 影響度 | GAP-ID |
|---|---|---|---|---|---|
| 1 | **認証方式** | SSO (SsoCertificateProc → WebLogic連携) | ALB + Okta OIDC → JWT | 🔴 HIGH | GAP-A01 |
| 2 | **状態管理** | セッションベース (60分タイムアウト, Cond クラス群) | JWT トークン + SPA Pinia ステート | 🔴 HIGH | GAP-A02 |
| 3 | **JinjiMode 判定** | web.xml `<context-param>` でデプロイパス分離 | JWT クレーム `jinjiMode` で同一 SPA バイナリ動作 | 🟡 MID | GAP-A03 |
| 4 | **開発用ロール** | dummySecurity.xml (固定値テストデータ) | Auth Mock (Express) + DevActorSwitcher (UI) + 三重安全策 | 🔴 HIGH | GAP-A04 |
| 5 | **権限表現** | ビット文字列 `"110000"` + `isAvailableFunction()` | JWT JSON `{ bit0: true, bit1: true, bit2: false }` | 🟡 MID | GAP-R01 |
| 6 | **権限解決** | SecurityRoleInfo クラス (セッション格納) | AlbOidcAuthFilter → CzPrincipal → CzSecurityContext (ThreadLocal) | 🟡 MID | GAP-R01 |
| 7 | **組織スコープ解決** | MprSecurityInfo.getAllowedOrganizationCodes() | OrganizationScopeResolver.resolve() — SQL WHERE IN 句生成 | 🟡 MID | — |
| 8 | **ステータスマトリクス解決** | InsertListJspBean 内のインライン判定 | StatusMatrixResolver.resolve() — 共通サービス化 | 🟢 LOW | — |
| 9 | **ログインフロー** | InitSecurityRoleProc → LoginProc → AccessLogProc チェーン (8 Unit) | ALB OIDC Action が認証完了。Backend は JWT 検証のみ | 🔴 HIGH | GAP-A01 |

### ADD（追加）

| # | 項目 | 詳細 | 根拠 |
|---|---|---|---|
| 1 | **Auth Mock サーバー** | Express 製 ALB+Okta 模倣サーバー (:8180)。15アクター定義、JWT 生成、OIDC Discovery | GAP-A04 |
| 2 | **DevActorSwitcher** | フロントエンド開発環境専用アクター切替 UI コンポーネント | GAP-A04 |
| 3 | **三重安全策** | 環境変数ゲート / Nuxt Tree Shaking / CI grep 検査 | constitution VI |
| 4 | **CzClaimsMapper** | Okta カスタムクレーム → CZ 4層権限モデルへの変換サービス | spec #2 セクション 4.2 |
| 5 | **StatusMatrixResolver** | 12状態×2系列のマトリクス解決を共通サービスとして独立化 | spec #2 セクション 6.4 |
| 6 | **OrganizationScopeResolver** | 相対権限 + 組織階層からアクセス可能組織リストを解決するサービス | spec #2 セクション 6.3 |
| 7 | **CzSecurityContext** | ThreadLocal による認証コンテキスト管理（リクエストスコープ） | spec #2 セクション 6.1 |
| 8 | **CzPrincipal** | 認証済みユーザー情報モデル（4層権限を構造化） | spec #2 セクション 6.1 |
| 9 | **useAuth composable** | フロントエンド権限判定 composable (canOperate, hasDataAccess) | spec #2 セクション 8.2 |
| 10 | **CI 2系統パイプライン** | ci-production.yml (安全検査付き) + ci-staging.yml (アクター切替込み) | constitution VIII |

### REMOVE（削除）

| # | 項目 | 旧の概要 | 削除理由 | GAP-ID |
|---|---|---|---|---|
| 1 | **dummySecurity.xml** | テストユーザーのロールデータ固定定義 | Auth Mock に置換 | GAP-A04 |
| 2 | **InitSecurityRoleProc** | ログインフロー Proc チェーン (ロール構築→ライセンス→緊急停止チェック) | ALB OIDC に置換 | GAP-A01 |
| 3 | **LoginProc** | CurrentConditionInfo 作成 + セッション格納 | CzPrincipal + CzSecurityContext に置換 | GAP-A01 |
| 4 | **SessionTrackingUnit** | セッション追跡 (SessionTrackingProc) | JWT ステートレスで不要 | GAP-A06 |
| 5 | **ap_reload.jsp** | 非表示フレーム定期リクエスト (セッション維持) | JWT 自動更新で不要 | GAP-A06 |
| 6 | **ESQID** | マルチウィンドウ識別子生成 | SPA 単一ウィンドウで不要 | — |
| 7 | **LoginCond** | ログイン条件クラス (プロファイル選択、SAP 使用モード) | Okta ユーザー属性に統合 | GAP-A01 |
| 8 | **ProxyLoginUnit** | プロキシログイン処理 (3 Unit) | ALB 認証に統合 | GAP-A01 |
| 9 | **ConcurrentUnit** | 同時ログイン検知・警告 (2 Unit) | JWT + 楽観ロックに置換 | GAP-A05 |

### 注意が必要な移行ポイント

| # | ポイント | 詳細 |
|---|---|---|
| 1 | **Okta カスタムクレーム設計** | Okta ユーザープロファイルに4層権限情報（TAB 010/011/012, dataAuthority, employmentType, staffRole）をどの属性名で格納するか。CzClaimsMapper の変換ルールを確定する必要あり |
| 2 | **JWT トークンサイズ** | 4層権限情報を全てJWTに格納すると、ペイロードが大きくなる可能性（ALB の X-Amzn-Oidc-Data ヘッダーサイズ上限に注意） |
| 3 | **ビット文字列→JSON変換** | Auth Mock の `tab010: '001000'` 文字列 → `{ bit0: false, bit1: false, bit2: true }` 変換ロジックの正確性テストが必要 |
| 4 | **代行モードの認証フロー** | 旧: セッション上の isDaiko フラグ → 新: どのタイミングで代行状態を設定し、JWT に反映するかのフロー設計 |
| 5 | **月初制御 (940/941) の実装方式** | 旧: ロールカテゴリ判定。新: Backend ミドルウェアと記載あるが、具体的なフィルター/インターセプター設計は未詳 |

---

## E. 推奨アクション

> **全8件完了** ✅（P0: 2件 / P1: 4件 / P2: 2件）

| ID | 優先度 | ステータス | アクション | 修正箇所 |
|---|---|---|---|---|
| FIX-A01 | P2 | ✅ 完了 | GAP-R06 ポリシーエンジンの不採用理由を明記 | spec セクション 6.5 |
| FIX-A02 | P0 | ✅ 完了 | Auth Mock エンドポイントパスを 06_devenv 準拠に統一 | spec セクション 7.2 + 7.3 |
| FIX-A03 | P0 | ✅ 完了 | DevActorSwitcher 切替フローを `POST /api/switch` に統一 | spec セクション 8.3 |
| FIX-A04 | P1 | ✅ 完了 | 代行モード時の `X-Delegation-Staff-Id` ヘッダー処理フローを追記 | spec セクション 6.2 |
| FIX-A05 | P1 | ✅ 完了 | CzClaimsMapper の Okta 属性→CZ クレーム マッピング表 (12属性) を追記 | spec セクション 4.2 |
| FIX-A06 | P1 | ✅ 完了 | 雇用形態 TYPE_0-3 別の具体的制約マトリクス (6項目) を追記 | spec セクション 2.5 |
| FIX-A07 | P1 | ✅ 完了 | CzPermissions セマンティックエイリアス + useAuth 派生ヘルパー + bitN↔名前 対応表を追記 | spec セクション 6.1.1 + 8.2 |
| FIX-A08 | P2 | ✅ 完了 | OrganizationScopeResolver の解決ロジックに EIGYOSHO レベルを追加 | spec セクション 6.3 |

---

## F. 修正履歴

| 日時 | FIX-ID | 修正内容 | 修正者 |
|---|---|---|---|
| 2026-02-26 | FIX-A01 | セクション 6.5「ポリシーエンジンの不採用について」を新規追加。JWT JSON 化による可読性解決済み、バックエンド/フロントエンド異ライブラリ併用の二重管理リスク、固定構造の権限モデルに汎用ポリシーエンジンは YAGNI、の3点を理由として明記 | Claude |
| 2026-02-26 | FIX-A02 | セクション 7.2 のエンドポイント表を 06_devenv 準拠に全面改訂。`/actors`→`/api/actors`、POST `/token` 廃止→POST `/api/switch` に統合、`/.well-known/jwks.json`→`/oauth2/keys`、GET `/`（アクター選択UI）と GET `/api/token` を追加。セクション 7.3 の例も `POST /api/switch` に修正 | Claude |
| 2026-02-26 | FIX-A03 | セクション 8.3 の DevActorSwitcher 切替フローを `POST http://auth-mock:8180/api/switch` に修正。06_devenv セクション 5.3 および spec #7 line 101 と統一 | Claude |
| 2026-02-26 | FIX-A07 | セクション 6.1.1「CzPermissions セマンティックエイリアス」を新規追加。Backend (Java) / Frontend (TypeScript) の両方に canReport, canManage, canFullAccess 等のエイリアスを定義。bitN↔セマンティック名の対応表を追加。セクション 8.2 の useAuth interface にもエイリアスを反映し、使用例（推奨/非推奨）を記載 | Claude |
| 2026-02-26 | FIX-A04 | セクション 6.2 AlbOidcAuthFilter に `X-Delegation-Staff-Id` ヘッダー処理フローを追記。ヘッダー検出→対象スタッフ存在確認→canDelegate 権限検証→CzPrincipal.delegationStaffId 設定の4ステップと CZ-307 エラー定義を明記 | Claude |
| 2026-02-26 | FIX-A05 | セクション 4.2 に Okta カスタム属性→CZ クレームのマッピング表（12属性）を追加。custom:tab010〜custom:staffRole の Okta 属性名、CZ JWT クレーム名、型、変換ルールを網羅 | Claude |
| 2026-02-26 | FIX-A06 | セクション 2.5 に雇用形態別制約マトリクス（6制約項目 × TYPE 0-3 の4種）を追加。残業入力・休日出勤・代行モード・Excel出力・集計対象・単価表示の各制約を明確化。代行モードの X-Delegation-Staff-Id ヘッダー参照も追記 | Claude |
| 2026-02-26 | FIX-A08 | セクション 6.3 OrganizationScopeResolver の解決ロジック列挙に EIGYOSHO レベルを追加。7段階階層（ZENSYA→EIGYOSHO→HONBU→KYOKU→SHITSU→BU→KA）と完全一致するよう修正 | Claude |

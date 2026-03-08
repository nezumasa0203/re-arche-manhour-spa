# #7 Common Components — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `07-common-components/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1-#6/#8-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| Nuxt.js 3 SPA | ✅ 合致 | constitution 技術スタック「Nuxt.js 3」。spec: layouts/default.vue + NuxtPage 構成 |
| PrimeVue UI | ✅ 合致 | constitution 技術スタック「PrimeVue」。spec: Button, Tree, DataTable, Dialog, Dropdown, Toast, Menu, Paginator 等を全面採用 |
| UX-First | ✅ 合致 | constitution V「操作フィードバック」。spec: MessageToast (success/warn/error/info)、ConfirmDialog、StatusBadge 色分け |
| レスポンシブ対応 | ✅ 合致 | constitution V「両対応」。spec: セクション 3.4 で 3 ブレークポイント定義（1280px/960px/960px未満） |
| フラットデザイン | ✅ 合致 | constitution V「影なし・border 基調」。spec: セクション 15.1 で `--cz-border-color` 定義、セクション 15.2 で Lara プリセット（フラット基調）+ ダークモード無効 |
| TDD | ✅ 合致 | constitution IV。spec: セクション 16 で Vitest + Playwright テスト要件定義（168 パターンのステータスマトリクステスト含む） |
| Docker-First | ✅ 合致 | API 呼出先が Backend コンテナ上の REST API。auth-mock:8180 連携 |
| 12状態ステータスマトリクス | ✅ 合致 | constitution I。spec: セクション 13.1 で Frontend 用 statusMatrix.ts を定義、Backend StatusMatrixResolver と同一ロジック |
| 4層権限モデル | ✅ 合致 | constitution VII。spec: セクション 3.2 で権限ベースのメニュー表示制御 |
| エラーメッセージ体系 | ✅ 合致 | constitution I「CZ-000〜CZ-999」。spec: セクション 11.2 でコード範囲別 Toast severity 定義、セクション 7.3 で確認ダイアログメッセージ一覧 |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| FRAMESET 入れ子構造（40+ フレーム） | ヘッダー/ナビ/メイン/フッター | CSS Grid 単一レイアウト + SideNav | ✅ IMPROVE (GAP-N01) |
| メインメニュー画面 | 専用メニュー画面 | SPA サイドナビに統合 | ✅ IMPROVE (GAP-N02) |
| window.open() ポップアップ (DLG_001-005) | 別ウィンドウ表示 | PrimeVue Dialog モーダルに移行 | ✅ IMPROVE (GAP-D01〜D05) |
| パンくずリスト | なし（フレーム構成で不要） | AppBreadcrumb.vue 追加 | ✅ ADD (GAP-N05) |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| CzPermissions セマンティックエイリアス | ⚠️ 名称不一致 | セクション 3.2 で `canManageReports`, `canNavigateBetweenForms`, `canFullManage` を使用。spec #2 定義は `canManage`, `canNavigateForms`, `canFullAccess` → **FIX-C01** |
| DevActorSwitcher 表示条件 | ⚠️ 環境変数不一致 | セクション 2.3 で `process.env.NODE_ENV === 'development'` を使用。spec #2 セクション 8.1/9 では `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH === 'true'` + 三重安全策 → **FIX-C02** |
| 15 アクター切替 | ✅ 合致 | セクション 2.3 で 15 アクター Dropdown 切替 |
| ステータスマトリクス 12状態×2系列 | ✅ 合致 | セクション 13.1 で `resolveStatusMatrix(statusKey, isTanSeries)` |
| 代行モード | ✅ 合致 | セクション 6.3 で `purpose === 'delegation'` → `/delegation/available-staff` |
| 組織階層 | ✅ 合致 | セクション 4.3 で `scopeFilter` による組織スコープ制限 |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 件名バイト長制限 128 バイト | ✅ 合致 | セクション 12.1 `calculateByteLength` で全角2/半角1/半角カタカナ2 のバイト長計算 |
| 時間入力 HH:MM 15分単位 | ✅ 合致 | セクション 9.2/9.3 で BR-006 入力補助 + 15分単位バリデーション (CZ-147) |
| 確認ダイアログ CZ-5xx | ✅ 合致 | セクション 7.3 で CZ-505/506/507/508/516/518/802/803/804 の一覧定義 |

### 04_screen_transition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| メニュー → 各画面遷移 | ✅ 合致 | セクション 3.2 SideNav メニューで FORM_010/020/030-042 へのルーティング |
| ダイアログ (DLG_001-005) | ✅ 合致 | 5 ダイアログコンポーネントが遷移図のモーダル定義に対応 |

### 05_gap_analysis.md との整合

| 項目 | GAP ID | 結果 | 詳細 |
|---|---|---|---|
| FRAMESET → CSS Grid | GAP-N01 | ✅ 合致 | セクション 1.1 で CSS Grid + 56px ヘッダー + 220px/56px SideNav |
| メインメニュー → SideNav | GAP-N02 | ✅ 合致 | セクション 3 で折畳/展開/オーバーレイ対応の SideNav |
| パンくずリスト追加 | GAP-N05 | ✅ 合致 | セクション 1.2 で AppBreadcrumb.vue 配置 |
| window.open → モーダル | GAP-D01〜D05 | ✅ 合致 | セクション 4-6 で PrimeVue Dialog 実装 |

### 06_devenv_infrastructure.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| Auth Mock ポート 8180 | ✅ 合致 | セクション 2.3 切替時の API は Auth Mock 経由 |
| 三重安全策 | ⚠️ 不一致 | → **FIX-C02** で対応 |

---

## C. 他 Spec との整合性

### spec #1 (database-schema) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 組織テーブル mcz12_orgn_kr | ✅ 合致 | セクション 4.4 で組織ツリー API が階層構造返却 |
| SS テーブル mcz13 系 | ✅ 合致 | セクション 5.4 で SS 一覧 API が systemNo/subsystemNo を返却 |

### spec #2 (auth-infrastructure) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| CzPrincipal.userName | ✅ 合致 | セクション 2.2 ヘッダーでユーザー名表示 |
| jinjiMode バッジ | ✅ 合致 | セクション 2.2 でモードバッジ（人事:青 / 管理:緑） |
| 代行バッジ | ✅ 合致 | セクション 2.2 で "代行中: {対象者名}" 表示 |
| CzPermissions エイリアス | ⚠️ 名称不一致 | → **FIX-C01** |
| DevActorSwitcher 表示条件 | ⚠️ 不一致 | → **FIX-C02** |
| JWT 自動付与 | ✅ 合致 | セクション 14.2 で Authorization: Bearer ヘッダー付与 |
| X-Delegation-Staff-Id | ✅ 合致 | セクション 14.2 で代行モード時のヘッダー付与 |
| StatusMatrixResolver | ✅ 合致 | セクション 13.1 で Frontend 版を定義。12状態 × 2系列のロジック |

### spec #3 (core-api-design) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| GET /masters/organizations/tree | ✅ 合致 | セクション 4.4 で呼出 |
| GET /masters/subsystems | ✅ 合致 | セクション 5.4 で呼出（keyword, page, pageSize パラメータ） |
| GET /masters/staff | ✅ 合致 | セクション 6.4 で呼出（name, matchType パラメータ） |
| GET /delegation/available-staff | ✅ 合致 | セクション 6.4 で呼出 |
| 楽観的ロック 409 応答 | ✅ 合致 | セクション 14.3 で 409 → warn Toast + リフレッシュ |
| エラーレスポンス構造 | ✅ 合致 | セクション 14.3 で `error.data.error.message` / `.code` を参照 |

### spec #4 (work-hours-input) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| HoursInput 共用 | ✅ 合致 | セクション 9.1 で FORM_010 + FORM_020 共用 |
| MonthSelector 共用 | ✅ 合致 | セクション 10.1 で FORM_010 + FORM_020 共用 |
| StatusBadge 色定義 | ✅ 合致 | セクション 8.2 の色が spec #4 のステータスセル色と一致 |
| CZ-505/506/518 確認メッセージ | ✅ 合致 | セクション 7.3 で FORM_010 用メッセージ定義 |

### spec #5 (work-status-list) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| CZ-507/508 確認メッセージ | ✅ 合致 | セクション 7.3 で FORM_020 用承認/承認取消メッセージ |
| CZ-802/803/804 月次制御 | ✅ 合致 | セクション 7.3 で未確認/確認/集約の確認メッセージ |
| OrganizationSearchDialog | ✅ 合致 | FORM_020 の組織フィルタで使用 |

### spec #6 (analysis-screens) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| OrganizationSearchDialog | ✅ 合致 | FORM_030-042 の組織選択で使用 |
| MonthSelector | ✅ 合致 | 分析画面の期間選択で使用 |

---

## D. レガシーシステムとの差異分類

### KEEP（踏襲）

| 項目 | 現行 | 新システム |
|---|---|---|
| ステータス色分け（4色） | Java 定数定義 | CSS カスタムプロパティ `--cz-status-0〜9` |
| HH:MM 入力補助 | JavaScript 自動変換 | TypeScript parseHours/formatHours |
| 15分単位バリデーション | サーバーサイドチェック | フロント + バックエンド双方 |
| バイト長計算（全角2/半角1/半角カタカナ2） | Java バイト計算 | TypeScript calculateByteLength |
| 12状態ステータスマトリクス | Java 定数配列 | TypeScript resolveStatusMatrix + Java StatusMatrixResolver |
| 確認ダイアログ CZ-5xx 系 | window.confirm() + サーバー | PrimeVue Dialog + 統一メッセージ体系 |
| 組織ツリー検索 | window.open() ポップアップ | PrimeVue Tree モーダル |
| SS 選択 | window.open() ポップアップ | PrimeVue DataTable モーダル |
| 担当者選択（組織ツリー + 検索） | window.open() ポップアップ | PrimeVue タブ付きモーダル |

### IMPROVE（改善）

| 項目 | 現行 | 新システム | GAP ID |
|---|---|---|---|
| 画面レイアウト | FRAMESET 40+ フレーム | CSS Grid 2カラム | GAP-N01 |
| ナビゲーション | メインメニュー専用画面 | SideNav 常時表示 + 折畳/レスポンシブ | GAP-N02 |
| パンくずリスト | なし | AppBreadcrumb.vue | GAP-N05 |
| モーダルダイアログ | window.open() 別ウィンドウ | PrimeVue Dialog インライン | GAP-D01〜D05 |
| エラー通知 | alert() + 画面遷移 | PrimeVue Toast + severity 別自動消去 | — |
| テーマ管理 | CSS 直書き | CSS カスタムプロパティ + PrimeVue Lara プリセット | — |
| 組織検索 | 全件表示のみ | インクリメンタルサーチ（300ms デバウンス） | — |
| SS 検索 | 全件表示のみ | キーワード検索 + ページネーション（50件/ページ） | — |

### ADD（新規追加）

| 項目 | 説明 |
|---|---|
| DevActorSwitcher | 開発環境での 15 アクター切替 UI（三重安全策付き） |
| API クライアント共通 (useApi) | JWT 自動付与 + エラーインターセプター + ローディング管理 |
| SideNav レスポンシブ動作 | 3段階（展開/折畳/オーバーレイ）+ localStorage 保存 |
| MessageToast composable | CZ コード連携の統一通知フレームワーク |

### REMOVE（廃止）

| 項目 | 理由 |
|---|---|
| FRAMESET 入れ子構造 | SPA 化により不要 |
| window.open() ポップアップ | PrimeVue Dialog に置換 |
| メインメニュー専用画面 | SideNav に統合 |

---

## E. 推奨アクション

### FIX-C01: SideNav メニュー定義のセマンティックエイリアス名不一致 (P1)

**箇所**: セクション 3.2 メニュー定義テーブル

**問題**: SideNav のメニュー表示条件で使用しているセマンティックエイリアス名が
spec #2 セクション 6.1.1 の正式定義と一致していない。

| メニュー | spec #7 現行 | spec #2 正式名 | 対応ビット |
|---|---|---|---|
| 工数管理 | `canManageReports` | `canManage` | tab010.bit1 |
| 分析 | `canNavigateBetweenForms` | `canNavigateForms` | tab011.bit1 |
| 設定 | `canFullManage` (tab010.bit0+bit1+bit2) | `canFullAccess` | tab010.bit2 |

**設定メニューの追加問題**: `canFullManage (tab010.bit0+bit1+bit2)` は3ビットの AND 条件を
示唆するが、ACT-03（全権管理者）は tab010='001000'（bit2=1, bit0=0, bit1=0）であり
AND 条件を満たさない。セクション 2.2 の「設定ページ遷移（全権管理者のみ）」と
整合させるなら `canFullAccess` (tab010.bit2) が正しい。

**修正**: 3項目のエイリアス名を spec #2 定義に統一する。

---

### FIX-C02: DevActorSwitcher の表示条件が三重安全策と不一致 (P1)

**箇所**: セクション 2.3 DevActorSwitcher

**問題**: `process.env.NODE_ENV === 'development'` を表示条件としているが、
spec #2 セクション 8.1 では `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH === 'true'` が正式条件。

**spec #2 セクション 9 の三重安全策**:
1. 環境変数ゲート: `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH=true`
2. Nuxt.js ビルド時 Tree Shaking: `config.public.enableActorSwitch !== 'true'` で除外
3. CI パイプライン: ビルド成果物から `ActorSwitch` / `ENABLE_ACTOR_SWITCH` を検出したら FATAL

`process.env.NODE_ENV` を使用すると:
- 三重安全策の環境変数ゲート（策1）がバイパスされる
- Tree Shaking（策2）が機能しない（NODE_ENV は常に存在する）
- CI 検出（策3）の対象パターンと一致しない

**修正**: `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH === 'true'` に変更する。

---

## F. 変更履歴

| 日付 | FIX ID | 内容 | 対象ファイル | 状態 |
|---|---|---|---|---|
| 2026-02-26 | FIX-C01 | SideNav セマンティックエイリアス名修正（canManageReports→canManage, canNavigateBetweenForms→canNavigateForms, canFullManage→canFullAccess） | 07-common-components/spec.md | ✅ 完了 |
| 2026-02-26 | FIX-C02 | DevActorSwitcher 表示条件修正（process.env.NODE_ENV→NUXT_PUBLIC_ENABLE_ACTOR_SWITCH） | 07-common-components/spec.md | ✅ 完了 |
| 2026-02-26 | FIX-V02 | 確認ダイアログコード修正（CZ-802/803/804→CZ-509/510/511）— #8 整合性チェックで検出 | 07-common-components/spec.md | ✅ 完了 |

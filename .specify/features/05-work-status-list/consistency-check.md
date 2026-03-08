# #5 Work Status List — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `05-work-status-list/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1-#4/#6-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| Nuxt.js 3 SPA | ✅ 合致 | constitution 技術スタック「Nuxt.js 3」。spec: pages/work-status.vue |
| PrimeVue UI | ✅ 合致 | constitution 技術スタック「PrimeVue」。spec: DataTable, Column, Paginator, Dropdown, Button, Checkbox, InputText, Toast 使用 |
| UX-First | ✅ 合致 | constitution V「操作フィードバック」。spec: Toast エラー通知、確認ダイアログ CZ-507/508/802/803/804、ローディング状態 |
| レスポンシブ対応 | ✅ 合致 | constitution V「両対応」。spec: セクション 8 で 3 ブレークポイント定義（1280px/960px） |
| フラットデザイン | ✅ 合致 | constitution V「影なし・border 基調」。spec: ステータスバッジ色分け #FBFBB6/#BDEAAD/#9DBDFE、MonthlyControlBar 色もフラットカラー |
| TDD | ✅ 合致 | constitution IV。spec: セクション 10 で Vitest + Playwright テスト要件定義 |
| Docker-First | ✅ 合致 | API 呼出先が Backend コンテナ上の REST API |
| 12状態ステータスマトリクス | ✅ 合致 | constitution I。spec: セクション 6.2 で管理者系列マトリクス (12状態) 全パターン定義 |
| 4層権限モデル | ✅ 合致 | constitution VII。spec: セクション 6.1 でアクター別 UI 差異定義、6.3 で DataAccessScope 定義 |
| デザインスタイル(色分け) | ✅ 合致 | constitution V「ソフトカラー」。STATUS_0 黄、STATUS_1 緑、STATUS_2 青の3色統一 |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| FRAMESET 左右フレーム (head_l/r + body_l/r) | 左固定列 + 右スクロール | CSS sticky + PrimeVue frozen | ✅ IMPROVE (GAP-F20-02) |
| 手動 JS 同期 | head_l/body_l フレーム同期 | 単一 DataTable コンポーネント | ✅ IMPROVE |
| 管理者画面 FORM_020 | 配下担当者の一覧閲覧・承認 | 同一の管理者ワークフロー | ✅ KEEP |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| FORM_020 の主要アクター | ✅ 合致 | ACT-02 (報告管理者)、ACT-04 (管理モードユーザー)、ACT-05 (人事モードユーザー) |
| 15 アクター対応 | ✅ 合致 | セクション 6.1 で ACT-02/03/04/10/13 の UI 差異定義 |
| 管理者系列 (man) 固定 | ✅ 合致 | FORM_020 は管理モード画面のため、常に管理者系列 (`StatusMatrixResolver.resolve(key, false)`) |
| TAB 012 セマンティックエイリアス | ⚠️ 未適用 | セクション 4.2/5.2/5.3/5.4/6.1/7.4/10.1 で `tab012.bit0`/`tab012.bit1` を直接参照 → **FIX-S01** |
| 組織階層レベル | ⚠️ 不一致 | セクション 6.3 の BUSYO/JIBUN が spec #2 定義の 7 階層に不在 → **FIX-S04** |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| US-020〜US-027 (8件) | ✅ 合致 | セクション 3.2 Actions で全 8 US に対応するアクション定義 |
| 確認ダイアログ CZ-507/508/802/803/804 | ✅ 合致 | セクション 4.2/4.3/5.5/5.6 で全ダイアログ定義 |
| エラーメッセージ CZ-101/106/109/110 | ✅ 合致 | セクション 7.1〜7.4 で全エラーコード参照 |

### 04_screen_transition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| SCR-020 (FORM_020) 画面仕様 | ✅ 合致 | SPA 統合提案の単一ページ化を実現 |
| DLG_001/002 (組織選択) | ✅ 合致 | セクション 2「OrganizationSearchDialog.vue」 |
| DLG_005 (担当者選択) | ✅ 合致 | セクション 2「StaffSearchDialog.vue」 |

### 05_gap_analysis.md との整合

| GAP-ID | 区分 | spec での対応 | 結果 |
|---|---|---|---|
| GAP-F20-01 | KEEP | MonthlyControlBar 月次確認→集約 3段階 | ✅ セクション 4.2 |
| GAP-F20-02 | IMPROVE | CSS sticky + PrimeVue frozen 固定列 | ✅ セクション 4.4 |
| GAP-F20-03 | IMPROVE | Pagination 件数設定可能 (20/50/100/200) | ✅ セクション 4.6 |
| GAP-F20-04 | KEEP | CZ-802/803/804 確認ダイアログ | ✅ セクション 5.2〜5.4 |
| GAP-F20-05 | IMPROVE | HoursEditCell HH:MM 入力マスク + バリデーション | ✅ セクション 4.5 |
| GAP-F20-06 | KEEP | チェックボックス + 承認/戻し一括操作 | ✅ セクション 4.3 |
| GAP-F20-07 | ADD/P2 | ステータス別フィルタ (作成中も表示チェック) | ✅ セクション 4.1 |
| GAP-F20-08 | ADD/P2 | DataTable 列リサイズ (`resizableColumns`) | ✅ P2 スコープ |

### 06_devenv_infrastructure.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| Frontend コンテナ (Nuxt.js 3, :3000) | ✅ 合致 | ページ URL `/work-status` が Frontend コンテナで提供 |
| ホットリロード | ✅ 合致 | 開発時の即時反映が Docker 構成でサポート |
| ORM 記載 | ℹ️ 注意 | 06_devenv の JPA+Hibernate 記載は既知事項。Frontend spec には直接影響なし |

---

## C. 他 Spec (#1-#4, #6-#10) との整合性

### 検出された不整合

| # | 不整合内容 | 重要度 | FIX-ID | 関連 |
|---|---|---|---|---|
| 1 | ~~セクション 4.2/5.2/5.3/5.4/6.1/7.4/10.1 で `tab012.bit0`/`tab012.bit1` を直接参照。spec #2 FIX-A07 定義のセマンティックエイリアス `canInputPeriod`/`canAggregate` に統一すべき~~ | ~~🟡 P1~~ | FIX-S01 ✅ | #2 auth FIX-A07 |
| 2 | ~~セクション 5.4 月次未確認戻しの権限条件「tab012.bit0 === true または両ビット未設定」が spec #3 section 3.20 の `canInputPeriod()` と不整合。また、spec #5 は gjkt_flg と data_sk_flg の両方をリセットするが、spec #3 は gjkt_flg のみリセットし data_sk_flg=0 を前提条件とする~~ | ~~🟡 P1~~ | FIX-S02 ✅ | #3 core-api 3.20 |
| 3 | ~~セクション 4.4 行スタイルで `isSts_man_j_upd` (旧命名) を使用。StatusMatrixResolver の `statusUpdate` / `matrix.update` 用語に統一すべき~~ | ~~🟢 P2~~ | FIX-S03 ✅ | #2 auth 6.4 |
| 4 | ~~セクション 6.3 DataAccessScope で `BUSYO`/`JIBUN` を使用。spec #2 section 2.4 の 7 階層 (ZENSYA/EIGYOSHO/HONBU/KYOKU/SHITSU/BU/KA) と語彙が不整合~~ | ~~🟢 P2~~ | FIX-S04 ✅ | #2 auth 2.4 |

### 不整合の詳細

#### FIX-S01: セマンティックエイリアス未適用（P1 — 用語統一）

spec #2 FIX-A07 で定義済みのセマンティックエイリアス:
- `tab012.bit0` → `canInputPeriod`
- `tab012.bit1` → `canAggregate`

spec #5 での未適用箇所（7箇所）:

| セクション | 該当記述 | 修正後 |
|---|---|---|
| 4.2 ボタン色 (L225) | `tab012.bit0 権限時のみ表示` | `canInputPeriod 権限時のみ表示` |
| 4.2 ボタン色 (L226) | `tab012.bit0 権限時のみ表示` | `canInputPeriod 権限時のみ表示` |
| 4.2 ボタン色 (L227) | `tab012.bit1 権限時のみ表示` | `canAggregate 権限時のみ表示` |
| 5.2 月次確認 (L417) | `tab012.bit0 === true` | `canInputPeriod() === true` |
| 5.3 月次集約 (L435) | `tab012.bit1 === true` | `canAggregate() === true` |
| 6.1 アクター表 (L542-543) | `tab012.bit0` / `tab012.bit1` | `canInputPeriod` / `canAggregate` |
| 7.4 権限不足 (L614) | `tab012.bit1 === false` | `canAggregate() === false` |
| 10.1 テスト (L663) | `tab012.bit0/bit1` | `canInputPeriod/canAggregate` |

注: セクション 3.1 WorkStatusPermissions の `canConfirm`/`canAggregate` コメント内の参照はトレーサビリティ目的の注釈であり、修正不要。セクション 4.2 ボタン定義表の `canConfirm (tab012.bit0)` / `canAggregate (tab012.bit1)` も注釈として許容。

#### FIX-S02: 月次未確認戻しの権限条件・動作不整合（P1 — ロジックバグ）

**権限条件の不整合:**

spec #5 セクション 5.4:
```
権限チェック: tab012.bit0 === true または両ビット未設定
```

spec #3 セクション 3.20:
```
権限チェック: canInputPeriod() が true であること
```

「両ビット未設定」条件の問題:
- ACT-04 (管理モードユーザー) は tab012 = '00'（両ビット未設定）
- この条件では ACT-04 が月次未確認戻しを実行可能になるが、ACT-04 は月次制御権限を持たないため不正
- spec #3 の `canInputPeriod()` のみが正しい条件

**動作仕様の不整合:**

spec #5 セクション 5.4:
```
gjkt_flg = '0', data_sk_flg = '0' に更新  ← 両フラグリセット
```

spec #3 セクション 3.20:
```
3. DataSyuukeiFlg = 0 であること（集約済み状態では未確認戻し不可）
4. GetsujiKakuteiFlg = 0 に更新  ← gjkt_flg のみリセット
```

spec #5 の状態遷移図は集約済(11)→未確認(00) の直接遷移を示す:
```
集約済 (11) ──[未確認]──→ 未確認 (00)
```

しかし spec #3 では:
- DataSyuukeiFlg = 0 が前提条件 → 集約済(11)からの直接遷移を**ブロック**
- GetsujiKakuteiFlg のみリセット → data_sk_flg はそのまま

集約解除専用エンドポイントが未定義のため、spec #5 の「両フラグリセット」が実用的に正しい。
→ **spec #3 section 3.20 を spec #5 に合わせて修正する**

#### FIX-S03: 旧命名 `isSts_man_j_upd`（P2 — 用語統一）

セクション 4.4 行スタイル (L301-303):
```
- STATUS_0: 工数セルは編集不可（管理者判定で isSts_man_j_upd が false の場合）
- STATUS_1: 工数セルは編集可能（isSts_man_j_upd が true）
- STATUS_2: 工数セルは編集可能（isSts_man_j_upd が true）
```

`isSts_man_j_upd` は旧 MPA の `SecurityRoleInfo` メソッド名。
新システムでは `StatusMatrixResolver.resolve(statusKey, false).update` で判定する。
セクション 4.5 HoursEditCell.vue では既に正しく `StatusMatrixResolver` を使用しており、
セクション 4.4 との用語不整合が生じている。

#### FIX-S04: DataAccessScope 語彙の不整合（P2 — 用語統一）

セクション 6.3 DataAccessScope:
```
| BUSYO | ログインユーザーの部署のみ |
| JIBUN | 自分のレコードのみ（FORM_020 では通常なし） |
```

spec #2 セクション 2.4 の組織階層レベル:
```
ZENSYA (255) → EIGYOSHO (127) → HONBU (63) → KYOKU (31) → SHITSU (15) → BU (7) → KA (3)
```

不整合点:
- `BUSYO` は spec #2 の階層に存在しない。最小単位は `KA`（課）
- `JIBUN` は spec #2 の階層に存在しない。雇用形態制限（TYPE_1/2/3）で実現される概念
- `EIGYOSHO`、`SHITSU`、`BU` の 3 レベルが欠落

### 整合している点

| 項目 | 結果 |
|---|---|
| spec #1 (database-schema) — tcz01_hosyu_kousuu テーブルの参照・更新操作が PATCH/POST/approve/revert で整合 | ✅ |
| spec #1 (database-schema) — mcz04_ctrl テーブルの GetsujiKakuteiFlg/DataSyuukeiFlg が MonthlyControl に正確にマッピング | ✅ |
| spec #1 (database-schema) — updatedAt 楽観ロック (CZ-101) がセクション 5.7/7.3 で実装 | ✅ |
| spec #2 (auth-infrastructure) — StatusMatrixResolver の管理者系列 (man) 固定呼出がセクション 4.3/4.5 で整合 | ✅ |
| spec #2 (auth-infrastructure) — OrganizationScopeResolver による組織フィルタがセクション 5.1/6.3 で使用 | ✅ |
| spec #3 (core-api-design) — GET /work-status のクエリパラメータ (yearMonth/organizationCode/staffId/statusFilter/page/pageSize/sort) が整合 | ✅ |
| spec #3 (core-api-design) — PATCH /work-status/{id}/hours のリクエスト/レスポンス構造が整合 | ✅ |
| spec #3 (core-api-design) — POST approve/revert/monthly-confirm/monthly-aggregate のリクエスト構造が整合 | ✅ |
| spec #4 (work-hours-input) — FORM_010 vs FORM_020 の差異（自分CRUD vs 管理者閲覧承認）が明確に定義 | ✅ |
| spec #4 (work-hours-input) — ステータスバッジ色 (#FBFBB6/#BDEAAD/#9DBDFE) が FORM_010/020 で統一 | ✅ |
| spec #7 (common-components) — OrganizationSearchDialog / StaffSearchDialog の共有 | ✅ |
| spec #7 (common-components) — CZ-507/508/802/803/804 確認ダイアログの定義が整合 | ✅ |
| spec #8 (validation-error-system) — HH:MM バリデーション (CZ-125/129/146/147) が HoursEditCell で参照 | ✅ |
| spec #9 (excel-export) — GET /work-status/export/excel が整合 | ✅ |

---

## D. 旧システムとの仕様整合性・変更点まとめ

### KEEP（踏襲）— 変更なし

| # | 項目 | 旧 | 新 | 根拠 |
|---|---|---|---|---|
| 1 | 月次制御 3段階 | 未確認→確認→集約 (gjkt_flg/data_sk_flg) | 同一の 3段階遷移 | GAP-F20-01 |
| 2 | ステータスバッジ色分け | 黄(作成中)/緑(確認)/青(確定) | 同一の 3色 (#FBFBB6/#BDEAAD/#9DBDFE) | — |
| 3 | 承認/戻し一括操作 | チェックボックス選択 → フッターボタン | チェックボックス選択 → Toolbar ボタン | GAP-F20-06 |
| 4 | 月次確認ダイアログ | CZ-802/803/804 確認メッセージ | 同一のメッセージ体系 | GAP-F20-04 |
| 5 | 管理者系列 (man) 固定 | FORM_020 は管理者系列のマトリクスで判定 | StatusMatrixResolver.resolve(key, false) | — |
| 6 | 排他制御 (月次操作) | SELECT FOR UPDATE | 同一の悲観ロック | — |
| 7 | 楽観的ロック (工数編集) | updatedAt 比較 | 同一の楽観ロック (409 Conflict + CZ-101) | — |
| 8 | 検索条件 | 年月 + 組織 + 担当者 | 同一の検索条件 | — |
| 9 | 組織スコープフィルタ | 相対権限による組織限定 | OrganizationScopeResolver で同一ロジック | — |

### IMPROVE（改善）— 技術的改善

> 下表の「影響度」は移行作業の規模を示す指標であり、未解決の問題ではありません。

| # | 項目 | 旧 (Java MPA) | 新 (Nuxt.js 3 SPA) | 影響度 | GAP-ID |
|---|---|---|---|---|---|
| 1 | **画面構成** | FRAMESET 左右分割 (head_l/r + body_l/r) + 手動 JS 同期 | SPA 単一ページ (PrimeVue DataTable + CSS sticky) | 🔴 HIGH | GAP-F20-02 |
| 2 | **固定列実装** | FRAMESET フレーム分離で左固定 | CSS `position:sticky` + PrimeVue `frozen` | 🔴 HIGH | GAP-F20-02 |
| 3 | **データ送受信** | form submit + ActionForward | REST API (GET/POST/PATCH) + JSON | 🔴 HIGH | — |
| 4 | **状態管理** | JSP ビューヘルパー | Pinia Store (workStatus.ts) | 🟡 MID | — |
| 5 | **ページネーション** | 固定 200件/ページ | 可変 20/50/100/200 件 (デフォルト 50) | 🟡 MID | GAP-F20-03 |
| 6 | **インライン工数編集** | TdMask or 独自 Ajax | PrimeVue InputText + PATCH API | 🟡 MID | GAP-F20-05 |
| 7 | **月次制御 UI** | フッター/ヘッダー領域に配置 | MonthlyControlBar 専用コンポーネント | 🟢 LOW | — |

### ADD（追加）

| # | 項目 | 詳細 | 根拠 |
|---|---|---|---|
| 1 | **PrimeVue DataTable** | ヘッダー固定 + frozen 列 + 水平スクロール + 列ソート | UI 刷新 |
| 2 | **Pinia Store** | WorkStatusState 型安全な状態管理 (records, monthlyControl, permissions, UI状態) | SPA 設計 |
| 3 | **MonthlyControlBar** | 3段階ステータス表示 + 色付きボタンの専用コンポーネント | コンポーネント分離 |
| 4 | **HoursEditCell** | HH:MM 入力補助 + 15分単位バリデーション + 自動変換 | UX 改善 |
| 5 | **StatusBadge** | STATUS_0/1/2 の色分け表示コンポーネント | コンポーネント分離 |
| 6 | **ステータスフィルタ** | 「作成中も表示」チェックボックス | UX 改善 |
| 7 | **レスポンシブ対応** | 3ブレークポイント (1280px/960px)、固定列の動的切替 | Mobile 対応 |
| 8 | **Vitest + Playwright テスト** | コンポーネント単体テスト + Pinia Store テスト + E2E テスト | TDD |

### REMOVE（削除）

| # | 項目 | 旧の概要 | 削除理由 |
|---|---|---|---|
| 1 | **FRAMESET 左右分割** | head_l/r + body_l/r 4フレーム + 手動 JS 同期 | PrimeVue DataTable 単一コンポーネントに統合 |
| 2 | **JSP ビューヘルパー** | SecurityRoleInfo ベースの権限判定 + isSts_man_* メソッド群 | StatusMatrixResolver + CzPermissions に置換 |
| 3 | **手動 JS 同期** | 左右フレーム間の JavaScript スクロール同期 | SPA 単一テーブルで不要 |
| 4 | **ESQID パラメータ** | マルチウィンドウ識別子 | SPA 単一ウィンドウで不要 |

### 注意が必要な移行ポイント

| # | ポイント | 詳細 |
|---|---|---|
| 1 | **固定列の再現** | 旧 FRAMESET による物理分割を CSS sticky で再現する際、水平スクロール時の影の表現やブレークポイントでの固定列数の動的変更に注意 |
| 2 | **月次操作の排他制御** | 旧システムは同期的な form submit。新システムは非同期 API のため、操作中の UI ロック（ボタン disabled + loading）が必要 |
| 3 | **ページネーションの UX 変更** | 旧: 200件固定。新: デフォルト50件。既存ユーザーが件数の変化に違和感を持つ可能性。デフォルト値の検討が必要 |

---

## E. 推奨アクション

> **全4件完了** ✅（P1: 2件 / P2: 2件）

| ID | 優先度 | ステータス | アクション | 修正箇所 |
|---|---|---|---|---|
| FIX-S01 | P1 | ✅ 完了 | `tab012.bit0`/`tab012.bit1` → `canInputPeriod`/`canAggregate` セマンティックエイリアスに統一 (7箇所) | spec #5 セクション 4.2, 5.2, 5.3, 6.1, 7.4, 10.1 |
| FIX-S02 | P1 | ✅ 完了 | (a) セクション 5.4 の権限条件を `canInputPeriod()` に修正（「両ビット未設定」条件を削除）。(b) spec #3 section 3.20 を更新: 前提条件 DataSyuukeiFlg=0 を削除し、両フラグリセットに変更 | spec #5 セクション 5.4 + spec #3 セクション 3.20 |
| FIX-S03 | P2 | ✅ 完了 | `isSts_man_j_upd` → `StatusMatrixResolver の statusUpdate` に用語統一 | spec #5 セクション 4.4 |
| FIX-S04 | P2 | ✅ 完了 | DataAccessScope を spec #2 の 7 階層語彙に整合: BUSYO/JIBUN → 7 階層 + 注記に置換 | spec #5 セクション 6.3 |

---

## F. 修正履歴

| 日時 | FIX-ID | 修正内容 | 修正者 |
|---|---|---|---|
| 2026-02-26 | FIX-S01 | セクション 4.2/5.2/5.3/6.1/7.4/10.1 の `tab012.bit0`/`tab012.bit1` を `canInputPeriod`/`canAggregate` セマンティックエイリアスに統一（7箇所） | Claude |
| 2026-02-26 | FIX-S02 | (a) セクション 5.4 の権限条件を「tab012.bit0 === true または両ビット未設定」→「canInputPeriod() === true」に修正。(b) spec #3 セクション 3.20 を更新: 前提条件 `DataSyuukeiFlg = 0` を削除し、処理を `GetsujiKakuteiFlg = 0, DataSyuukeiFlg = 0 に更新（確認済・集約済いずれの状態からも未確認に戻す）` に変更。レスポンス JSON に `dataSyuukei: false` を追加 | Claude |
| 2026-02-26 | FIX-S03 | セクション 4.4 行スタイルの `isSts_man_j_upd` を `StatusMatrixResolver.resolve(statusKey, false).update` に更新 | Claude |
| 2026-02-26 | FIX-S04 | セクション 6.3 DataAccessScope テーブルを spec #2 セクション 2.4 の 7 階層 (ZENSYA/EIGYOSHO/HONBU/KYOKU/SHITSU/BU/KA) に置換。BUSYO/JIBUN を削除し、KA レベルでの実質的な動作を注記として追加 | Claude |

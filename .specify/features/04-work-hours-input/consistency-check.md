# #4 Work Hours Input — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `04-work-hours-input/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1-#3/#5-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| Nuxt.js 3 SPA | ✅ 合致 | constitution 技術スタック「Nuxt.js 3」。spec: pages/work-hours.vue |
| PrimeVue UI | ✅ 合致 | constitution 技術スタック「PrimeVue」。spec: DataTable, Calendar, Dropdown, Dialog, InputText, Toast 等を使用 |
| UX-First | ✅ 合致 | constitution V「操作フィードバック」。spec: セル赤枠+ツールチップ、Toast、ローディング表示、3秒自動消去 |
| レスポンシブ対応 | ✅ 合致 | constitution V「両対応」。spec: セクション 9 で 3 ブレークポイント定義（1280px/960px） |
| フラットデザイン | ✅ 合致 | constitution V「影なし・border 基調」。spec: ステータスセルの色分けがフラットカラー、影なしバッジ |
| TDD | ✅ 合致 | constitution IV。spec: セクション 10 で Vitest + Playwright テスト要件定義 |
| Docker-First | ✅ 合致 | API 呼出先が Backend コンテナ上の REST API |
| 12状態ステータスマトリクス | ✅ 合致 | constitution I。spec: セクション 4.2 で StatusMatrixResolver によるボタン制御定義 |
| 4層権限モデル | ✅ 合致 | constitution VII。spec: セクション 7 でアクター別 UI 差異定義 |
| デザインスタイル(色分け) | ✅ 合致 | constitution V「ソフトカラー」。STATUS 色: 黄#FBFBB6, 緑#BDEAAD, 青#9DBDFE, 灰#5D5D5D |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| FRAMESET 4フレーム構成 | ヘッダー/ナビ/メイン/フッター | SPA 単一ページに統合 | ✅ IMPROVE (GAP-F10-06) |
| TdMask Ajax 編集 | TdMask ライブラリ使用 | PrimeVue DataTable インライン編集に置換 | ✅ IMPROVE (GAP-F10-01) |
| InsertListJspBean | 画面ヘルパークラス | Pinia Store + composable に分解 | ✅ IMPROVE |
| 88 Unit のうち FORM_010 関連 | InsertList 系 Unit 群 | 10 REST API エンドポイントに集約 | ✅ IMPROVE |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| TAB 010 bit2 → 系列選択 | ⚠️ ロジック反転 | セクション 4.2「`tab010.bit2 ? "tan" : "man"`」は**反転している**。bit2=1(全管理)→管理者系列(man)が正 → **FIX-W01** |
| 15 アクター対応 | ✅ 合致 | セクション 7.1 で ACT-01/02/03/04 の UI 差異を定義 |
| 雇用形態 TYPE_0-3 | ✅ 合致 | セクション 7.2 で 4 種の制限を定義 |
| 代行モード | ⚠️ API 未呼出 | セクション 5.6 で POST /delegation/switch を経由せず直接 staffId を設定 → **FIX-W02** |
| ステータス遷移ルール | ✅ 合致 | セクション 4.4 StatusCell: 担当者系列 0↔1、管理者系列 0→1→2→1→0 |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| US-010〜US-01D (13件) | ✅ 合致 | セクション 3.2 Actions で全 13 US に対応するアクション定義 |
| VR-001〜015 | ✅ 合致 | セクション 6.1 フロント + 6.2 バックエンドで全ルールカバー |
| BR-001〜007 | ✅ 合致 | セクション 4.4 HoursCell で BR-002/003/006/007 実装 |
| 確認ダイアログ CZ-505/506/516/518 | ✅ 合致 | セクション 4.2/4.5/5.3/5.4/5.8 で全ダイアログ定義 |
| エラーメッセージ CZ-125/126/129/137/141/142/144/146/147 | ✅ 合致 | セクション 6.1/6.2 で全エラーコード参照 |

### 04_screen_transition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| SCR-010 (FORM_010) 画面仕様 | ✅ 合致 | SPA 統合提案の単一ページ化を実現 |
| FORM_010 → FORM_020 遷移 | ✅ 合致 | セクション 7.1「画面遷移リンク(020) tab011.bit1 必要」|
| DLG_003/004 (SS 選択) | ✅ 合致 | セクション 2「SubsystemSearchDialog.vue」|
| DLG_005 (担当者選択) | ✅ 合致 | セクション 2「StaffSearchDialog.vue」|

### 05_gap_analysis.md との整合

| GAP-ID | 区分 | spec での対応 | 結果 |
|---|---|---|---|
| GAP-F10-01 | IMPROVE | TdMask → PrimeVue inline edit | ✅ セクション 4.3 |
| GAP-F10-02 | IMPROVE | コピー → copyRecords() | ✅ セクション 3.2 |
| GAP-F10-03 | IMPROVE | 一括確認 → batchConfirm() | ✅ セクション 3.2 |
| GAP-F10-04 | ADD | 翌月転写 → TransferDialog | ✅ セクション 4.6 |
| GAP-F10-05 | IMPROVE | 月切替 submit → changeMonth() | ✅ セクション 5.5 |
| GAP-F10-06 | IMPROVE | FRAMESET → SPA 単一ページ | ✅ セクション 1 |
| GAP-F10-09 | IMPROVE | ソート → DataTable sortField | ✅ セクション 4.3/5.7 |
| GAP-F10-10 | ADD | レスポンシブ対応 | ✅ セクション 9 |
| GAP-F10-11 | ADD | HH:MM 入力補助 | ✅ セクション 4.4 HoursCell |

### 06_devenv_infrastructure.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| Frontend コンテナ (Nuxt.js 3, :3000) | ✅ 合致 | ページ URL `/work-hours` が Frontend コンテナで提供 |
| ホットリロード | ✅ 合致 | 開発時の即時反映が Docker 構成でサポート |
| ORM 記載 | ℹ️ 注意 | 06_devenv の JPA+Hibernate 記載は既知事項。Frontend spec には直接影響なし |

---

## C. 他 Spec (#1-#3, #5-#10) との整合性

### 検出された不整合

| # | 不整合内容 | 重要度 | FIX-ID | 関連 |
|---|---|---|---|---|
| 1 | ~~セクション 4.2 Toolbar の系列選択ロジックが反転。`tab010.bit2 ? "tan" : "man"` は bit2=1(全管理)→管理者系列(man) が正~~ | ~~🔴 P0~~ | FIX-W01 ✅ | #2 auth, 02_actor |
| 2 | ~~セクション 3.2/5.6 の switchDaiko が `POST /delegation/switch` API を呼んでいない~~ | ~~🟡 P1~~ | FIX-W02 ✅ | #3 core-api 3.21 |
| 3 | ~~セクション 5.1 の新規追加フロー（POST yearMonth のみ）と spec #3 section 3.2（POST に全 VR バリデーション）の不整合~~ | ~~🟡 P1~~ | FIX-W03 ✅ | #3 core-api 3.2 |
| 4 | ~~セクション 7.1 の `tab011.bit1` を `canNavigateForms` セマンティックエイリアスに統一すべき~~ | ~~🟢 P2~~ | FIX-W04 ✅ | #2 auth FIX-A07 |

### 不整合の詳細

#### FIX-W01: 系列選択ロジックの反転（P0 — 機能バグ）

セクション 4.2 Toolbar のボタン表示ロジック:
```
系列 = permissions.tab010.bit2 ? "tan" : "man"    ← 反転！
```

02_actor_definition.md + spec #2 FIX-A07 より:
- `tab010.bit2 = 1` → 全管理グループ → `canFullAccess()` → **管理者系列 (man)**
- `tab010.bit2 = 0` → 報告担当グループ → `useTanSeries()` → **担当者系列 (tan)**

正しいロジック:
```
系列 = permissions.useTanSeries ? "tan" : "man"
```

この反転により、ACT-01（報告担当者）に管理者系列のボタン制御が適用され、ACT-03（全権管理者）に担当者系列が適用される。ステータスマトリクスの全ボタン表示が逆転する致命的なバグ。

#### FIX-W02: 代行切替 API の未呼出

セクション 3.2 Actions:
```
switchDaiko(staffId) → 担当者切替 → fetchRecords
```

セクション 5.6 のフロー:
```
3. Frontend: switchDaiko(selectedStaffId)
   → staffId = selectedStaffId
   → isDaiko = true
   → fetchRecords()
```

spec #3 section 3.21 で定義された `POST /delegation/switch` API を経由していない。
この API は `canDelegate` 権限検証 + `isAllowedStaff()` 検証を行うため、セキュリティ上必須。

正しいフロー:
```
3. Frontend: POST /delegation/switch { targetStaffId }
   → Backend: canDelegate + isAllowedStaff 検証
   → 成功: { delegationStaffId, isDaiko: true }
   → Frontend: staffId 設定 + X-Delegation-Staff-Id ヘッダー付与
   → fetchRecords()
```

#### FIX-W03: 新規追加フローのバリデーション方針不整合

spec #4 セクション 5.1:
```
2. Frontend: POST /work-hours (yearMonth のみ、他は空)
3. Backend: STATUS_0 で空レコード作成、id 採番
```

spec #3 セクション 3.2 の POST バリデーション表:
- VR-001: 作業日**必須** → 空レコードでは失敗
- VR-003: 対象サブシステム**必須** → 空レコードでは失敗
- VR-005: 保守カテゴリ**必須** → 空レコードでは失敗

旧システムの動作: 空行追加 → インライン入力 → 一括確認時にバリデーション。
spec #3 と #4 で「ドラフト作成時のバリデーション省略」方針を明示する必要あり。

#### FIX-W04: セマンティックエイリアス未統一

セクション 7.1:
```
画面遷移リンク(020) | tab011.bit1 必要
```

spec #2 FIX-A07 で定義済みのセマンティックエイリアス `canNavigateForms` を使用すべき:
```
画面遷移リンク(020) | canNavigateForms 必要
```

### 整合している点

| 項目 | 結果 |
|---|---|
| spec #1 (database-schema) — tcz01_hosyu_kousuu テーブル操作が全 CRUD に対応 | ✅ |
| spec #1 (database-schema) — updatedAt 楽観ロック (CZ-101) がセクション 8.1 で実装 | ✅ |
| spec #2 (auth-infrastructure) — StatusMatrixResolver の呼出方式がセクション 4.2/4.3 で整合 | ✅ |
| spec #2 (auth-infrastructure) — useTanSeries エイリアスをセクション 4.3 isEditable で使用 | ✅ |
| spec #3 (core-api-design) — 10 エンドポイント（GET/POST/PATCH/DELETE/copy/transfer/batch-confirm/batch-revert/project-summary/excel）が全対応 | ✅ |
| spec #3 (core-api-design) — PATCH per-field 方式がセクション 5.2 インライン編集で整合 | ✅ |
| spec #5 (work-status-list) — FORM_010 vs FORM_020 の差異（自分CRUD vs 管理者閲覧承認）が整合 | ✅ |
| spec #7 (common-components) — SubsystemSearchDialog / StaffSearchDialog の共有 | ✅ |
| spec #7 (common-components) — CZ-505/506/518 確認ダイアログの定義が整合 | ✅ |
| spec #7 (common-components) — HoursInput 15分単位バリデーションの共有 | ✅ |
| spec #8 (validation-error-system) — VR-001〜015, BR-001〜007 の全ルール参照が整合 | ✅ |
| spec #8 (validation-error-system) — error.field / error.recordId によるセル特定が整合 | ✅ |
| spec #9 (excel-export) — FORM_010 工数明細テンプレート (US-01A) が整合 | ✅ |
| spec #10 (batch-processing) — tcz01 → tcz13 集計のデータソースが整合 | ✅ |

---

## D. 旧システムとの仕様整合性・変更点まとめ

### KEEP（踏襲）— 変更なし

| # | 項目 | 旧 | 新 | 根拠 |
|---|---|---|---|---|
| 1 | インライン編集方式 | セルクリック→入力→onBlur→保存 | セルクリック→入力→フォーカスアウト→PATCH | GAP-F10-01 |
| 2 | ステータス色分け | 黄/緑/青/灰 4色 | 同一の 4 色 (#FBFBB6/#BDEAAD/#9DBDFE/#5D5D5D) | — |
| 3 | テーブル列構成 (14列) | CHK〜依頼書No | 同一の 14 列 | — |
| 4 | 一括確認/一括作成中 | フッターボタン | 同一のフッターボタン (StatusFooter) | US-015/016 |
| 5 | 月切替ナビゲーション | 前月/翌月ボタン | 同一の <</>>/Dropdown | US-017 |
| 6 | コピー機能 | 選択レコード複製 (STATUS→0, SEQNO→新規) | 同一の動作 | US-012 |
| 7 | 翌月転写 | 選択レコードを指定月にコピー | 同一の動作 (カテゴリ存在確認含む) | US-013 |
| 8 | プロジェクト別工数参照 | ダイアログ表示 | 同一のダイアログ (ProjectSummaryDialog) | US-019 |
| 9 | 代行モード | 担当者選択→代行入力 | 同一の操作フロー | US-01C |
| 10 | バイト長計算 | 全角2/半角1/半角カナ2 | 同一のロジック (セクション 6.3) | VR-006 |
| 11 | 禁止ワードチェック | 12語 | 同一の12語 | VR-007 |
| 12 | フロント+バックエンド 2段バリデーション | JSP→Action 2段 | Frontend→API 2段 | — |

### IMPROVE（改善）— 技術的改善

> 下表の「影響度」は移行作業の規模を示す指標であり、未解決の問題ではありません。

| # | 項目 | 旧 (Java MPA) | 新 (Nuxt.js 3 SPA) | 影響度 | GAP-ID |
|---|---|---|---|---|---|
| 1 | **画面構成** | FRAMESET 4フレーム (ヘッダー/ナビ/メイン/フッター) | SPA 単一ページ (AppHeader + SideNav + WorkHoursPage) | 🔴 HIGH | GAP-F10-06 |
| 2 | **インライン編集** | TdMask ライブラリ (独自 Ajax オーバーレイ) | PrimeVue DataTable セル編集 (標準コンポーネント) | 🔴 HIGH | GAP-F10-01 |
| 3 | **データ送受信** | form submit + ActionForward | REST API (PATCH per-field) | 🔴 HIGH | GAP-B04 |
| 4 | **状態管理** | InsertListJspBean (JSP ビューヘルパー) | Pinia Store (workHours.ts) | 🟡 MID | — |
| 5 | **月切替** | form submit + ページリロード | changeMonth() + API 非同期取得 | 🟡 MID | GAP-F10-05 |
| 6 | **ソート** | サーバーサイド固定順 | DataTable sortField/sortOrder + API sort パラメータ | 🟡 MID | GAP-F10-09 |
| 7 | **セルバリデーション** | onBlur→Action→エラーページ | onBlur→即時ツールチップ + PATCH→エラーレスポンス | 🟡 MID | — |
| 8 | **ステータスマトリクス** | InsertListJspBean 内インライン判定 | StatusMatrixResolver 共通サービス | 🟢 LOW | — |
| 9 | **レスポンシブ** | 固定幅レイアウト | 3ブレークポイント (1280/960px) + 水平スクロール | 🟢 LOW | GAP-F10-10 |

### ADD（追加）

| # | 項目 | 詳細 | 根拠 |
|---|---|---|---|
| 1 | **PrimeVue DataTable** | ヘッダー固定 + 水平スクロール + 列ソート + チェックボックス選択 | UI 刷新 |
| 2 | **Pinia Store** | WorkHoursState 型安全な状態管理 (records, summary, permissions, UI状態) | SPA 設計 |
| 3 | **セルコンポーネント 7種** | StatusCell, DateCell, SubsystemCell, CategoryCell, SubjectCell, HoursCell, TextCell | コンポーネント分離 |
| 4 | **HH:MM 入力補助** | "3"→"03:00", "330"→"03:30", 空→"00:00" の自動変換 (BR-006) | UX 改善 |
| 5 | **TransferDialog** | 転写先月のチェックボックス選択 UI (最大12ヶ月) | UX 改善 |
| 6 | **フロントバリデーション** | セル赤枠 + ツールチップでの即時エラー表示 | UX-First |
| 7 | **エラー行スクロール** | batch-confirm エラー時、recordId で該当行にスクロール + ハイライト | UX 改善 |
| 8 | **レスポンシブ対応** | 3ブレークポイント、固定列 (CHK/STATUS/日付/対象SS名) | Mobile 対応 |
| 9 | **Vitest + Playwright テスト** | コンポーネント単体テスト + E2E テスト | TDD |

### REMOVE（削除）

| # | 項目 | 旧の概要 | 削除理由 |
|---|---|---|---|
| 1 | **FRAMESET レイアウト** | ヘッダー/ナビ/メイン/フッター 4フレーム | SPA 単一ページに統合 |
| 2 | **TdMask ライブラリ** | 独自 Ajax セル編集オーバーレイ | PrimeVue DataTable に置換 |
| 3 | **InsertListJspBean** | JSP ビューヘルパー (ステータス/権限/データ整形) | Pinia Store + composable に分解 |
| 4 | **InsertList 系 Unit 群** | InsertListDetailOutputUnit 等 | REST API エンドポイントに置換 |
| 5 | **ESQID パラメータ** | マルチウィンドウ識別子 | SPA 単一ウィンドウで不要 |
| 6 | **ActionDispatcher チェーン** | Action 内のネストされた処理委譲 | Service 層に集約 |

### 注意が必要な移行ポイント

| # | ポイント | 詳細 |
|---|---|---|
| 1 | **TdMask → PrimeVue 編集差異** | TdMask はセル上にオーバーレイで入力欄表示。PrimeVue はセル自体が入力モードに遷移。操作感の違いについてユーザーテストが必要 |
| 2 | **フィールド単位 PATCH** | 旧: フォーム全体を一括送信。新: フィールドごとに個別 API 呼出。ネットワーク呼出回数が増加するため、デバウンス考慮が必要 |
| 3 | **一括確認の UX 変更** | 旧: エラー時はエラーページに遷移。新: エラー行にスクロール + セルハイライト。ユーザーに対するフィードバック方式が変わる |
| 4 | **カテゴリ Dropdown の年度跨ぎ** | 月切替で年度が変わるとカテゴリリストが変化。Dropdown のオプション再取得タイミングに注意 |

---

## E. 推奨アクション

> **全4件完了** ✅（P0: 1件 / P1: 2件 / P2: 1件）

| ID | 優先度 | ステータス | アクション | 修正箇所 |
|---|---|---|---|---|
| FIX-W01 | P0 | ✅ 完了 | 系列選択ロジックを修正。`tab010.bit2 ? "tan" : "man"` → `useTanSeries ? "tan" : "man"` | spec セクション 4.2 |
| FIX-W02 | P1 | ✅ 完了 | switchDaiko フローに `POST /delegation/switch` API 呼出を追加 | spec セクション 3.2, 5.6 |
| FIX-W03 | P1 | ✅ 完了 | 新規追加フローのドラフト作成方針を明確化（#3 との整合） | spec セクション 5.1 + #3 セクション 3.2 |
| FIX-W04 | P2 | ✅ 完了 | `tab011.bit1` → `canNavigateForms` セマンティックエイリアスに更新 | spec セクション 7.1 |

---

## F. 修正履歴

| 日時 | FIX-ID | 修正内容 | 修正者 |
|---|---|---|---|
| 2026-02-26 | FIX-W01 | セクション 4.2 Toolbar のボタン表示ロジックを修正。`permissions.tab010.bit2 ? "tan" : "man"` → `permissions.useTanSeries ? "tan" : "man"` に変更。bit2=1(全管理グループ)→管理者系列(man)、bit2=0(報告担当)→担当者系列(tan) の正しいマッピングに修正 | Claude |
| 2026-02-26 | FIX-W02 | セクション 3.2 Actions の switchDaiko を `POST /delegation/switch` API 経由に変更。セクション 5.6 の代行モードフローを全面改訂: 担当者選択→POST /delegation/switch（canDelegate + isAllowedStaff 検証）→X-Delegation-Staff-Id ヘッダー付与→fetchRecords、代行解除フローも追加 | Claude |
| 2026-02-26 | FIX-W03 | セクション 5.1 の新規追加フローに「ドラフトモード」注記を追加。STATUS_0 での空レコード作成時は VR-001〜015 バリデーション省略、一括確認時に isInputCheck() で一括検証する旨を明記。spec #3 セクション 3.2 にも同一のドラフトモード方針を追記 | Claude |
| 2026-02-26 | FIX-W04 | セクション 7.1 の画面遷移リンク条件を `tab011.bit1 必要` → `canNavigateForms 必要` に更新。spec #2 FIX-A07 で定義済みのセマンティックエイリアスに統一 | Claude |

# 工数入力画面 (FORM_010) タスク一覧

## 概要
- 総タスク数: 57
- 見積もり合計: 47.5 時間（タスク単位積み上げ）

保守担当者が日々の保守工数を入力・管理する画面。
現行 MPA の FRAMESET 4フレーム + TdMask Ajax インライン編集を、
Nuxt.js 3 SPA の単一ページ + PrimeVue DataTable インライン編集に移行する。
13 ユーザーストーリー（US-010〜US-01D）をカバーする。

Backend API は spec #3 で実装済み。本 spec はフロントエンド実装に集中する。

### 仕様分類サマリー（spec.md セクション参照）

| 分類 | 件数 | 主要項目 |
|------|:----:|----------|
| 既存踏襲（KEEP） | 12 | 15分単位入力、ステータス遷移（0→1→2）、12状態マトリクス、禁止語句12語、バイト長計算、確認ダイアログ |
| 改善（IMPROVE） | 9 | TdMask→PrimeVue インライン編集、空行追加→即座追加、画面リロード→リアクティブ更新 |
| 新規追加（ADD） | 9 | Pinia Store、StatusFooter、MonthSelector、ショートカットキー、楽観的ロック UI |
| 廃止（REMOVE） | 6 | FRAMESET 4フレーム構成、TdMask.js、ActionDispatcher、InsertList Unit/Action、hidden iframe Excel |

詳細は [spec.md 仕様分類サマリー](./spec.md) および [consistency-check.md セクション D](./consistency-check.md) を参照。

---

## タスク一覧

### Phase 1: Pinia Store（workHours.ts）

- [ ] **T-001**: workHours Store の State / Getters テストを作成
  - 種別: テスト
  - 内容: WorkHoursState の初期値テスト。canAdd, canCopy, canDelete, canBatchConfirm, canBatchRevert, isEditable の各 Getter のテスト。ステータスマトリクス（12状態×2系列）に基づく判定ロジックの全パターンテスト
  - 成果物: `tests/stores/workHours.test.ts`
  - 完了条件: 全 Getter のテストが Red
  - 依存: なし
  - 見積もり: 2 時間

- [ ] **T-002**: workHours Store の fetchRecords Action テストを作成
  - 種別: テスト
  - 内容: GET /work-hours API モック → records, summary, monthControl, permissions, statusMatrix の state 更新を検証。loading 状態の遷移（true→false）。空配列レスポンス時の挙動
  - 成果物: `tests/stores/workHours.test.ts`（追記）
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-003**: workHours Store の createRecord Action テストを作成
  - 種別: テスト
  - 内容: POST /work-hours（ドラフトモード: yearMonth のみ）API モック → 新規レコードが records 先頭に追加されることを検証
  - 成果物: `tests/stores/workHours.test.ts`（追記）
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-004**: workHours Store の updateField Action テストを作成
  - 種別: テスト
  - 内容: PATCH /work-hours/{id} API モック → セル値更新 + summary.totalHours 更新。409 Conflict（CZ-101）時の挙動（元の値に復元 + updatedAt 更新）。400 バリデーションエラー時の挙動
  - 成果物: `tests/stores/workHours.test.ts`（追記）
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-005**: workHours Store の一括操作 Action テストを作成
  - 種別: テスト
  - 内容: deleteRecords（DELETE /work-hours）、copyRecords（POST /work-hours/copy）、transferNextMonth（POST /work-hours/transfer-next-month）、batchConfirm（POST /work-hours/batch-confirm）、batchRevert（POST /work-hours/batch-revert）の各 Action テスト。batchConfirm エラー時の recordId ハンドリング
  - 成果物: `tests/stores/workHours.test.ts`（追記）
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-006**: workHours Store の changeMonth / switchDaiko Action テストを作成
  - 種別: テスト
  - 内容: changeMonth → fetchRecords 再実行確認。switchDaiko → POST /delegation/switch → staffId/isDaiko 更新 → fetchRecords 再実行。代行解除（targetStaffId=null）
  - 成果物: `tests/stores/workHours.test.ts`（追記）
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-007**: workHours Store を実装
  - 種別: 実装
  - 内容: stores/workHours.ts。State（records, summary, yearMonth, staffId, isDaiko, sort, monthControl, permissions, statusMatrix, selectedIds, editingCell, loading, message）。Actions 10 個。Getters 6 個。useApi 連携
  - 成果物: `stores/workHours.ts`
  - 完了条件: T-001〜T-006 のテストが全て Green
  - 依存: T-001, T-002, T-003, T-004, T-005, T-006
  - 見積もり: 2.5 時間

### Phase 2: SearchPanel コンポーネント

- [ ] **T-008**: SearchPanel のテストを作成
  - 種別: テスト
  - 内容: 年月 Dropdown（±12ヶ月）のオプション生成、変更時 changeMonth 呼出確認。<</>>/ボタンで前月/翌月切替。リセットボタン（初期値復元 + fetchRecords）。代行モード表示（isDaiko=true 時「代行中」バッジ + 解除ボタン）
  - 成果物: `tests/components/work-hours/SearchPanel.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 1 時間

- [ ] **T-009**: SearchPanel を実装
  - 種別: 実装
  - 内容: components/work-hours/SearchPanel.vue。MonthSelector（#7）利用。担当者 InputText(readonly) + StaffSearchDialog 起動。代行モードバッジ。リセットボタン
  - 成果物: `components/work-hours/SearchPanel.vue`
  - 完了条件: T-008 のテストが全て Green
  - 依存: T-008
  - 見積もり: 1 時間

### Phase 3: Toolbar コンポーネント

- [ ] **T-010**: Toolbar のテストを作成
  - 種別: テスト
  - 内容: ステータスマトリクスに応じたボタン表示（9:非表示、0:グレーアウト、1:有効）。追加/コピー/転写/削除の表示条件と有効条件（selectedIds.length > 0）。合計工数表示。PJ工数/Excel ボタン
  - 成果物: `tests/components/work-hours/Toolbar.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 1 時間

- [ ] **T-011**: Toolbar を実装
  - 種別: 実装
  - 内容: components/work-hours/Toolbar.vue。buildStatusKey → resolveStatusMatrix → ボタン制御。ConfirmDialog（CZ-506 削除確認、CZ-516 Excel 確認）連携
  - 成果物: `components/work-hours/Toolbar.vue`
  - 完了条件: T-010 のテストが全て Green
  - 依存: T-010
  - 見積もり: 1 時間

### Phase 4: セルコンポーネント群

- [ ] **T-012**: StatusCell のテストを作成
  - 種別: テスト
  - 内容: 表示モード（ステータスラベル + 色付きバッジ: 0=黄, 1=緑, 2=青, 9=灰）。編集モード（Dropdown: 0/1/2 選択肢）。常時編集可（isEditable とは独立）。変更時 PATCH API 呼出確認。担当者系列: 0↔1 のみ、管理者系列: 全遷移可
  - 成果物: `tests/components/work-hours/cells/StatusCell.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 1 時間

- [ ] **T-013**: StatusCell を実装
  - 種別: 実装
  - 内容: components/work-hours/cells/StatusCell.vue。StatusBadge（#7）利用。PrimeVue Dropdown。ステータス遷移ルールに基づくオプションフィルタ
  - 成果物: `components/work-hours/cells/StatusCell.vue`
  - 完了条件: T-012 のテストが全て Green
  - 依存: T-012
  - 見積もり: 0.5 時間

- [ ] **T-014**: DateCell のテストを作成
  - 種別: テスト
  - 内容: 表示モード（MM/DD 短縮表示）。編集モード（PrimeVue Calendar）。STATUS_0 のみ編集可。VR-001（必須）、VR-002（月内範囲）バリデーション。変更時 PATCH API 呼出
  - 成果物: `tests/components/work-hours/cells/DateCell.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-015**: DateCell を実装
  - 種別: 実装
  - 内容: components/work-hours/cells/DateCell.vue。PrimeVue Calendar（dateFormat="yy-mm-dd"）。useValidation 連携
  - 成果物: `components/work-hours/cells/DateCell.vue`
  - 完了条件: T-014 のテストが全て Green
  - 依存: T-014
  - 見積もり: 0.5 時間

- [ ] **T-016**: SubsystemCell のテストを作成
  - 種別: テスト
  - 内容: 表示モード（"SS001 会計モジュール"、sysKbn=1 で先頭 "◆" マーカー）。編集モード（クリックで SubsystemSearchDialog 起動）。mode="target"/"cause" 切替。STATUS_0 のみ編集可
  - 成果物: `tests/components/work-hours/cells/SubsystemCell.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-017**: SubsystemCell を実装
  - 種別: 実装
  - 内容: components/work-hours/cells/SubsystemCell.vue。SubsystemSearchDialog（#7）連携
  - 成果物: `components/work-hours/cells/SubsystemCell.vue`
  - 完了条件: T-016 のテストが全て Green
  - 依存: T-016
  - 見積もり: 0.5 時間

- [ ] **T-018**: CategoryCell のテストを作成
  - 種別: テスト
  - 内容: 表示モード（カテゴリ名）。編集モード（PrimeVue Dropdown、年度別カテゴリリスト）。VR-005（必須）。STATUS_0 のみ編集可
  - 成果物: `tests/components/work-hours/cells/CategoryCell.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-019**: CategoryCell を実装
  - 種別: 実装
  - 内容: components/work-hours/cells/CategoryCell.vue。GET /masters/categories?fiscalYear= で年度別取得
  - 成果物: `components/work-hours/cells/CategoryCell.vue`
  - 完了条件: T-018 のテストが全て Green
  - 依存: T-018
  - 見積もり: 0.5 時間

- [ ] **T-020**: SubjectCell のテストを作成
  - 種別: テスト
  - 内容: 表示モード（30文字折り返し）。編集モード（InputText maxlength=128）。VR-006（必須 + 128バイト）バリデーション。改行コード自動除去。STATUS_0 のみ編集可
  - 成果物: `tests/components/work-hours/cells/SubjectCell.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-021**: SubjectCell を実装
  - 種別: 実装
  - 内容: components/work-hours/cells/SubjectCell.vue。calculateByteLength（#7）利用。useValidation 連携
  - 成果物: `components/work-hours/cells/SubjectCell.vue`
  - 完了条件: T-020 のテストが全て Green
  - 依存: T-020
  - 見積もり: 0.5 時間

- [ ] **T-022**: HoursCell のテストを作成
  - 種別: テスト
  - 内容: 表示モード（"03:30"）。編集モード（HoursInput #7 利用）。自動変換（1桁〜4桁 + コロン付き）。VR-008（必須）、VR-009（15分単位）、BR-007（最小0:15）。変更成功時 summary.totalHours 更新確認
  - 成果物: `tests/components/work-hours/cells/HoursCell.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 1 時間

- [ ] **T-023**: HoursCell を実装
  - 種別: 実装
  - 内容: components/work-hours/cells/HoursCell.vue。HoursInput（#7）利用。blur 時バリデーション → PATCH API
  - 成果物: `components/work-hours/cells/HoursCell.vue`
  - 完了条件: T-022 のテストが全て Green
  - 依存: T-022
  - 見積もり: 0.5 時間

- [ ] **T-024**: TextCell のテストを作成
  - 種別: テスト
  - 内容: TMR番号（5文字以内・半角数字 VR-011）、依頼書No（空 or 7文字固定 VR-012）、依頼者名（40文字以内 VR-013）の各フィールドの表示/編集テスト。STATUS_0 のみ編集可
  - 成果物: `tests/components/work-hours/cells/TextCell.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-025**: TextCell を実装
  - 種別: 実装
  - 内容: components/work-hours/cells/TextCell.vue。PrimeVue InputText。field prop で TMR/依頼書/依頼者を切替。useValidation 連携
  - 成果物: `components/work-hours/cells/TextCell.vue`
  - 完了条件: T-024 のテストが全て Green
  - 依存: T-024
  - 見積もり: 0.5 時間

### Phase 5: WorkHoursDataTable コンポーネント

- [ ] **T-026**: WorkHoursDataTable のテストを作成
  - 種別: テスト
  - 内容: 14列の PrimeVue DataTable レンダリング。ヘッダー固定 + 水平スクロール。セルクリック → 編集モード遷移（isEditable 判定）。チェックボックス選択 → selectedIds 更新。ソート（列ヘッダークリック → sort パラメータ構築 → fetchRecords）。行スタイル（STATUS_0:通常、STATUS_1/2:グレー背景 cell-readonly）
  - 成果物: `tests/components/work-hours/WorkHoursDataTable.test.ts`
  - 完了条件: テストが Red
  - 依存: T-013, T-015, T-017, T-019, T-021, T-023, T-025
  - 見積もり: 1.5 時間

- [ ] **T-027**: WorkHoursDataTable を実装
  - 種別: 実装
  - 内容: components/work-hours/WorkHoursDataTable.vue。PrimeVue DataTable + 14 Column 定義。各セルコンポーネント統合。sortField/sortOrder → API sort パラメータ変換。固定列（CHK, ステータス, 作業日, 対象SS名）
  - 成果物: `components/work-hours/WorkHoursDataTable.vue`
  - 完了条件: T-026 のテストが全て Green
  - 依存: T-026
  - 見積もり: 2 時間

### Phase 6: StatusFooter コンポーネント

- [ ] **T-028**: StatusFooter のテストを作成
  - 種別: テスト
  - 内容: 一括確認ボタン（canBatchConfirm かつ STATUS_0 件数 > 0）。一括作成中ボタン（canBatchRevert かつ STATUS_1 件数 > 0）。ステータス件数表示（STATUS_0: >0 で赤文字）。確認ダイアログ（CZ-505, CZ-518）。メッセージエリア（成功:緑 3秒消去、エラー:赤 手動消去）
  - 成果物: `tests/components/work-hours/StatusFooter.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 1 時間

- [ ] **T-029**: StatusFooter を実装
  - 種別: 実装
  - 内容: components/work-hours/StatusFooter.vue。ConfirmDialog（#7）利用。batchConfirm / batchRevert Action 連携
  - 成果物: `components/work-hours/StatusFooter.vue`
  - 完了条件: T-028 のテストが全て Green
  - 依存: T-028
  - 見積もり: 0.5 時間

### Phase 7: ダイアログコンポーネント

- [ ] **T-030**: TransferDialog のテストを作成
  - 種別: テスト
  - 内容: 選択レコード件数表示。転写先月チェックボックス（現在月+1 から最大12ヶ月）。転写実行 → POST /work-hours/transfer-next-month 呼出確認。カテゴリ年度不存在の注意書き表示
  - 成果物: `tests/components/work-hours/TransferDialog.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-031**: TransferDialog を実装
  - 種別: 実装
  - 内容: components/work-hours/TransferDialog.vue。PrimeVue Dialog (modal)。チェックボックスリスト
  - 成果物: `components/work-hours/TransferDialog.vue`
  - 完了条件: T-030 のテストが全て Green
  - 依存: T-030
  - 見積もり: 0.5 時間

- [ ] **T-032**: ProjectSummaryDialog のテストを作成
  - 種別: テスト
  - 内容: GET /work-hours/project-summary API モック → システム/SS 別工数テーブル表示。合計行表示。読み取り専用
  - 成果物: `tests/components/work-hours/ProjectSummaryDialog.test.ts`
  - 完了条件: テストが Red
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-033**: ProjectSummaryDialog を実装
  - 種別: 実装
  - 内容: components/work-hours/ProjectSummaryDialog.vue。PrimeVue Dialog (modal, width:600px)。DataTable 読み取り専用
  - 成果物: `components/work-hours/ProjectSummaryDialog.vue`
  - 完了条件: T-032 のテストが全て Green
  - 依存: T-032
  - 見積もり: 0.5 時間

### Phase 8: ページ統合

- [ ] **T-034**: WorkHoursPage のテストを作成
  - 種別: テスト
  - 内容: pages/work-hours.vue の統合テスト。onMounted で現在月の fetchRecords 自動実行。SearchPanel + Toolbar + DataTable + StatusFooter の統合レンダリング。ローディング状態（DataTable loading プロパティ）。空データ表示（「データがありません」メッセージ）
  - 成果物: `tests/pages/work-hours.test.ts`
  - 完了条件: テストが Red
  - 依存: T-009, T-011, T-027, T-029
  - 見積もり: 1 時間

- [ ] **T-035**: WorkHoursPage を実装
  - 種別: 実装
  - 内容: pages/work-hours.vue。Store 初期化 + 子コンポーネント配置。onMounted で fetchRecords()。4状態表示（loading/empty/error/success）
  - 成果物: `pages/work-hours.vue`
  - 完了条件: T-034 のテストが全て Green
  - 依存: T-034
  - 見積もり: 1 時間

### Phase 9: 代行モード統合

- [ ] **T-036**: 代行モード統合テストを作成
  - 種別: テスト
  - 内容: StaffSearchDialog（#7）→ 担当者選択 → POST /delegation/switch → isDaiko=true → X-Delegation-Staff-Id ヘッダー付与 → fetchRecords（代行対象レコード）。代行解除 → targetStaffId=null → isDaiko=false → ヘッダー除去 → fetchRecords（自分のレコード）
  - 成果物: `tests/integration/delegation-mode.test.ts`
  - 完了条件: テストが Red
  - 依存: T-009, T-007
  - 見積もり: 1 時間

- [ ] **T-037**: 代行モード統合を実装
  - 種別: 実装
  - 内容: SearchPanel の担当者選択 → switchDaiko Action 呼出 → useApi に X-Delegation-Staff-Id 設定。ヘッダー「代行中: {対象者名}」バッジ表示
  - 成果物: 既存コンポーネントの修正
  - 完了条件: T-036 のテストが全て Green
  - 依存: T-036
  - 見積もり: 1 時間

### Phase 10: レスポンシブ対応

- [ ] **T-038**: レスポンシブレイアウトのテストを作成
  - 種別: テスト
  - 内容: >= 1280px（全14列表示）、960-1279px（水平スクロール）、< 960px（必須列固定 + スクロール）のブレークポイント別表示テスト。固定列: CHK, ステータス, 作業日, 対象SS名
  - 成果物: `tests/components/work-hours/responsive.test.ts`
  - 完了条件: テストが Red
  - 依存: T-027
  - 見積もり: 0.5 時間

- [ ] **T-039**: レスポンシブレイアウトを実装
  - 種別: 実装
  - 内容: DataTable の frozenColumns 設定。CSS メディアクエリ。サイドナビ折畳時のテーブル幅自動調整
  - 成果物: 既存コンポーネントの修正
  - 完了条件: T-038 のテストが全て Green
  - 依存: T-038
  - 見積もり: 0.5 時間

### Phase 11: 受け入れ基準テスト（AC-WH-01〜AC-WH-13）

spec.md「受け入れ基準（Given-When-Then）」に定義された 13 件の AC を検証する統合テスト。
各テストはコンポーネント統合レベルで AC の Given-When-Then を忠実に再現する。

- [ ] **T-040**: AC-WH-01 テスト — 新規行追加
  - 種別: テスト
  - 内容: Given: STATUS_0 権限を持つユーザーが工数入力画面を表示。When: [追加] ボタンクリック。Then: DataTable 先頭に空行追加、作業日セルにフォーカス
  - 対応 AC: AC-WH-01（US-010）
  - 成果物: `tests/acceptance/work-hours/ac-wh-01-add-row.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-041**: AC-WH-02 テスト — インライン編集
  - 種別: テスト
  - 内容: Given: STATUS_0 レコード表示。When: 工数セルクリック→"130"入力→フォーカスアウト。Then: "1:30"に自動変換、PATCH API 送信・保存
  - 対応 AC: AC-WH-02（US-011）
  - 成果物: `tests/acceptance/work-hours/ac-wh-02-inline-edit.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-042**: AC-WH-03 テスト — 短縮入力変換
  - 種別: テスト
  - 内容: Given: HoursCell が編集モード。When: "8"入力→フォーカスアウト。Then: "8:00"に自動変換。追加パターン: "3"→"3:00", "12"→"12:00", "330"→"3:30", "1230"→"12:30"
  - 対応 AC: AC-WH-03
  - 成果物: `tests/acceptance/work-hours/ac-wh-03-shortcut-input.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-023
  - 見積もり: 0.5 時間

- [ ] **T-043**: AC-WH-04 テスト — 一括確定
  - 種別: テスト
  - 内容: Given: STATUS_0 レコード3件、全て必須項目入力済み。When: [一括確定]→CZ-505 確認「はい」。Then: 全3件 STATUS_0→STATUS_1、StatusFooter 更新
  - 対応 AC: AC-WH-04（US-015）
  - 成果物: `tests/acceptance/work-hours/ac-wh-04-batch-confirm.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-044**: AC-WH-05 テスト — 一括確定バリデーションエラー
  - 種別: テスト
  - 内容: Given: STATUS_0 レコード3件、1件は件名空。When: [一括確定] 実行。Then: CZ-126 エラー表示、該当行ハイライト+スクロール、全件ステータス未変更
  - 対応 AC: AC-WH-05（US-015）
  - 成果物: `tests/acceptance/work-hours/ac-wh-05-batch-confirm-error.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-045**: AC-WH-06 テスト — 楽観的ロック競合
  - 種別: テスト
  - 内容: Given: 同一レコード（version=1）を表示。When: PATCH API が 409 Conflict を返す。Then: CZ-101 Toast 表示、コンフリクトセルが最新値で上書き、updatedAt 更新
  - 対応 AC: AC-WH-06（US-01B）
  - 成果物: `tests/acceptance/work-hours/ac-wh-06-optimistic-lock.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-046**: AC-WH-07 テスト — 削除操作
  - 種別: テスト
  - 内容: Given: STATUS_0 レコード選択済み。When: [削除]→確認ダイアログ「はい」。Then: レコードが DataTable から消去
  - 対応 AC: AC-WH-07（US-014）
  - 成果物: `tests/acceptance/work-hours/ac-wh-07-delete.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-047**: AC-WH-08 テスト — 月切替
  - 種別: テスト
  - 内容: Given: 2025年02月のデータ表示中。When: MonthSelector で2025年01月選択。Then: 2025年01月データが API から取得・表示
  - 対応 AC: AC-WH-08（US-017）
  - 成果物: `tests/acceptance/work-hours/ac-wh-08-month-switch.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-048**: AC-WH-09 テスト — 代行モード
  - 種別: テスト
  - 内容: Given: canDelegate=true でログイン。When: 代行先を正社員 A に設定。Then: 正社員 A の工数データ表示、以降の操作は正社員 A 名義
  - 対応 AC: AC-WH-09（US-01C）
  - 成果物: `tests/acceptance/work-hours/ac-wh-09-delegation.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-037
  - 見積もり: 0.5 時間

- [ ] **T-049**: AC-WH-10 テスト — サービス時間外操作
  - 種別: テスト
  - 内容: Given: サービス時間外（23:31 JST）を模擬（API が 403 + CZ-102 を返すモック）。When: 操作を試みる。Then: CZ-102 フルスクリーンオーバーレイ表示、全操作ブロック
  - 対応 AC: AC-WH-10
  - 成果物: `tests/acceptance/work-hours/ac-wh-10-service-hours.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-050**: AC-WH-11 テスト — ステータス別ボタン制御
  - 種別: テスト
  - 内容: Given: STATUS_1 レコード表示中。When: 画面確認。Then: 編集・削除ボタン無効化、差戻ボタン表示（管理者系列）。担当者系列では差戻非表示も検証
  - 対応 AC: AC-WH-11
  - 成果物: `tests/acceptance/work-hours/ac-wh-11-status-button-control.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-051**: AC-WH-12 テスト — Excel 出力
  - 種別: テスト
  - 内容: Given: 工数データ表示中。When: [Excel]→確認ダイアログ「はい」。Then: Blob + createObjectURL で .xlsx ダウンロード、完了 Toast 表示
  - 対応 AC: AC-WH-12（US-01A）
  - 成果物: `tests/acceptance/work-hours/ac-wh-12-excel-export.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

- [ ] **T-052**: AC-WH-13 テスト — 状態表示パターン
  - 種別: テスト
  - 内容: Given: 画面遷移直後。When: API データ取得中。Then: スピナーオーバーレイ表示→取得完了→データ描画。データ0件→「データがありません」メッセージ表示
  - 対応 AC: AC-WH-13
  - 成果物: `tests/acceptance/work-hours/ac-wh-13-state-display.test.ts`
  - 完了条件: AC の Given-When-Then が全て検証される
  - 依存: T-035
  - 見積もり: 0.5 時間

### Phase 12: E2E テスト

- [ ] **T-053**: 基本フロー E2E テスト
  - 種別: テスト
  - 内容: Playwright。ログイン → 工数入力画面 → [追加] → 各フィールド入力（作業日, 対象SS, 原因SS, カテゴリ, 件名, 工数）→ [一括確認] → STATUS_0→1 変更確認
  - 成果物: `tests/e2e/work-hours-basic.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-035
  - 見積もり: 2 時間

- [ ] **T-054**: バリデーション E2E テスト
  - 種別: テスト
  - 内容: 15分単位違反（03:10→CZ-147）、24h超過（合計24:15→CZ-146）、禁止ワード（件名に"カ層"→CZ-141）→ エラー表示確認。セル赤枠 + ツールチップ
  - 成果物: `tests/e2e/work-hours-validation.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-035
  - 見積もり: 1.5 時間

- [ ] **T-055**: 権限テスト E2E
  - 種別: テスト
  - 内容: ACT-01（担当者）vs ACT-03（全権管理者）でボタン表示差異確認。担当者: STATUS_2 編集不可。管理者: STATUS_2 編集可
  - 成果物: `tests/e2e/work-hours-permissions.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-035
  - 見積もり: 1.5 時間

- [ ] **T-056**: 代行モード E2E テスト
  - 種別: テスト
  - 内容: 管理者でログイン → 担当者選択 → 代行入力 → レコード追加 → 登録者IDが代行元正社員ID → 代行解除 → 自分のレコード表示
  - 成果物: `tests/e2e/work-hours-delegation.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-037
  - 見積もり: 1.5 時間

- [ ] **T-057**: コピー・転写・月切替 E2E テスト
  - 種別: テスト
  - 内容: レコード選択 → [コピー] → STATUS_0 でコピー確認。レコード選択 → [翌月転写] → TransferDialog → 実行。前月/翌月切替 → データリロード確認。ソート（列ヘッダークリック → 並び替え）
  - 成果物: `tests/e2e/work-hours-operations.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-035
  - 見積もり: 1.5 時間

---

## 依存関係図

```
Phase 1 (Store)
T-001→T-007  T-002→T-007  T-003→T-007  T-004→T-007  T-005→T-007  T-006→T-007

Phase 2-3 (Search + Toolbar)
T-008→T-009  T-010→T-011

Phase 4 (Cells)
T-012→T-013  T-014→T-015  T-016→T-017  T-018→T-019
T-020→T-021  T-022→T-023  T-024→T-025

Phase 5 (DataTable)
T-026→T-027  (T-013,T-015,T-017,T-019,T-021,T-023,T-025 → T-026)

Phase 6-7 (Footer + Dialogs)
T-028→T-029  T-030→T-031  T-032→T-033

Phase 8 (Page)
T-034→T-035  (T-009,T-011,T-027,T-029 → T-034)

Phase 9 (Delegation)
T-036→T-037

Phase 10 (Responsive)
T-038→T-039

Phase 11 (Acceptance Criteria: AC-WH-01〜AC-WH-13)
T-040(AC-01)  T-041(AC-02)  T-042(AC-03)  T-043(AC-04)  T-044(AC-05)
T-045(AC-06)  T-046(AC-07)  T-047(AC-08)  T-048(AC-09)  T-049(AC-10)
T-050(AC-11)  T-051(AC-12)  T-052(AC-13)
  (T-035 → T-040〜T-052、ただし T-042→T-023 依存、T-048→T-037 依存)

Phase 12 (E2E)
T-053  T-054  T-055  T-056  T-057
```

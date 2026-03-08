# 工数状況一覧 (FORM_020) タスク一覧

## 概要
- 総タスク数: 42
- 見積もり合計: 56 時間
- 対応ユーザーストーリー: US-020〜US-027（8件）
- 依存先 spec: #1 database-schema, #2 auth-infrastructure, #3 core-api-design, #7 common-components, #8 validation-error-system, #10 batch-processing

## タスク一覧

### Phase 1: Pinia Store 基盤（テストファースト）

- [ ] **T-001**: workStatus Store — State / Getters 型定義
  - 種別: 実装
  - 内容: `stores/workStatus.ts` の State インターフェース（WorkStatusState, WorkStatusRecord, MonthlyControl, WorkStatusPermissions）、Getters（filteredStatusFilter, canApproveSelected, canRevertSelected, monthlyStatusLevel, isHoursEditable, totalPages）の型定義と初期値を作成
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: TypeScript コンパイルが通る。Store が Pinia で正常に定義される
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-002**: workStatus Store — fetchRecords テスト
  - 種別: テスト
  - 内容: `fetchRecords()` アクションのテスト。API モックで GET /work-status 呼出を検証。レスポンスから records / monthlyControl / permissions が正しく state にセットされることを検証。yearMonth / organizationCode / statusFilter / page / pageSize パラメータの送信を検証
  - 成果物: `frontend/tests/stores/workStatus.test.ts`
  - 完了条件: テストが Red 状態（fetchRecords 未実装のため失敗）
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-003**: workStatus Store — fetchRecords 実装
  - 種別: 実装
  - 内容: `fetchRecords()` アクションの実装。useApi composable を使用して GET /work-status を呼出。レスポンスを state にセット。loading 状態管理を含む
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: T-002 のテストが Green 状態
  - 依存: T-002
  - 見積もり: 1 時間

- [ ] **T-004**: workStatus Store — 月次制御アクション テスト
  - 種別: テスト
  - 内容: `monthlyConfirm()` / `monthlyAggregate()` / `monthlyUnconfirm()` の3アクションのテスト。各 API 呼出（POST）を検証。monthlyControl フラグの更新（gjkt_flg, data_sk_flg）を検証。成功後に fetchRecords が再呼出されることを検証
  - 成果物: `frontend/tests/stores/workStatus.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-005**: workStatus Store — 月次制御アクション 実装
  - 種別: 実装
  - 内容: `monthlyConfirm()` / `monthlyAggregate()` / `monthlyUnconfirm()` の実装。POST /work-status/monthly-confirm, monthly-aggregate, monthly-unconfirm を呼出。成功時に monthlyControl を更新し fetchRecords() を再実行
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: T-004 のテストが Green 状態
  - 依存: T-004
  - 見積もり: 1 時間

- [ ] **T-006**: workStatus Store — approveRecords / revertRecords テスト
  - 種別: テスト
  - 内容: `approveRecords(ids)` / `revertRecords(ids)` のテスト。POST /work-status/approve, /work-status/revert 呼出を検証。承認時: 対象レコードの STATUS_1 → STATUS_2 更新、戻し時: STATUS_2 → STATUS_1 更新を検証。成功メッセージ CZ-801 / CZ-800 表示、selectedIds クリアを検証
  - 成果物: `frontend/tests/stores/workStatus.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-007**: workStatus Store — approveRecords / revertRecords 実装
  - 種別: 実装
  - 内容: `approveRecords(ids)` / `revertRecords(ids)` の実装。フロントエンド側で選択レコードから対象ステータスのみ抽出して送信（承認: STATUS_1 のみ、戻し: STATUS_2 のみ）
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: T-006 のテストが Green 状態
  - 依存: T-006
  - 見積もり: 1 時間

- [ ] **T-008**: workStatus Store — updateHours テスト
  - 種別: テスト
  - 内容: `updateHours(id, value)` のテスト。PATCH /work-status/{id}/hours 呼出を検証。楽観的更新（即時 UI 反映）と API 失敗時のロールバックを検証。updatedAt による楽観的ロックパラメータ送信を検証
  - 成果物: `frontend/tests/stores/workStatus.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1 時間

- [ ] **T-009**: workStatus Store — updateHours 実装
  - 種別: 実装
  - 内容: `updateHours(id, value)` の実装。PATCH /work-status/{id}/hours を呼出。楽観的更新パターンを適用
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: T-008 のテストが Green 状態
  - 依存: T-008
  - 見積もり: 0.5 時間

- [ ] **T-010**: workStatus Store — changePage / changeMonth テスト
  - 種別: テスト
  - 内容: `changePage(page)` / `changeMonth(yearMonth)` のテスト。page 更新 → fetchRecords 再呼出を検証。changePage 時の selectedIds クリアを検証。changeMonth 時の page=1 リセットを検証
  - 成果物: `frontend/tests/stores/workStatus.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1 時間

- [ ] **T-011**: workStatus Store — changePage / changeMonth 実装
  - 種別: 実装
  - 内容: `changePage(page)` / `changeMonth(yearMonth)` の実装
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: T-010 のテストが Green 状態
  - 依存: T-010
  - 見積もり: 0.5 時間

- [ ] **T-012**: workStatus Store — Getters テスト
  - 種別: テスト
  - 内容: 全 Getters のテスト。`filteredStatusFilter`（includeStatus0 に応じたフィルタ配列）、`canApproveSelected` / `canRevertSelected`（選択レコードのステータス判定）、`monthlyStatusLevel`（月次制御レベル 0/1/2）、`isHoursEditable`（ステータスマトリクス 12状態 × man系列の全パターン、パラメタライズドテスト）、`totalPages`（ページ数計算）
  - 成果物: `frontend/tests/stores/workStatus.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 2 時間

- [ ] **T-013**: workStatus Store — Getters 実装
  - 種別: 実装
  - 内容: 全 Getters の実装。isHoursEditable は StatusMatrixResolver（spec #3 で定義）と連携して man 系列のステータスマトリクスを評価
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: T-012 のテストが Green 状態
  - 依存: T-012
  - 見積もり: 1.5 時間

- [ ] **T-014**: workStatus Store — リファクタリング
  - 種別: リファクタ
  - 内容: Store 全体のリファクタリング。重複コードの抽出、エラーハンドリングの統一、loading 状態管理の共通化
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: 既存テスト（T-002〜T-012）が全て Green のまま維持
  - 依存: T-003, T-005, T-007, T-009, T-011, T-013
  - 見積もり: 1 時間

### Phase 2: 検索パネルコンポーネント

- [ ] **T-015**: SearchPanel コンポーネントテスト
  - 種別: テスト
  - 内容: SearchPanel.vue のテスト。年月 Dropdown（±12ヶ月のオプション生成、変更で changeMonth 呼出）、組織選択（OrganizationSearchDialog 連携）、担当者選択（StaffSearchDialog 連携）、「作成中も表示」チェックボックス（statusFilter への 0 追加）、検索ボタン（fetchRecords 呼出）、リセットボタン（検索条件初期化）の検証
  - 成果物: `frontend/tests/components/work-status/SearchPanel.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-016**: SearchPanel コンポーネント実装
  - 種別: 実装
  - 内容: SearchPanel.vue の実装。PrimeVue Dropdown（年月）、InputText + ダイアログ（組織/担当者）、Checkbox（作成中表示）、Button（検索/リセット）。デフォルト表示は STATUS_1 + STATUS_2。ログインユーザーの組織コードを初期値に設定
  - 成果物: `frontend/components/work-status/SearchPanel.vue`
  - 完了条件: T-015 のテストが Green 状態
  - 依存: T-015
  - 見積もり: 1.5 時間

### Phase 3: 月次制御バーコンポーネント

- [ ] **T-017**: MonthlyControlBar コンポーネントテスト
  - 種別: テスト
  - 内容: MonthlyControlBar.vue のテスト。3段階ステータス表示（未確認:黄 #FBFBB6 / 確認:緑 #BDEAAD / 集約:青 #9DBDFE）、ボタン表示条件（canUnconfirm / canConfirm / canAggregate）、ボタン有効条件（monthlyStatusLevel に応じた有効/無効）、確認ダイアログ表示（CZ-509 / CZ-510 / CZ-511）の検証
  - 成果物: `frontend/tests/components/work-status/MonthlyControlBar.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 2 時間

- [ ] **T-018**: MonthlyControlBar コンポーネント実装
  - 種別: 実装
  - 内容: MonthlyControlBar.vue の実装。月次ステータス表示（色付きラベル）、3つの操作ボタン（未確認/確認/集約）、確認ダイアログ連携（ConfirmDialog composable）、状態遷移ロジック
  - 成果物: `frontend/components/work-status/MonthlyControlBar.vue`
  - 完了条件: T-017 のテストが Green 状態
  - 依存: T-017
  - 見積もり: 1.5 時間

### Phase 4: メインテーブル + インライン編集 + ページネーション

- [ ] **T-019**: StatusBadge セルコンポーネントテスト
  - 種別: テスト
  - 内容: StatusBadge.vue のテスト。STATUS_0（作成中: 黄 #FBFBB6）、STATUS_1（確認: 緑 #BDEAAD）、STATUS_2（確定: 青 #9DBDFE）の色分け表示を検証
  - 成果物: `frontend/tests/components/work-status/cells/StatusBadge.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-020**: StatusBadge セルコンポーネント実装
  - 種別: 実装
  - 内容: StatusBadge.vue の実装。ステータス値に応じた色分け Badge 表示
  - 成果物: `frontend/components/work-status/cells/StatusBadge.vue`
  - 完了条件: T-019 のテストが Green 状態
  - 依存: T-019
  - 見積もり: 0.5 時間

- [ ] **T-021**: HoursEditCell コンポーネントテスト
  - 種別: テスト
  - 内容: HoursEditCell.vue のテスト。表示モード（"03:30" 太字表示）、編集可否判定（isHoursEditable 連携）、編集モード遷移（クリック → InputText 表示）、HH:MM パース、短縮入力の自動変換（"3"→"03:00", "12"→"12:00", "330"→"03:30", "1230"→"12:30"）、バリデーション（HH:MM 形式 CZ-125、15分単位 CZ-147、最大24:00 CZ-146、最小0:15 CZ-129、空値エラー）、フォーカスアウト/Enter で確定、エラー時の赤枠 + ツールチップ表示を検証
  - 成果物: `frontend/tests/components/work-status/cells/HoursEditCell.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 2 時間

- [ ] **T-022**: HoursEditCell コンポーネント実装
  - 種別: 実装
  - 内容: HoursEditCell.vue の実装。表示/編集モード切替、短縮入力の自動変換ロジック（BR-006）、フロントバリデーション（VR-008〜010）、PATCH API 呼出、エラー表示
  - 成果物: `frontend/components/work-status/cells/HoursEditCell.vue`
  - 完了条件: T-021 のテストが Green 状態
  - 依存: T-021
  - 見積もり: 1.5 時間

- [ ] **T-023**: WorkStatusDataTable コンポーネントテスト
  - 種別: テスト
  - 内容: WorkStatusDataTable.vue のテスト。15列のレンダリング、固定列4列（CHK / ステータス / 所属 / 担当者名）の PrimeVue frozen 設定、チェックボックス選択（selectedIds 更新）、ソート（列ヘッダークリック → sort 更新）、行スタイル（STATUS に応じたバッジ色）の検証
  - 成果物: `frontend/tests/components/work-status/WorkStatusDataTable.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001, T-020, T-022
  - 見積もり: 2 時間

- [ ] **T-024**: WorkStatusDataTable コンポーネント実装
  - 種別: 実装
  - 内容: WorkStatusDataTable.vue の実装。PrimeVue DataTable + Column（15列定義）、frozen 列設定、Checkbox 列、StatusBadge / HoursEditCell 組込み、ソート連携、loading / 空データ表示
  - 成果物: `frontend/components/work-status/WorkStatusDataTable.vue`
  - 完了条件: T-023 のテストが Green 状態
  - 依存: T-023
  - 見積もり: 2 時間

- [ ] **T-025**: Pagination コンポーネントテスト
  - 種別: テスト
  - 内容: Pagination.vue のテスト。ページ番号生成、表示件数変更（20/50/100/200）、ページ変更 → changePage 呼出、境界値（1ページ目で「前」無効、最終ページで「次」無効）、表示情報テキスト（全 N 件中 X-Y 件表示）の検証
  - 成果物: `frontend/tests/components/work-status/Pagination.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1 時間

- [ ] **T-026**: Pagination コンポーネント実装
  - 種別: 実装
  - 内容: Pagination.vue の実装。PrimeVue Paginator ラッパー。表示件数 Dropdown（デフォルト 50）、ページサイズ変更時の page=1 リセット + fetchRecords 再実行
  - 成果物: `frontend/components/work-status/Pagination.vue`
  - 完了条件: T-025 のテストが Green 状態
  - 依存: T-025
  - 見積もり: 1 時間

### Phase 5: ツールバーコンポーネント

- [ ] **T-027**: Toolbar コンポーネントテスト
  - 種別: テスト
  - 内容: Toolbar.vue のテスト。承認ボタン（表示条件: statusUpdate===1、有効条件: selectedIds に STATUS_1 レコードあり）、戻しボタン（有効条件: selectedIds に STATUS_2 レコードあり）、全選択/全解除ボタン、Excel ボタン（有効条件: records > 0）、確認ダイアログ表示（CZ-507 / CZ-508 / CZ-516）の検証
  - 成果物: `frontend/tests/components/work-status/Toolbar.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-028**: Toolbar コンポーネント実装
  - 種別: 実装
  - 内容: Toolbar.vue の実装。承認/戻し/全選択/全解除/Excel ボタン、ステータスマトリクス連携（StatusMatrixResolver）、確認ダイアログ連携、レコード件数表示
  - 成果物: `frontend/components/work-status/Toolbar.vue`
  - 完了条件: T-027 のテストが Green 状態
  - 依存: T-027
  - 見積もり: 1.5 時間

### Phase 6: ページ統合

- [ ] **T-029**: WorkStatusPage ページコンテナテスト
  - 種別: テスト
  - 内容: pages/work-status.vue のテスト。マウント時の自動 fetchRecords 呼出（yearMonth=現在月、organizationCode=ログインユーザー組織）、子コンポーネント（SearchPanel / MonthlyControlBar / Toolbar / WorkStatusDataTable / Pagination）の描画確認、MessageArea の表示を検証
  - 成果物: `frontend/tests/pages/work-status.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-016, T-018, T-024, T-026, T-028
  - 見積もり: 1.5 時間

- [ ] **T-030**: WorkStatusPage ページコンテナ実装
  - 種別: 実装
  - 内容: pages/work-status.vue の実装。全子コンポーネントの配置、onMounted での自動データ取得、メッセージエリア表示
  - 成果物: `frontend/pages/work-status.vue`
  - 完了条件: T-029 のテストが Green 状態
  - 依存: T-029
  - 見積もり: 1 時間

### Phase 7: 権限・ステータスマトリクス統合テスト

- [ ] **T-031**: ステータスマトリクス統合テスト（管理者系列 12状態）
  - 種別: テスト
  - 内容: FORM_020 固有のステータスマトリクス（管理者系列 man）の全12状態（000/010/011/100/110/111/200/210/211/900/910/911）× statusUpdate / statusView のパラメタライズドテスト。状況更新（承認/戻し/工数編集）可否と状況参照可否の全パターンを検証
  - 成果物: `frontend/tests/components/work-status/statusMatrix.integration.test.ts`
  - 完了条件: テストが通り、12状態の全パターンがカバーされる
  - 依存: T-013, T-022, T-028
  - 見積もり: 2 時間

- [ ] **T-032**: 権限別 UI 制御テスト
  - 種別: テスト
  - 内容: アクター別 UI 差異のテスト。ACT-02（報告管理者: 配下のみ承認可）、ACT-04（管理モード: 配下のみ承認可）、ACT-10（全社スタッフ: 承認不可、Excel のみ）、ACT-13（局スタッフ: 承認不可、Excel のみ）の各アクターで WorkStatusPage を描画し、ボタン表示/非表示、操作可否を検証
  - 成果物: `frontend/tests/components/work-status/permissions.integration.test.ts`
  - 完了条件: テストが通り、全アクターパターンがカバーされる
  - 依存: T-030
  - 見積もり: 1.5 時間

### Phase 8: エラーハンドリング

- [ ] **T-033**: エラーハンドリングテスト
  - 種別: テスト
  - 内容: エラーケースの統合テスト。月次制御の排他制御エラー（409 Conflict → Toast + monthlyControl 更新）、承認対象ステータス不整合（400 + CZ-109 → Toast + fetchRecords + selectedIds クリア）、同時編集競合（409 Conflict → Toast + セル値更新）、権限不足（403 + CZ-106 → Toast）、ネットワークエラー（→ Toast + セル値復元）の検証
  - 成果物: `frontend/tests/stores/workStatus.errors.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005, T-007, T-009
  - 見積もり: 1.5 時間

- [ ] **T-034**: エラーハンドリング実装
  - 種別: 実装
  - 内容: Store アクションにエラーハンドリングを追加。409 Conflict / 400 / 403 / ネットワークエラーの各ケースで適切な Toast 通知と UI 状態復元を実装
  - 成果物: `frontend/stores/workStatus.ts`
  - 完了条件: T-033 のテストが Green 状態
  - 依存: T-033
  - 見積もり: 1 時間

### Phase 9: レスポンシブ対応

- [ ] **T-035**: レスポンシブレイアウトテスト
  - 種別: テスト
  - 内容: 3ブレークポイントでのレイアウト検証。>=1280px（フルテーブル、固定列4列）、960-1279px（サイドナビ折りたたみ、水平スクロール、固定列4列）、<960px（サイドナビ非表示、固定列2列: CHK+ステータス）の検証
  - 成果物: `frontend/tests/components/work-status/responsive.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-024
  - 見積もり: 1 時間

- [ ] **T-036**: レスポンシブレイアウト実装
  - 種別: 実装
  - 内容: WorkStatusDataTable の CSS メディアクエリ追加。ブレークポイント別の固定列制御、サイドナビとの連携レイアウト
  - 成果物: `frontend/components/work-status/WorkStatusDataTable.vue`
  - 完了条件: T-035 のテストが Green 状態
  - 依存: T-035
  - 見積もり: 1 時間

### Phase 10: リファクタリング

- [ ] **T-037**: コンポーネント層リファクタリング
  - 種別: リファクタ
  - 内容: 全コンポーネントの横断的リファクタリング。重複ロジックの composable 抽出、Props/Emit の型定義整理、CSS クラスの命名統一、PrimeVue パススルー設定の整理
  - 成果物: 各コンポーネントファイル
  - 完了条件: 既存テスト（T-015〜T-036）が全て Green のまま維持
  - 依存: T-030, T-031, T-032, T-034, T-036
  - 見積もり: 1.5 時間

### Phase 11: E2E テスト

- [ ] **T-038**: E2E — 基本フロー + 検索
  - 種別: テスト
  - 内容: Playwright E2E テスト。管理者ログイン → サイドナビ「工数管理」選択 → 工数状況一覧表示 → ステータスカラーコード確認（黄/緑/青）。検索パネルで年月変更 → 一覧更新。「作成中も表示」チェック → STATUS_0 レコード表示確認
  - 成果物: `frontend/tests/e2e/work-status/basic-flow.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-030
  - 見積もり: 2 時間

- [ ] **T-039**: E2E — 月次制御フロー（確認→集約→未確認戻し）
  - 種別: テスト
  - 内容: Playwright E2E テスト。[確認] → CZ-510 確認ダイアログ → OK → ステータスバー色変化（黄→緑）→ 一覧更新。[集約] → CZ-511 → OK → バー色変化（緑→青）。[未確認] → CZ-509 → OK → バー色リセット（青→黄）
  - 成果物: `frontend/tests/e2e/work-status/monthly-control.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-030
  - 見積もり: 2 時間

- [ ] **T-040**: E2E — 承認/戻し + インライン工数編集
  - 種別: テスト
  - 内容: Playwright E2E テスト。STATUS_1 レコード選択 → [承認] → CZ-508 確認 → ステータス青に変化。STATUS_2 レコード選択 → [戻す] → CZ-507 確認 → ステータス緑に変化。工数セルクリック → "04:00" 入力 → Enter → 保存確認。"04:10" 入力 → 15分単位エラー表示確認
  - 成果物: `frontend/tests/e2e/work-status/approve-edit.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-030
  - 見積もり: 2 時間

- [ ] **T-041**: E2E — ページネーション + 表示件数変更
  - 種別: テスト
  - 内容: Playwright E2E テスト。次ページ → データ更新確認 → 選択状態クリア確認。表示件数 50→100 変更 → 1ページ目リセット + 件数変更確認。最終ページで「次」ボタン無効確認
  - 成果物: `frontend/tests/e2e/work-status/pagination.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-030
  - 見積もり: 1 時間

- [ ] **T-042**: E2E — 権限テスト + 排他制御
  - 種別: テスト
  - 内容: Playwright E2E テスト。ACT-04（管理モード）で承認ボタン表示確認。ACT-10（スタッフ）で承認ボタン非表示確認。2ユーザー同時月次操作 → 409 Conflict → エラーメッセージ表示確認
  - 成果物: `frontend/tests/e2e/work-status/permissions-concurrency.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-030
  - 見積もり: 1.5 時間

---

## 依存関係図

```
Phase 1 (Store):
T-001 → T-002 → T-003
      → T-004 → T-005
      → T-006 → T-007
      → T-008 → T-009
      → T-010 → T-011
      → T-012 → T-013
T-003 + T-005 + T-007 + T-009 + T-011 + T-013 → T-014

Phase 2-5 (Components):
T-001 → T-015 → T-016
T-001 → T-017 → T-018
T-019 → T-020
T-001 → T-021 → T-022
T-001 + T-020 + T-022 → T-023 → T-024
T-001 → T-025 → T-026
T-001 → T-027 → T-028

Phase 6 (Page):
T-016 + T-018 + T-024 + T-026 + T-028 → T-029 → T-030

Phase 7-8 (Integration):
T-013 + T-022 + T-028 → T-031
T-030 → T-032
T-003 + T-005 + T-007 + T-009 → T-033 → T-034

Phase 9 (Responsive):
T-024 → T-035 → T-036

Phase 10-11:
T-030 + T-031 + T-032 + T-034 + T-036 → T-037
T-030 → T-038, T-039, T-040, T-041, T-042
```

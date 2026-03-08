# 分析画面 (FORM_030-042) タスク一覧

## 概要
- 総タスク数: 54
- 見積もり合計: 72.5 時間
- 対応ユーザーストーリー: US-030〜US-045（16件）
- 受け入れ基準: AC-AN-01〜AC-AN-13（13件、spec セクション 10）
- 依存先 spec: #1 database-schema, #2 auth-infrastructure, #3 core-api-design, #7 common-components, #8 validation-error-system, #10 batch-processing

## タスク一覧

### Phase 1: Pinia Store 基盤（テストファースト）

- [ ] **T-001**: analytics Store — State / Getters 型定義
  - 種別: 実装
  - 内容: `stores/analytics.ts` の State インターフェース（AnalyticsState, DrilldownContext, AnalyticsRow, MonthlyValue）、Getters（currentApiBase, breadcrumbItems, dynamicColumns, monthColumns, filteredRows, canExportExcel）の型定義と初期値を作成
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: TypeScript コンパイルが通る。Store が Pinia で正常に定義される
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-002**: analytics Store — fetchCategories テスト（STEP_0）
  - 種別: テスト
  - 内容: `fetchCategories()` アクションのテスト。半期推移タブ: GET /half-trends/categories、月別内訳タブ: GET /monthly-breakdown/categories の API モック検証。レスポンスから rows / grandTotal / monthLabels が正しく state にセットされることを検証。fiscalYear / halfPeriod / organizationCode パラメータの送信を検証。月別内訳タブでは month パラメータも検証
  - 成果物: `frontend/tests/stores/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-003**: analytics Store — fetchCategories 実装
  - 種別: 実装
  - 内容: `fetchCategories()` アクションの実装。currentApiBase getter に応じた API パスを使用。loading 状態管理を含む
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-002 のテストが Green 状態
  - 依存: T-002
  - 見積もり: 1 時間

- [ ] **T-004**: analytics Store — fetchSystems / fetchSubsystems テスト（STEP_1 / STEP_2）
  - 種別: テスト
  - 内容: `fetchSystems(cat1, cat2)` / `fetchSubsystems(cat1, cat2, sysNo)` のテスト。半期推移: GET /half-trends/systems, /half-trends/subsystems、月別内訳: GET /monthly-breakdown/systems, /monthly-breakdown/subsystems の API モック検証。category1Code / category2Code / systemNo パラメータの送信を検証。MYシステムフラグ（isMy）の正しいマッピングを検証
  - 成果物: `frontend/tests/stores/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-005**: analytics Store — fetchSystems / fetchSubsystems 実装
  - 種別: 実装
  - 内容: `fetchSystems()` / `fetchSubsystems()` アクションの実装
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-004 のテストが Green 状態
  - 依存: T-004
  - 見積もり: 1 時間

- [ ] **T-006**: analytics Store — drillDown / drillUp / goToStep テスト
  - 種別: テスト
  - 内容: ドリルダウン遷移のテスト。`drillDown(row)`: step 0→1 で drilldownContext.category1/2 設定 + fetchSystems 呼出、step 1→2 で drilldownContext.system 設定 + fetchSubsystems 呼出。`drillUp()`: step 逆遷移 + context クリア。`goToStep(n)`: 任意階層ジャンプ（step=2 から step=0 へ直接戻り等）+ 不要 context クリアを検証
  - 成果物: `frontend/tests/stores/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 2 時間

- [ ] **T-007**: analytics Store — drillDown / drillUp / goToStep 実装
  - 種別: 実装
  - 内容: ドリルダウン遷移アクションの実装。step 更新 + drilldownContext 管理 + 対応する fetch アクション呼出
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-006 のテストが Green 状態
  - 依存: T-006
  - 見積もり: 1 時間

- [ ] **T-008**: analytics Store — switchTab テスト
  - 種別: テスト
  - 内容: `switchTab(tab)` のテスト。activeTab 切替を検証。ドリルダウン階層（step, drilldownContext）が維持されることを検証。月別内訳タブ切替時に month がデフォルト設定されることを検証。現在の step に応じた正しい API が呼出されることを検証
  - 成果物: `frontend/tests/stores/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-009**: analytics Store — switchTab 実装
  - 種別: 実装
  - 内容: `switchTab(tab)` の実装。activeTab 更新 + step 維持 + 月別内訳タブの month デフォルト設定 + 再取得
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-008 のテストが Green 状態
  - 依存: T-008
  - 見積もり: 0.5 時間

- [ ] **T-010**: analytics Store — toggleMy テスト
  - 種別: テスト
  - 内容: `toggleMy(systemNo)` のテスト。楽観的更新: mySystems リストの追加/削除が即時反映されることを検証。POST /my-systems（登録）/ DELETE /my-systems/{systemNo}（解除）の API 呼出を検証。API 失敗時のロールバック（mySystems リスト復元）を検証
  - 成果物: `frontend/tests/stores/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-011**: analytics Store — toggleMy 実装
  - 種別: 実装
  - 内容: `toggleMy(systemNo)` の実装。楽観的更新パターン（UI 即時反映 → API 呼出 → 失敗時ロールバック）
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-010 のテストが Green 状態
  - 依存: T-010
  - 見積もり: 0.5 時間

- [ ] **T-012**: analytics Store — exportExcel / changeSort テスト
  - 種別: テスト
  - 内容: `exportExcel(type?)` のテスト。半期推移タブ: GET /half-trends/export/excel（テンプレート固定）、月別内訳タブ: GET /monthly-breakdown/export/excel?type=standard|management|management-detail のAPIパス生成を検証。Blob ダウンロード処理を検証。`changeSort(sortKey)` のテスト。sort パラメータ更新 → 再取得を検証
  - 成果物: `frontend/tests/stores/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-013**: analytics Store — exportExcel / changeSort 実装
  - 種別: 実装
  - 内容: `exportExcel()` / `changeSort()` の実装。Blob + createObjectURL によるダウンロード処理
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-012 のテストが Green 状態
  - 依存: T-012
  - 見積もり: 1 時間

- [ ] **T-014**: analytics Store — Getters テスト
  - 種別: テスト
  - 内容: 全 Getters のテスト。`currentApiBase`（activeTab に応じた /half-trends or /monthly-breakdown）、`breadcrumbItems`（step + drilldownContext からのパンくず生成、step 0/1/2 各パターン）、`dynamicColumns`（step に応じた左列定義: 分類/システム/サブシステム）、`monthColumns`（monthLabels からの月別列定義生成）、`filteredRows`（filterType=my 時の mySystems フィルタ）、`canExportExcel`（rows.length > 0 AND canExportHours AND canNavigateForms）の検証
  - 成果物: `frontend/tests/stores/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 2 時間

- [ ] **T-015**: analytics Store — Getters 実装
  - 種別: 実装
  - 内容: 全 Getters の実装
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-014 のテストが Green 状態
  - 依存: T-014
  - 見積もり: 1.5 時間

- [ ] **T-016**: FiscalYearResolver ユーティリティテスト
  - 種別: テスト
  - 内容: FiscalYearResolver の全パターンテスト（パラメタライズドテスト）。2014年度以前: 上期=4月〜9月、下期=10月〜3月（6ヶ月）。2015年度（特殊）: 上期=4月〜9月（6ヶ月）、下期=10月〜12月（3ヶ月のみ）。2016年度以降: 上期=1月〜6月、下期=7月〜12月（6ヶ月）。月ラベル生成の正確性を検証
  - 成果物: `frontend/tests/utils/fiscalYearResolver.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-017**: FiscalYearResolver ユーティリティ実装
  - 種別: 実装
  - 内容: FiscalYearResolver の実装。年度と半期から月ラベル配列と月範囲を返すユーティリティ。2015年度下期の3ヶ月特殊ケースを含む
  - 成果物: `frontend/utils/fiscalYearResolver.ts`
  - 完了条件: T-016 のテストが Green 状態
  - 依存: T-016
  - 見積もり: 1 時間

- [ ] **T-018**: analytics Store — リファクタリング
  - 種別: リファクタ
  - 内容: Store 全体のリファクタリング。重複コードの抽出、fetch 系アクションの共通化、エラーハンドリング統一、loading 状態管理の共通化
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: 既存テスト（T-002〜T-016）が全て Green のまま維持
  - 依存: T-003, T-005, T-007, T-009, T-011, T-013, T-015, T-017
  - 見積もり: 1 時間

### Phase 2: 検索パネル + URL パラメータ同期

- [ ] **T-019**: SearchPanel コンポーネントテスト
  - 種別: テスト
  - 内容: SearchPanel.vue のテスト。年度 Dropdown（2014〜現在年度、変更で再検索）、半期 Dropdown（上期/下期）、組織選択（OrganizationSearchDialog 連携）、工数/コスト SelectButton（displayMode 切替）、全部/指定/MY SelectButton（filterType 切替）、検索ボタン（fetchCategories + step=0 リセット）、リセットボタン（検索条件初期化）の検証
  - 成果物: `frontend/tests/components/analytics/SearchPanel.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-020**: SearchPanel コンポーネント実装
  - 種別: 実装
  - 内容: SearchPanel.vue の実装。PrimeVue Dropdown（年度/半期）、InputText + ダイアログ（組織）、SelectButton（工数・コスト / 全部・指定・MY）、Button（検索/リセット）。指定フィルタ時は SubsystemSearchDialog 連携
  - 成果物: `frontend/components/analytics/SearchPanel.vue`
  - 完了条件: T-019 のテストが Green 状態
  - 依存: T-019
  - 見積もり: 1.5 時間

- [ ] **T-021**: URL パラメータバリデーション＋同期テスト
  - 種別: テスト
  - 内容: URL パラメータ 10 項目のバリデーションとフォールバックルールのテスト（spec セクション 4.1「URL パラメータバリデーション」準拠）。各パラメータの正常値・不正値テスト:（1）tab: "half"/"month" 許容、不正値→"half"、（2）year: 2014〜現在年度、範囲外→現在年度、（3）half: "FIRST"/"SECOND"、不正値→現在半期、（4）step: 0/1/2、不正値→0、（5）cat1: step≧1 時に有効な分類コード必須、不足→step=0 に降格、（6）cat2: step≧1 時に有効なサブ分類コード必須、不足→step=0 に降格、（7）systemNo: step=2 時に有効なシステム番号必須、不足→step=1 に降格、（8）month: tab=month 時に "01"〜"12"（半期範囲内）、範囲外→半期の先頭月、（9）displayMode: "hours"/"cost"、不正値→"hours"、（10）filterType: "all"/"system"/"my"、不正値→"all"。さらに Store → URL 双方向同期テスト: Store 変更時の URL 自動更新、URL 直接アクセス時のストア状態復元、ドリルダウン遷移時の URL 更新を検証
  - 成果物: `frontend/tests/composables/useAnalyticsUrlSync.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 2.5 時間

- [ ] **T-022**: URL パラメータバリデーション＋同期実装
  - 種別: 実装
  - 内容: `composables/useAnalyticsUrlSync.ts` の実装。10 パラメータのバリデーション関数（validateUrlParams）: 各パラメータの型チェック・範囲チェック・フォールバック値適用。step 自動降格ロジック（cat1/cat2/systemNo 不足時）。半期範囲外 month のリセット。useRoute / useRouter の watch で双方向同期。URL パラメータの初期値からストア状態を復元する起動ロジック。不正パラメータ時はエラー表示せず静かにフォールバック（ユーザビリティ優先）
  - 成果物: `frontend/composables/useAnalyticsUrlSync.ts`
  - 完了条件: T-021 のテストが Green 状態
  - 依存: T-021
  - 見積もり: 1.5 時間

### Phase 3: タブ構造

- [ ] **T-023**: TabView + タブコンテナ コンポーネントテスト
  - 種別: テスト
  - 内容: TabView.vue / HalfTrendsTab.vue / MonthlyBreakdownTab.vue のテスト。タブ切替 → switchTab 呼出、ドリルダウン階層維持（step + drilldownContext 保持）の検証。月別内訳タブでのみ MonthSelector が表示されることを検証
  - 成果物: `frontend/tests/components/analytics/TabView.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-024**: TabView + タブコンテナ コンポーネント実装
  - 種別: 実装
  - 内容: TabView.vue（PrimeVue TabView ラッパー）、HalfTrendsTab.vue、MonthlyBreakdownTab.vue の実装
  - 成果物: `frontend/components/analytics/TabView.vue`, `HalfTrendsTab.vue`, `MonthlyBreakdownTab.vue`
  - 完了条件: T-023 のテストが Green 状態
  - 依存: T-023
  - 見積もり: 1 時間

- [ ] **T-025**: MonthSelector コンポーネントテスト
  - 種別: テスト
  - 内容: MonthSelector.vue のテスト。月別内訳タブでのみ表示されることを検証。monthLabels からの選択肢生成を検証（FiscalYearResolver 連携: 通常6ヶ月、2015年度下期3ヶ月）。月変更 → 再取得を検証
  - 成果物: `frontend/tests/components/analytics/MonthSelector.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001, T-017
  - 見積もり: 1 時間

- [ ] **T-026**: MonthSelector コンポーネント実装
  - 種別: 実装
  - 内容: MonthSelector.vue の実装。PrimeVue Dropdown で月選択。monthLabels に基づく選択肢生成
  - 成果物: `frontend/components/analytics/MonthSelector.vue`
  - 完了条件: T-025 のテストが Green 状態
  - 依存: T-025
  - 見積もり: 0.5 時間

### Phase 4: メインデータテーブル + 動的列生成

- [ ] **T-027**: AnalyticsDataTable コンポーネントテスト — STEP_0 分類別
  - 種別: テスト
  - 内容: AnalyticsDataTable.vue の STEP_0 テスト。分類1 / 分類2 の固定列 + 月別列（M1〜M6）+ 合計列のレンダリング。行クリック → drillDown 呼出。合計行の太字表示。セル値フォーマット（工数: "120:30"、コスト: "960,000円"）。ゼロ値の薄いグレー表示を検証
  - 成果物: `frontend/tests/components/analytics/AnalyticsDataTable.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 2 時間

- [ ] **T-028**: AnalyticsDataTable コンポーネント実装 — STEP_0 分類別
  - 種別: 実装
  - 内容: AnalyticsDataTable.vue の STEP_0 実装。PrimeVue DataTable + 動的列生成（dynamicColumns + monthColumns getter 使用）。固定列 + 月別列 + 合計列。行クリックイベント。displayMode に応じたセル値フォーマット
  - 成果物: `frontend/components/analytics/AnalyticsDataTable.vue`
  - 完了条件: T-027 のテストが Green 状態
  - 依存: T-027
  - 見積もり: 2 時間

- [ ] **T-029**: AnalyticsDataTable — STEP_1 システム別 テスト
  - 種別: テスト
  - 内容: STEP_1 の動的列テスト。★（MYシステム）+ SYS No + システム名 + 月別列 + 合計列のレンダリング。行クリック → drillDown 呼出（STEP_2 へ）。isMy フラグに応じた ★/☆ 表示を検証
  - 成果物: `frontend/tests/components/analytics/AnalyticsDataTable.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-028
  - 見積もり: 1 時間

- [ ] **T-030**: AnalyticsDataTable — STEP_1 システム別 実装
  - 種別: 実装
  - 内容: STEP_1 の列構成実装。dynamicColumns getter の STEP_1 用定義に基づく列レンダリング
  - 成果物: `frontend/components/analytics/AnalyticsDataTable.vue`
  - 完了条件: T-029 のテストが Green 状態
  - 依存: T-029
  - 見積もり: 1 時間

- [ ] **T-031**: AnalyticsDataTable — STEP_2 サブシステム別 テスト
  - 種別: テスト
  - 内容: STEP_2 の動的列テスト。★ + SYS No + システム名 + SS No + SS名 + 月別列 + 合計列のレンダリング。STEP_2 では行クリックでドリルダウンしないことを検証
  - 成果物: `frontend/tests/components/analytics/AnalyticsDataTable.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-030
  - 見積もり: 1 時間

- [ ] **T-032**: AnalyticsDataTable — STEP_2 サブシステム別 実装
  - 種別: 実装
  - 内容: STEP_2 の列構成実装。ソート機能（列ヘッダークリック → changeSort 呼出）を全 STEP 共通で実装
  - 成果物: `frontend/components/analytics/AnalyticsDataTable.vue`
  - 完了条件: T-031 のテストが Green 状態
  - 依存: T-031
  - 見積もり: 1 時間

### Phase 5: パンくず + ドリルダウンナビゲーション

- [ ] **T-033**: Breadcrumb コンポーネントテスト
  - 種別: テスト
  - 内容: Breadcrumb.vue のテスト。step=0: 「分類別」（非リンク）。step=1: 「分類別 > 障害対応 / 本番障害」（分類別がリンク）。step=2: 「分類別 > 障害対応 / 本番障害 > 基幹システム」（分類別、障害対応/本番障害がリンク）。パンくずクリック → goToStep(n) 呼出を検証
  - 成果物: `frontend/tests/components/analytics/Breadcrumb.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1 時間

- [ ] **T-034**: Breadcrumb コンポーネント実装
  - 種別: 実装
  - 内容: Breadcrumb.vue の実装。breadcrumbItems getter からパンくずリスト生成。クリックイベントで goToStep 呼出
  - 成果物: `frontend/components/analytics/Breadcrumb.vue`
  - 完了条件: T-033 のテストが Green 状態
  - 依存: T-033
  - 見積もり: 0.5 時間

### Phase 6: MYシステム星マーク

- [ ] **T-035**: MySystemStar コンポーネントテスト
  - 種別: テスト
  - 内容: MySystemStar.vue のテスト。★（MYシステム登録済み: ゴールド #FFD700）/ ☆（未登録: グレー #CCC）の表示切替。クリック → toggleMy 呼出を検証。楽観的更新の即時反映を検証
  - 成果物: `frontend/tests/components/analytics/cells/MySystemStar.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1 時間

- [ ] **T-036**: MySystemStar コンポーネント実装
  - 種別: 実装
  - 内容: MySystemStar.vue の実装。isMy props に応じた ★/☆ 表示。クリックイベントで toggleMy 呼出
  - 成果物: `frontend/components/analytics/cells/MySystemStar.vue`
  - 完了条件: T-035 のテストが Green 状態
  - 依存: T-035
  - 見積もり: 0.5 時間

### Phase 7: ツールバー + Excel 出力

- [ ] **T-037**: Toolbar コンポーネントテスト
  - 種別: テスト
  - 内容: Toolbar.vue のテスト。「分類別に戻る」ボタン（step > 0 で表示、goToStep(0) 呼出）、Excel 出力ボタン（canExportExcel 条件）、テーブル/グラフ切替ボタン（viewMode トグル）を検証。月別内訳タブ時の ExcelExportDialog 表示（4種テンプレート選択）を検証
  - 成果物: `frontend/tests/components/analytics/Toolbar.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-038**: Toolbar コンポーネント実装
  - 種別: 実装
  - 内容: Toolbar.vue の実装。分類別戻りボタン、Excel 出力ボタン + ExcelExportDialog（月別内訳用テンプレート選択）、テーブル/グラフ切替ボタン。確認ダイアログ CZ-516 連携
  - 成果物: `frontend/components/analytics/Toolbar.vue`
  - 完了条件: T-037 のテストが Green 状態
  - 依存: T-037
  - 見積もり: 1.5 時間

### Phase 8: グラフ表示

- [ ] **T-039**: AnalyticsChart コンポーネントテスト
  - 種別: テスト
  - 内容: AnalyticsChart.vue / BarChart.vue / LineChart.vue のテスト。棒グラフ: X軸=月ラベル、Y軸=工数/コスト、系列=分類別/システム別上位5件+その他。折れ線グラフ: 合計ラインの太線表示。displayMode 切替でのY軸ラベル変更を検証。viewMode=chart 時のみレンダリングされることを検証
  - 成果物: `frontend/tests/components/analytics/AnalyticsChart.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-040**: AnalyticsChart コンポーネント実装
  - 種別: 実装
  - 内容: AnalyticsChart.vue / BarChart.vue / LineChart.vue の実装。Chart.js (vue-chartjs) による棒グラフ・折れ線グラフ。rows データからチャートデータセット生成
  - 成果物: `frontend/components/analytics/AnalyticsChart.vue`, `BarChart.vue`, `LineChart.vue`
  - 完了条件: T-039 のテストが Green 状態
  - 依存: T-039
  - 見積もり: 2 時間

### Phase 9: ページ統合

- [ ] **T-041**: AnalyticsPage ページコンテナテスト
  - 種別: テスト
  - 内容: pages/analytics.vue のテスト。マウント時の URL パラメータ読取 + デフォルト値設定（fiscalYear=現在年度、halfPeriod=現在半期、activeTab=half、step=0）+ fetchCategories 自動呼出を検証。子コンポーネント（SearchPanel / TabView / Breadcrumb / Toolbar / AnalyticsDataTable / AnalyticsChart）の描画確認
  - 成果物: `frontend/tests/pages/analytics.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-020, T-022, T-024, T-026, T-028, T-032, T-034, T-036, T-038, T-040
  - 見積もり: 1.5 時間

- [ ] **T-042**: AnalyticsPage ページコンテナ実装
  - 種別: 実装
  - 内容: pages/analytics.vue の実装。全子コンポーネントの配置、onMounted での URL パラメータ解析 + 初期データ取得、useAnalyticsUrlSync composable 適用
  - 成果物: `frontend/pages/analytics.vue`
  - 完了条件: T-041 のテストが Green 状態
  - 依存: T-041
  - 見積もり: 1 時間

### Phase 10: 権限・エラーハンドリング

- [ ] **T-043**: 権限別 UI 制御テスト
  - 種別: テスト
  - 内容: アクター別 UI 差異のテスト。canNavigateForms=false のアクター（ACT-01, ACT-04, ACT-05）が画面にアクセスできないことを検証。ACT-09（外部契約者）のアクセス拒否を検証。組織検索範囲の制限（ACT-01: 自組織のみ、ACT-02: 配下、ACT-03: 全組織）を検証。canExportHours による Excel ボタン表示/非表示を検証
  - 成果物: `frontend/tests/components/analytics/permissions.integration.test.ts`
  - 完了条件: テストが通り、全アクターパターンがカバーされる
  - 依存: T-042
  - 見積もり: 1.5 時間

- [ ] **T-044**: エラーハンドリングテスト
  - 種別: テスト
  - 内容: エラーケースのテスト。データなし（rows=[] → エンプティメッセージ表示）、MYシステム操作失敗（500 → 楽観的更新ロールバック + Toast）、Excel 出力エラー（500 → Toast）、ネットワークエラー（→ Toast + ローディング解除）の検証
  - 成果物: `frontend/tests/stores/analytics.errors.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005, T-011, T-013
  - 見積もり: 1 時間

- [ ] **T-045**: エラーハンドリング実装
  - 種別: 実装
  - 内容: Store アクションにエラーハンドリングを追加。各エラーケースで適切な Toast 通知と UI 状態復元を実装
  - 成果物: `frontend/stores/analytics.ts`
  - 完了条件: T-044 のテストが Green 状態
  - 依存: T-044
  - 見積もり: 0.5 時間

### Phase 11: 受け入れ基準テスト（AC-AN-01〜AC-AN-13）

- [ ] **T-046**: 受け入れ基準テスト — 初期表示・ドリルダウン（AC-AN-01〜AC-AN-04）
  - 種別: テスト
  - 内容: spec セクション 10 の GWT 受け入れ基準テスト（前半）。AC-AN-01（初期表示）: canNavigateForms 権限ユーザーがログイン → 半期推移タブ選択状態で現在年度・現在半期の STEP_0 分類別集計テーブルが表示。AC-AN-02（分類→システム ドリルダウン）: STEP_0 分類行クリック → STEP_1 テーブル + Breadcrumb 更新 + URL に step=1&cat1&cat2 反映。AC-AN-03（システム→サブシステム ドリルダウン）: STEP_1 システム行クリック → STEP_2 テーブル + Breadcrumb 3階層更新。AC-AN-04（ドリルアップ Breadcrumb）: STEP_2 で Breadcrumb「分類別」クリック → STEP_0 に戻り + URL リセット
  - 成果物: `frontend/tests/acceptance/analytics-drilldown.ac.test.ts`
  - 完了条件: テストが Green 状態（実装完了後のため）
  - 依存: T-042
  - 見積もり: 2 時間

- [ ] **T-047**: 受け入れ基準テスト — タブ切替・MYシステム（AC-AN-05〜AC-AN-07）
  - 種別: テスト
  - 内容: AC-AN-05（タブ切替）: 半期推移タブでドリルダウン中 → 「月別内訳」タブクリック → 月別内訳テーブル表示 + URL に tab=month 反映 + ドリルダウン状態維持。AC-AN-06（MYシステム登録）: STEP_1 で未登録 ☆ クリック → 即座に ★（楽観的更新）+ API 成功で維持 + API 失敗時は ☆ に戻り Toast。AC-AN-07（MYシステム解除）: STEP_1 で登録済み ★ クリック → 即座に ☆ + API で削除
  - 成果物: `frontend/tests/acceptance/analytics-tab-my.ac.test.ts`
  - 完了条件: テストが Green 状態
  - 依存: T-042
  - 見積もり: 1.5 時間

- [ ] **T-048**: 受け入れ基準テスト — 表示モード・特殊ケース（AC-AN-08〜AC-AN-11）
  - 種別: テスト
  - 内容: AC-AN-08（工数/コスト切替）: 工数モード（HH:MM）→ コスト切替 → 全セルが 3桁カンマ+円 表示に変化。AC-AN-09（2015年度下期特殊ケース）: year=2015, half=SECOND → 月別列が M1〜M3 の3列（10月,11月,12月）のみ。AC-AN-10（URL パラメータ同期）: STEP_1 + 月別内訳タブの URL をコピー → 新タブで開く → 同一の分類・ステップ・タブ状態で復元。AC-AN-11（ゼロ値の表示）: 工数ゼロのセル → 薄グレー (#CCC) で "0:00" 表示
  - 成果物: `frontend/tests/acceptance/analytics-display.ac.test.ts`
  - 完了条件: テストが Green 状態
  - 依存: T-042
  - 見積もり: 2 時間

- [ ] **T-049**: 受け入れ基準テスト — データなし・Excel出力（AC-AN-12〜AC-AN-13）
  - 種別: テスト
  - 内容: AC-AN-12（データなし）: 対象年度・半期にデータなし（rows=[]）→ 「該当する集計データがありません」メッセージ表示。AC-AN-13（Excel 出力）: データ表示中 → [Excel] ボタン → 確認ダイアログ「はい」→ テンプレート Excel ダウンロード実行。半期推移タブ: テンプレート固定ダウンロード。月別内訳タブ: テンプレート選択ダイアログ → 選択 → ダウンロード
  - 成果物: `frontend/tests/acceptance/analytics-error-export.ac.test.ts`
  - 完了条件: テストが Green 状態
  - 依存: T-042, T-045
  - 見積もり: 1.5 時間

### Phase 12: レスポンシブ + リファクタリング

- [ ] **T-050**: レスポンシブレイアウト実装
  - 種別: 実装
  - 内容: 3ブレークポイント対応。>=1280px（フルテーブル、固定列 + 月別6列 + 合計）、960-1279px（サイドナビ折りたたみ + 水平スクロール）、<960px（サイドナビ非表示 + 固定列最小化）。STEP 別の固定列制御（STEP_0: 分類1,2 / STEP_1: ★,システム名 / STEP_2: ★,SS名）
  - 成果物: `frontend/components/analytics/AnalyticsDataTable.vue`
  - 完了条件: 各ブレークポイントで固定列とスクロールが正しく動作する
  - 依存: T-032
  - 見積もり: 1 時間

- [ ] **T-051**: コンポーネント層リファクタリング
  - 種別: リファクタ
  - 内容: 全コンポーネントの横断的リファクタリング。重複ロジックの composable 抽出、Props/Emit の型定義整理、動的列生成ロジックの最適化、PrimeVue パススルー設定の整理
  - 成果物: 各コンポーネントファイル
  - 完了条件: 既存テスト（T-019〜T-049）が全て Green のまま維持
  - 依存: T-042, T-043, T-045, T-049, T-050
  - 見積もり: 1.5 時間

### Phase 13: E2E テスト

- [ ] **T-052**: E2E — 基本フロー + ドリルダウン3階層
  - 種別: テスト
  - 内容: Playwright E2E テスト。ログイン → 分析画面 → 分類別テーブル表示確認。分類行クリック → システム別テーブル（パンくず更新確認）。システム行クリック → サブシステム別テーブル。パンくず「分類別」クリック → STEP_0 に戻り。[分類別に戻る] ボタンで STEP_0 に直接戻り
  - 成果物: `frontend/tests/e2e/analytics/drilldown.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-042
  - 見積もり: 2 時間

- [ ] **T-053**: E2E — タブ切替 + 月選択 + MYシステム + Excel
  - 種別: テスト
  - 内容: Playwright E2E テスト。半期推移 → 月別内訳タブ切替 → ドリルダウン階層維持確認。月別内訳 → 月変更 → データ再取得確認。STEP_1 で ☆クリック → ★に変化。フィルタ「MY」選択 → 登録システムのみ表示。半期推移 Excel 出力 → ダウンロード確認。月別内訳 Excel → テンプレート選択ダイアログ表示
  - 成果物: `frontend/tests/e2e/analytics/tab-my-excel.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-042
  - 見積もり: 2 時間

- [ ] **T-054**: E2E — 工数/コスト切替 + URL 状態 + 年度半期ルール + 権限
  - 種別: テスト
  - 内容: Playwright E2E テスト。[コスト] 選択 → テーブル値がカンマ区切り円表示に変化。ドリルダウン → URL パラメータ反映 → URL 直接アクセスで状態復元確認。2015年度下期 → 月ラベルが3列（10月,11月,12月）のみ確認。列ヘッダークリック → ソート確認。ACT-01（担当者）→ 組織スコープが自組織のみに制限されることを確認
  - 成果物: `frontend/tests/e2e/analytics/display-url-permission.spec.ts`
  - 完了条件: E2E テストが通る
  - 依存: T-042
  - 見積もり: 2 時間

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
      → T-014 → T-015
T-016 → T-017
T-003 + T-005 + T-007 + T-009 + T-011 + T-013 + T-015 + T-017 → T-018

Phase 2 (Search + URL Validation):
T-001 → T-019 → T-020
T-001 → T-021 → T-022

Phase 3 (Tabs):
T-001 → T-023 → T-024
T-001 + T-017 → T-025 → T-026

Phase 4 (DataTable):
T-001 → T-027 → T-028 → T-029 → T-030 → T-031 → T-032

Phase 5-6 (Navigation + MY):
T-001 → T-033 → T-034
T-001 → T-035 → T-036

Phase 7-8 (Toolbar + Chart):
T-001 → T-037 → T-038
T-001 → T-039 → T-040

Phase 9 (Page):
T-020 + T-022 + T-024 + T-026 + T-028 + T-032 + T-034 + T-036 + T-038 + T-040 → T-041 → T-042

Phase 10 (Permissions + Error):
T-042 → T-043
T-003 + T-005 + T-011 + T-013 → T-044 → T-045

Phase 11 (Acceptance Criteria AC-AN-01〜13):
T-042 → T-046 (AC-AN-01〜04)
T-042 → T-047 (AC-AN-05〜07)
T-042 → T-048 (AC-AN-08〜11)
T-042 + T-045 → T-049 (AC-AN-12〜13)

Phase 12 (Responsive + Refactoring):
T-032 → T-050
T-042 + T-043 + T-045 + T-049 + T-050 → T-051

Phase 13 (E2E):
T-042 → T-052, T-053, T-054
```

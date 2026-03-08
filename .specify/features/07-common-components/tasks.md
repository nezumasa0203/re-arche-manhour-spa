# 共通コンポーネント タスク一覧

## 概要
- 総タスク数: 46
- 見積もり合計: 61 時間

全画面（FORM_010〜042）で共有するレイアウト、ナビゲーション、
ダイアログ（モーダル）、入力、表示コンポーネント群。
現行 MPA の FRAMESET 入れ子構造（40+ フレーム）+ window.open() ポップアップを、
Nuxt.js 3 SPA の CSS Grid レイアウト + PrimeVue モーダルに移行する。

### 移行元ソースコード参照

実装・テスト時に以下の移行元コードを参照し、振る舞いの同等性を検証すること。
全ファイルは `D:\PROJECT02\migration_source\migration_soource_irpmng_czConsv\` 配下。

| コンポーネント | 踏襲元機能 | 移行元ファイル |
|---------------|-----------|---------------|
| StatusBadge | ステータス色定義 | `czResources/cssjs/czstyle.css` |
| HoursInput | 時間入力マスク・自動変換 | `czResources/cssjs/TdMask.js` |
| HoursInput | バイト長計算 | `czResources/cssjs/ApCheck.js`（chkChara 関数） |
| useStatusMatrix | 12状態マトリクス | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/proc/StatusKeyManager.java` |
| ConfirmDialog | CZ-5xx 確認ダイアログ | `czResources/cssjs/ApMessage.xml` |
| OrganizationSearchDialog | 組織ツリー検索 | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/OrganizationSearchJspBean.java` |
| SubsystemSearchDialog | SS検索 | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/SubsystemSearchJspBean.java` |
| StaffSearchDialog | 職員検索 | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/StaffSearchJspBean.java` |

---

## タスク一覧

### Phase 1: ユーティリティ（テストファースト）

- [ ] **T-001**: statusMatrix ユーティリティのテストを作成
  - 種別: テスト
  - 内容: buildStatusKey の 16 パターンテスト（STATUS 0/1/2/9 × getsujiKakutei true/false × dataSyuukei true/false → うち有効 12 キー: 000,010,011,100,110,111,200,210,211,900,910,911）。resolveStatusMatrix の 168 パターンテスト（12状態 × 2系列 × 7操作 = add/copy/delete/update/view/statusUpdate/statusView）
  - 移行元参照: `czConsv/WEB-INF/src/jp/co/isid/cz/integ/proc/StatusKeyManager.java` のロジックと照合
  - 成果物: `tests/utils/statusMatrix.test.ts`
  - 完了条件: 168 パターンのパラメタライズドテストが Red
  - 依存: なし
  - 見積もり: 2 時間

- [ ] **T-002**: statusMatrix ユーティリティを実装
  - 種別: 実装
  - 内容: utils/statusMatrix.ts。buildStatusKey() 関数。resolveStatusMatrix() 関数。ENABLED=1, DISABLED=0, HIDDEN=9 定数。Backend StatusMatrixResolver と同一ロジック
  - 成果物: `utils/statusMatrix.ts`
  - 完了条件: T-001 のテストが全て Green
  - 依存: T-001
  - 見積もり: 1.5 時間

- [ ] **T-003**: byteLength ユーティリティのテストを作成
  - 種別: テスト
  - 内容: calculateByteLength の全パターンテスト。半角英数字("abc"→3)、全角("あいう"→6)、半角カタカナ("ｱｲｳ"→6)、混在("aあｱ"→5)、空文字列(""→0)、128バイト境界値、BMP外文字
  - 移行元参照: `czResources/cssjs/ApCheck.js`（chkChara 関数）のバイト長計算ロジックと照合
  - 成果物: `tests/utils/byteLength.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-004**: byteLength ユーティリティを実装
  - 種別: 実装
  - 内容: utils/byteLength.ts。for...of ループ。U+FF61〜U+FF9F→2B、U+0080以上→2B、U+0000〜U+007F→1B
  - 成果物: `utils/byteLength.ts`
  - 完了条件: T-003 のテストが全て Green
  - 依存: T-003
  - 見積もり: 0.5 時間

### Phase 2: Composables

- [ ] **T-005**: useStatusMatrix composable のテストを作成
  - 種別: テスト
  - 内容: resolveOperationState(record, monthControl, isTanSeries, operation) → ENABLED/DISABLED/HIDDEN。canPerformOperation(record, operation) → boolean。ステータスマトリクス結果に基づくUI制御
  - 成果物: `tests/composables/useStatusMatrix.test.ts`
  - 完了条件: テストが Red
  - 依存: T-002
  - 見積もり: 1 時間

- [ ] **T-006**: useStatusMatrix composable を実装
  - 種別: 実装
  - 内容: composables/useStatusMatrix.ts。statusMatrix.ts のラッパー。リアクティブな状態管理
  - 成果物: `composables/useStatusMatrix.ts`
  - 完了条件: T-005 のテストが全て Green
  - 依存: T-005
  - 見積もり: 0.5 時間

- [ ] **T-007**: useHoursFormat composable のテストを作成
  - 種別: テスト
  - 内容: parseHours の全変換パターン（空→null、"3"→{h:3,m:0}、"12"→{h:12,m:0}、"330"→{h:3,m:30}、"0330"→{h:3,m:30}、"3:30"→{h:3,m:30}、"03:30"→{h:3,m:30}、"abc"→null）。formatHours。validate（15分単位、最小0:15、最大24:00）
  - 移行元参照: `czResources/cssjs/TdMask.js`（時間マスク処理）の変換ロジックと照合
  - 成果物: `tests/composables/useHoursFormat.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-008**: useHoursFormat composable を実装
  - 種別: 実装
  - 内容: composables/useHoursFormat.ts。parseHours, formatHours, validateHours 関数
  - 成果物: `composables/useHoursFormat.ts`
  - 完了条件: T-007 のテストが全て Green
  - 依存: T-007
  - 見積もり: 0.5 時間

- [ ] **T-009**: useApi composable のテストを作成
  - 種別: テスト
  - 内容: get/post/patch/delete/getBlob メソッド。JWT 自動付与（Authorization: Bearer）。X-Delegation-Staff-Id 代行ヘッダー。エラーインターセプター（401→ログイン、403→Toast、409→再読込、500→エラーToast）。リトライ（ネットワークエラー時3回、指数バックオフ）
  - 成果物: `tests/composables/useApi.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-010**: useApi composable を実装
  - 種別: 実装
  - 内容: composables/useApi.ts。$fetch ベース。baseURL: /api/v1。useAuth 連携（JWT 取得）。代行モードヘッダー。CZ コードレンジ別自動 severity 判定。getBlob（Excel 出力用 Blob）
  - 成果物: `composables/useApi.ts`
  - 完了条件: T-009 のテストが全て Green
  - 依存: T-009
  - 見積もり: 1.5 時間

- [ ] **T-011**: useConfirmDialog composable のテストを作成
  - 種別: テスト
  - 内容: confirm(message, options) → Promise<boolean>。CZ-505〜518 メッセージ対応。severity 別アイコン。OK/キャンセルラベルカスタマイズ
  - 成果物: `tests/composables/useConfirmDialog.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-012**: useConfirmDialog composable を実装
  - 種別: 実装
  - 内容: composables/useConfirmDialog.ts。Promise ベースの確認ダイアログ管理
  - 成果物: `composables/useConfirmDialog.ts`
  - 完了条件: T-011 のテストが全て Green
  - 依存: T-011
  - 見積もり: 0.5 時間

- [ ] **T-013**: useMessage composable のテストを作成
  - 種別: テスト
  - 内容: success/warn/error/info メソッド。CZ コードレンジ自動判定。自動消去（success:3秒、warn:5秒、error:手動、info:3秒）
  - 成果物: `tests/composables/useMessage.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-014**: useMessage composable を実装
  - 種別: 実装
  - 内容: composables/useMessage.ts。PrimeVue Toast ラッパー
  - 成果物: `composables/useMessage.ts`
  - 完了条件: T-013 のテストが全て Green
  - 依存: T-013
  - 見積もり: 0.5 時間

### Phase 3: テーマ・デザイントークン

- [ ] **T-015**: CSS カスタムプロパティとテーマ設定
  - 種別: 実装
  - 内容: :root に CZ デザイントークン定義（--cz-status-0〜9、--cz-header-height、--cz-sidenav-width 等）。PrimeVue Lara プリセット設定。nuxt.config.ts の PrimeVue テーマ設定（unstyled:false、darkModeSelector:false）。Constitution 準拠（elevation="0" + border、Slate-700 テキスト色）
  - 成果物: `assets/css/variables.css`, `nuxt.config.ts`（PrimeVue 設定）
  - 完了条件: CSS 変数がアプリ全体で利用可能
  - 依存: なし
  - 見積もり: 1 時間

### Phase 4: レイアウト基盤

- [ ] **T-016**: AppHeader のテストを作成
  - 種別: テスト
  - 内容: アプリ名表示。ユーザー名（CzPrincipal.userName）表示。モード表示（jinjiMode: "人事"青/"管理"緑）。代行表示（isDaiko=true: "代行中:{対象者名}"バッジ）。ハンバーガーボタン（sideNavCollapsed トグル）。DevActorSwitcher（NUXT_PUBLIC_ENABLE_ACTOR_SWITCH=true のみ表示）
  - 成果物: `tests/components/layout/AppHeader.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-017**: AppHeader を実装
  - 種別: 実装
  - 内容: components/layout/AppHeader.vue。PrimeVue Button（ハンバーガー）、Badge（モード/代行）。useAuth 連携
  - 成果物: `components/layout/AppHeader.vue`
  - 完了条件: T-016 のテストが全て Green
  - 依存: T-016
  - 見積もり: 1 時間

- [ ] **T-018**: AppSideNav のテストを作成
  - 種別: テスト
  - 内容: メニュー表示条件（canReport→工数入力、canManage→工数管理、canNavigateForms→分析、canFullAccess→設定）。アクティブ状態（現在ルート一致）。折畳/展開（220px⇔56px）。レスポンシブ3段階（>=1280px展開、960-1279px折畳、<960pxオーバーレイ）。localStorage 永続化
  - 成果物: `tests/components/layout/AppSideNav.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-019**: AppSideNav を実装
  - 種別: 実装
  - 内容: components/layout/AppSideNav.vue。PrimeVue Menu。CSS transition（幅アニメーション）。localStorage で折畳状態保存。window.matchMedia でレスポンシブ判定
  - 成果物: `components/layout/AppSideNav.vue`
  - 完了条件: T-018 のテストが全て Green
  - 依存: T-018
  - 見積もり: 1 時間

- [ ] **T-020**: AppBreadcrumb のテストを作成
  - 種別: テスト
  - 内容: ルート定義に基づく自動生成テスト。/work-hours → [ホーム, 工数入力]。/analytics/half-trends → [ホーム, 分析, 半期推移]
  - 成果物: `tests/components/layout/AppBreadcrumb.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-021**: AppBreadcrumb を実装
  - 種別: 実装
  - 内容: components/layout/AppBreadcrumb.vue。PrimeVue Breadcrumb。useRoute() からパス解析
  - 成果物: `components/layout/AppBreadcrumb.vue`
  - 完了条件: T-020 のテストが全て Green
  - 依存: T-020
  - 見積もり: 0.5 時間

- [ ] **T-022**: default レイアウトを実装
  - 種別: 実装
  - 内容: layouts/default.vue。CSS Grid（AppHeader 56px 固定 + AppSideNav + NuxtPage）。sideNavCollapsed 状態管理
  - 成果物: `layouts/default.vue`
  - 完了条件: レイアウトが正しくレンダリングされる
  - 依存: T-017, T-019, T-021
  - 見積もり: 1 時間

### Phase 5: 基本表示コンポーネント

- [ ] **T-023**: StatusBadge のテストを作成
  - 種別: テスト
  - 内容: 4ステータスの色分け表示（0=黄 #FBFBB6、1=緑 #BDEAAD、2=青 #9DBDFE、9=灰 #5D5D5D 白文字）。size='sm'/'md' の2サイズ。ラベルテキスト（作成中/確認/確定/非表示）
  - 移行元参照: `czResources/cssjs/czstyle.css`（ステータス色定義）の色コードと照合
  - 成果物: `tests/components/common/StatusBadge.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-024**: StatusBadge を実装
  - 種別: 実装
  - 内容: components/common/StatusBadge.vue。CSS 変数（--cz-status-0〜9）使用。inline-block + border-radius
  - 成果物: `components/common/StatusBadge.vue`
  - 完了条件: T-023 のテストが全て Green
  - 依存: T-023
  - 見積もり: 0.5 時間

- [ ] **T-025**: MessageToast のテストを作成
  - 種別: テスト
  - 内容: PrimeVue Toast ラッパー。success(緑, 3秒)/warn(黄, 5秒)/error(赤, 手動)/info(青, 3秒)。CZ コードレンジ連携テスト
  - 成果物: `tests/components/common/MessageToast.test.ts`
  - 完了条件: テストが Red
  - 依存: T-014
  - 見積もり: 0.5 時間

- [ ] **T-026**: MessageToast を実装
  - 種別: 実装
  - 内容: components/common/MessageToast.vue。PrimeVue Toast コンポーネントのラッパー
  - 成果物: `components/common/MessageToast.vue`
  - 完了条件: T-025 のテストが全て Green
  - 依存: T-025
  - 見積もり: 0.5 時間

### Phase 6: 入力コンポーネント

- [ ] **T-027**: HoursInput のテストを作成
  - 種別: テスト
  - 内容: 自動変換全パターン（空→エラー、"3"→"03:00"、"12"→"12:00"、"330"→"03:30"、"0330"→"03:30"、"3:30"→"03:30"）。バリデーション（必須CZ-126、HH:MM形式CZ-125、15分単位CZ-147、最小0:15 CZ-129、最大24:00 CZ-146）。readonly モード。blur イベントで変換+バリデーション
  - 成果物: `tests/components/common/HoursInput.test.ts`
  - 完了条件: テストが Red
  - 依存: T-008
  - 見積もり: 1 時間

- [ ] **T-028**: HoursInput を実装
  - 種別: 実装
  - 内容: components/common/HoursInput.vue。PrimeVue InputText。blur → parseHours → formatHours → emit('update:modelValue')。validated イベントで ValidationResult 通知
  - 成果物: `components/common/HoursInput.vue`
  - 完了条件: T-027 のテストが全て Green
  - 依存: T-027
  - 見積もり: 1 時間

- [ ] **T-029**: MonthSelector のテストを作成
  - 種別: テスト
  - 内容: ±12ヶ月 Dropdown オプション生成（現在月基準25件）。<</>>/ナビゲーション。範囲外でボタン無効化。update:modelValue イベント発火
  - 成果物: `tests/components/common/MonthSelector.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-030**: MonthSelector を実装
  - 種別: 実装
  - 内容: components/common/MonthSelector.vue。PrimeVue Dropdown + ナビボタン
  - 成果物: `components/common/MonthSelector.vue`
  - 完了条件: T-029 のテストが全て Green
  - 依存: T-029
  - 見積もり: 0.5 時間

### Phase 7: ダイアログコンポーネント

- [ ] **T-031**: OrganizationSearchDialog のテストを作成
  - 種別: テスト
  - 内容: PrimeVue Tree でツリー表示（GET /masters/organizations/tree）。インクリメンタルサーチ（300ms デバウンス → フィルタ + ハイライト）。単一選択 (mode='single')。複数選択 (mode='multiple', チェックボックス)。scopeFilter（スコープ外グレーアウト）。initialValue（初期展開）
  - 移行元参照: `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/OrganizationSearchJspBean.java` のツリー構築・検索ロジックと照合
  - 成果物: `tests/components/common/OrganizationSearchDialog.test.ts`
  - 完了条件: テストが Red
  - 依存: T-010
  - 見積もり: 1.5 時間

- [ ] **T-032**: OrganizationSearchDialog を実装
  - 種別: 実装
  - 内容: components/common/OrganizationSearchDialog.vue。PrimeVue Dialog + Tree。useApi で API 呼出。デバウンス検索
  - 成果物: `components/common/OrganizationSearchDialog.vue`
  - 完了条件: T-031 のテストが全て Green
  - 依存: T-031
  - 見積もり: 1.5 時間

- [ ] **T-033**: SubsystemSearchDialog のテストを作成
  - 種別: テスト
  - 内容: PrimeVue DataTable + Paginator。インクリメンタルサーチ（300ms デバウンス → GET /masters/subsystems?keyword=）。◆マーカー（sysKbn=1）。50件/ページ。mode='target'/'cause' タイトル切替。行クリック → 選択確定
  - 移行元参照: `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/SubsystemSearchJspBean.java` の検索・表示ロジックと照合
  - 成果物: `tests/components/common/SubsystemSearchDialog.test.ts`
  - 完了条件: テストが Red
  - 依存: T-010
  - 見積もり: 1 時間

- [ ] **T-034**: SubsystemSearchDialog を実装
  - 種別: 実装
  - 内容: components/common/SubsystemSearchDialog.vue。PrimeVue Dialog + DataTable + Paginator
  - 成果物: `components/common/SubsystemSearchDialog.vue`
  - 完了条件: T-033 のテストが全て Green
  - 依存: T-033
  - 見積もり: 1 時間

- [ ] **T-035**: StaffSearchDialog のテストを作成
  - 種別: テスト
  - 内容: 2タブ構成（組織ツリータブ / 検索タブ）。組織ツリータブ: 左PrimeVue Tree + 右担当者リスト。検索タブ: 氏名入力 + 一致タイプ(完全/部分)。purpose='delegation'（GET /delegation/available-staff）vs purpose='search'（GET /masters/staff）
  - 移行元参照: `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/StaffSearchJspBean.java` のタブ構成・検索ロジックと照合
  - 成果物: `tests/components/common/StaffSearchDialog.test.ts`
  - 完了条件: テストが Red
  - 依存: T-010
  - 見積もり: 1 時間

- [ ] **T-036**: StaffSearchDialog を実装
  - 種別: 実装
  - 内容: components/common/StaffSearchDialog.vue。PrimeVue Dialog + TabView + Tree + DataTable
  - 成果物: `components/common/StaffSearchDialog.vue`
  - 完了条件: T-035 のテストが全て Green
  - 依存: T-035
  - 見積もり: 1.5 時間

- [ ] **T-037**: ConfirmDialog のテストを作成
  - 種別: テスト
  - 内容: メッセージ表示（CZ-505〜518）。severity 別アイコン・色制御（info/warn）。OK → confirm イベント。キャンセル → cancel イベント。confirmLabel/cancelLabel カスタマイズ
  - 移行元参照: `czResources/cssjs/ApMessage.xml`（CZ-5xx メッセージ定義）と照合
  - 成果物: `tests/components/common/ConfirmDialog.test.ts`
  - 完了条件: テストが Red
  - 依存: T-012
  - 見積もり: 0.5 時間

- [ ] **T-038**: ConfirmDialog を実装
  - 種別: 実装
  - 内容: components/common/ConfirmDialog.vue。PrimeVue Dialog (modal)
  - 成果物: `components/common/ConfirmDialog.vue`
  - 完了条件: T-037 のテストが全て Green
  - 依存: T-037
  - 見積もり: 0.5 時間

### Phase 7b: ダイアログ共通状態表示パターン

- [ ] **T-043**: ダイアログ共通状態表示パターンのテストを作成
  - 種別: テスト
  - 内容: spec.md「ダイアログ共通状態表示パターン」6パターンのテスト。(1) ローディング中: ダイアログ内にスケルトンローダー（PrimeVue Skeleton）表示、操作ボタンdisabled。(2) 検索結果0件: 「該当するデータが見つかりません」メッセージ表示、[選択]ボタンdisabled（SubsystemSearchDialog, StaffSearchDialog）。(3) ツリー読込中: ツリーエリアにスピナー表示（OrganizationSearchDialog）。(4) ツリーノードなし: 「組織データが存在しません」メッセージ表示（OrganizationSearchDialog）。(5) APIエラー: ダイアログ内にToastエラー通知表示、ダイアログは閉じない（リトライ可能）。(6) useApiローディング: useApiのloading refがtrueの間、呼出元にローディング表示
  - 成果物: `tests/components/common/DialogStatePatterns.test.ts`
  - 完了条件: 6パターンのテストが Red
  - 依存: T-010, T-031, T-033, T-035
  - 見積もり: 1.5 時間

- [ ] **T-044**: OrganizationSearchDialog にローディング/空状態を実装
  - 種別: 実装
  - 内容: ツリー読込中にPrimeVue ProgressSpinner表示。ツリーノードなし時に「組織データが存在しません」EmptyMessage表示。API呼出中は[選択]ボタンdisabled。APIエラー時はダイアログ内Toastでエラー表示しダイアログを閉じない
  - 成果物: `components/common/OrganizationSearchDialog.vue`（更新）
  - 完了条件: T-043 の OrganizationSearchDialog 関連テストが Green
  - 依存: T-032, T-043
  - 見積もり: 1 時間

- [ ] **T-045**: SubsystemSearchDialog にローディング/空状態を実装
  - 種別: 実装
  - 内容: 検索中にPrimeVue Skeleton表示。検索結果0件時に「該当するデータが見つかりません」EmptyMessage表示、[選択]ボタンdisabled。APIエラー時はダイアログ内Toastでエラー表示しダイアログを閉じない
  - 成果物: `components/common/SubsystemSearchDialog.vue`（更新）
  - 完了条件: T-043 の SubsystemSearchDialog 関連テストが Green
  - 依存: T-034, T-043
  - 見積もり: 1 時間

- [ ] **T-046**: StaffSearchDialog にローディング/空状態を実装
  - 種別: 実装
  - 内容: 検索中にPrimeVue Skeleton表示。検索結果0件時に「該当するデータが見つかりません」EmptyMessage表示、[選択]ボタンdisabled。APIエラー時はダイアログ内Toastでエラー表示しダイアログを閉じない
  - 成果物: `components/common/StaffSearchDialog.vue`（更新）
  - 完了条件: T-043 の StaffSearchDialog 関連テストが Green
  - 依存: T-036, T-043
  - 見積もり: 1 時間

### Phase 8: DevActorSwitcher（開発環境限定）

- [ ] **T-039**: DevActorSwitcher のテストを作成
  - 種別: テスト
  - 内容: NUXT_PUBLIC_ENABLE_ACTOR_SWITCH='true' で表示、未設定で非表示。15アクター Dropdown。切替時: POST /api/switch → JWT 再取得 → 全ストアリセット → ページリロード。本番ビルドでバンドル非含有（三重安全策）
  - 成果物: `tests/components/dev/DevActorSwitcher.test.ts`
  - 完了条件: テストが Red
  - 依存: T-010
  - 見積もり: 0.5 時間

- [ ] **T-040**: DevActorSwitcher を実装
  - 種別: 実装
  - 内容: components/dev/DevActorSwitcher.vue。PrimeVue Dropdown。環境変数ゲート。Tree Shaking 対応（dynamic import）
  - 成果物: `components/dev/DevActorSwitcher.vue`
  - 完了条件: T-039 のテストが全て Green
  - 依存: T-039
  - 見積もり: 0.5 時間

### Phase 9: E2E テスト

- [ ] **T-041**: サイドナビ + レスポンシブ E2E テスト
  - 種別: テスト
  - 内容: Playwright。メニュー遷移→各ページロード確認。権限でメニュー項目制限。1280px→960px→800px でサイドナビの展開/折畳/非表示
  - 成果物: `tests/e2e/common-sidenav.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-022
  - 見積もり: 1.5 時間

- [ ] **T-042**: ダイアログ E2E テスト
  - 種別: テスト
  - 内容: Playwright。組織選択（ツリー展開→検索→選択確定）。SS選択（検索→◆マーカー確認→ページ送り→選択）。担当者選択（組織ツリータブ→選択、検索タブ→部分一致検索→選択）。DevActorSwitcher（dev環境: アクター切替→権限変化確認）。ダイアログ共通状態表示パターン: ローディング中のスケルトン表示、検索結果0件メッセージ、APIエラー時のリトライ可能表示
  - 成果物: `tests/e2e/common-dialogs.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-032, T-034, T-036, T-040, T-044, T-045, T-046
  - 見積もり: 2.5 時間

---

## 依存関係図

```
Phase 1 (Utilities)
T-001→T-002  T-003→T-004

Phase 2 (Composables)
T-005→T-006  T-007→T-008  T-009→T-010  T-011→T-012  T-013→T-014

Phase 3 (Theme)
T-015

Phase 4 (Layout)
T-016→T-017  T-018→T-019  T-020→T-021
T-017,T-019,T-021 → T-022

Phase 5 (Display)
T-023→T-024  T-025→T-026

Phase 6 (Input)
T-027→T-028  T-029→T-030

Phase 7 (Dialogs)
T-031→T-032  T-033→T-034  T-035→T-036  T-037→T-038

Phase 7b (Dialog State Patterns)
T-043 (テスト: 依存 T-010,T-031,T-033,T-035)
T-043→T-044 (OrganizationSearchDialog, 依存 T-032)
T-043→T-045 (SubsystemSearchDialog, 依存 T-034)
T-043→T-046 (StaffSearchDialog, 依存 T-036)

Phase 8 (Dev)
T-039→T-040

Phase 9 (E2E)
T-041  T-042 (依存追加: T-044,T-045,T-046)
```

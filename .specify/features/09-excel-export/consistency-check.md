# #9 Excel Export — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `09-excel-export/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1-#8/#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| フラットデザイン | ✅ 合致 | constitution V「影なし・border 基調」。spec: セクション 2.3「交互色なし（フラットデザイン準拠）」 |
| Spring Boot REST API | ✅ 合致 | constitution 技術スタック。spec: セクション 4 で Controller + Service 構成 |
| Doma 2 ORM | ✅ 合致 | spec: DAO 経由でデータ取得 |
| TDD | ✅ 合致 | constitution IV。spec: セクション 7 で Backend + E2E テスト要件 |
| Docker-First | ✅ 合致 | API 呼出先が Backend コンテナ |
| Migration-First | ✅ 合致 | 既存 4 テンプレートを踏襲。.xls → .xlsx への形式改善のみ |
| 年度期間ルール | ✅ 合致 | constitution I「2015年特例3ヶ月」。spec: セクション 7.1 テストで「2015年度下期 → 3列」確認 |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| Apache POI HSSF | .xls (Excel 97-2003) | .xlsx (XSSF, OpenXML) に移行 | ✅ IMPROVE (GAP-E01) |
| IFRAME ダウンロード | 非表示 IFRAME + ap_dummy.jsp | Blob + createObjectURL | ✅ IMPROVE (GAP-E03) |
| テンプレート 4種 | InsertListDetail / HalfSuii / MonthUtiwake 3種 | 6テンプレート（月別内訳を3種に分離） | ✅ KEEP + IMPROVE |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| US-01A 工数明細 Excel | ✅ 合致 | セクション 3.1 テンプレート 1 |
| US-034 半期推移 Excel | ✅ 合致 | セクション 3.3 テンプレート 3 |
| US-043 月別内訳 Excel | ✅ 合致 | セクション 3.4-3.6 テンプレート 4-6 |

### 05_gap_analysis.md との整合

| 項目 | GAP ID | 結果 | 詳細 |
|---|---|---|---|
| POI 形式改善 | GAP-E01 | ✅ 合致 | HSSF → XSSF |
| テンプレート踏襲 | GAP-E02 | ✅ 合致 | 4種テンプレート保全 |
| DL 方式改善 | GAP-E03 | ✅ 合致 | Blob ダウンロード |
| CSV 出力 | GAP-E04 | ✅ 合致 | P2 将来対応 |
| PDF 出力 | GAP-E05 | ✅ 合致 | P3 将来対応 |
| プレビュー | GAP-E06 | ✅ 合致 | P2 将来対応 |
| 非同期生成 | GAP-E07 | ✅ 合致 | P1 将来対応（大量データ時） |

---

## C. 他 Spec との整合性

### spec #2 (auth-infrastructure) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| canExportHours 権限制御 | ⚠️ 記載なし | spec #2 セクション 6.1.1 で `canExportHours` (tab011.bit0) を「保守H時間出力機能の利用可否」と定義。しかし spec #9 では 6 エンドポイントすべてに権限チェック記載なし → **FIX-E01** |
| JWT 認証 | ✅ 合致 | セクション 5.1 で Authorization: Bearer ヘッダー付与 |
| DataAuthority スコープ | ✅ 合致 | セクション 7.2 E2E テスト「組織スコープが Excel 出力範囲に反映」 |

### spec #3 (core-api-design) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| GET /work-hours/export/excel | ✅ 合致 | spec #3 のエンドポイント定義 |
| CZ-516 確認ダイアログ | ✅ 合致 | セクション 5.2 で出力前確認 |
| CZ-317 Excel エラー | ✅ 合致 | spec #8 セクション 7.3 で定義 |

### spec #4 (work-hours-input) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 工数明細列定義 | ✅ 合致 | セクション 3.1 の 15 列が FORM_010 のデータ構造に対応 |
| ソート条件反映 | ✅ 合致 | パラメータ `sort=workDate:asc` |

### spec #5 (work-status-list) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 工数状況一覧テンプレート | ✅ 合致 | セクション 3.2 で FORM_020 のデータをそのまま出力 |

### spec #6 (analysis-screens) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 半期推移 STEP_0/1/2 | ✅ 合致 | セクション 3.3 でドリルダウン階層に応じた出力 |
| 月別内訳 3 種 | ✅ 合致 | セクション 3.4-3.6 で type=standard/management/management-detail |
| displayMode (工数/コスト) | ✅ 合致 | セクション 3.3 パラメータに displayMode 含む |

### spec #7 (common-components) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| CZ-516 確認ダイアログ | ✅ 合致 | spec #7 セクション 7.3 に CZ-516 定義 |
| useApi composable | ⚠️ 未使用 | セクション 5.1 で `useApi()` をインポートしつつ raw `fetch` を使用。Blob 取得のためだが、JWT 取得元が未定義 → 実装時の注意事項（FIX 不要） |

### spec #8 (validation-error-system) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| CZ-315 ダウンロード失敗 | ✅ 合致 | spec #8 セクション 7.3 で定義 |
| CZ-317 Excel 出力失敗 | ✅ 合致 | spec #8 セクション 7.3 で定義 |

---

## D. レガシーシステムとの差異分類

### KEEP（踏襲）

| 項目 | 現行 | 新システム |
|---|---|---|
| テンプレート 4 種構成 | InsertListDetail / HalfSuii / MonthUtiwake 3種 | 6テンプレート（月別内訳を3サブタイプに明示分離） |
| 共通フォーマット（MS ゴシック 10pt） | テンプレートファイル埋込 | ExcelStyleHelper で一元管理 |
| 印刷設定（A4 横向き、ヘッダー繰返し） | テンプレートファイル埋込 | コード生成 |
| シートレイアウト（タイトル→ヘッダー→データ→合計） | 固定レイアウト | 同一構造 |

### IMPROVE（改善）

| 項目 | 現行 | 新システム | GAP ID |
|---|---|---|---|
| ファイル形式 | .xls (HSSF, 65536行上限) | .xlsx (XSSF, 104万行) | GAP-E01 |
| DL方式 | 非表示 IFRAME | Blob + createObjectURL | GAP-E03 |
| ライブラリ | POI HSSF | POI XSSF | GAP-E01 |
| Content-Type | application/vnd.ms-excel | application/vnd.openxmlformats-... | — |
| スタイル管理 | 各 Creator に個別定義 | ExcelStyleHelper で共通化 | — |

### ADD（新規追加）

| 項目 | 説明 | 優先度 |
|---|---|---|
| CSV 出力 (BOM 付き UTF-8) | — | P2 将来対応 |
| PDF 出力 | — | P3 将来対応 |
| 出力前プレビュー | — | P2 将来対応 |
| 大量データ非同期生成 | — | P1 将来対応 |

### REMOVE（廃止）

| 項目 | 理由 |
|---|---|
| 非表示 IFRAME ダウンロード | Blob DL に置換 |
| .xls 形式 | .xlsx に統一 |
| テンプレートファイル (.xls) | コード生成に移行 |

---

## E. 推奨アクション

### FIX-E01: Excel 出力エンドポイントの権限チェック記載漏れ (P1)

**箇所**: spec 全体（セクション 1, 4.3）

**問題**: 6 つの Excel 出力エンドポイントすべてに権限チェックの記載がない。
spec #2 セクション 6.1.1 で `canExportHours` (tab011.bit0) が
「保守H時間出力機能の利用可否」として定義されているが、
spec #9 ではどのエンドポイントにこの権限が適用されるか不明。

**アクター別の tab011 値**:
| アクター | tab011 | canExportHours (bit0) | Excel 出力 |
|---|---|---|---|
| ACT-01 報告担当者 | '00' | false | ？ |
| ACT-02 報告管理者 | '11' | true | 可能 |
| ACT-03 全権管理者 | '11' | true | 可能 |
| ACT-04 管理モードユーザー | '10' | true | 可能 |
| ACT-05 人事モードユーザー | '00' | false | ？ |
| ACT-06〜09 人事モード系 | '00' | false | ？ |
| ACT-10〜14 管理スタッフ系 | '11' or '10' | true | 可能 |
| ACT-15 営業 | '10' | true | 可能 |

**修正**: セクション 1 テンプレート一覧テーブルに「権限」列を追加し、
各エンドポイントの必要権限を明記する。推奨:
- FORM_010 工数明細: `canExportHours` (tab011.bit0) 必要
- FORM_020 工数状況一覧: `canManage` (tab010.bit1) + `canExportHours` 必要
- FORM_030-040 分析系: 各画面アクセス権限 + `canExportHours` 必要

**注意**: ACT-01（報告担当者）は canExportHours=false のため、
自身の工数明細 Excel 出力もできない可能性がある。
レガシーシステムの動作を確認し、ACT-01 の FORM_010 Excel ボタン
表示/非表示を明確にする必要がある。

---

## F. 変更履歴

| 日付 | FIX ID | 内容 | 対象ファイル | 状態 |
|---|---|---|---|---|
| 2026-02-26 | FIX-E01 | Excel 出力権限チェック追記（6エンドポイントに canExportHours + 画面アクセス権限を明記） | 09-excel-export/spec.md | ✅ 完了 |

# Excel 出力: テンプレート・生成・ダウンロード仕様

## 概要

全画面（FORM_010/020/030/040）からの Excel 出力機能。
現行 Apache POI HSSF (.xls, Excel 97-2003) + 非表示 IFRAME ダウンロードを、
Apache POI XSSF (.xlsx, OpenXML) + Blob ダウンロードに移行する。

**対応ユーザーストーリー**: US-01A, US-034, US-043
**対応 GAP**: GAP-E01〜E07, GAP-D04

---

## 1. テンプレート一覧

| # | テンプレート | 画面 | エンドポイント | 権限 | US |
|---|------------|------|---------------|------|-----|
| 1 | 工数明細 | FORM_010 | `GET /work-hours/export/excel` | `canExportHours` (tab011.bit0) | US-01A |
| 2 | 工数状況一覧 | FORM_020 | `GET /work-status/export/excel` | `canManage` (tab010.bit1) + `canExportHours` | - |
| 3 | 半期推移 | FORM_030 | `GET /half-trends/export/excel` | `canNavigateForms` (tab011.bit1) + `canExportHours` | US-034 |
| 4 | 月別内訳（標準） | FORM_040 | `GET /monthly-breakdown/export/excel?type=standard` | `canNavigateForms` + `canExportHours` | US-043 |
| 5 | 月別内訳（管理用） | FORM_040 | `GET /monthly-breakdown/export/excel?type=management` | `canNavigateForms` + `canExportHours` | US-043 |
| 6 | 月別内訳（管理詳細） | FORM_040 | `GET /monthly-breakdown/export/excel?type=management-detail` | `canNavigateForms` + `canExportHours` | US-043 |

> **権限注意**: `canExportHours` (tab011.bit0) が false のアクター（ACT-01, ACT-05〜09）は
> Excel 出力ボタンを非表示にする。Frontend では `canExportHours` で Excel ボタンの v-if 制御、
> Backend では各 export エンドポイントの先頭で `canExportHours` チェックを行い、
> 権限不足時は CZ-308 (403) を返す。

---

## 2. 共通仕様

### 2.1 ファイル形式

| 項目 | 現行 | 新規 |
|------|------|------|
| 形式 | .xls (HSSF, Excel 97-2003) | **.xlsx (XSSF, OpenXML)** |
| ライブラリ | Apache POI HSSF | Apache POI XSSF |
| 最大行数 | 65,536 行 | 1,048,576 行 |
| Content-Type | application/vnd.ms-excel | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |

### 2.2 レスポンスヘッダー

```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="work_hours_202502.xlsx"
Cache-Control: no-cache, no-store, must-revalidate
```

ファイル名規則: `{テンプレート種別}_{YYYYMM}.xlsx`

| テンプレート | ファイル名例 |
|------------|------------|
| 工数明細 | `work_hours_202502.xlsx` |
| 工数状況一覧 | `work_status_202502.xlsx` |
| 半期推移 | `half_trends_2025_first.xlsx` |
| 月別内訳（標準） | `monthly_breakdown_202502.xlsx` |
| 月別内訳（管理用） | `monthly_breakdown_mgmt_202502.xlsx` |
| 月別内訳（管理詳細） | `monthly_breakdown_mgmt_detail_202502.xlsx` |

### 2.3 共通フォーマット

| 項目 | 設定 |
|------|------|
| フォント | MS ゴシック, 10pt |
| ヘッダー行 | 背景: 薄灰色 (#D9D9D9), 太字, 中央揃え |
| データ行 | 交互色なし（フラットデザイン準拠） |
| 罫線 | 細線（全セル） |
| 工数値 | 右揃え, HH:MM 形式 |
| コスト値 | 右揃え, #,##0 形式 |
| 日付 | YYYY/MM/DD 形式 |
| シート名 | テンプレート種別名 |
| 印刷設定 | A4 横向き, ヘッダー行繰り返し |

---

## 3. テンプレート詳細

### 3.1 工数明細 (FORM_010)

**対応 US**: US-01A
**現行クラス**: `InsertListDetailOutputUnit`

#### シートレイアウト

```
┌───┬─────────────────────────────────────────────────┐
│ A │ 工数明細                                         │ ← タイトル行
│   │ 対象月: 2025年02月  担当者: 鈴木花子 (d10623)     │ ← サブタイトル
├───┼────┬──────┬───────┬───────┬─────┬──────┬────────┤
│   │STS │作業日│対象SS  │原因SS  │分類  │件名   │工数    │ ← ヘッダー
├───┼────┼──────┼───────┼───────┼─────┼──────┼────────┤
│ 1 │作成│02/25 │会計    │人事    │障害  │月次..  │03:30   │
│ 2 │確認│02/24 │CRM    │CRM    │保守  │定期..  │02:00   │
│...│    │      │       │       │     │      │        │
├───┼────┴──────┴───────┴───────┴─────┴──────┼────────┤
│   │                                  合計   │120:30  │ ← 合計行
└───┴─────────────────────────────────────────┴────────┘
```

#### 列定義

| 列 | ヘッダー | 幅 (文字) | データ型 |
|----|---------|----------|---------|
| A | No | 5 | 連番 |
| B | ステータス | 8 | 文字列 (作成中/確認/確定) |
| C | 作業日 | 12 | 日付 (YYYY/MM/DD) |
| D | 保守担当所属 | 20 | 文字列 |
| E | 保守担当者名 | 15 | 文字列 |
| F | 対象SS No | 10 | 文字列 |
| G | 対象SS名 | 25 | 文字列 |
| H | 原因SS No | 10 | 文字列 |
| I | 原因SS名 | 25 | 文字列 |
| J | 保守カテゴリ | 20 | 文字列 |
| K | 件名 | 35 | 文字列 |
| L | 工数 | 8 | HH:MM |
| M | TMR番号 | 8 | 文字列 |
| N | 依頼書No | 10 | 文字列 |
| O | 依頼者名 | 15 | 文字列 |

#### パラメータ

```
GET /api/v1/work-hours/export/excel
  ?yearMonth=2025-02
  &staffId=d10623         (代行時)
  &sort=workDate:asc      (現在のソート条件を反映)
```

### 3.2 工数状況一覧 (FORM_020)

#### シートレイアウト

```
┌─────────────────────────────────────────────────────┐
│ 工数状況一覧                                         │
│ 対象月: 2025年02月  組織: IT推進部  月次: 確認済      │
├────┬──────┬───────┬──────┬───────┬─────┬──────┬─────┤
│STS │所属   │担当者 │作業日│対象SS  │分類  │件名   │工数  │
├────┼──────┼───────┼──────┼───────┼─────┼──────┼─────┤
│確認│開発1課│鈴木   │02/25 │会計    │障害  │月次..  │03:30│
│確定│開発2課│田中   │02/24 │CRM    │保守  │定期..  │02:00│
└────┴──────┴───────┴──────┴───────┴─────┴──────┴─────┘
```

パラメータ: 検索条件（yearMonth, organizationCode, statusFilter）をそのまま反映。

### 3.3 半期推移 (FORM_030)

**対応 US**: US-034
**現行テンプレート**: `tmp_HalfSuii.xls`
**現行クラス**: `HalfSuiiExcelReportCreator.java`

#### シートレイアウト

```
┌──────────────────────────────────────────────────────────┐
│ 半期推移                                                  │
│ 2025年度 上期  組織: IT推進部  表示: 工数                   │
├──────┬───────┬───────┬───────┬───────┬───────┬───────┬───┤
│分類1 │分類2  │ 1月   │ 2月   │ 3月   │ 4月   │ 5月   │合計│
├──────┼───────┼───────┼───────┼───────┼───────┼───────┼───┤
│障害  │本番   │120:00 │130:00 │110:00 │115:00 │125:00 │600│
│障害  │開発不具│ 80:00 │ 75:00 │ 85:00 │ 70:00 │ 90:00 │400│
│保守  │定期   │ 60:00 │ 55:00 │ 65:00 │ 50:00 │ 70:00 │300│
├──────┴───────┼───────┼───────┼───────┼───────┼───────┼───┤
│     合計      │260:00 │260:00 │260:00 │235:00 │285:00 │1300│
└──────────────┴───────┴───────┴───────┴───────┴───────┴───┘
```

#### 出力内容

- 現在のドリルダウン階層 (STEP_0/1/2) のデータをそのまま出力
- STEP_0: 分類1 + 分類2 + 月別6列 + 合計
- STEP_1: ★(MY) + SYS No + システム名 + 月別6列 + 合計
- STEP_2: ★ + SYS No + システム名 + SS No + SS名 + 月別6列 + 合計
- 合計行は太字
- 印刷ヘッダー繰り返し設定

#### パラメータ

```
GET /api/v1/half-trends/export/excel
  ?fiscalYear=2025
  &halfPeriod=FIRST
  &organizationCode=100210
  &displayMode=hours
  &filterType=all
  &step=0                    (現在のドリルダウン階層)
  &category1Code=01          (STEP_1/2 時)
  &category2Code=01-01       (STEP_1/2 時)
  &systemNo=SYS001           (STEP_2 時)
```

### 3.4 月別内訳 — 標準 (FORM_040, type=standard)

**現行テンプレート**: `tmp_MonthUtiwake.xls`

半期推移と同じ構造。月パラメータを追加で指定。
特定月のデータのみ1列で出力。

```
GET /api/v1/monthly-breakdown/export/excel
  ?type=standard
  &fiscalYear=2025
  &halfPeriod=FIRST
  &month=01
  &organizationCode=100210
```

### 3.5 月別内訳 — 管理用 (FORM_040, type=management)

**現行テンプレート**: `tmp_Kanri_MonthUtiwake.xls`

管理者向けの集計フォーマット。STEP 階層（分類→システム→サブシステム）ごとに
保全カテゴリ別工数を集計する。

**シートレイアウト（復元元: `MonthUtiwakeExcelReportCreator.java` / `tmp_Kanri_MonthUtiwake.xls`）**

#### 固定左列（STEP 階層）

| 列 | 内容 | 幅 |
|---|---|---|
| A | 分類（STEP 名称） | 20 |
| B | システム名 | 25 |
| C | サブシステム名 | 25 |

#### 動的右列（保全カテゴリ別工数）

保全カテゴリ（`mcz02_hosyu_kategori`）の件数に応じて動的に列が生成される。

| 列グループ | 内容 | 備考 |
|---|---|---|
| D〜 | 各保全カテゴリの工数 | カテゴリ数 × 2列（保全/運用） |
| 最終列 | 合計 | 全カテゴリの合算 |

#### ヘッダ構造（2行ヘッダ）

```
行1: | 分類 | システム | サブシステム | 保全1(保全) | 保全1(運用) | 保全2(保全) | 保全2(運用) | ... | 合計 |
行2: |      |         |             | (カテゴリ名) | (カテゴリ名) | (カテゴリ名) | (カテゴリ名) | ... |      |
```

#### データ変換

- DB 格納値: 時間 × 24（整数）
- 表示値: DB値 ÷ 24（小数 → `#,##0.00` 書式）

#### パラメータ

```
GET /api/v1/monthly-breakdown/export/excel
  ?type=management
  &fiscalYear=2025
  &halfPeriod=FIRST
  &month=01
  &organizationCode=100210
```

### 3.6 月別内訳 — 管理詳細 (FORM_040, type=management-detail)

**現行テンプレート**: `tmp_KanriDetail_MonthUtiwake.xls`
**現行クラス**: `MonthUtiwakeKanriDetailOutputCmd.java`

管理詳細は個々のレコード明細を含む。作業単位の詳細データを25固定列で出力し、
保全カテゴリ別の工数を動的列で追加する。

**シートレイアウト（復元元: `MonthUtiwakeKanriDetailExcelReportCreator.java` / `tmp_KanriDetail_MonthUtiwake.xls`）**

#### 固定左列（25列）

| 列 | # | 内容 | 備考 |
|---|---|---|---|
| A | 1 | No（連番） | 自動採番 |
| B | 2 | 作業日 | YYYY/MM/DD 書式 |
| C〜G | 3〜7 | 対象システム情報 | 分類/システム/サブシステム/グループキー/名称 |
| H〜L | 8〜12 | 原因システム情報 | 分類/システム/サブシステム/グループキー/名称 |
| M | 13 | 担当者 | 社員名 |
| N | 14 | 件名 | 作業件名 |
| O | 15 | 依頼書No | 依頼書番号 |
| P | 16 | 依頼者名 | 依頼者社員名 |
| Q | 17 | TMR No | TMR 管理番号 |
| R〜V | 18〜22 | 予備列 | レガシー互換 |
| W | 23 | ステータス | 状態表示 |
| X | 24 | 人事区分 | 雇用形態 |
| Y | 25 | 合計工数 | 全カテゴリ合算 |

#### 動的右列（保全カテゴリ別工数）

固定25列の右に、保全カテゴリ（`mcz02_hosyu_kategori`）の件数分だけ列が追加される。

| 列グループ | 内容 | 備考 |
|---|---|---|
| Z〜 | 各保全カテゴリの工数 | カテゴリごとに1列 |

#### データ変換

- DB 格納値: 時間 × 24（整数）
- 表示値: DB値 ÷ 24（小数 → `#,##0.00` 書式）

#### 集計行

最終行にサマリー行（合計）を出力。固定列は空白、工数列のみ SUM。

#### パラメータ

```
GET /api/v1/monthly-breakdown/export/excel
  ?type=management-detail
  &fiscalYear=2025
  &halfPeriod=FIRST
  &month=01
  &organizationCode=100210
```

---

## 4. Backend 実装設計

### 4.1 パッケージ構成

```
com.example.czConsv/
├── service/
│   └── ExcelExportService.java          ← Excel 生成の統括
├── excel/
│   ├── WorkHoursExcelCreator.java       ← テンプレート 1
│   ├── WorkStatusExcelCreator.java      ← テンプレート 2
│   ├── HalfTrendsExcelCreator.java      ← テンプレート 3
│   ├── MonthlyBreakdownExcelCreator.java ← テンプレート 4,5,6
│   └── ExcelStyleHelper.java           ← 共通スタイル
└── controller/
    ├── WorkHoursController.java         ← exportExcel()
    ├── WorkStatusController.java        ← exportExcel()
    ├── HalfTrendsController.java        ← exportExcel()
    └── MonthlyBreakdownController.java  ← exportExcel()
```

### 4.2 ExcelExportService

```java
@Service
public class ExcelExportService {

    public byte[] exportWorkHours(String yearMonth, String staffId, String sort) {
        List<WorkHoursEntity> records = workHoursDao.findForExport(yearMonth, staffId, sort);
        return new WorkHoursExcelCreator().create(records, yearMonth, staffId);
    }

    public byte[] exportHalfTrends(HalfTrendsExportParams params) {
        // step に応じて categories/systems/subsystems を取得
        // HalfTrendsExcelCreator で .xlsx 生成
    }

    public byte[] exportMonthlyBreakdown(MonthlyBreakdownExportParams params) {
        // type に応じたテンプレートで生成
    }
}
```

### 4.3 Controller エンドポイント

```java
@GetMapping("/export/excel")
public ResponseEntity<byte[]> exportExcel(
        @RequestParam String yearMonth,
        @RequestParam(required = false) String staffId) {

    byte[] content = excelExportService.exportWorkHours(yearMonth, staffId, sort);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"work_hours_" + yearMonth.replace("-","") + ".xlsx\"")
        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .body(content);
}
```

### 4.4 API パラメータバリデーション

| パラメータ | 形式 | 必須 | 許容値 | エラー時 |
|-----------|------|:----:|--------|---------|
| yearMonth | `YYYY-MM` | Yes | 2015-04 〜 現在月 | 400 Bad Request |
| staffId | String(6) | No（デフォルト: ログインユーザー） | 有効な従業員ID | 400 Bad Request |
| sort | `{field}:{dir}` | No（デフォルト: workDate:asc） | field: workDate/status/systemName, dir: asc/desc | デフォルト値使用 |
| fiscalYear | Integer(4) | Yes（テンプレート3-6） | 2014 〜 現在年度 | 400 Bad Request |
| halfPeriod | Enum | Yes（テンプレート3-6） | FIRST, SECOND | 400 Bad Request |
| organizationCode | String(6) | Yes（テンプレート2） | 有効な組織コード | 400 Bad Request |
| displayMode | Enum | Yes（テンプレート3-6） | hours, cost | 400 Bad Request |
| step | Integer(1) | Yes（テンプレート3-6） | 0, 1, 2 | 400 Bad Request |
| type | Enum | Yes（テンプレート4-6） | standard, management, management-detail | 400 Bad Request |
| month | String(2) | Yes（テンプレート4-6） | 01-12（半期範囲内） | 400 Bad Request |

---

## 5. Frontend 実装設計

### 5.1 ダウンロード方式 (GAP-E03)

現行の非表示 IFRAME (`ap_dummy.jsp`) を Blob ダウンロードに移行。

```typescript
// composables/useExcelExport.ts
export function useExcelExport() {
  const { getBlob } = useApi()

  async function download(url: string, filename: string) {
    // useApi の getBlob() を使用（JWT ヘッダー自動付与・エラーハンドリング統一）
    const blob = await getBlob(url)
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    link.click()
    URL.revokeObjectURL(objectUrl)
  }

  return { download }
}
```

> **実装メモ**: `useApi` composable（spec #3）に `getBlob()` メソッドを追加する。
> 内部的には `$fetch(url, { responseType: 'blob' })` で Blob レスポンスを取得し、
> JWT ヘッダー付与・エラーハンドリング（CZ-315 ダウンロードエラー等）を統一する。

### Excel 出力フロー図

```
[ユーザー]                [Frontend]              [Backend]
    |                         |                       |
    |-- Excel ボタンクリック -->|                       |
    |                         |-- CZ-516 確認表示 ---->|
    |                         |                       |
    |-- 「はい」選択 --------->|                       |
    |                         |-- スピナー表示         |
    |                         |-- GET /excel/xxx ---->|
    |                         |                       |-- 権限チェック
    |                         |                       |   NG: 403 CZ-308
    |                         |                       |   OK: Excel 生成
    |                         |                       |       失敗: 500 CZ-317
    |                         |<-- Blob レスポンス ----|
    |                         |-- Blob DL 実行        |
    |                         |   失敗: CZ-315 Toast  |
    |<-- ファイル保存ダイアログ|                       |
    |                         |-- スピナー解除         |
    |                         |-- 完了 Toast 表示     |
    |                         |                       |
    |-- 「いいえ」選択 ------->|                       |
    |                         |-- ダイアログ閉じ       |
    |                         |-- 元の画面状態維持     |
```

### 5.2 確認ダイアログ

Excel 出力前に CZ-516 確認ダイアログを表示:
「Excel出力します。時間がかかる場合があります。よろしいですか？」

### 5.3 ローディング表示

ダウンロード中はボタンにスピナー表示 + 無効化。
完了時に Toast で「Excel出力が完了しました」。

---

## 6. GAP 対応マッピング

| GAP ID | 区分 | 本 spec での対応 |
|--------|------|----------------|
| GAP-D04 | IMPROVE | セクション 2.1 — .xls → .xlsx (XSSF) |
| GAP-E01 | IMPROVE | セクション 2.1 — Apache POI XSSF |
| GAP-E02 | KEEP | セクション 3 — 4種テンプレート踏襲 |
| GAP-E03 | IMPROVE | セクション 5.1 — Blob + createObjectURL DL |
| GAP-E04 | ADD/P2 | P2: CSV 出力オプション（BOM 付き UTF-8）は将来対応 |
| GAP-E05 | ADD/P3 | P3: PDF 出力は将来対応 |
| GAP-E06 | ADD/P2 | P2: 出力前プレビューは将来対応 |
| GAP-E07 | IMPROVE/P1 | P1: 大量データ時の非同期生成は将来対応 |

---

## 7. テスト要件

### 7.1 Backend テスト

| テスト | 内容 |
|--------|------|
| WorkHoursExcelCreator | 列定義・ヘッダー・データ行・合計行の正確性 |
| HalfTrendsExcelCreator | STEP_0/1/2 の動的列構成、月ラベル、合計行 |
| MonthlyBreakdownExcelCreator | 4種 type の出力差異 |
| ExcelStyleHelper | フォント・罫線・背景色の共通スタイル |
| ファイルサイズ | 1000行データで .xlsx サイズが妥当（< 1MB） |
| 年度半期ルール | 2015年度下期 → 3列（10月,11月,12月） |

### 7.2 E2E テスト

| シナリオ | 内容 |
|---------|------|
| 工数明細 DL | Excel ボタン → CZ-516 確認 → .xlsx ダウンロード → ファイル検証 |
| 半期推移 DL | 各 STEP での出力 → 列構成の正確性 |
| 月別4種 | テンプレート選択 → 各 type の出力確認 |
| 空データ | レコード 0 件 → ヘッダーのみの Excel が生成 |
| 権限 | 組織スコープが Excel 出力範囲に反映されること |

### 受け入れ基準（Given-When-Then）

**AC-EX-01: 工数明細 Excel ダウンロード（テンプレート1）**
- Given: FORM_010 で 2025年02月の工数明細が表示されている
- When: [Excel] ボタンをクリックし、CZ-516 確認ダイアログで「はい」を選択する
- Then: work_hours_202502.xlsx がダウンロードされ、完了 Toast が表示される

**AC-EX-02: Excel ファイル内容の正確性**
- Given: 対象月に10件のレコードが存在する
- When: 工数明細 Excel をダウンロードする
- Then: ヘッダー行が15列定義通りで、データ行が画面と同一ソート順、合計行の工数が一致する

**AC-EX-03: 空データ時の Excel 出力**
- Given: 対象月にレコードが0件
- When: Excel ダウンロードを実行する
- Then: ヘッダー行のみの .xlsx ファイルが生成される

**AC-EX-04: 権限不足時の拒否**
- Given: canExportHours=false のユーザー
- When: Excel ダウンロード API に直接アクセスする
- Then: 403 Forbidden、CZ-308 が返される

**AC-EX-05: ダウンロードキャンセル**
- Given: [Excel] ボタンをクリックし CZ-516 確認ダイアログが表示されている
- When: 「いいえ」を選択する
- Then: ダイアログが閉じ、ダウンロードは実行されず、元の画面状態に戻る

**AC-EX-06: ダウンロード失敗時のエラー表示**
- Given: Excel ダウンロードを実行中
- When: ネットワークエラーが発生する
- Then: CZ-315 エラー Toast が表示され、スピナーが解除される

**AC-EX-07: Excel 生成エラー**
- Given: サーバー側で Excel 生成処理を実行中
- When: テンプレート生成でエラーが発生する
- Then: CZ-317 エラーが返され、フロントエンドでエラー Toast が表示される

**AC-EX-08: 半期推移 Excel（テンプレート3）**
- Given: FORM_030 で 2025年度上期の半期推移データが表示されている
- When: Excel 出力を実行する
- Then: half_trend_2025_FIRST.xlsx がダウンロードされ、M1〜M6 の6列が含まれる

**AC-EX-09: 2015年度下期特殊ケース（テンプレート3）**
- Given: 2015年度下期の半期推移が表示されている
- When: Excel 出力を実行する
- Then: M1〜M3 の3列（10月, 11月, 12月）のみが含まれる Excel が生成される

**AC-EX-10: ローディング中のボタン無効化**
- Given: Excel ダウンロードを開始した
- When: ダウンロード処理中
- Then: [Excel] ボタンがスピナー表示+disabled となり、二重クリックが防止される

# コア API 設計: REST API エンドポイント定義

## 概要

CZ（保有資源管理システム）の SPA 化に伴う REST API 設計。
現行 MPA の 88 Unit / 139 JSP を **4 SPA ページ + 5 モーダル** に統合し、
それを支える Backend REST API を定義する。

**対象分析ドキュメント**:
- `analysis/03_user_stories.md` — 45 ユーザーストーリー、40 ビジネスルール
- `analysis/04_screen_transition.md` — 画面遷移図、SPA 統合提案
- `analysis/05_gap_analysis.md` — 69 GAP 項目

**API 設計原則**:
- RESTful リソース指向（画面単位ではなくリソース単位）
- JSON レスポンス（エラーコード CZ-000〜999 体系踏襲）
- 4層権限モデルによるアクセス制御（auth-infrastructure spec 準拠）
- ステータスマトリクス（12状態×2系列）に基づく操作可否判定

---

## 1. API ベース設計

### 1.1 共通仕様

| 項目 | 値 |
|------|-----|
| ベースパス | `/api/v1` |
| 認証 | `Authorization: Bearer <JWT>` or `X-Amzn-Oidc-Data` ヘッダー |
| Content-Type | `application/json` |
| 文字コード | UTF-8 |
| 日付形式 | `YYYY-MM-DD` (ISO 8601) |
| 時刻形式 | `HH:mm` (工数は15分単位: 00/15/30/45) |

### 1.2 共通レスポンス構造

```json
{
  "data": { ... },
  "meta": {
    "totalCount": 150,
    "page": 1,
    "pageSize": 50
  }
}
```

### 1.3 エラーレスポンス構造

```json
{
  "error": {
    "code": "CZ-126",
    "message": "{0}は必須入力です",
    "field": "workDate",
    "params": ["作業日"]
  }
}
```

バッチ操作（batch-confirm 等）では、エラー発生行を特定するため `recordId` を追加:

```json
{
  "error": {
    "code": "CZ-126",
    "message": "{0}は必須入力です",
    "field": "workDate",
    "params": ["作業日"],
    "recordId": 12345
  }
}
```

エラーコード体系は現行 CZ-000〜999 を踏襲:
- CZ-000〜099: 成功メッセージ
- CZ-100〜299: 警告メッセージ（バリデーションエラー含む）
- CZ-300〜499: システムエラー
- CZ-500〜799: 確認ダイアログ（Frontend で処理）
- CZ-800〜999: 情報メッセージ

---

## 2. エンドポイント一覧

### 2.1 認証 (`/api/v1/auth`)

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/auth/me` | 現在ユーザー情報 + 4層権限 | US-050 |
| GET | `/auth/status-matrix` | ステータスマトリクス取得 | - |

### 2.2 工数入力 (`/api/v1/work-hours`) — FORM_010

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/work-hours` | 工数レコード一覧（月指定） | US-017 |
| POST | `/work-hours` | 工数レコード新規登録 | US-010 |
| PATCH | `/work-hours/{id}` | 工数レコード部分更新（Ajax 編集） | US-011 |
| DELETE | `/work-hours` | 工数レコード一括削除 | US-014 |
| POST | `/work-hours/copy` | 工数レコードコピー | US-012 |
| POST | `/work-hours/transfer-next-month` | 翌月以降への転写 | US-013 |
| POST | `/work-hours/batch-confirm` | 一括確認 (STATUS_0→1) | US-015 |
| POST | `/work-hours/batch-revert` | 一括作成中戻し (STATUS_1→0) | US-016 |
| GET | `/work-hours/project-summary` | プロジェクト別工数参照 | US-019 |
| GET | `/work-hours/export/excel` | Excel 出力 | US-01A |

### 2.3 工数状況一覧 (`/api/v1/work-status`) — FORM_020

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/work-status` | 工数状況一覧（月・組織・担当者で検索） | US-020 |
| PATCH | `/work-status/{id}/hours` | インライン工数編集 | US-026 |
| POST | `/work-status/approve` | レコード承認 (STATUS_1→2) | US-024 |
| POST | `/work-status/revert` | 承認取消 (STATUS_2→1) | US-025 |
| POST | `/work-status/monthly-confirm` | 月次確認 (GetsujiKakutei=1) | US-022 |
| POST | `/work-status/monthly-aggregate` | 月次集約 (DataSyuukei=1) | US-023 |
| POST | `/work-status/monthly-unconfirm` | 月次未確認戻し | US-021 |
| GET | `/work-status/export/excel` | Excel 出力 | - |

### 2.4 半期推移 (`/api/v1/half-trends`) — FORM_030/031/032

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/half-trends/categories` | 分類別集計 (STEP_0) | US-030 |
| GET | `/half-trends/systems` | システム別ドリルダウン (STEP_1) | US-031 |
| GET | `/half-trends/subsystems` | サブシステム別ドリルダウン (STEP_2) | US-032 |
| GET | `/half-trends/export/excel` | Excel 出力 | US-034 |

### 2.5 月別内訳 (`/api/v1/monthly-breakdown`) — FORM_040/041/042

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/monthly-breakdown/categories` | 分類別集計 (STEP_0) | US-040 |
| GET | `/monthly-breakdown/systems` | システム別ドリルダウン (STEP_1) | US-041 |
| GET | `/monthly-breakdown/subsystems` | サブシステム別ドリルダウン (STEP_2) | US-042 |
| GET | `/monthly-breakdown/export/excel` | Excel 出力（4種） | US-043 |
| GET | `/monthly-breakdown/detail` | 月別内訳詳細 | - |

### 2.6 MY システム (`/api/v1/my-systems`)

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/my-systems` | MY システム一覧 | US-033 |
| POST | `/my-systems` | MY システム登録 | US-033 |
| DELETE | `/my-systems/{systemNo}` | MY システム解除 | US-033/045 |

### 2.7 マスタ参照 (`/api/v1/masters`)

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/masters/organizations` | 組織一覧（階層付き） | DLG_001/002 |
| GET | `/masters/organizations/tree` | 組織ツリー | DLG_001/002 |
| GET | `/masters/systems` | システム一覧 | DLG_003/004 |
| GET | `/masters/subsystems` | サブシステム一覧 | DLG_003/004 |
| GET | `/masters/staff` | 担当者検索 | DLG_005 |
| GET | `/masters/categories` | 保守カテゴリ一覧（年度別） | US-010 |
| GET | `/masters/control` | 月次コントロール情報 | US-020 |

### 2.8 代行 (`/api/v1/delegation`) — US-01C

| メソッド | パス | 説明 | US |
|---------|------|------|-----|
| GET | `/delegation/available-staff` | 代行可能な担当者一覧 | US-01C |
| POST | `/delegation/switch` | 代行モード切替 | US-01C |

---

## 3. エンドポイント詳細

### 3.1 GET `/api/v1/work-hours` — 工数レコード一覧

**対応 US**: US-017（月切替）、US-01B（ソート）、US-01D（管理者操作）

**クエリパラメータ**:

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:---:|------|
| `yearMonth` | `YYYY-MM` | O | 対象年月 |
| `staffId` | `string` | - | 担当者 ID（代行モード時に指定） |
| `sort` | `string` | - | ソートキー。形式: `<column>:<asc\|desc>[,<column>:<asc\|desc>]`。複数カラムソート対応（カンマ区切り）。デフォルト: `workDate:asc`。対応カラム: workDate, status, categoryCode, systemNo, hours |

**レスポンス**:

```json
{
  "data": {
    "records": [
      {
        "id": 12345,
        "seqNo": 1,
        "status": 0,
        "statusLabel": "作成中",
        "workDate": "2025-02-25",
        "targetSubsystem": {
          "systemNo": "SYS001",
          "subsystemNo": "SUB001",
          "systemName": "基幹システム",
          "subsystemName": "会計モジュール",
          "sysKbn": 1
        },
        "causeSubsystem": {
          "systemNo": "SYS002",
          "subsystemNo": "SUB003",
          "systemName": "人事システム",
          "subsystemName": "給与モジュール"
        },
        "category": {
          "code": "01",
          "name": "障害対応"
        },
        "subject": "月次処理エラー修正",
        "hours": "03:30",
        "hoursMinutes": 210,
        "tmrNo": "12345",
        "workRequestNo": "1234567",
        "workRequesterName": "山田太郎",
        "registrantId": "d10623",
        "registrantName": "鈴木花子",
        "updatedAt": "2025-02-25T10:30:00"
      }
    ],
    "summary": {
      "totalHours": "120:30",
      "totalMinutes": 7230,
      "statusCounts": { "0": 3, "1": 5, "2": 10 }
    },
    "permissions": {
      "canAdd": true,
      "canCopy": true,
      "canDelete": true,
      "canBatchConfirm": true,
      "canBatchRevert": true,
      "canExport": true,
      "statusMatrix": {
        "000": { "add": 9, "copy": 1, "delete": 1, "update": 1, "view": 1 }
      }
    },
    "monthControl": {
      "getsujiKakutei": false,
      "dataSyuukei": false,
      "statusKey": "000"
    }
  },
  "meta": {
    "yearMonth": "2025-02",
    "staffId": "d10623",
    "staffName": "鈴木花子",
    "isDaiko": false
  }
}
```

### 3.2 POST `/api/v1/work-hours` — 工数レコード新規登録

**対応 US**: US-010

**ドラフトモード**: `yearMonth` のみを指定した場合、STATUS_0 の空レコード（ドラフト）を作成する。
ドラフト作成時は下記バリデーション (VR-001〜015) を**省略**し、必須フィールド未入力を許容する。
バリデーションは一括確認 (`POST /work-hours/batch-confirm`) 時に `isInputCheck()` で一括実施する。
これは旧システムの「空行追加 → インライン入力 → 一括確認時バリデーション」フローを踏襲するための設計。

**リクエストボディ**（全フィールド指定の場合）:

```json
{
  "yearMonth": "2025-02",
  "workDate": "2025-02-25",
  "targetSubsystemNo": "SUB001",
  "causeSubsystemNo": "SUB003",
  "categoryCode": "01",
  "subject": "月次処理エラー修正",
  "hours": "03:30",
  "tmrNo": "12345",
  "workRequestNo": "1234567",
  "workRequesterName": "山田太郎"
}
```

**バリデーション** (BR/VR ルール準拠):

| ルール | 内容 | エラー |
|--------|------|--------|
| VR-001 | 作業日必須、YYYY-MM-DD 形式 | CZ-126 |
| VR-002 | 作業日は対象月の範囲内 | CZ-144 |
| VR-003 | 対象サブシステム必須 | CZ-126 |
| VR-004 | 原因サブシステム必須 | CZ-126 |
| VR-005 | 保守カテゴリ必須 | CZ-126 |
| VR-006 | 件名必須、128バイト以内 | CZ-126 |
| VR-007 | 件名に禁止ワードなし (HOSYU_SYUBETU_0) | CZ-141 |
| VR-008 | 工数必須、0より大 | CZ-126 |
| VR-009 | 工数 HH:mm 形式、15分単位 | CZ-125, CZ-147 |
| VR-010 | 日次合計24時間以下 | CZ-146 |
| VR-011 | TMR番号 5文字以内、半角英数字 | - |
| VR-012 | 作業依頼書No 空 or 7文字固定 | CZ-137 |
| VR-013 | 作業依頼者名 40文字以内（全角可） | chkChara |
| VR-014 | 特定カテゴリ時、依頼書No+依頼者名必須 | CZ-142 |
| VR-015 | 同一サブシステム＋同一件名で異なるカテゴリは不可 | CZ-132 |

**レスポンス**: `201 Created` + 登録されたレコード

### 3.3 PATCH `/api/v1/work-hours/{id}` — Ajax インライン編集

**対応 US**: US-011

部分更新。フィールド単位で送信可能。

```json
{
  "field": "hours",
  "value": "04:00",
  "updatedAt": "2025-02-25T10:30:00"
}
```

> `updatedAt` は楽観的ロック用（セクション 6.1）。DB の値と不一致時は `409 Conflict` + CZ-101。

対応フィールド:

| field | 型 | バリデーション |
|-------|-----|-------------|
| `status` | `0`/`1`/`2` | ステータスマトリクス判定 |
| `workDate` | `YYYY-MM-DD` | VR-001, VR-002 |
| `categoryCode` | `string` | VR-005 |
| `subject` | `string` | VR-006, VR-007 |
| `hours` | `HH:mm` | VR-008, VR-009, VR-010 |
| `tmrNo` | `string` | VR-011 |
| `workRequestNo` | `string` | VR-012 |
| `workRequesterName` | `string` | VR-013 |

**レスポンス**:

```json
{
  "data": {
    "id": 12345,
    "field": "hours",
    "oldValue": "03:30",
    "newValue": "04:00",
    "summary": { "totalHours": "121:00", "totalMinutes": 7260 }
  }
}
```

### 3.4 POST `/api/v1/work-hours/copy` — レコードコピー

**対応 US**: US-012

```json
{
  "ids": [12345, 12346, 12347]
}
```

コピー内容: 全フィールド値保持。STATUS→0 リセット、SEQNO→新規採番。

### 3.5 POST `/api/v1/work-hours/transfer-next-month` — 翌月転写

**対応 US**: US-013

```json
{
  "ids": [12345, 12346],
  "targetMonths": ["2025-03", "2025-04"]
}
```

注意: カテゴリが対象年度に存在しない場合はカテゴリをブランクにする（エラーではない）。

### 3.6 POST `/api/v1/work-hours/batch-confirm` — 一括確認

**対応 US**: US-015

```json
{
  "yearMonth": "2025-02"
}
```

処理:
1. 対象月の全 STATUS_0 レコードに `isInputCheck()` バリデーション実行
2. 最初のバリデーションエラーで中断、エラーレコード ID を返す
3. 全パスなら STATUS_0→STATUS_1 に一括更新

**エラーレスポンス**:

```json
{
  "error": {
    "code": "CZ-126",
    "message": "作業日は必須入力です",
    "recordId": 12345,
    "field": "workDate"
  }
}
```

### 3.7 GET `/api/v1/work-status` — 工数状況一覧

**対応 US**: US-020, US-027

**クエリパラメータ**:

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:---:|------|
| `yearMonth` | `YYYY-MM` | O | 対象年月 |
| `organizationCode` | `string` | - | 組織コード |
| `staffId` | `string` | - | 担当者 ID |
| `statusFilter` | `string` | - | ステータスフィルタ (例: `0,1,2`) |
| `page` | `int` | - | ページ番号 (デフォルト: 1) |
| `pageSize` | `int` | - | ページサイズ |
| `sort` | `string` | - | ソートキー |

**レスポンス**:

```json
{
  "data": {
    "records": [
      {
        "id": 12345,
        "status": 1,
        "statusLabel": "確認",
        "statusColor": "#BDEAAD",
        "workDate": "2025-02-25",
        "staffId": "d10623",
        "staffName": "鈴木花子",
        "department": "IT推進部 開発1課",
        "targetSubsystem": { ... },
        "causeSubsystem": { ... },
        "category": { ... },
        "subject": "月次処理エラー修正",
        "hours": "03:30",
        "hoursMinutes": 210
      }
    ],
    "monthlyControl": {
      "yearMonth": "2025-02",
      "getsujiKakutei": true,
      "dataSyuukei": false,
      "statusLabel": "確認",
      "statusColor": "#BDEAAD"
    },
    "permissions": {
      "canConfirm": true,
      "canAggregate": false,
      "canUnconfirm": true,
      "canApprove": true,
      "canRevert": true
    }
  },
  "meta": {
    "totalCount": 150,
    "page": 1,
    "pageSize": 50
  }
}
```

### 3.8 POST `/api/v1/work-status/approve` — レコード承認

**対応 US**: US-024

```json
{
  "ids": [12345, 12346]
}
```

処理: STATUS_1→STATUS_2 に変更。権限チェック（`isSts_man_j_upd`）。

### 3.9 POST `/api/v1/work-status/monthly-confirm` — 月次確認

**対応 US**: US-022

```json
{
  "yearMonth": "2025-02",
  "organizationCode": "100210"
}
```

処理: `MCZ04CTRLMST` の `GetsujiKakuteiFlg=1`, `DataSyuukeiFlg=0` に更新。
権限: `canInputPeriod() = true` 必須。

### 3.10 GET `/api/v1/half-trends/categories` — 分類別集計

**対応 US**: US-030

**クエリパラメータ**:

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:---:|------|
| `fiscalYear` | `int` | O | 年度 |
| `halfPeriod` | `FIRST`/`SECOND` | O | 半期 |
| `organizationCode` | `string` | - | 組織コード |
| `displayMode` | `hours`/`cost` | - | 工数/コスト表示切替 |
| `filterType` | `all`/`system`/`my` | - | 全部/指定/MY |
| `sort` | `string` | - | ソートキー |

**レスポンス**:

```json
{
  "data": {
    "rows": [
      {
        "category1": { "code": "01", "name": "障害対応" },
        "category2": { "code": "01-01", "name": "本番障害" },
        "months": {
          "M1": { "hours": "120:00", "minutes": 7200, "cost": 960000 },
          "M2": { "hours": "130:00", "minutes": 7800, "cost": 1040000 },
          "M3": { ... },
          "M4": { ... },
          "M5": { ... },
          "M6": { ... }
        },
        "total": { "hours": "780:00", "minutes": 46800, "cost": 6240000 }
      }
    ],
    "grandTotal": {
      "months": { "M1": { ... }, "M2": { ... }, ... },
      "total": { ... }
    },
    "monthLabels": ["01月", "02月", "03月", "04月", "05月", "06月"]
  }
}
```

### 3.11 GET `/api/v1/half-trends/systems` — システム別ドリルダウン

**対応 US**: US-031

追加パラメータ: `category1Code`, `category2Code`

```json
{
  "data": {
    "drilldownContext": {
      "category1": { "code": "01", "name": "障害対応" },
      "category2": { "code": "01-01", "name": "本番障害" }
    },
    "rows": [
      {
        "systemNo": "SYS001",
        "systemName": "基幹システム",
        "isMy": true,
        "months": { ... },
        "total": { ... }
      }
    ]
  }
}
```

### 3.12 GET `/api/v1/monthly-breakdown/categories` — 月別内訳分類別

**対応 US**: US-040

`half-trends/categories` と同じ構造。追加パラメータ `month` で特定月を指定。

### 3.13 GET `/api/v1/masters/subsystems` — サブシステム検索

**対応 US**: US-018 (DLG_003)

**クエリパラメータ**:

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `organizationCode` | `string` | 組織コードでフィルタ |
| `keyword` | `string` | システム名/サブシステム名検索 |
| `page` | `int` | ページ番号 |
| `pageSize` | `int` | ページサイズ |

### 3.14 GET `/api/v1/masters/staff` — 担当者検索

**対応 US**: US-01C (DLG_005)

**クエリパラメータ**:

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `organizationCode` | `string` | 組織コードでフィルタ |
| `name` | `string` | 氏名検索 |
| `matchType` | `exact`/`partial` | 一致タイプ |

### 3.15 DELETE `/api/v1/work-hours` — 工数レコード一括削除

**対応 US**: US-014

**リクエストボディ**:

```json
{
  "ids": [12345, 12346, 12347]
}
```

**処理**:
1. 各レコードのステータスが STATUS_0（作成中）であることを検証
2. STATUS_0 以外のレコードが含まれる場合 → `403` + CZ-106（操作不可）
3. `dataAuthority.upd` が null でないこと + ステータスマトリクス判定
4. 楽観ロックは不要（削除対象が STATUS_0 のみのため競合リスク低）

**レスポンス**: `200 OK`

```json
{
  "data": {
    "deletedCount": 3,
    "deletedIds": [12345, 12346, 12347]
  }
}
```

### 3.16 POST `/api/v1/work-hours/batch-revert` — 一括作成中戻し

**対応 US**: US-016

**リクエストボディ**:

```json
{
  "yearMonth": "2025-02"
}
```

**処理**:
1. 対象月の全 STATUS_1（確認）レコードを取得
2. 権限チェック: `canManage()` or `canFullAccess()` が true であること
3. ステータスマトリクス判定（管理者系列の STATUS_1 → 戻しボタン可否）
4. STATUS_1 → STATUS_0 に一括更新

**レスポンス**: `200 OK`

```json
{
  "data": {
    "revertedCount": 5,
    "yearMonth": "2025-02"
  }
}
```

### 3.17 PATCH `/api/v1/work-status/{id}/hours` — インライン工数編集

**対応 US**: US-026

**リクエストボディ**:

```json
{
  "hours": "04:00",
  "updatedAt": "2025-02-25T10:30:00"
}
```

**バリデーション**:

| ルール | 内容 | エラー |
|--------|------|--------|
| VR-008 | 工数必須、0より大 | CZ-126 |
| VR-009 | HH:mm 形式、15分単位 | CZ-125, CZ-147 |
| VR-010 | 日次合計24時間以下 | CZ-146 |
| BR-007 | 最小 0:15 | CZ-129 |

**権限チェック**: `canManage()` or `canFullAccess()` が true であること（管理者による他者工数の編集）。
**楽観ロック**: `updatedAt` 不一致の場合 → `409 Conflict` + CZ-101

**レスポンス**: `200 OK`

```json
{
  "data": {
    "id": 12345,
    "oldHours": "03:30",
    "newHours": "04:00",
    "summary": { "totalHours": "121:00", "totalMinutes": 7260 }
  }
}
```

### 3.18 POST `/api/v1/work-status/revert` — 承認取消

**対応 US**: US-025

**リクエストボディ**:

```json
{
  "ids": [12345, 12346]
}
```

**処理**:
1. 各レコードのステータスが STATUS_2（承認）であることを検証
2. 権限チェック: `canManage()` or `canFullAccess()` が true であること
3. ステータスマトリクス判定（管理者系列の STATUS_2 → 戻しボタン可否）
4. STATUS_2 → STATUS_1 に一括更新

**レスポンス**: `200 OK`

```json
{
  "data": {
    "revertedCount": 2,
    "revertedIds": [12345, 12346]
  }
}
```

### 3.19 POST `/api/v1/work-status/monthly-aggregate` — 月次集約

**対応 US**: US-023

**リクエストボディ**:

```json
{
  "yearMonth": "2025-02",
  "organizationCode": "100210"
}
```

**処理**:
1. 権限チェック: `canAggregate()` が true であること
2. `MCZ04CTRLMST` を `SELECT FOR UPDATE` でロック取得
3. `GetsujiKakuteiFlg` = 1 であること（未確認状態では集約不可）
4. `DataSyuukeiFlg` = 1 に更新

**レスポンス**: `200 OK`

```json
{
  "data": {
    "yearMonth": "2025-02",
    "organizationCode": "100210",
    "dataSyuukei": true,
    "updatedAt": "2025-02-25T15:00:00"
  }
}
```

### 3.20 POST `/api/v1/work-status/monthly-unconfirm` — 月次未確認戻し

**対応 US**: US-021

**リクエストボディ**:

```json
{
  "yearMonth": "2025-02",
  "organizationCode": "100210"
}
```

**処理**:
1. 権限チェック: `canInputPeriod()` が true であること
2. `MCZ04CTRLMST` を `SELECT FOR UPDATE` でロック取得
3. `GetsujiKakuteiFlg` = 0, `DataSyuukeiFlg` = 0 に更新（確認済・集約済いずれの状態からも未確認に戻す）

**レスポンス**: `200 OK`

```json
{
  "data": {
    "yearMonth": "2025-02",
    "organizationCode": "100210",
    "getsujiKakutei": false,
    "dataSyuukei": false,
    "updatedAt": "2025-02-25T15:00:00"
  }
}
```

### 3.21 POST `/api/v1/delegation/switch` — 代行モード切替

**対応 US**: US-01C

**リクエストボディ**:

```json
{
  "targetStaffId": "e20456"
}
```

`targetStaffId` が null または空の場合、代行モード解除。

**処理**:
1. 権限チェック: JWT の `canDelegate` が true であること（外部契約者 TYPE_3 のみ）
2. `isAllowedStaff(targetStaffId)` で代行可否を検証（同一組織 + 代行許可リスト）
3. 検証パス → 以降のリクエストで `X-Delegation-Staff-Id` ヘッダーに設定する targetStaffId を返却
4. 検証失敗 → `403` + CZ-307（代行権限なし）

**レスポンス**: `200 OK`

```json
{
  "data": {
    "delegationStaffId": "e20456",
    "delegationStaffName": "佐藤一郎",
    "isDaiko": true
  }
}
```

代行解除時:

```json
{
  "data": {
    "delegationStaffId": null,
    "delegationStaffName": null,
    "isDaiko": false
  }
}
```

---

## 4. ビジネスルール実装

### 4.1 時間・工数制約 (BR-001〜007)

| ルール | API での実装 |
|--------|-------------|
| BR-001 | サービス時間 6:00〜23:30: Backend ミドルウェアで判定、CZ-102 |
| BR-002 | 15分単位: POST/PATCH バリデーション、CZ-147 |
| BR-003 | 日次24h上限: POST/PATCH で合計計算、CZ-146 |
| BR-004 | 月次基準160h: レスポンスの summary に含める（参考値） |
| BR-005 | 日次基準8h: レスポンスの summary に含める（参考値） |
| BR-006 | HH:MM形式: パース、HH→HH:00 自動変換、CZ-125 |
| BR-007 | 最小0:15: POST/PATCH バリデーション、CZ-129 |

### 4.2 ステータス制御 (12状態マトリクス)

Backend `StatusMatrixResolver` で判定。
API レスポンスの `permissions` フィールドに各操作の可否を含める。

```
リクエスト受信
  → JWT から CzPermissions 取得
  → canFullAccess() 判定 → 担当者(useTanSeries)/管理者系列選択
  → 対象レコードのステータスキー(000-911) 取得
  → StatusMatrixResolver.resolve() でボタン制御マップ取得
  → 操作不可の場合 → 403 + CZ-106〜110 エラー
```

### 4.3 年度・期間ルール

| 年度 | 上期 | 下期 |
|------|------|------|
| 2014年以前 | 4月〜9月 | 10月〜3月 |
| 2015年（特殊） | 4月〜9月 | 10月〜12月（3ヶ月） |
| 2016年以降 | 1月〜6月 | 7月〜12月 |

`FiscalYearResolver` サービスで半期の月リスト・基準日を解決。

### 4.4 禁止ワードチェック

HOSYU_SYUBETU_0 の場合、件名に以下の12語を含まないことを検証:
`カ層`, `@機能別1`, `連絡器`, `（相談`, `（課程`, `・限定`,
`・共存作成`, `取得`, `賃金`, `導入`, `経理`, `実績演算`

### 4.5 バイト長計算

| 文字種 | バイト数 |
|--------|---------|
| 全角文字 | 2 |
| 半角文字 | 1 |
| 半角カタカナ | 2 |

件名は128バイト以内。

---

## 5. データアクセス制御

### 5.1 組織スコープフィルタ

全データ取得 API に適用。`OrganizationScopeResolver` で
JWT の `dataAuthority` + `organizationCode` から
許可組織コードリストを解決し、SQL WHERE 句に適用。

```
GET /api/v1/work-hours?yearMonth=2025-02
  → JWT: dataAuthority.ref = "KYOKU", orgCode = "100200"
  → OrganizationScopeResolver: ["100200", "100210", "100211", ...]
  → SQL: WHERE organization_code IN (...)
```

### 5.2 操作権限チェック

| 操作 | チェック内容 |
|------|------------|
| 登録 (POST) | `dataAuthority.ins` が null でないこと |
| 更新 (PATCH) | `dataAuthority.upd` が null でないこと + ステータスマトリクス |
| 削除 (DELETE) | `dataAuthority.upd` が null でないこと + ステータスマトリクス |
| 承認/戻し | `canManage()` or `canFullAccess()` + 管理モード |
| 月次確認 | `canInputPeriod()` |
| 月次集約 | `canAggregate()` |

### 5.3 代行モード

外部契約者(TYPE_SUBCONTRACT)の代行時:
- `X-Delegation-Staff-Id` ヘッダーで代行対象を指定
- 登録者 ID は代行元の正社員 ID を使用
- `isAllowedStaff()` で代行可否を検証

---

## 6. 同時編集制御

### 6.1 楽観的ロック

全更新 API で `updatedAt` フィールドを使った楽観的ロック（DELETE は対象外。DELETE は STATUS_0 のみ対象で競合リスク低のため、セクション 3.15 参照）:

```json
PATCH /api/v1/work-hours/12345
{
  "field": "hours",
  "value": "04:00",
  "updatedAt": "2025-02-25T10:30:00"
}
```

`updatedAt` が DB の値と不一致の場合 → `409 Conflict` + CZ-101

### 6.2 月次制御の排他

月次確認/集約/未確認操作は `MCZ04CTRLMST` のレコードロック（`SELECT FOR UPDATE`）で
同時実行を防止。

---

## 7. Excel 出力

### 7.1 出力テンプレート一覧

| 画面 | テンプレート | エンドポイント |
|------|------------|---------------|
| FORM_010 | 工数明細 | `GET /work-hours/export/excel` |
| FORM_020 | 工数状況一覧 | `GET /work-status/export/excel` |
| FORM_030 | 半期推移 | `GET /half-trends/export/excel` |
| FORM_040 | 月別内訳（標準） | `GET /monthly-breakdown/export/excel?type=standard` |
| FORM_040 | 月別内訳（管理用） | `GET /monthly-breakdown/export/excel?type=management` |
| FORM_040 | 月別内訳（管理詳細） | `GET /monthly-breakdown/export/excel?type=management-detail` |

### 7.2 レスポンス

```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="work_hours_202502.xlsx"
```

Apache POI (XSSF) で .xlsx 生成。

---

## 8. Backend パッケージ構成

```
com.example.czConsv/
├── controller/
│   ├── AuthController.java            ← 認証 API
│   ├── WorkHoursController.java       ← 工数入力 CRUD
│   ├── WorkStatusController.java      ← 工数状況一覧
│   ├── HalfTrendsController.java      ← 半期推移
│   ├── MonthlyBreakdownController.java ← 月別内訳
│   ├── MySystemController.java        ← MY システム
│   ├── MasterController.java          ← マスタ参照
│   └── DelegationController.java      ← 代行
├── service/
│   ├── WorkHoursService.java
│   ├── WorkStatusService.java
│   ├── HalfTrendsService.java
│   ├── MonthlyBreakdownService.java
│   ├── MySystemService.java
│   ├── MasterService.java
│   ├── ExcelExportService.java
│   ├── FiscalYearResolver.java        ← 年度半期解決
│   └── ValidationService.java         ← VR-001〜015 + 禁止ワード
├── repository/ (Doma 2 DAO)
│   ├── WorkHoursDao.java
│   ├── WorkStatusDao.java
│   ├── ControlDao.java                ← MCZ04CTRLMST
│   ├── HalfTrendsDao.java
│   ├── MonthlyBreakdownDao.java
│   ├── MySystemDao.java
│   ├── OrganizationDao.java
│   ├── SubsystemDao.java
│   ├── StaffDao.java
│   └── CategoryDao.java
├── entity/ (Doma 2 Entity)
│   ├── WorkHoursEntity.java           ← tcz01_hosyu_kousuu
│   ├── ControlEntity.java             ← mcz04_ctrl
│   ├── CategoryEntity.java            ← mcz02_hosyu_kategori
│   ├── SystemEntity.java              ← mav01_sys
│   ├── SubsystemEntity.java           ← mav03_subsys
│   ├── OrganizationEntity.java        ← mcz12_orgn_kr
│   ├── SubsystemSumEntity.java        ← tcz13_subsys_sum
│   ├── GroupKeyEntity.java            ← tcz14_grp_key
│   ├── MySystemEntity.java            ← tcz19_my_sys
│   └── StaffHistoryEntity.java        ← tcz16_tnt_busyo_rireki
├── dto/
│   ├── request/
│   │   ├── WorkHoursCreateRequest.java
│   │   ├── WorkHoursUpdateRequest.java
│   │   ├── WorkHoursCopyRequest.java
│   │   ├── WorkHoursTransferRequest.java
│   │   ├── BatchConfirmRequest.java
│   │   ├── ApproveRequest.java
│   │   └── MonthlyControlRequest.java
│   └── response/
│       ├── WorkHoursListResponse.java
│       ├── WorkStatusListResponse.java
│       ├── HalfTrendsResponse.java
│       ├── MonthlyBreakdownResponse.java
│       └── ErrorResponse.java
├── security/ (認証基盤 — 実装済み)
│   ├── filter/AlbOidcAuthFilter.java
│   ├── model/CzPrincipal.java
│   ├── model/CzPermissions.java
│   ├── config/SecurityConfig.java
│   └── util/StatusMatrixResolver.java
└── config/
    └── WebConfig.java
```

---

## 9. テスト要件

### 9.1 API 単体テスト

| 対象 | テスト内容 |
|------|----------|
| WorkHoursController | 全 CRUD 操作、バリデーション全ルール (VR-001〜015) |
| WorkStatusController | 月次確認/集約/未確認、承認/戻し |
| HalfTrendsController | 3階層ドリルダウン、年度半期ルール |
| MonthlyBreakdownController | 3階層ドリルダウン、4種 Excel 出力 |
| ValidationService | 禁止ワード、バイト長計算、15分単位チェック |
| FiscalYearResolver | 2014以前/2015特殊/2016以降の全パターン |

### 9.2 権限テスト

15 アクター × 主要操作の組み合わせテスト:
- ACT-01（報告担当者）: 工数入力 CRUD、担当者系列マトリクス
- ACT-03（全権管理者）: 管理者系列マトリクス、全ステータス操作
- ACT-09（外部契約者）: 制限アクセス、代行モード
- ACT-10（全社スタッフ）: 全組織データアクセス
- ACT-13（局スタッフ）: 局配下のみデータアクセス

### 9.3 ステータスマトリクステスト

12状態 × 2系列 × 7操作 = **168 パターン**のパラメタライズドテスト。

### 9.4 同時編集テスト

楽観的ロックの競合シナリオ:
1. ユーザーA が取得 (updatedAt=T1)
2. ユーザーB が更新 (updatedAt=T2)
3. ユーザーA が更新試行 → 409 Conflict + CZ-101

### 受け入れ基準（Given-When-Then）

**AC-API-01: 工数レコード新規作成（下書き）**
- Given: STATUS_0 権限を持つユーザーがログインしている
- When: POST /work-hours に必須項目のみ（workDate, systemNo）を送信する
- Then: 201 Created でレコードが作成され、status=0（下書き）で保存される

**AC-API-02: 工数レコード一括確定**
- Given: STATUS_0 のレコードが5件、うち1件は workDate が空
- When: POST /work-hours/batch-confirm を yearMonth=2025-02 で実行する
- Then: 400 Bad Request、error.code=CZ-126、error.recordId に不正レコードの ID が返り、全件の STATUS は変更されない

**AC-API-03: 楽観的ロック競合**
- Given: ユーザー A とユーザー B が同一レコード（version=1）を取得済み
- When: ユーザー A が PATCH で更新（version→2）後、ユーザー B が version=1 で PATCH を送信する
- Then: ユーザー B に 409 Conflict、error.code=CZ-101 が返される

**AC-API-04: ステータス遷移（確認→確定）**
- Given: status=1（確認済）のレコードが存在する
- When: 管理者権限ユーザーが POST /work-status/approve を実行する
- Then: status が 1→2 に遷移し、200 OK が返される

**AC-API-05: 不正ステータス遷移の拒否**
- Given: status=0（下書き）のレコードが存在する
- When: POST /work-status/approve（0→2 直接遷移）を実行する
- Then: 400 Bad Request、error.code=CZ-106 が返される

**AC-API-06: 月次確定→集約シーケンス**
- Given: 2025年02月の全レコードが status=2（確定済）
- When: POST /work-status/monthly-confirm を yearMonth=2025-02 で実行する
- Then: mcz04_ctrl の getsuji_kakutei_flg=1 に更新され、triggerAggregation が同期実行される

**AC-API-07: 権限チェック（403）**
- Given: ACT-07（臨時職員1, canConfirm=false）でログインしている
- When: POST /work-status/approve を実行する
- Then: 403 Forbidden、error.code=CZ-308 が返される

**AC-API-08: 代行モードでの登録**
- Given: ACT-09 が X-Delegation-Staff-Id に正社員 ID を設定
- When: POST /work-hours でレコードを作成する
- Then: created_by に代行先 ID が記録される

**AC-API-09: 禁止語句チェック**
- Given: hosyuSyubetu=0 のレコード
- When: subject に禁止語句「カ層」を含む値で PATCH を送信する
- Then: 400 Bad Request、error.code=CZ-141、error.params に「カ層」が返される

**AC-API-10: ページネーション**
- Given: 対象月に50件のレコードが存在する
- When: GET /work-status?page=1&pageSize=20 を送信する
- Then: 20件のデータと totalCount=50、totalPages=3 がレスポンスに含まれる

**AC-API-11: Excel 出力権限チェック**
- Given: canExportHours=false のユーザー
- When: GET /excel/work-hours を送信する
- Then: 403 Forbidden、error.code=CZ-308 が返される

**AC-API-12: 年度期間ルール（2015年特例）**
- Given: fiscalYear=2015, halfPeriod=SECOND で集計を要求
- When: GET /half-trends/categories を送信する
- Then: 3ヶ月分（10月, 11月, 12月）のデータのみが返される（通常6ヶ月ではない）

### エッジケース・境界値

| カテゴリ | ケース | 期待動作 |
|----------|--------|----------|
| 工数時間 | 0:00（ゼロ） | バリデーションエラー CZ-129 |
| 工数時間 | 0:15（最小値） | 正常受付 |
| 工数時間 | 23:45（最大値-1単位） | 正常受付 |
| 工数時間 | 24:00（日次上限） | 正常受付（日次合計で検証） |
| 工数時間 | 24:15（上限超過） | バリデーションエラー CZ-146 |
| 件名 | 127バイト | 正常受付 |
| 件名 | 128バイト（上限） | 正常受付 |
| 件名 | 129バイト（超過） | バリデーションエラー CZ-128 |
| 件名 | 全角/半角混在の128バイト境界 | octet_length で正確に判定 |
| 一括操作 | 空配列 `[]` | 400 Bad Request |
| 一括操作 | ID 1件のみ | 正常処理 |
| 一括操作 | 重複 ID 含む配列 | 重複除去して処理 or エラー |
| 一括操作 | 異なるステータスの ID 混在 | 不正 ID でエラー、全件ロールバック |
| ページネーション | page=0 | 1ページ目として処理（正規化） |
| ページネーション | page=-1 | 400 Bad Request |
| ページネーション | pageSize=0 | 400 Bad Request |
| ページネーション | pageSize > totalCount | 全件返却、totalPages=1 |
| 年月境界 | 月末日（2月28日/29日） | 正常処理 |
| 年月境界 | 閏年 2月29日 | 正常処理 |
| 年度境界月 | 上期→下期の切替月 | FiscalYearResolver で正確に判定 |
| 同時操作 | 2ユーザーが同月を同時に月次確定 | 先勝ち、後発は 409 Conflict |
| 同時操作 | 一括確定中に別ユーザーが個別編集 | 楽観的ロックで競合検出 |
| 代行 | 自分自身への代行設定 | 400 Bad Request |
| 代行 | 存在しない staffId への代行 | 400 Bad Request |

# E2E テスト実施報告書

## Feature 04: 工数入力画面 (FORM_010)

**実施日**: 2026-03-09
**実施環境**: GitHub Codespaces (Linux 6.8.0-1044-azure)
**テストツール**: Playwright 1.58.2 + Chromium 145.0.7632.6

---

## 1. 環境構成

| サービス | イメージ/バージョン | ポート | ステータス |
|---------|-------------------|--------|-----------|
| Frontend | Nuxt.js 3.21.1 (SPA mode) | 3000 | healthy |
| Backend | Spring Boot 3.4 + Java 21 | 8080 | healthy |
| Auth Mock | Express + Node.js 22 | 8180 | healthy |
| Database | PostgreSQL 16-alpine | 5432 | healthy |
| Redis | Redis 7-alpine | 6379 | healthy |

### 環境構築で修正した項目

| # | 問題 | 原因 | 修正内容 |
|---|------|------|----------|
| 1 | Backend 起動失敗 | JWT Secret が 120bit（最低 256bit 必要） | 全箇所で 256bit 以上のシークレットに統一 |
| 2 | コンポーネント未描画 | Nuxt auto-import 名が `WorkHoursToolbar` 等だが `Toolbar` で参照 | `work-hours.vue` のテンプレートを正しい名前に修正 |
| 3 | SSR ハイドレーション失敗 | SSR 時に API エラー → CSR 不一致 | `ssr: false`（SPA モード）に変更 |
| 4 | devProxy パス不一致 | `/api` → `backend:8080/work-hours` に誤転送（`/api` prefix 欠落） | target に `/api` を追加 |
| 5 | store staffId 未設定 | auth store → workHours store の連携なし | `onMounted` で auth から staffId/staffName を初期化 |
| 6 | fetchRecords 未処理例外 | API エラー時に try-catch なし | catch で records リセット + エラーメッセージ表示 |
| 7 | API レスポンス形式不一致 | Backend が配列 `[]` を返す（store はオブジェクト期待） | **WorkHoursMapper 追加、Controller を DTO 返却に修正（完了）** |

---

## 2. テスト結果サマリー

### 第1回（Backend API 統合修正前）

| 指標 | 値 |
|------|-----|
| テストケース数 | 31 |
| Pass | 4 |
| Fail | 27 |
| Pass 率 | 12.9% |

### 第2回（Backend API 統合修正後） ← 最新

| 指標 | 値 |
|------|-----|
| テストケース数 | 31 |
| Pass | **9** |
| Fail | **19** |
| Skipped | 3 |
| Pass 率 | **29.0%** |

### ファイル別結果（第2回）

| ファイル | テスト数 | Pass | Fail | Skip | 備考 |
|---------|---------|------|------|------|------|
| work-hours-basic.spec.ts (T-053) | 8 | 4 | 4 | 0 | 表示系 Pass、CRUD 操作系 Fail（データ不在） |
| work-hours-validation.spec.ts (T-054) | 6 | 0 | 6 | 0 | 行追加が前提のため全 Fail（データ不在） |
| work-hours-permissions.spec.ts (T-055) | 6 | 3 | 2 | 1 | 表示/権限系 Pass、差戻ボタン/遷移 Fail |
| work-hours-delegation.spec.ts (T-056) | 5 | 1 | 4 | 0 | 非表示チェック Pass、代行UI未実装で Fail |
| work-hours-operations.spec.ts (T-057) | 6 | 1 | 3 | 2 | 月切替/ソート Pass、データ操作系 Fail |

### work-hours-basic.spec.ts 詳細

| テストケース | 第1回 | 第2回 | 失敗理由 |
|-------------|------|------|---------|
| 工数入力画面が表示される | **Pass** | **Pass** | — |
| 新規行を追加できる | Fail | Fail | POST 後にテーブル行が表示されない（data-testid 不一致の可能性） |
| 工数セルに短縮入力して自動変換される | Fail | Fail | 行追加失敗のため `hours-display` 要素なし |
| 件名を入力して保存される | Fail | Fail | 同上（`subject-display` 要素なし） |
| 一括確認で STATUS_0 → STATUS_1 に遷移 | Fail | Fail | 同上 |
| ステータスフッターにステータス件数が表示される | **Pass** | **Pass** | — |
| 月切替で SearchPanel の年月が変わる | **Pass** | **Pass** | — |
| データ0件のとき空メッセージが表示される | **Pass** | **Pass** | — |

---

## 3. 失敗の根本原因分析

### 第1回の原因（解消済み）
Backend Controller が raw Entity（`List<Tcz01HosyuKousuu>`）を返していたため、Frontend の期待する DTO 形式と不一致。

### 第2回の残存原因

#### A. テストデータ不在（19件中14件）
- E2E テストが `addBtn.click()` で行追加 → `data-testid="subject-display"` 等をクリック、という流れ
- `POST /api/work-hours` で行追加は成功するが、DataTable の `data-testid` セレクタとコンポーネントの実装が合っていない可能性
- 行追加後のレコードに対するセル編集系テストが全滅

#### B. 代行UI未実装（4件）
- `[data-testid="staff-selector"]` が `SearchPanel` に存在しない
- 代行機能は Phase 04 の後工程として計画

#### C. 差戻ボタン非表示（1件）
- `[data-testid="batch-revert-btn"]` が見つからない
- STATUS_1 レコードが DB に存在しないため条件付き表示が機能しない

---

## 4. Backend API 統合修正の内容

### 新規作成

| ファイル | 内容 |
|---------|------|
| `backend/.../dto/response/WorkHoursMapper.java` | Entity → DTO 変換マッパー（toListResponse, toRecord, resolvePermissions） |

### 修正

| ファイル | 変更内容 |
|---------|---------|
| `backend/.../controller/WorkHoursController.java` | GET→`WorkHoursListResponse`, POST→`WorkHoursRecord`, PATCH→`WorkHoursRecord`, copy/transfer→`List<WorkHoursRecord>` |
| `backend/.../controller/WorkHoursControllerTest.java` | 新レスポンス形式に合わせたアサーション全面更新 |
| `frontend/stores/workHours.ts` | `Array.isArray` ワークアラウンド削除、正規 DTO 形式のみ対応 |
| `frontend/tests/unit/stores/workHours.test.ts` | エラーメッセージ期待値の code フィールド追加 |

### API レスポンス形式（修正後）

```json
// GET /api/work-hours?staffId=ACT-01&yearMonth=2025-02
{
  "records": [],
  "summary": { "monthlyTotal": 0, "dailyTotal": 0 },
  "permissions": {
    "canCreate": true, "canEdit": true, "canDelete": true,
    "canConfirm": true, "canRevert": false, "canCopy": true, "canTransfer": true
  },
  "monthControl": { "yearMonth": "2025-02", "status": "OPEN", "isLocked": false }
}
```

---

## 5. ユニットテスト影響確認

```
Backend:  BUILD SUCCESSFUL (WorkHoursControllerTest 全 pass)
Frontend: Test Files 41 passed (41), Tests 550 passed (550)
```

全テスト Pass。リグレッションなし。

---

## 6. 次のステップ

| 優先度 | 作業内容 | 影響テスト数 |
|--------|---------|------------|
| 高 | DataTable コンポーネントに `data-testid` 属性を追加（セル表示/編集モード） | 14件 |
| 高 | E2E テスト用フィクスチャ（API経由でのテストデータ作成）を beforeEach に追加 | 14件 |
| 中 | 代行UI（staff-selector）の SearchPanel への実装 | 4件 |
| 低 | STATUS 遷移を伴うテストの seed data 準備 | 1件 |

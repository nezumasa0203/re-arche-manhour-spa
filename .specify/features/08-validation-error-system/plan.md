# バリデーション & エラー体系 実装計画

## 概要

CZ（保有資源管理システム）全画面で適用されるバリデーション・エラーメッセージ体系を、
現行 `ApCheck.js` + `ApMessage.xml` + 各 Proc `isInputCheck()` から
Frontend composable + Backend ValidationService の2層構成に移行する。

本機能は Layer 0（基盤層）であり、他の全機能（#4 工数入力、#5 工数一覧、
#6 分析画面、#7 共通コンポーネント）が依存する最優先実装対象である。

CZ-000〜CZ-999 のエラーコード体系（92件）を100%踏襲し、
VR-001〜015 のバリデーションルール、BR-001〜007 のビジネスルール、
12状態ステータスマトリクスの操作制御を新アーキテクチャ上で完全再現する。

---

## アーキテクチャ

### フロントエンド

#### useValidation composable

`composables/useValidation.ts` として実装。現行 `ApCheck.js` の
`chkChara()` / `isInputCheck()` 相当の機能を TypeScript に移植する。

| メソッド | 対応ルール | 説明 |
|---|---|---|
| `validateRequired(value, fieldName)` | VR-001〜005 | 必須チェック |
| `validateHoursFormat(value)` | VR-008, VR-009 | HH:MM 形式 + 15分単位 |
| `validate15MinUnit(value)` | BR-002 | 15分単位制約 |
| `validateByteLength(value, max, fieldName)` | VR-006 | バイト長上限 |
| `validateDateInMonth(date, yearMonth)` | VR-002 | 月範囲内チェック |
| `validateFixedLength(value, length, fieldName)` | VR-012 | 固定長チェック |
| `validateCharType(value, mode, fieldName)` | VR-011, VR-013 | 文字種チェック |
| `validateWorkHoursRecord(record)` | VR-001〜013 | 一括バリデーション |

- Layer 2 専用ルール（VR-007, VR-010, VR-014, VR-015）は Frontend では実行しない

#### メッセージカタログ (constants/messages.ts)

- CZ-000〜CZ-999 の全92件を定義
- `resolveMessage(code, params?)` で `{0}` パラメータ展開
- `CZ_CONFIRM_CODES` で CZ-500〜799 の確認ダイアログ判定

#### バイト長計算 (utils/byteLength.ts)

- 半角英数字・記号 (U+0000〜U+007F): 1バイト
- 半角カタカナ (U+FF61〜U+FF9F): 2バイト
- 全角文字 (U+0080以上、半角カタカナ除く): 2バイト
- `for...of` ループでサロゲートペアを安全にイテレーション

#### エラー表示パターン

| パターン | コンポーネント | トリガー条件 |
|---------|-------------|------------|
| セルエラー | セル赤枠 + ツールチップ | Layer 1 バリデーション失敗 |
| 行エラー | 行ハイライト + スクロール | API エラーの recordId 指定 |
| Toast | PrimeVue Toast (severity 別) | 操作成功/失敗通知 |
| オーバーレイ | 画面全体マスク + メッセージ | CZ-102 サービス時間外 |
| ConfirmDialog | PrimeVue ConfirmDialog | CZ-500〜799 確認系 |

#### PrimeVue 統合

- `useNotification` composable: Toast ラッパー（success/warn/error/info）
- `useConfirmAction` composable: ConfirmDialog ラッパー（CZ-500〜799）
- API レスポンスのエラーコードレンジで自動振り分け

### バックエンド

#### ValidationService

`service/ValidationService.java` として実装。

| メソッド | 対応ルール | 説明 |
|---|---|---|
| `validateWorkHours(entity)` | VR-001〜015 | 一括バリデーション |
| `validateWorkDate(workDate, yearMonth)` | VR-001, VR-002 | 日付チェック |
| `validateHours(hours)` | VR-008, VR-009 | 工数チェック |
| `validateSubject(subject, hosyuSyubetu)` | VR-006, VR-007 | 件名 + 禁止ワード |
| `validateWorkRequestNo(no, category)` | VR-012, VR-014 | 依頼書No |
| `checkDailyTotal(staffId, workDate, newMin, excId)` | VR-010 | 日次合計 |
| `checkForbiddenWords(subject, hosyuSyubetu)` | VR-007 | 禁止ワード12語 |
| `calculateByteLength(str)` | — | バイト長計算 |

- 禁止ワード12語は `application.yml` で管理

#### CzBusinessException + GlobalExceptionHandler

- `CzBusinessException`: code, params, field, recordId を保持
- コードレンジ → HTTPステータス自動マッピング（CZ-1xx→400、CZ-102→403、CZ-3xx→500）
- `GlobalExceptionHandler`: 楽観ロック競合 → 409 + CZ-101

#### ServiceTimeFilter (BR-001: 6:00-23:30)

- `OncePerRequestFilter` 継承
- `ZoneId.of("Asia/Tokyo")` で明示的にタイムゾーン指定
- GET リクエストは時間外でも許可

#### StatusMatrixResolver 連携

- `resolve(statusKey, isTanSeries)` → 操作別状態マップ
- 操作不可時は CZ-106〜CZ-110 をスロー

### データベース

N/A — エラー体系はアプリケーション層のみで完結する。

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | CZ-000〜999（92件）100%踏襲。VR-001〜015、BR-001〜007、禁止ワード12語、12状態マトリクスすべて現行仕様準拠 |
| II. 仕様完全性 | ✅ | 全バリデーションルール、エラーレスポンス構造、表示パターン、テスト要件を定義済み |
| III. Docker-First | ✅ | ServiceTimeFilter のタイムゾーンは `ZoneId.of("Asia/Tokyo")` で明示指定 |
| IV. TDD | ✅ | 168パターン（マトリクス）+ VR境界値テスト + 禁止ワード12語テスト + HH:MM変換。踏襲ロジック100% |
| V. UX-First | ✅ | セルエラー（赤枠+ツールチップ）、Toast、オーバーレイ、ConfirmDialog。PrimeVue 統合 |
| VI. Production Safety | — | バリデーション層に開発専用機能なし |
| VIII. CI/CD Safety | ✅ | VR/BR テストは H2 で高速実行。VR-010 のみ統合テスト |
| IX. 最適技術選定 | ✅ | PrimeVue Toast/ConfirmDialog 標準活用 |
| X. コード品質 | ✅ | ValidationService / useValidation に集約。messages.yml/messages.ts で一元管理 |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| バイト長計算の FE/BE 一貫性 | 中 | FE は `for...of`（コードポイント単位）。同一テストケースで FE/BE 一致を保証 |
| サロゲートペア処理 | 低 | BMP 外文字は Phase 1 では現行互換（4バイト計算）を維持 |
| メッセージパラメータ展開の FE/BE 不一致 | 中 | 同一の `{0}` 記法。E2E テストで FE/BE メッセージ一致を検証 |
| ServiceTimeFilter のタイムゾーン | 低 | `ZoneId.of("Asia/Tokyo")` 明示指定 + `Clock` インジェクションで境界値テスト |
| ステータスマトリクスの完全性 | 高 | 168パターンのパラメタライズドテストで網羅 |

---

## 依存関係

### 依存先

なし — Layer 0（基盤層）として独立実装可能。

### 依存元

| spec | 依存内容 |
|---|---|
| #4 工数入力 | useValidation、VR-001〜015、セルエラー表示、CZ メッセージカタログ |
| #5 工数一覧 | StatusMatrixResolver、ステータス別操作制御、CZ-106〜110、ConfirmDialog |
| #6 分析画面 | Toast 通知、エラーハンドリング共通基盤 |
| #7 共通コンポーネント | バイト長計算、文字種バリデーション、メッセージ解決 |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| Frontend composable | 3 | useValidation, useNotification, useConfirmAction |
| Frontend constants | 1 | messages.ts（CZ メッセージカタログ92件） |
| Frontend utils | 2 | byteLength.ts, hoursFormat.ts |
| Frontend types | 1 | validation.ts（型定義） |
| Backend Service | 2 | ValidationService, StatusMatrixResolver |
| Backend Exception | 1 | CzBusinessException |
| Backend Handler | 1 | GlobalExceptionHandler |
| Backend Filter | 1 | ServiceTimeFilter |
| Backend Resource | 1 | messages.yml |
| Backend Util | 2 | MessageResolver, ByteLengthCalculator |
| テスト | ~20 | 約400テストパターン（168マトリクス + VR境界値 + 禁止ワード + HH:MM + バイト長） |
| **合計** | **~35** | |

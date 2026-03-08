# バリデーション & エラー体系 タスク一覧

## 概要
- 総タスク数: 40
- 見積もり合計: 55 時間

CZ 全画面で適用されるバリデーション・エラーメッセージ体系を、
現行 ApCheck.js + ApMessage.xml + 各 Proc isInputCheck() から
Frontend composable + Backend ValidationService の2層構成に移行する。
Layer 0（基盤層）として全機能（#4, #5, #6, #7）が依存する最優先実装対象。

---

## タスク一覧

### Phase 1: CZ メッセージカタログ（Frontend）

- [x] **T-001**: メッセージカタログのテストを作成
  - 種別: テスト
  - 内容: CZ-000〜CZ-999 の 92 件全メッセージの定義確認テスト。resolveMessage(code, params) のパラメータ展開テスト（"{0}は必須入力です" + ["作業日"] → "作業日は必須入力です"）。CZ_CONFIRM_CODES（CZ-500〜799）の判定テスト
  - 成果物: `tests/constants/messages.test.ts`
  - 完了条件: 全メッセージの存在確認 + パラメータ展開テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [x] **T-002**: メッセージカタログを実装
  - 種別: 実装
  - 内容: constants/messages.ts に CZ_MESSAGES（92件）を定義。resolveMessage() 関数（{0} パラメータ展開）。CZ_CONFIRM_CODES セット（CZ-500〜799 判定用）。コードレンジ定数（SUCCESS_RANGE, WARN_RANGE, ERROR_RANGE, CONFIRM_RANGE, INFO_RANGE）
  - 成果物: `constants/messages.ts`
  - 完了条件: T-001 のテストが全て Green
  - 依存: T-001
  - 見積もり: 1.5 時間

### Phase 2: バイト長計算ユーティリティ

- [x] **T-003**: バイト長計算（Frontend）のテストを作成
  - 種別: テスト
  - 内容: calculateByteLength の全パターンテスト。半角英数字("abc"→3)、全角文字("あいう"→6)、半角カタカナ("ｱｲｳ"→6)、混在("aあｱ"→5)、空文字列(""→0)、128バイト境界値
  - 成果物: `tests/utils/byteLength.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [x] **T-004**: バイト長計算（Frontend）を実装
  - 種別: 実装
  - 内容: utils/byteLength.ts。for...of ループでサロゲートペア安全イテレーション。U+FF61〜U+FF9F（半角カタカナ）→2B、U+0080以上→2B、U+0000〜U+007F→1B
  - 成果物: `utils/byteLength.ts`
  - 完了条件: T-003 のテストが全て Green
  - 依存: T-003
  - 見積もり: 0.5 時間

- [x] **T-005**: バイト長計算（Backend）のテストを作成
  - 種別: テスト
  - 内容: ByteLengthCalculator.calculateByteLength の全パターンテスト。Frontend と同一テストケース（FE/BE 一致保証）
  - 成果物: `src/test/java/.../util/ByteLengthCalculatorTest.java`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [x] **T-006**: バイト長計算（Backend）を実装
  - 種別: 実装
  - 内容: util/ByteLengthCalculator.java。Frontend と同一アルゴリズム
  - 成果物: `src/main/java/.../util/ByteLengthCalculator.java`
  - 完了条件: T-005 のテストが全て Green
  - 依存: T-005
  - 見積もり: 0.5 時間

### Phase 3: useValidation composable（Frontend Layer 1）

- [x] **T-007**: useValidation のテストを作成（必須・形式チェック）
  - 種別: テスト
  - 内容: validateRequired（空文字列, null, undefined, 空白のみ）、validateHoursFormat（HH:MM パース全パターン: 空, 1桁, 2桁, 3桁, 4桁, コロン付き, 不正値）、validate15MinUnit（00/15/30/45→OK, 01〜14等→NG）、validateByteLength（128バイト境界値）
  - 成果物: `tests/composables/useValidation.test.ts`
  - 完了条件: テストが Red
  - 依存: T-004
  - 見積もり: 1.5 時間

- [x] **T-008**: useValidation のテストを作成（日付・文字種・固定長チェック）
  - 種別: テスト
  - 内容: validateDateInMonth（2025-02-01〜2025-02-28 境界値、月外日付）、validateFixedLength（7文字固定: 0文字→OK, 1〜6文字→NG, 7文字→OK, 8文字→NG）、validateCharType（mode="10111" TMR番号: 半角数字のみ OK）
  - 成果物: `tests/composables/useValidation.test.ts`（追記）
  - 完了条件: テストが Red
  - 依存: T-004
  - 見積もり: 1 時間

- [x] **T-009**: useValidation のテストを作成（一括バリデーション）
  - 種別: テスト
  - 内容: validateWorkHoursRecord（VR-001〜013 の一括実行テスト）。全フィールド正常値→OK、各フィールド欠損→対応エラー、複数エラー同時検出
  - 成果物: `tests/composables/useValidation.test.ts`（追記）
  - 完了条件: テストが Red
  - 依存: T-004
  - 見積もり: 1 時間

- [x] **T-010**: useValidation composable を実装
  - 種別: 実装
  - 内容: composables/useValidation.ts。各バリデーション関数 + validateWorkHoursRecord（isInputCheck 相当）。ValidationResult 型（valid, code, message, field）。Layer 2 専用ルール（VR-007, VR-010, VR-014, VR-015）は含めない
  - 成果物: `composables/useValidation.ts`, `types/validation.ts`
  - 完了条件: T-007, T-008, T-009 のテストが全て Green
  - 依存: T-007, T-008, T-009
  - 見積もり: 2 時間

### Phase 4: Backend ValidationService（Layer 2）

- [x] **T-011**: ValidationService のテストを作成（VR-001〜009）
  - 種別: テスト
  - 内容: validateWorkDate（必須 + YYYY-MM-DD + 月内範囲）、validateHours（必須 + 0超 + HH:MM + 15分単位 + 最小0:15）、validateSubject（必須 + 128バイト）の境界値テスト
  - 成果物: `src/test/java/.../service/ValidationServiceTest.java`
  - 完了条件: テストが Red
  - 依存: T-006
  - 見積もり: 1.5 時間

- [x] **T-012**: ValidationService のテストを作成（VR-010〜015）
  - 種別: テスト
  - 内容: checkDailyTotal（23:45+0:30=24:15→CZ-146）、TMR番号5文字(VR-011)、依頼書No7文字固定(VR-012)、依頼者名40文字(VR-013)、条件付き必須(VR-014)、カテゴリ重複(VR-015)
  - 成果物: `src/test/java/.../service/ValidationServiceTest.java`（追記）
  - 完了条件: テストが Red
  - 依存: T-006
  - 見積もり: 1.5 時間

- [x] **T-013**: 禁止ワード12語のテストを作成
  - 種別: テスト
  - 内容: checkForbiddenWords の全12語テスト。各語を含む件名→CZ-141。HOSYU_SYUBETU_0 以外→チェックスキップ。禁止ワードなし→OK。先頭/末尾/中間にヒットするケース
  - 成果物: `src/test/java/.../service/ForbiddenWordTest.java`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [x] **T-014**: ValidationService を実装
  - 種別: 実装
  - 内容: service/ValidationService.java。VR-001〜015 の全ルール + 禁止ワード12語 + バイト長計算。validateWorkHours()（isInputCheck 相当）。禁止ワードリストは @ConfigurationProperties で application.yml から注入
  - 成果物: `src/main/java/.../service/ValidationService.java`
  - 完了条件: T-011, T-012, T-013 のテストが全て Green
  - 依存: T-011, T-012, T-013
  - 見積もり: 2 時間

### Phase 5: CzBusinessException + GlobalExceptionHandler

- [x] **T-015**: CzBusinessException のテストを作成
  - 種別: テスト
  - 内容: code, params, field, recordId の保持テスト。httpStatus() マッピング（CZ-1xx→400、CZ-102→403、CZ-3xx→500）
  - 成果物: `src/test/java/.../exception/CzBusinessExceptionTest.java`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [x] **T-016**: CzBusinessException を実装
  - 種別: 実装
  - 内容: exception/CzBusinessException.java。RuntimeException 継承。code, params, field, recordId フィールド。httpStatus() メソッド
  - 成果物: `src/main/java/.../exception/CzBusinessException.java`
  - 完了条件: T-015 のテストが全て Green
  - 依存: T-015
  - 見積もり: 0.5 時間

- [x] **T-017**: GlobalExceptionHandler のテストを作成
  - 種別: テスト
  - 内容: CzBusinessException → エラーレスポンス JSON 変換、OptimisticLockException → 409 + CZ-101、一般 Exception → 500 + CZ-300、メッセージパラメータ展開テスト
  - 成果物: `src/test/java/.../config/GlobalExceptionHandlerTest.java`
  - 完了条件: テストが Red
  - 依存: T-016
  - 見積もり: 1 時間

- [x] **T-018**: GlobalExceptionHandler を実装
  - 種別: 実装
  - 内容: @RestControllerAdvice。CzBusinessException / OptimisticLockException / 一般 Exception ハンドリング。MessageResolver でパラメータ展開
  - 成果物: `src/main/java/.../config/GlobalExceptionHandler.java`
  - 完了条件: T-017 のテストが全て Green
  - 依存: T-017
  - 見積もり: 1 時間

- [x] **T-019**: Backend メッセージリソース (messages.yml) を作成
  - 種別: 実装
  - 内容: CZ-000〜CZ-999 の 92 件を messages.yml に定義。MessageResolver クラスで yml から読み込み + {0} パラメータ展開
  - 成果物: `src/main/resources/messages.yml`, `src/main/java/.../util/MessageResolver.java`
  - 完了条件: 全メッセージコードが解決可能
  - 依存: T-018
  - 見積もり: 1 時間

### Phase 6: ServiceTimeFilter（BR-001）

- [x] **T-020**: ServiceTimeFilter のテストを作成
  - 種別: テスト
  - 内容: BR-001 サービス提供時間（6:00〜23:30）チェック。境界値テスト（5:59→拒否, 6:00→許可, 23:30→許可, 23:31→拒否）。GET は時間外でも許可。Clock インジェクションで時刻制御
  - 成果物: `src/test/java/.../security/ServiceTimeFilterTest.java`
  - 完了条件: 境界値テストが Red
  - 依存: T-016
  - 見積もり: 1 時間

- [x] **T-021**: ServiceTimeFilter を実装
  - 種別: 実装
  - 内容: OncePerRequestFilter 継承。ZoneId.of("Asia/Tokyo") で明示的タイムゾーン。GET はパススルー。POST/PATCH/DELETE は時間外→403 + CZ-102
  - 成果物: `src/main/java/.../security/ServiceTimeFilter.java`
  - 完了条件: T-020 のテストが全て Green
  - 依存: T-020
  - 見積もり: 0.5 時間

### Phase 7: useNotification / useConfirmAction composable（Frontend）

- [x] **T-022**: useNotification のテストを作成
  - 種別: テスト
  - 内容: success/warn/error/info メソッドのテスト。CZ コードレンジ自動判定（CZ-000〜099→success、CZ-100〜299→warn、CZ-300〜499→error、CZ-800〜999→info）。自動消去タイマー（success:3秒、warn:5秒、error:手動）
  - 成果物: `tests/composables/useNotification.test.ts`
  - 完了条件: テストが Red
  - 依存: T-002
  - 見積もり: 1 時間

- [x] **T-023**: useNotification composable を実装
  - 種別: 実装
  - 内容: PrimeVue Toast ラッパー。CZ コードレンジ → severity 自動判定。life パラメータ設定
  - 成果物: `composables/useNotification.ts`
  - 完了条件: T-022 のテストが全て Green
  - 依存: T-022
  - 見積もり: 1 時間

- [x] **T-024**: useConfirmAction のテストを作成
  - 種別: テスト
  - 内容: CZ-505〜518 の確認ダイアログ表示テスト。OK → confirm イベント、キャンセル → cancel イベント、Promise ベースの結果返却
  - 成果物: `tests/composables/useConfirmAction.test.ts`
  - 完了条件: テストが Red
  - 依存: T-002
  - 見積もり: 0.5 時間

- [x] **T-025**: useConfirmAction composable を実装
  - 種別: 実装
  - 内容: CZ-500〜799 コード → ConfirmDialog 自動表示。Promise<boolean> で結果返却
  - 成果物: `composables/useConfirmAction.ts`
  - 完了条件: T-024 のテストが全て Green
  - 依存: T-024
  - 見積もり: 0.5 時間

### Phase 8: エラー表示コンポーネント統合

- [x] **T-026**: セルエラー表示のテストを作成
  - 種別: テスト
  - 内容: セル赤枠 + ツールチップ表示のテスト。useValidation の結果をセルに反映。エラー時: border-color: red + ツールチップにメッセージ。正常時: 通常スタイル
  - 成果物: `tests/components/common/CellError.test.ts`
  - 完了条件: テストが Red
  - 依存: T-010
  - 見積もり: 1 時間

- [x] **T-027**: セルエラー表示を実装
  - 種別: 実装
  - 内容: CSS クラス (.cell-error: border + tooltip)、useCellValidation composable（フォーカスアウト → useValidation 実行 → スタイル適用）
  - 成果物: `composables/useCellValidation.ts`, `assets/css/cell-error.css`
  - 完了条件: T-026 のテストが全て Green
  - 依存: T-026
  - 見積もり: 1 時間

- [x] **T-028**: 行エラーハイライト + スクロールのテストを作成
  - 種別: テスト
  - 内容: API エラーレスポンスの recordId から行を特定し、ハイライト + スクロール + セルフォーカスのテスト（一括確認エラー時）
  - 成果物: `tests/composables/useRecordError.test.ts`
  - 完了条件: テストが Red
  - 依存: T-010
  - 見積もり: 0.5 時間

- [x] **T-029**: 行エラーハイライト + スクロールを実装
  - 種別: 実装
  - 内容: useRecordError composable。recordId → DOM 行特定 → scrollIntoView → フォーカス → ハイライトアニメーション
  - 成果物: `composables/useRecordError.ts`
  - 完了条件: T-028 のテストが全て Green
  - 依存: T-028
  - 見積もり: 0.5 時間

- [x] **T-030**: サービス時間外オーバーレイのテストを作成
  - 種別: テスト
  - 内容: CZ-102 受信時の画面全体オーバーレイ表示テスト。メッセージ表示、操作不可状態
  - 成果物: `tests/components/common/ServiceTimeOverlay.test.ts`
  - 完了条件: テストが Red
  - 依存: T-002
  - 見積もり: 0.5 時間

- [x] **T-031**: サービス時間外オーバーレイを実装
  - 種別: 実装
  - 内容: components/common/ServiceTimeOverlay.vue。画面全体マスク + 中央メッセージ。useApi のエラーインターセプターから自動呼出
  - 成果物: `components/common/ServiceTimeOverlay.vue`
  - 完了条件: T-030 のテストが全て Green
  - 依存: T-030
  - 見積もり: 0.5 時間

### Phase 9: FE/BE 一貫性テスト

- [x] **T-032**: バイト長計算の FE/BE 一致テストを作成
  - 種別: テスト
  - 内容: 同一テストケース（半角/全角/半角カタカナ混在の代表パターン 20 件）で Frontend calculateByteLength と Backend ByteLengthCalculator の結果が一致することを E2E テストで検証
  - 成果物: `tests/e2e/validation-consistency.spec.ts`
  - 完了条件: FE/BE 結果一致テスト Green
  - 依存: T-004, T-006
  - 見積もり: 1 時間

- [x] **T-033**: メッセージ展開の FE/BE 一致テストを作成
  - 種別: テスト
  - 内容: CZ-126 等のパラメータ付きメッセージが Frontend resolveMessage と Backend MessageResolver で同一結果を返すことを検証
  - 成果物: `tests/e2e/message-consistency.spec.ts`
  - 完了条件: FE/BE メッセージ一致テスト Green
  - 依存: T-002, T-019
  - 見積もり: 0.5 時間

### Phase 10: E2E テスト

- [ ] **T-034**: バリデーションエラー E2E テスト（必須・形式）
  - 種別: テスト
  - 内容: Playwright で工数入力画面を操作。空の件名で一括確認→セル赤枠+CZ-126。15分単位違反（03:10）→CZ-147。HH:MM 形式違反→CZ-125
  - 成果物: `tests/e2e/validation-required.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-010, T-014, T-027
  - 見積もり: 1.5 時間

- [ ] **T-035**: バリデーションエラー E2E テスト（ビジネスルール）
  - 種別: テスト
  - 内容: 禁止ワード（件名に"カ層"含む→CZ-141）、日次24h超過（合計24:15→CZ-146）、カテゴリ重複（VR-015→CZ-132）
  - 成果物: `tests/e2e/validation-business.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-014, T-027
  - 見積もり: 1.5 時間

- [ ] **T-036**: 楽観的ロック競合 E2E テスト
  - 種別: テスト
  - 内容: 2ブラウザ同時編集シナリオ。ユーザーA取得→ユーザーB更新→ユーザーA更新→409→CZ-101 Toast 表示確認
  - 成果物: `tests/e2e/optimistic-lock.spec.ts`
  - 完了条件: E2E テスト Green
  - 依存: T-018, T-023
  - 見積もり: 1.5 時間

### Phase 11: 受け入れ基準テスト（AC-VE-01〜AC-VE-12）

- [ ] **T-039**: 受け入れ基準テスト（AC-VE-01〜AC-VE-06）を作成
  - 種別: テスト
  - 内容: spec.md「受け入れ基準（Given-When-Then）」の AC-VE-01〜AC-VE-06 を検証する統合テスト。AC-VE-01: FE Layer 1 バリデーション（件名空 → CZ-126 セル赤枠 + API コール無し）。AC-VE-02: BE Layer 2 バリデーション（日次合計24h超 → 400 + CZ-146）。AC-VE-03: サービス時間制限（23:31 POST → 403 + CZ-102 + オーバーレイ）。AC-VE-04: 楽観的ロック競合（同時更新 → 409 + CZ-101 + Toast + リロード）。AC-VE-05: 禁止語句検出（hosyuSyubetu=0 + 「カ層」 → CZ-141 + params に禁止語）。AC-VE-06: 一括確定エラー（最初のエラーで停止 + recordId 付き CZ-126 + 行ハイライト + スクロール）
  - 成果物: `tests/e2e/acceptance-ve-01-06.spec.ts`
  - 完了条件: AC-VE-01〜AC-VE-06 の GWT シナリオが全て Green
  - 依存: T-034, T-035, T-036
  - 見積もり: 1.5 時間

- [ ] **T-040**: 受け入れ基準テスト（AC-VE-07〜AC-VE-12）を作成
  - 種別: テスト
  - 内容: spec.md「受け入れ基準（Given-When-Then）」の AC-VE-07〜AC-VE-12 を検証する統合テスト。AC-VE-07: 確認ダイアログ（CZ-505 → ConfirmDialog 表示 + はい/いいえ制御）。AC-VE-08: エラーレスポンス構造・単一（{ error: { code, message, field, params } }）。AC-VE-09: エラーレスポンス構造・複数（{ errors: [...] } 配列構造）。AC-VE-10: 15分単位バリデーション（3:20 → CZ-147、3:15/3:30 → 正常）。AC-VE-11: バイト長境界値（128バイト → OK、129バイト → CZ-128）。AC-VE-12: CZ コード → HTTP ステータスマッピング（CZ-1xx→400、CZ-102→403、CZ-3xx→500、楽観ロック→409）
  - 成果物: `tests/e2e/acceptance-ve-07-12.spec.ts`
  - 完了条件: AC-VE-07〜AC-VE-12 の GWT シナリオが全て Green
  - 依存: T-034, T-035, T-036
  - 見積もり: 1.5 時間

### Phase 12: リファクタリング + 品質

- [ ] **T-037**: バリデーション関連コードのリファクタリング
  - 種別: リファクタ
  - 内容: useValidation / ValidationService の共通パターン抽出。重複コード削除。テスト可読性改善
  - 成果物: 既存ファイルのリファクタリング
  - 完了条件: 既存テストが全て Green のまま
  - 依存: T-039, T-040
  - 見積もり: 1.5 時間

- [ ] **T-038**: カバレッジ確認
  - 種別: テスト
  - 内容: VR-001〜015 / BR-001〜007 / 禁止ワード12語 / バイト長計算 / メッセージカタログ92件の踏襲ロジック 100% カバレッジ確認
  - 成果物: カバレッジレポート
  - 完了条件: 踏襲ロジック 100% カバレッジ達成
  - 依存: T-037
  - 見積もり: 1 時間

---

## 依存関係図

```
Phase 1 (Messages)
T-001→T-002

Phase 2 (ByteLength)
T-003→T-004 (FE)  T-005→T-006 (BE)

Phase 3 (useValidation)
T-007→T-010  T-008→T-010  T-009→T-010

Phase 4 (ValidationService)
T-011→T-014  T-012→T-014  T-013→T-014

Phase 5 (Exception)
T-015→T-016→T-017→T-018→T-019

Phase 6 (ServiceTime)
T-020→T-021

Phase 7 (Notification)
T-022→T-023  T-024→T-025

Phase 8 (Error Display)
T-026→T-027  T-028→T-029  T-030→T-031

Phase 9 (Consistency)
T-032  T-033

Phase 10 (E2E)
T-034  T-035  T-036

Phase 11 (AC Acceptance)
T-039  T-040

Phase 12 (Quality)
T-037→T-038
```

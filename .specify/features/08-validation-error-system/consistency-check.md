# #8 Validation & Error System — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `08-validation-error-system/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1-#7/#9-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| エラーメッセージ体系 CZ-000〜999 | ✅ 合致 | constitution I「CZ-000〜CZ-999 のエラーコード体系（92件）は踏襲」。spec: セクション 7 で全コード定義 |
| バリデーション 15件保全 | ✅ 合致 | constitution I「入力バリデーション 15 件」。spec: セクション 3.1 で VR-001〜VR-015 定義 |
| 時間工数制約 7件保全 | ✅ 合致 | constitution I「時間工数制約 7 件」。spec: セクション 2.1 で BR-001〜BR-007 定義 |
| 禁止語句 12語保全 | ✅ 合致 | constitution I「禁止語句 12 語」。spec: セクション 4.1 で 12語リスト |
| 12状態ステータスマトリクス | ✅ 合致 | constitution I。spec: セクション 6 でステータスマトリクスバリデーション定義 |
| TDD | ✅ 合致 | constitution IV。spec: セクション 10 で Backend/Frontend/E2E テスト要件定義（168パターン含む） |
| Spring Boot REST API | ✅ 合致 | constitution 技術スタック。spec: GlobalExceptionHandler + CzBusinessException |
| Doma 2 ORM | ✅ 合致 | constitution 技術スタック。spec: Backend バリデーションで DAO 参照 |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| ApCheck.js 入力検証 | クライアントサイドチェック | Frontend Layer 1 + Backend Layer 2 | ✅ IMPROVE（2層化） |
| ApMessage.xml メッセージ管理 | XML ベース | messages.yml + TypeScript カタログ | ✅ IMPROVE |
| Proc.isInputCheck() | サーバーサイドチェック | ValidationService.validateWorkHours() | ✅ KEEP（ロジック踏襲） |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| useTanSeries() | ✅ 合致 | セクション 6.3 で `principal.permissions().useTanSeries()` 使用 |
| StatusMatrixResolver | ✅ 合致 | セクション 6.3 で 12状態 × 2系列のバリデーション |
| CZ-307 認証エラー | ✅ 合致 | セクション 7.3 で定義 |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| ビジネスルール BR-001〜007 | ✅ 合致 | analysis セクション 2 の 7件を完全踏襲 |
| バリデーション VR-001〜015 | ✅ 合致 | analysis セクション 2 の 15件を完全踏襲 |
| エラーカタログ 92件 | ✅ 合致 | analysis セクション 3 のメッセージを移植 |

### 05_gap_analysis.md との整合

| 項目 | GAP | 結果 | 詳細 |
|---|---|---|---|
| 業務ロジック保全 | セクション 4 | ✅ 合致 | 時間制約7件・ステータス5件・バリデーション15件・年度3件・禁止語句12語すべてカバー |
| エラーメッセージ保全 | — | ✅ 合致 | 92件のエラーコード体系を踏襲 |

---

## C. 他 Spec との整合性

### spec #2 (auth-infrastructure) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| StatusMatrixResolver API | ✅ 合致 | spec #2 セクション 6.4 の resolve(statusKey, isTab010Bit2) と同一 |
| useTanSeries() 使用 | ✅ 合致 | spec #2 セクション 6.1.1 のセマンティックエイリアス使用 |
| CZ-307 セキュリティエラー | ✅ 合致 | spec #2 セクション 6.2 の認証エラーコード |
| CZ-102 サービス時間外 | ✅ 合致 | BR-001 の 403 応答 |

### spec #3 (core-api-design) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| エラーレスポンス構造 | ✅ 合致 | `{ error: { code, message, field, params, recordId } }` |
| 楽観的ロック 409 | ✅ 合致 | CZ-101 エラーコード |
| 複数エラーレスポンス | ✅ 合致 | `{ errors: [...] }` 形式（一括操作時） |

### spec #4 (work-hours-input) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| HH:MM 入力補助 BR-006 | ✅ 合致 | spec #4 の入力変換パターンと同一 |
| VR-001〜015 適用 | ✅ 合致 | FORM_010 のバリデーション |
| セルエラー表示 | ✅ 合致 | セクション 9.3 のセル赤枠 + ツールチップ |

### spec #5 (work-status-list) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 月次確認/未確認/集約 | ✅ 合致 | ステータスマトリクスバリデーションが月次制御に適用 |

### spec #7 (common-components) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| MessageToast severity マッピング | ✅ 合致 | spec #7 セクション 11.2 のコード範囲 (000-099:success, 100-299:warn, 300-499:error, 800-999:info) |
| ConfirmDialog メッセージ一覧 | ⚠️ コード範囲不一致 | spec #7 セクション 7.3 で CZ-802/803/804 を確認ダイアログとして使用。しかし spec #8 では CZ-802/803/804 は情報メッセージ (800〜999)、確認ダイアログは CZ-509/510/511 (500〜799) → **FIX-V02** |
| calculateByteLength | ✅ 合致 | spec #7 セクション 12.1 と spec #8 セクション 5.2 で同一実装 |
| useApi エラーハンドリング | ✅ 合致 | spec #7 セクション 14.3 の 401/403/409 処理 |

---

## D. レガシーシステムとの差異分類

### KEEP（踏襲）

| 項目 | 現行 | 新システム |
|---|---|---|
| CZ エラーコード体系 (92件) | ApMessage.xml | messages.yml + TypeScript カタログ |
| BR-001〜007 時間工数制約 | Proc 内ロジック | ValidationService |
| VR-001〜015 入力バリデーション | ApCheck.js + isInputCheck() | useValidation() + ValidationService |
| 禁止ワード 12語 | ハードコード | application.yml 外部化 |
| バイト長計算（全角2/半角1/半角カタカナ2） | Java calculateByteLength | Frontend + Backend 同一ロジック |
| 文字種制御 mode (5桁制御文字列) | ApCheck.chkChara() | validateCharType() |
| サービス時間チェック (6:00-23:30) | サーバーサイド | ServiceTimeFilter (GET は許可) |

### IMPROVE（改善）

| 項目 | 現行 | 新システム | 理由 |
|---|---|---|---|
| バリデーション層 | サーバーサイドのみ（一部 JS） | 2層（Frontend 即時 + Backend 確定） | UX 向上（即時フィードバック） |
| エラー表示 | alert() + 画面再表示 | セル赤枠 + ツールチップ + Toast | 該当セルへのフォーカス |
| メッセージ管理 | XML ファイル | YAML + TypeScript 定数 | 型安全 + IDE 補完 |
| 禁止ワード管理 | ハードコード | application.yml 外部化 | 変更容易性 |
| 日次合計チェック | 画面遷移時一括 | API 呼出時リアルタイム | 即座のフィードバック |

### ADD（新規追加）

| 項目 | 説明 |
|---|---|
| 複数エラーレスポンス | 一括操作時の `errors[]` 配列形式 |
| GlobalExceptionHandler | 統一例外処理 + CzBusinessException |
| エラー種別別 Toast 表示 | severity 別の色・自動消去時間制御 |
| サービス時間外オーバーレイ | CZ-102 受信時の画面全体マスク |

### REMOVE（廃止）

| 項目 | 理由 |
|---|---|
| ApCheck.js | useValidation composable に置換 |
| ApMessage.xml | messages.yml + TypeScript カタログに置換 |
| chkChara mode 定数のハードコード | 構造化された CharMode 型に移行 |

---

## E. 推奨アクション

### FIX-V01: CzBusinessException.httpStatus() のコード範囲判定ロジック誤り (P2)

**箇所**: セクション 8.3

**問題**: `code.substring(0, 5)` による switch 判定では、CZ-3xx 系の全コードを
正しくマッピングできない。

```java
// 現行ロジック
return switch (code.substring(0, 5)) {
    case "CZ-10" -> code.equals("CZ-102") ? 403 : 400;
    case "CZ-30" -> 500;
    default -> 400;
};
```

`substring(0, 5)` は5文字を抽出するため:
- "CZ-300" → "CZ-30" → 500 ✅
- "CZ-310" → "CZ-31" → default 400 ❌（システムエラーなので 500 が正しい）
- "CZ-317" → "CZ-31" → default 400 ❌
- "CZ-321" → "CZ-32" → default 400 ❌
- "CZ-330" → "CZ-33" → default 400 ❌

CZ-310〜CZ-330 の 8 コード（表示/更新/削除/ソート/遷移/DL/Excel/JSON エラー）が
500 ではなく 400 を返してしまう。

**修正**: `substring(0, 4)` で "CZ-3" を判定に変更する。

---

### FIX-V02: spec #7 確認ダイアログのメッセージコード範囲不一致 (P1)

**箇所**: spec #7 (`07-common-components/spec.md`) セクション 7.3

**問題**: 月次制御の確認ダイアログに CZ-802/803/804 を使用しているが、
spec #8 のメッセージカタログでは:
- CZ-500〜799 = 確認ダイアログ範囲
- CZ-800〜999 = 情報メッセージ範囲

正しいマッピング:
| 操作 | 確認ダイアログ（操作前） | 情報メッセージ（操作後） |
|---|---|---|
| 月次未確認 | **CZ-509** 「記入可能」に戻します。よろしいですか？ | CZ-802 入力可能に戻しました |
| 月次確認 | **CZ-510** 「登録確認」に変更します。よろしいですか？ | CZ-803 確認しました |
| 月次集約 | **CZ-511** 「データ承認」に変更します。よろしいですか？ | CZ-804 データ承認に変更しました |

spec #7 セクション 7.3 の CZ-802/803/804 行を CZ-509/510/511 に修正する。
spec #7 セクション 11.2 の severity マッピング（800〜999 = info, 3秒自動消去）とも
整合する。

---

## F. 変更履歴

| 日付 | FIX ID | 内容 | 対象ファイル | 状態 |
|---|---|---|---|---|
| 2026-02-26 | FIX-V01 | CzBusinessException.httpStatus() コード範囲判定修正（substring(0,5)→substring(0,4)） | 08-validation-error-system/spec.md | ✅ 完了 |
| 2026-02-26 | FIX-V02 | spec #7 確認ダイアログコード修正 (CZ-802/803/804 → CZ-509/510/511) | 07-common-components/spec.md | ✅ 完了 |

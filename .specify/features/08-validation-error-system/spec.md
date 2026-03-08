# バリデーション & エラー体系: 入力検証・メッセージカタログ仕様

## 概要

CZ（保有資源管理システム）全画面で適用される入力検証ルール、
ビジネスルール、エラーメッセージ体系の包括的仕様。
現行 `ApCheck.js` + `ApMessage.xml` + 各 Proc の `isInputCheck()` を、
Frontend バリデーション + Backend バリデーション の2層構成に移行する。

**対応分析ドキュメント**:
- `analysis/03_user_stories.md` セクション 2（ビジネスルール）、セクション 3（エラーカタログ）
- `analysis/05_gap_analysis.md` セクション 4（業務ロジック保全要件）

---

## 1. バリデーション2層アーキテクチャ

```
ユーザー入力
  │
  ▼
[Layer 1: Frontend バリデーション]
  即時フィードバック（セル赤枠 + ツールチップ）
  - 必須チェック
  - 形式チェック (HH:MM, 文字種, バイト長)
  - 範囲チェック (月内日付, 15分単位)
  ※ 通過した場合のみ API 呼出
  │
  ▼
[Layer 2: Backend バリデーション]
  確定的判定（API エラーレスポンス）
  - 日次合計24h チェック (他レコードの合計が必要)
  - 禁止ワードチェック (ワードリストは Backend 管理)
  - ステータスマトリクス判定
  - 楽観的ロック (updatedAt)
  - カテゴリ重複チェック (VR-015)
  - 条件付き必須チェック (VR-014)
  │
  ▼
[API レスポンス]
  成功: { data: { ... } }
  失敗: { error: { code, message, field, params, recordId } }
```

### 1.1 エラーレスポンス構造

```json
{
  "error": {
    "code": "CZ-126",
    "message": "作業日は必須入力です",
    "field": "workDate",
    "params": ["作業日"],
    "recordId": 12345
  }
}
```

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `code` | string | CZ エラーコード |
| `message` | string | `params` 展開済みメッセージ |
| `field` | string? | エラー対象フィールド名 |
| `params` | string[]? | メッセージパラメータ |
| `recordId` | number? | エラー対象レコード ID（一括操作時） |

### 1.2 複数エラーレスポンス（一括操作時）

```json
{
  "errors": [
    {
      "code": "CZ-126",
      "message": "作業日は必須入力です",
      "field": "workDate",
      "recordId": 12345
    },
    {
      "code": "CZ-147",
      "message": "工数は15分単位で入力してください",
      "field": "hours",
      "recordId": 12346
    }
  ]
}
```

一括確認 (`batch-confirm`) では最初のエラーで中断し、単一エラーを返す。

---

## 2. ビジネスルール (BR-001〜007)

### 2.1 時間・工数制約

| ルールID | ルール名 | 制約 | エラーコード | 実装レイヤー |
|---------|---------|------|------------|------------|
| BR-001 | サービス提供時間 | 6:00〜23:30 のみ操作可能 | CZ-102 | Backend ミドルウェア |
| BR-002 | 工数入力単位 | 15分単位 (00/15/30/45) | CZ-147 | Frontend + Backend |
| BR-003 | 日次上限 | 1日合計 24:00 (1440分) 以下 | CZ-146 | Backend のみ |
| BR-004 | 月次基準工数 | 160時間/人月（参考値） | - | レスポンス summary |
| BR-005 | 日次基準工数 | 8時間/人日（参考値） | - | レスポンス summary |
| BR-006 | 入力形式 | HH:MM 形式。HH のみ → HH:00 自動変換 | CZ-125 | Frontend + Backend |
| BR-007 | 最小工数 | 0:15 (15分) 以上 | CZ-129 | Frontend + Backend |

### 2.2 サービス時間チェック (BR-001)

```java
// Backend: AlbOidcAuthFilter or 専用ミドルウェア
@Component
public class ServiceTimeFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Tokyo"));
        LocalTime start = LocalTime.of(6, 0);
        LocalTime end = LocalTime.of(23, 30);

        if (now.isBefore(start) || now.isAfter(end)) {
            // GET は許可、POST/PATCH/DELETE は拒否
            if (!request.getMethod().equals("GET")) {
                response.setStatus(403);
                // CZ-102 エラーレスポンス
            }
        }
    }
}
```

Frontend: API エラー 403 + CZ-102 受信時、画面全体にオーバーレイ表示。

### 2.3 工数入力・変換ルール (BR-006)

| 入力値 | 変換結果 | ルール |
|--------|---------|--------|
| `""` (空) | `"00:00"` | 空値デフォルト |
| `"3"` | `"03:00"` | 1桁数字 → HH:00 |
| `"12"` | `"12:00"` | 2桁数字 → HH:00 |
| `"330"` | `"03:30"` | 3桁数字 → H:MM |
| `"0330"` | `"03:30"` | 4桁数字 → HH:MM |
| `"3:30"` | `"03:30"` | コロン付き → ゼロパディング |
| `"03:30"` | `"03:30"` | 正規 HH:MM → そのまま |
| `"abc"` | エラー | 数字・コロン以外 → CZ-125 |

---

## 3. バリデーションルール (VR-001〜015)

### 3.1 一覧

| ルールID | 対象フィールド | 制約 | エラーコード | Layer 1 | Layer 2 |
|---------|-------------|------|------------|:---:|:---:|
| VR-001 | 作業日 | 必須、YYYY-MM-DD 形式 | CZ-126 | O | O |
| VR-002 | 作業日 | 対象月の月初〜月末の範囲内 | CZ-144 | O | O |
| VR-003 | 対象サブシステム | 必須、マスタ選択 | CZ-126 | O | O |
| VR-004 | 原因サブシステム | 必須、マスタ選択 | CZ-126 | O | O |
| VR-005 | 保守カテゴリ | 必須、マスタ選択 | CZ-126 | O | O |
| VR-006 | 件名 | 必須、128バイト以内 | CZ-126 | O | O |
| VR-007 | 件名 | 禁止ワード不含 (HOSYU_SYUBETU_0) | CZ-141 | - | O |
| VR-008 | 工数 | 必須、0 より大 | CZ-126 | O | O |
| VR-009 | 工数 | HH:MM 形式、15分単位 | CZ-125, CZ-147 | O | O |
| VR-010 | 工数 | 日次合計 24:00 以下 | CZ-146 | - | O |
| VR-011 | TMR番号 | 5文字以内、半角数字 | CZ-138 | O | O |
| VR-012 | 作業依頼書No | 空 or 7文字固定 (1〜6文字はエラー) | CZ-137 | O | O |
| VR-013 | 作業依頼者名 | 40文字以内 | CZ-139 | O | O |
| VR-014 | 依頼書関連 | 特定カテゴリ時、依頼書No + 依頼者名の両方必須 | CZ-142 | - | O |
| VR-015 | カテゴリ重複 | 同一SS + 同一件名で異なるカテゴリ不可 | CZ-132 | - | O |

### 3.2 Layer 1 のみ / Layer 2 のみのルール

**Layer 2 (Backend) のみで判定するルール**:
- **VR-007**: 禁止ワードリストは Backend で管理（環境変数 or DB）
- **VR-010**: 日次合計チェックは他レコードの合計が必要
- **VR-014**: カテゴリとの条件付き必須は Backend で確定判定
- **VR-015**: 同一 SS + 件名の重複チェックは DB 問い合わせが必要

### 3.3 文字種バリデーション

現行 `ApCheck.js` の `chkChara(obj, required, leng, mode, name)` を移植。

```typescript
// mode: 5桁の制御文字列
// mode[0]: 半角英字 (0=許可, 1=禁止)
// mode[1]: 半角数字 (0=許可, 1=禁止)
// mode[2]: 半角カタカナ (0=許可, 1=禁止)
// mode[3]: 特殊文字 (0=引用符&<>制限, 1=全記号許可, 2=禁止)
// mode[4]: 全角文字 (0=許可, 1=禁止)

type CharMode = string  // "10111", "00000", etc.
```

| フィールド | mode | 説明 |
|-----------|------|------|
| TMR番号 | `10111` | 半角数字のみ |
| 作業依頼書No | `10111` | 半角数字のみ |
| 件名 | `00000` | 制限なし（引用符制限あり） |
| 依頼者名 | `00000` | 制限なし（IME ON） |

---

## 4. 禁止ワード (VR-007)

### 4.1 禁止ワードリスト

`HOSYU_SYUBETU_0`（保守種別が "0"）の場合に件名に含めてはならない12語:

| # | 禁止ワード |
|---|-----------|
| 1 | カ層 |
| 2 | @機能別1 |
| 3 | 連絡器 |
| 4 | （相談 |
| 5 | （課程 |
| 6 | ・限定 |
| 7 | ・共存作成 |
| 8 | 取得 |
| 9 | 賃金 |
| 10 | 導入 |
| 11 | 経理 |
| 12 | 実績演算 |

### 4.2 チェックロジック

```java
// Backend: ValidationService
public Optional<String> checkForbiddenWords(String subject, String hosyuSyubetu) {
    if (!"0".equals(hosyuSyubetu)) return Optional.empty();

    for (String word : FORBIDDEN_WORDS) {
        if (subject.contains(word)) {
            return Optional.of(word); // 最初にヒットしたワードを返す
        }
    }
    return Optional.empty();
}
```

エラーメッセージ: CZ-141「{禁止ワード}は業務概要としても定義されているため、記述できません」

### 4.3 管理方式

禁止ワードリストは `application.yml` で管理:

```yaml
app:
  validation:
    forbidden-words:
      - "カ層"
      - "@機能別1"
      - "連絡器"
      # ... 12語
```

---

## 5. バイト長計算

### 5.1 計算ルール

| 文字種 | バイト数 | コードポイント範囲 |
|--------|---------|-----------------|
| 半角英数字・記号 | 1 | U+0000〜U+007F |
| 半角カタカナ | 2 | U+FF61〜U+FF9F |
| 全角文字 | 2 | U+0080 以上（半角カタカナ除く） |

### 5.2 Frontend 実装

```typescript
export function calculateByteLength(str: string): number {
  let bytes = 0
  for (const char of str) {
    const code = char.charCodeAt(0)
    if (code >= 0xFF61 && code <= 0xFF9F) {
      bytes += 2  // 半角カタカナ
    } else if (code > 0x7F) {
      bytes += 2  // 全角
    } else {
      bytes += 1  // 半角
    }
  }
  return bytes
}
```

### 5.3 Backend 実装

```java
public static int calculateByteLength(String str) {
    int bytes = 0;
    for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (c >= 0xFF61 && c <= 0xFF9F) {
            bytes += 2;
        } else if (c > 0x7F) {
            bytes += 2;
        } else {
            bytes += 1;
        }
    }
    return bytes;
}
```

### 5.4 適用箇所

| フィールド | 最大バイト長 | 画面 |
|-----------|------------|------|
| 件名 (subject) | 128 | FORM_010 |
| TMR番号 (tmrNo) | 5 | FORM_010 |
| 作業依頼書No (workRequestNo) | 7 (固定長) | FORM_010 |
| 作業依頼者名 (workRequesterName) | 40 | FORM_010 |

---

## 6. ステータスマトリクス バリデーション

### 6.1 ステータスキー構成

```
ステータスキー (3桁): [データステータス][月次確認フラグ][データ集約フラグ]

データステータス: 0=作成中, 1=確認, 2=確定, 9=緊急停止
月次確認フラグ:   0=未確認, 1=確認済
データ集約フラグ: 0=未集約, 1=集約済

有効な組み合わせ (12種):
000, 010, 011, 100, 110, 111, 200, 210, 211, 900, 910, 911
```

### 6.2 操作 × ステータスエラーコード

| 操作 | 権限不足エラー |
|------|-------------|
| 追加 (ins) | CZ-106 |
| 更新 (upd) | CZ-106 |
| 削除 (del) | CZ-107 |
| コピー (cpy) | CZ-108 |
| 確認 (batch-confirm) | CZ-109 |
| 戻し (revert) | CZ-110 |

### 6.3 Backend 判定フロー

```java
// WorkHoursService.java
public void validateOperation(int recordId, String operation) {
    CzPrincipal principal = CzSecurityContext.require();
    WorkHoursEntity record = workHoursDao.findById(recordId);
    ControlEntity ctrl = controlDao.findByYearMonth(record.getYyyymm());

    String statusKey = buildStatusKey(
        record.getStatus(),
        "1".equals(ctrl.getGjktFlg()),
        "1".equals(ctrl.getDataSkFlg())
    );

    boolean isTanSeries = principal.permissions().useTanSeries();
    Map<String, Integer> matrix = StatusMatrixResolver.resolve(statusKey, isTanSeries);

    int state = matrix.getOrDefault(operation, StatusMatrixResolver.HIDDEN);
    if (state != StatusMatrixResolver.ENABLED) {
        throw new CzBusinessException(errorCodeForOperation(operation));
    }
}
```

---

## 7. CZ メッセージカタログ

### 7.1 成功メッセージ (CZ-000〜099)

| コード | メッセージ | 用途 |
|--------|-----------|------|
| CZ-000 | 成功完了 | 汎用成功 |
| CZ-001 | 登録終了 | データ登録成功 |
| CZ-002 | 登録済み | 重複登録通知 |
| CZ-003 | 保有済み | 既保有通知 |

### 7.2 警告メッセージ (CZ-100〜299)

| コード | メッセージ | トリガー |
|--------|-----------|---------|
| CZ-100 | 当地にデータが登録されています | 重複登録警告 |
| CZ-101 | 別ユーザーにより更新が行われたため操作不可 | 楽観的ロック競合 (409) |
| CZ-102 | サービス提供時間外（6:00〜23:30） | サービス時間外 (403) |
| CZ-103 | 確認・承認変更が生じています | 他ユーザー操作中 |
| CZ-104 | 保有登録・管理システムは緊急停止中 | 人事モード緊急停止 |
| CZ-105 | 保有業・管理システムは緊急停止中 | 管理モード緊急停止 |
| CZ-106 | ステータス不正のため追加・更新不可 | ステータスマトリクス違反 |
| CZ-107 | ステータス不正のため削除不可 | ステータスマトリクス違反 |
| CZ-108 | ステータス不正のためコピー不可 | ステータスマトリクス違反 |
| CZ-109 | ステータス不正のため確認不可 | ステータスマトリクス違反 |
| CZ-110 | ステータス不正のため戻す不可 | ステータスマトリクス違反 |
| CZ-120 | {0}は範囲外 | 範囲チェック |
| CZ-121 | {0}のマイナス値は不可 | 負数チェック |
| CZ-122 | {0}は整数値のみ入力可 | 整数チェック |
| CZ-125 | {0}はHH:MM形式で入力してください | 時間形式チェック |
| CZ-126 | {0}は必須入力です | 必須チェック |
| CZ-127 | {0}は数値で入力してください | 数値チェック |
| CZ-128 | {0}を超える入力があります | 上限チェック |
| CZ-129 | 工数は最小0:15以上で入力してください | 最小工数チェック |
| CZ-130 | {0}を超えています | 上限チェック |
| CZ-132 | 確認・承認状態相違で異カテゴリ不可 | カテゴリ重複 (VR-015) |
| CZ-136 | システム管理Noは9文字で入力してください | 文字数チェック |
| CZ-137 | 作業依頼書Noは7文字で入力してください | 文字数チェック |
| CZ-138 | TMR番号は5文字以内の半角数字で入力してください | TMR番号形式 (VR-011) |
| CZ-139 | 作業依頼者名は40文字以内で入力してください | 依頼者名文字数 (VR-013) |
| CZ-140 | 変更対象がありません | 未選択エラー |
| CZ-141 | {0}は業務概要としても定義されているため記述できません | 禁止ワード (VR-007) |
| CZ-142 | 業務要素選択時は識別Noまたは名称入力必須 | 条件付き必須 (VR-014) |
| CZ-143 | ステータス「作成中」以外は変更不可 | 編集制限 |
| CZ-144 | 作業日は対象月の範囲内で入力してください | 月外日付 (VR-002) |
| CZ-146 | 日次合計が24時間を超過しています | 日次上限 (VR-010) |
| CZ-147 | 工数は15分単位で入力してください | 15分単位 (BR-002) |

### 7.3 システムエラー (CZ-300〜499)

| コード | メッセージ | トリガー |
|--------|-----------|---------|
| CZ-300 | サーバー側で入力処理エラーが発生しました | サーバーエラー |
| CZ-301 | データベースエラーが発生しました | DB接続エラー |
| CZ-302 | データベース情報の取得に失敗しました | DB読取エラー |
| CZ-303 | データベースの更新に失敗しました | DB書込エラー |
| CZ-304 | トランザクションの開始に失敗しました | TX開始エラー |
| CZ-305 | トランザクションのコミットに失敗しました | TXコミットエラー |
| CZ-306 | トランザクションのロールバックに失敗しました | TXロールバックエラー |
| CZ-307 | セキュリティ情報を取得できませんでした | 認証エラー |
| CZ-308 | この操作を実行する権限がありません | 認可エラー（権限不足） |
| CZ-309 | ログイン処理でエラーが発生しました | ログインエラー |
| CZ-310 | データ表示処理でエラーが発生しました | 表示エラー |
| CZ-311 | データ更新処理でエラーが発生しました | 更新エラー |
| CZ-312 | データ削除処理でエラーが発生しました | 削除エラー |
| CZ-313 | ソート処理でエラーが発生しました | ソートエラー |
| CZ-314 | 画面切り替えでエラーが発生しました | 遷移エラー |
| CZ-315 | ダウンロードに失敗しました | DLエラー |
| CZ-317 | Excel出力に失敗しました | Excel生成エラー |
| CZ-321 | {0}の情報にエラーが発生しました | 汎用処理エラー |
| CZ-327 | Jsonデータを取得できませんでした | APIレスポンスエラー |
| CZ-328 | ステータスを{0}に変更できませんでした | ステータス遷移エラー |
| CZ-329 | ステータスを{0}に戻せませんでした | ステータス戻しエラー |
| CZ-330 | 別ステータス変更のためデータ編集不可 | コントロールテーブル不整合 |

### 7.4 確認ダイアログ (CZ-500〜799)

| コード | メッセージ | トリガー | 画面 |
|--------|-----------|---------|------|
| CZ-500 | システムを終了します。よろしいですか？ | ログアウト | 全画面 |
| CZ-505 | 「作成中」を全て「確認」に変更します。よろしいですか？ | 一括確認 | FORM_010 |
| CZ-506 | 選択したレコードを削除します。よろしいですか？ | レコード削除 | FORM_010 |
| CZ-507 | 選択レコードを「確認」に戻します。よろしいですか？ | 承認取消 | FORM_020 |
| CZ-508 | 選択レコードを「承認」に変更します。よろしいですか？ | レコード承認 | FORM_020 |
| CZ-509 | 「記入可能」に戻します。よろしいですか？ | 未確認戻し | FORM_020 |
| CZ-510 | 「登録確認」に変更します。よろしいですか？ | 月次確認 | FORM_020 |
| CZ-511 | 「データ承認」に変更します。よろしいですか？ | 月次集約 | FORM_020 |
| CZ-512 | 画面移動します。未保存データは失われます | 画面遷移警告 | 全画面 |
| CZ-514 | 「作成中」のステータスが存在します | 未完了レコード警告 | FORM_010 |
| CZ-516 | Excel出力します。時間がかかる場合があります | Excel出力確認 | 全画面 |
| CZ-518 | 「確認」を全て「作成中」に変更します。よろしいですか？ | 一括戻し | FORM_010 |

### 7.5 情報メッセージ (CZ-800〜999)

| コード | メッセージ | 用途 |
|--------|-----------|------|
| CZ-800 | 対象を確認に戻しました | 承認取消完了 |
| CZ-801 | 対象を承認しました | 承認完了 |
| CZ-802 | 入力可能に戻しました | 月次未確認完了 |
| CZ-803 | 確認しました | 月次確認完了 |
| CZ-804 | データ承認に変更しました | 月次集約完了 |

---

## 8. Backend 実装設計

### 8.1 ValidationService.java

```java
@Service
public class ValidationService {

    // VR-001〜015 の一括実行 (isInputCheck 相当)
    public List<ValidationError> validateWorkHours(WorkHoursEntity entity);

    // 個別バリデーション
    public Optional<ValidationError> validateWorkDate(String workDate, String yearMonth);
    public Optional<ValidationError> validateHours(String hours);
    public Optional<ValidationError> validateSubject(String subject, String hosyuSyubetu);
    public Optional<ValidationError> validateWorkRequestNo(String no, String category);
    public Optional<ValidationError> checkDailyTotal(String staffId, String workDate, int newMinutes, Long excludeId);
    public Optional<String> checkForbiddenWords(String subject, String hosyuSyubetu);

    // バイト長計算
    public int calculateByteLength(String str);
}
```

### 8.2 メッセージリソース管理

```yaml
# messages.yml (Backend)
cz:
  messages:
    CZ-000: "成功完了"
    CZ-001: "登録終了"
    CZ-101: "別ユーザーにより更新が行われたため操作できません"
    CZ-102: "サービス提供時間外（6:00〜23:30）のためデータ操作できません"
    CZ-125: "{0}はHH:MM形式で入力してください"
    CZ-126: "{0}は必須入力です"
    CZ-137: "作業依頼書Noは7文字で入力してください"
    CZ-138: "TMR番号は5文字以内の半角数字で入力してください"
    CZ-139: "作業依頼者名は40文字以内で入力してください"
    CZ-141: "{0}は業務概要としても定義されているため、記述できません"
    CZ-142: "業務要素選択時は識別Noまたは名称の入力が必須です"
    CZ-144: "作業日は対象月の範囲内で入力してください"
    CZ-146: "日次合計が24時間を超過しています"
    CZ-147: "工数は15分単位で入力してください"
    # ...
```

### 8.3 CzBusinessException

```java
public class CzBusinessException extends RuntimeException {
    private final String code;
    private final String[] params;
    private final String field;
    private final Long recordId;

    // HTTP ステータスコードマッピング
    public int httpStatus() {
        if ("CZ-102".equals(code)) return 403;   // サービス時間外
        return switch (code.substring(0, 4)) {
            case "CZ-3" -> 500;                    // CZ-300〜330: システムエラー
            default -> 400;                        // CZ-1xx: バリデーション警告
        };
    }
}
```

### 8.4 GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CzBusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(CzBusinessException e) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", e.getCode());
        error.put("message", resolveMessage(e.getCode(), e.getParams()));
        if (e.getField() != null) error.put("field", e.getField());
        if (e.getRecordId() != null) error.put("recordId", e.getRecordId());
        return ResponseEntity.status(e.httpStatus()).body(Map.of("error", error));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(OptimisticLockException e) {
        // CZ-101: 楽観的ロック競合
        return ResponseEntity.status(409).body(Map.of("error", Map.of(
            "code", "CZ-101",
            "message", "別ユーザーにより更新が行われたため操作できません"
        )));
    }
}
```

---

## 9. Frontend 実装設計

### 9.1 バリデーション composable

```typescript
// composables/useValidation.ts
export function useValidation() {
  return {
    // 個別フィールド
    validateRequired(value: any, fieldName: string): ValidationResult
    validateHoursFormat(value: string): ValidationResult
    validate15MinUnit(value: string): ValidationResult
    validateByteLength(value: string, max: number, fieldName: string): ValidationResult
    validateDateInMonth(date: string, yearMonth: string): ValidationResult
    validateFixedLength(value: string, length: number, fieldName: string): ValidationResult
    validateCharType(value: string, mode: string, fieldName: string): ValidationResult

    // 一括（isInputCheck 相当）
    validateWorkHoursRecord(record: WorkHoursRecord): ValidationResult[]
  }
}

interface ValidationResult {
  valid: boolean
  code?: string       // "CZ-126"
  message?: string    // "作業日は必須入力です"
  field?: string
}
```

### 9.2 メッセージカタログ (Frontend)

```typescript
// constants/messages.ts
export const CZ_MESSAGES: Record<string, string> = {
  'CZ-000': '成功完了',
  'CZ-001': '登録終了',
  'CZ-101': '別ユーザーにより更新が行われたため操作できません',
  'CZ-102': 'サービス提供時間外（6:00〜23:30）のためデータ操作できません',
  'CZ-125': '{0}はHH:MM形式で入力してください',
  'CZ-126': '{0}は必須入力です',
  // ...
}

export function resolveMessage(code: string, params?: string[]): string {
  let msg = CZ_MESSAGES[code] || code
  if (params) {
    params.forEach((p, i) => {
      msg = msg.replace(`{${i}}`, p)
    })
  }
  return msg
}
```

### 9.3 エラー表示パターン

| パターン | 表示方法 | 用途 |
|---------|---------|------|
| セルエラー | セル赤枠 + ツールチップ | インライン編集のフィールドバリデーション |
| 行エラー | 行ハイライト + セルフォーカス | 一括確認時の recordId 指定エラー |
| Toast | PrimeVue Toast (右上) | 操作結果通知、サーバーエラー |
| オーバーレイ | 画面全体マスク | サービス時間外 (CZ-102) |
| ConfirmDialog | モーダル | CZ-500〜799 確認ダイアログ |

---

## 10. テスト要件

### 10.1 Backend バリデーションテスト

| テスト | 内容 |
|--------|------|
| VR-001〜015 | 各ルールの正常値・境界値・異常値 |
| 禁止ワード | 12語すべてのヒット/非ヒット |
| バイト長 | 全角/半角/半角カタカナ混在文字列 |
| 日次合計 | 23:45 + 0:30 = 24:15 → CZ-146 |
| ステータスマトリクス | 12状態 × 2系列 × 7操作 = 168パターン |
| サービス時間 | 5:59, 6:00, 23:30, 23:31 境界値 |
| 楽観的ロック | 同時更新 → CZ-101 |

### 10.2 Frontend バリデーションテスト

| テスト | 内容 |
|--------|------|
| HH:MM パース | 全変換パターン (空, 1桁, 2桁, 3桁, 4桁, コロン付き) |
| 15分単位 | 00/15/30/45 → OK, 01〜14/16〜29/31〜44/46〜59 → NG |
| バイト長計算 | "あいう" → 6, "abc" → 3, "ｱｲｳ" → 6 |
| 必須チェック | 空文字列, null, undefined, 空白のみ |
| 日付範囲 | 2025-02-01〜2025-02-28 の境界値 |
| メッセージ解決 | パラメータ展開 ("{0}は必須" + ["作業日"] → "作業日は必須") |

### 10.3 E2E テスト

| シナリオ | 内容 |
|---------|------|
| 必須エラー | 空の件名で確認 → セル赤枠 + CZ-126 ツールチップ |
| 15分単位エラー | "03:10" 入力 → CZ-147 表示 |
| 禁止ワード | 件名に "カ層" 含む → CZ-141 表示 |
| 日次24h超過 | 合計 24:15 → CZ-146 表示 |
| 楽観的ロック | 2ブラウザ同時編集 → 409 → CZ-101 Toast |
| サービス時間外 | 23:31 に POST → CZ-102 オーバーレイ |
| 一括確認エラー | 不完全レコード → 該当行スクロール + フォーカス |

### 受け入れ基準（Given-When-Then）

**AC-VE-01: フロントエンド Layer 1 バリデーション**
- Given: 件名が空の工数レコードを編集中
- When: 保存/確定操作をトリガーする
- Then: CZ-126 エラーがセル赤枠 + ツールチップで表示され、API コールは発行されない

**AC-VE-02: バックエンド Layer 2 バリデーション**
- Given: 日次合計が 24:00 を超えるレコード
- When: API が保存リクエストを受信する
- Then: 400 Bad Request、`{ error: { code: "CZ-146", message: "...", field: "hours" } }` が返される

**AC-VE-03: サービス時間制限**
- Given: 現在時刻が 23:31 JST（サービス時間外）
- When: 任意の API エンドポイントに POST リクエストを送信する
- Then: 403 Forbidden、CZ-102 エラーが返され、フロントエンドでフルスクリーンオーバーレイが表示される

**AC-VE-04: 楽観的ロック競合**
- Given: ユーザー A とユーザー B が同一レコードを取得済み
- When: ユーザー A が保存後、ユーザー B が保存する
- Then: ユーザー B に 409 Conflict、CZ-101 が返され、Toast 通知 + データリロードが実行される

**AC-VE-05: 禁止語句検出**
- Given: hosyuSyubetu=0 のレコードで件名に「カ層」を入力
- When: レコードを送信する
- Then: CZ-141 エラーが返され、params にマッチした禁止語「カ層」が含まれる

**AC-VE-06: 一括確定エラーハンドリング**
- Given: 複数レコードのうち recordId=12345 の workDate が空
- When: 一括確定を実行する
- Then: 最初のエラーで処理停止、CZ-126 が recordId: 12345 付きで返され、該当行がハイライト+スクロールされる

**AC-VE-07: 確認ダイアログ（CZ-500〜799）**
- Given: 一括確定操作を開始
- When: CZ-505 コードのメッセージが発生する
- Then: ConfirmDialog が表示され、「はい」で処理続行、「いいえ」で処理キャンセルとなる

**AC-VE-08: エラーレスポンス構造（単一）**
- Given: API がバリデーションエラーを検出
- When: エラーレスポンスを返す
- Then: `{ error: { code: "CZ-xxx", message: "...", field: "fieldName", params: {} } }` の構造で返される

**AC-VE-09: エラーレスポンス構造（複数）**
- Given: API が複数のバリデーションエラーを検出
- When: エラーレスポンスを返す
- Then: `{ errors: [{ code: "CZ-xxx", message: "...", field: "...", recordId: "..." }, ...] }` の配列構造で返される

**AC-VE-10: 15分単位バリデーション**
- Given: 工数入力で "3:20" を入力
- When: フォーカスアウトする
- Then: CZ-147（15分単位エラー）が表示される。"3:15" や "3:30" は正常受付される

**AC-VE-11: バイト長バリデーション（境界値）**
- Given: 件名フィールドに全角文字を入力中
- When: ちょうど 128 バイト（全角64文字）を入力する
- Then: 正常受付される。129 バイト目を入力すると CZ-128 エラーが表示される

**AC-VE-12: CZ エラーコード → HTTP ステータスマッピング**
- Given: CzBusinessException が CZ-1xx コードで throw される
- When: GlobalExceptionHandler が例外をキャッチする
- Then: CZ-1xx → 400、CZ-102 → 403、CZ-3xx → 500、楽観的ロック → 409 にマッピングされる

# 認証基盤: ALB + Okta OIDC 認証 & 4層権限モデル

## 概要

既存 CZ（保有資源管理システム）の認証・権限基盤を
ALB + Okta OIDC ベースの JWT 認証に移行する。
4層権限モデル（JinjiMode / ビット機能権限 / 相対データアクセス / 雇用形態）を
完全再現し、開発環境では Express ベースの認証モックサーバーで
15アクター × 2モード = 30パターンを切替可能にする。

**対象分析ドキュメント**:
- `analysis/02_actor_definition.md` — 4層権限モデル、15アクター定義
- `analysis/06_devenv_infrastructure.md` — 認証モック設計、三重安全策

**移行元ソースコード根拠**:
- `SecurityRoleInfo.java` — ビットベース権限判定 (`isAvailableFunction`)
- `InitSecurityRoleProc.java` — ログインフロー、ロール初期化
- `CurrentConditionInfo.java` — セッション情報（組織・担当者・代行モード）
- `BaseSecurity.java` — 相対権限による組織解決
- `MprTemporaryStaffInfo.java` — 雇用形態判定（カテゴリ 900/901/902）
- `web.xml` — JinjiMode コンテキストパラメータ
- `dummySecurity.xml` — テストユーザー定義（ロールデータ構造）

---

## 仕様分類サマリー

| 分類 | 対象 | 備考 |
|------|------|------|
| 既存踏襲 | 4層権限モデル（JinjiMode / ビット権限 / 相対権限 / 雇用形態） | SecurityRoleInfo.java 等から抽出 |
| 既存踏襲 | 12状態ステータスマトリクス（担当者/管理者系列） | isSts_tan_*/isSts_man_* から再現 |
| 既存踏襲 | 15アクター定義 | dummySecurity.xml から抽出 |
| 既存踏襲 | 代行モード（外部契約者） | MprTemporaryStaffInfo から抽出 |
| 既存踏襲 | 時間制御ロール 940/941 | ロールカテゴリ 900 系から抽出 |
| 変更 | JinjiMode 判定方式: context-param → JWT クレーム | SPA 化に伴う変更 |
| 変更 | ビット文字列 → JSON オブジェクト化 | 可読性向上のため |
| 変更 | セッション管理: HttpSession → JWT (Stateless) | ALB+Okta OIDC 移行 |
| 新規追加 | CzClaimsMapper（Okta 属性 → CZ クレーム変換） | Okta 連携のため |
| 新規追加 | Auth Mock サーバー（Express） | 開発環境支援 |
| 新規追加 | 三重安全策（本番混入防止） | Production Safety 原則 |
| 廃止 | web.xml context-param による JinjiMode 切替 | JWT クレーム化に伴い不要 |
| P2先送り | ロール 937（局跨がり組織選択） | 現時点では未実装 |
| P2先送り | ロール 951（局跨がり役職管理） | 現時点では未実装 |

---

## 1. 認証フロー

### 1.1 本番環境（ALB + Okta OIDC）

```
ブラウザ → ALB (OIDC Action) → Okta IdP → 認証
  → ALB → リクエストヘッダーに JWT 付与
  → Frontend (Nuxt.js) → Authorization: Bearer JWT
  → Backend (Spring Boot) → JWT 検証 + 4層権限解決
```

**ALB が付与するヘッダー:**

| ヘッダー | 内容 |
|---------|------|
| `X-Amzn-Oidc-Data` | JWT (base64url encoded) — ユーザー情報 + 権限クレーム |
| `X-Amzn-Oidc-Identity` | ユーザー ID（メールアドレス） |
| `X-Amzn-Oidc-Accesstoken` | Okta アクセストークン |

### 1.2 開発環境（Auth Mock）

```
開発者 → Auth Mock (:8180) → アクター選択 → JWT 発行
  → Frontend (:3000) → Authorization: Bearer JWT
  → Backend (:8080) → JWT 検証 + 4層権限解決（本番と同一ロジック）
```

**設計原則**: Backend の JWT 検証・権限解決ロジックは本番/開発で**完全に同一**。
差異は JWT の発行元（ALB vs Auth Mock）と署名アルゴリズムのみ。

**署名検証方式**:
- 本番: ALB が RS256（JWKS エンドポイント経由の公開鍵）で署名
- 開発: Auth Mock が HS256（共有鍵）で署名
- `AlbOidcAuthFilter` は `spring.security.oauth2.resourceserver.jwt.issuer-uri` を参照して署名検証方式を自動解決。`application.yml` のプロファイル分離（`application-dev.yml` / `application-prod.yml`）で対応

---

## 2. 4層権限モデル

### 2.1 モデル概要

```
Layer 1: アプリケーションモード（JinjiMode）    ← JWT クレーム jinjiMode で決定
Layer 2: 機能権限（ビットベース TAB 010/011/012） ← JWT クレーム permissions で決定
Layer 3: データアクセス権限（相対権限 201/202/211）← JWT クレーム dataAuthority で決定
Layer 4: 雇用形態（正社員/臨時職員/外部契約者）    ← JWT クレーム employmentType で決定
```

### 2.2 Layer 1: アプリケーションモード（JinjiMode）

**移行元**: `web.xml` の `<context-param name="JinjiMode">` → 新システムでは JWT クレームで判定

| モード | 旧URL | JWT クレーム | 説明 |
|--------|-------|-------------|------|
| 人事モード (Ent) | `/czEnt/` | `jinjiMode: true` | 保有資源報告登録システム (CZAPP002) |
| 管理モード (Mgr) | `/czMgr/` | `jinjiMode: false` | 保有資源管理システム (CZAPP001) |

**影響する UI/ロジック:**

| 影響箇所 | 人事モード (true) | 管理モード (false) |
|----------|:---:|:---:|
| ヘッダータイトル | 「保有資源報告登録」 | 「保有資源管理」 |
| メニュー構成 | 報告系メニューのみ | 全メニュー |
| コントロールテーブル参照 | M1 (SYS_01) | M0 (SYS_00) |
| 「戻る」ボタン | 非表示 | 表示 |
| ヘルプURL | help_url_ent | help_url_mgr |
| システムNo選択ダイアログ | 人事モード動作 | 管理モード動作 |

### 2.3 Layer 2: 機能権限（ビットベース制御）

**移行元**: `SecurityRoleInfo.isAvailableFunction(category, funcCode)`
→ ロールデータ文字列の指定位置が `'1'` かを判定

**新システム**: JWT クレーム `permissions` オブジェクト内にビット値を格納

#### TAB 010: 保守工数機能グループ（3ビット）

| ビット | 旧メソッド | 新クレームキー | 権限名 | 説明 |
|-------|-----------|-------------|--------|------|
| 0 | `canUseSbt010_0bit()` | `permissions.tab010.bit0` | 報告書作成担当グループ | 工数の入力・報告 |
| 1 | `canUseSbt010_1bit()` | `permissions.tab010.bit1` | 報告書管理担当グループ | 工数の入力、未確認/未集計の管理 |
| 2 | `canUseSbt010_2bit()` | `permissions.tab010.bit2` | 全管理グループ | 全データのフルアクセス |

**TAB 010 bit2 の UI 制御への影響:**
- `bit2 = true` → 担当者系列 (`isSts_tan_*`) のステータスマトリクスを使用
- `bit2 = false` → 管理者系列 (`isSts_man_*`) のステータスマトリクスを使用

#### TAB 011: 拡張機能グループ（2ビット）

| ビット | 旧メソッド | 新クレームキー | 権限名 | 説明 |
|-------|-----------|-------------|--------|------|
| 0 | `canUseSbt011_0bit()` | `permissions.tab011.bit0` | 保守H時間出力 | 保守H時間出力機能の利用可否 |
| 1 | `canUseSbt011_1bit()` | `permissions.tab011.bit1` | 画面遷移リンク | FORM_010↔FORM_020 間の遷移リンク表示 |

#### TAB 012: 状況管理グループ（2ビット）

| ビット | 旧メソッド | 新クレームキー | 権限名 | 説明 |
|-------|-----------|-------------|--------|------|
| 0 | `canUseSbt012_0bit()` | `permissions.tab012.bit0` | 期間入力条件 | 状況一覧での期間条件入力の可否 |
| 1 | `canUseSbt012_1bit()` | `permissions.tab012.bit1` | 期間集計 | 状況一覧での期間集計実行の可否 |

### 2.4 Layer 3: データアクセス権限（相対権限モデル）

**移行元**: `SecurityRoleInfo.getRelativeAuthority()` → 組織階層に基づくデータスコープ

| ロールコード | 新クレームキー | 権限名 | 操作 |
|------------|-------------|--------|------|
| 201 | `dataAuthority.ref` | 相対権限（参照） | データ参照のみ |
| 202 | `dataAuthority.ins` | 相対権限（登録） | データ登録が可能 |
| 211 | `dataAuthority.upd` | 相対権限（更新） | データ更新が可能 |

**組織階層レベル:**

| レベル | コード | 組織 | アクセス可能範囲 |
|--------|-------|------|----------------|
| 全社 | `ZENSYA` (9999999/255) | 全社 | 全組織のデータ |
| 営業所 | `EIGYOSHO` (127) | 営業所 | 営業所配下のデータ |
| 本部 | `HONBU` (63) | 本部 | 本部配下のデータ |
| 局 | `KYOKU` (31) | 局 | 局配下のデータ |
| 室 | `SHITSU` (15) | 室 | 室配下のデータ |
| 部 | `BU` (7) | 部 | 部配下のデータ |
| 課 | `KA` (3) | 課 | 課配下のデータ |

**データスコープ制御フロー:**
```
リクエスト → JWT から dataAuthority 抽出
  → ユーザー所属組織 + 相対権限レベルから許可組織コードリスト取得
  → SQL WHERE 句に組織コードフィルタ適用
  → ユーザーに見える範囲のデータのみ返却
```

### 2.5 Layer 4: 雇用形態による制御

**移行元**: `MprTemporaryStaffInfo.getTemporaryStaffType()`
→ カテゴリ 900/901/902 のロール有無で判定

| 型 | コード | 新クレームキー | 説明 | 制約 |
|----|-------|-------------|------|------|
| 正社員 | `0` (TYPE_OFFICIAL) | `employmentType: 0` | 全機能アクセス可 | なし |
| 臨時職員1 | `1` (TYPE_TEMPORARY_1) | `employmentType: 1` | 制限付きアクセス | カテゴリ 900 |
| 臨時職員2 | `2` (TYPE_TEMPORARY_2) | `employmentType: 2` | 制限付きアクセス | カテゴリ 901 |
| 外部契約者 | `3` (TYPE_SUBCONTRACT) | `employmentType: 3` | 最も制限されたアクセス | カテゴリ 902、代行モード特殊処理 |

**雇用形態別の具体的制約:**

| 制約項目 | TYPE_0 正社員 | TYPE_1 臨時1 | TYPE_2 臨時2 | TYPE_3 外部契約者 |
|---|:---:|:---:|:---:|:---:|
| 工数入力（自分のレコード） | ✅ | ✅ | ✅ | ✅（代行時は代行元ID） |
| 工数入力（他者のレコード） | ✅ | ❌ | ❌ | ❌ |
| ステータス確認・確定操作 | ✅ | ❌ | ❌ | ❌ |
| 分析画面の閲覧 | ✅ | ✅（自組織のみ） | ✅（自組織のみ） | ❌ |
| Excel 出力 | ✅ | ✅（自データのみ） | ✅（自データのみ） | ❌ |
| 代行モード利用 | N/A | N/A | N/A | ✅（canDelegate=true 時） |

> TYPE_1/TYPE_2 の「制限付きアクセス」は主に dataAuthority が `KA`（課）レベルに限定されることによる。
> 機能権限（TAB 010）は bit0 のみ付与されるため、管理操作（bit1/bit2）は不可。

**外部契約者の代行モード:**
```
外部契約者(TYPE_SUBCONTRACT) が代行モード(isDaiko=true) の場合:
  → 登録者IDは代行元の正社員IDを使用
  → isAllowedStaff(tantoCode) で代行可否を検証
  → リクエストヘッダー X-Delegation-Staff-Id に代行先の正社員IDを設定
  → Backend は CzPrincipal.delegationStaffId として保持
```

---

## 3. アクター定義（15名）

### 3.1 アクター一覧

| ID | アクター名 | Layer 1 | Layer 2 (TAB 010) | Layer 3 | Layer 4 |
|----|-----------|---------|-------------------|---------|---------|
| ACT-01 | 報告担当者 | Ent (true) | bit0=1 | 202 (登録) | TYPE=0 (正社員) |
| ACT-02 | 報告管理者 | Mgr (false) | bit1=1 | 211 (更新) | TYPE=0 (正社員) |
| ACT-03 | 全権管理者 | Mgr (false) | bit2=1 | 211 (更新) | TYPE=0 (正社員) |
| ACT-04 | 管理モードユーザー | Mgr (false) | bit0=1 | 202 (登録) | TYPE=0 (正社員) |
| ACT-05 | 人事モードユーザー | Ent (true) | bit0=1 | 201 (参照) | TYPE=0 (正社員) |
| ACT-06 | 正社員 | Ent (true) | bit0=1 | 202 (登録) | TYPE=0 (正社員) |
| ACT-07 | 臨時職員1 | Ent (true) | bit0=1 | 201 (参照) | TYPE=1 |
| ACT-08 | 臨時職員2 | Ent (true) | bit0=1 | 201 (参照) | TYPE=2 |
| ACT-09 | 外部契約者 | Ent (true) | bit0=1 | 201 (参照) | TYPE=3 |
| ACT-10 | 全社スタッフ | Mgr (false) | bit2=1 | ZENSYA (全社) | TYPE=0 (正社員) |
| ACT-11 | 事業スタッフ | Mgr (false) | bit1=1 | HONBU (本部) | TYPE=0 (正社員) |
| ACT-12 | 本社スタッフ | Mgr (false) | bit1=1 | HONBU (本部) | TYPE=0 (正社員) |
| ACT-13 | 局スタッフ | Mgr (false) | bit1=1 | KYOKU (局) | TYPE=0 (正社員) |
| ACT-14 | 局スタッフ（総務管理者） | Mgr (false) | bit2=1 | KYOKU (局) | TYPE=0 (正社員) |
| ACT-15 | 局スタッフ（営業） | Mgr (false) | bit1=1 | KYOKU (局) | TYPE=0 (正社員) |

### 3.2 追加属性

各アクターは以下の追加属性を持つ:

| 属性 | JWT クレームキー | 説明 |
|------|---------------|------|
| ユーザーID | `sub` | 一意識別子 |
| 氏名 | `name` | 表示名 |
| 所属組織コード | `organizationCode` | 所属する組織コード |
| 所属組織名 | `organizationName` | 所属する組織名（表示用） |
| スタッフ種別 | `staffRole` | 931-936（該当する場合） |
| TAB 011 | `permissions.tab011` | 拡張機能権限 |
| TAB 012 | `permissions.tab012` | 状況管理権限 |
| 代行可否 | `canDelegate` | true/false（外部契約者の代行モード） |

---

## 4. JWT ペイロード設計

### 4.1 クレーム構造

```json
{
  "sub": "d10623",
  "name": "田中太郎",
  "email": "tanaka@example.com",
  "iss": "https://mock-okta.example.com",
  "iat": 1709000000,
  "exp": 1709086400,          // ALB が Okta OIDC セッション（TTL: 8時間）を管理。JWT はリクエストごとに ALB が再署名するため、フロントエンドでのリフレッシュ不要

  "jinjiMode": false,
  "employmentType": 0,

  "organizationCode": "100200",
  "organizationName": "IT推進部 開発1課",

  "permissions": {
    "tab010": { "bit0": true, "bit1": false, "bit2": true },
    "tab011": { "bit0": true, "bit1": true },
    "tab012": { "bit0": true, "bit1": true }
  },

  "dataAuthority": {
    "ref": "HONBU",
    "ins": "KYOKU",
    "upd": "KYOKU"
  },

  "staffRole": null,
  "canDelegate": false
}
```

### 4.2 本番環境での JWT マッピング

Okta のカスタムクレームから CZ 権限モデルへのマッピングは
Backend の `CzClaimsMapper` で行う。Okta のユーザープロファイルに
保存された属性（groups, custom claims）を4層権限モデルに変換する。

**Okta 属性 → CZ クレーム マッピング表:**

| Okta 属性 | 型 | CZ クレーム | 変換ロジック |
|---|---|---|---|
| `custom:jinjiMode` | string (`"true"`/`"false"`) | `jinjiMode` (boolean) | 文字列→boolean 変換 |
| `custom:tab010` | string (`"110000"`) | `permissions.tab010` | 6文字ビット文字列→`{bit0,bit1,bit2}` オブジェクト変換。bit3〜bit5 は予約枠（常に '0'）、アプリケーションは bit0〜bit2 のみ参照する |
| `custom:tab011` | string (`"11"`) | `permissions.tab011` | 2文字ビット文字列→`{bit0,bit1}` オブジェクト変換 |
| `custom:tab012` | string (`"10"`) | `permissions.tab012` | 2文字ビット文字列→`{bit0,bit1}` オブジェクト変換 |
| `custom:dataAuthRef` | string (`"HONBU"`) | `dataAuthority.ref` | そのまま転記 |
| `custom:dataAuthIns` | string (`"KYOKU"` or `""`) | `dataAuthority.ins` | 空文字→`null` 変換 |
| `custom:dataAuthUpd` | string (`"KYOKU"` or `""`) | `dataAuthority.upd` | 空文字→`null` 変換 |
| `custom:employmentType` | string (`"0"`-`"3"`) | `employmentType` (int) | 文字列→整数変換 |
| `custom:staffRole` | string (`"931"`-`"936"` or `""`) | `staffRole` (int or null) | 空文字→`null` 変換 |
| `custom:canDelegate` | string (`"true"`/`"false"`) | `canDelegate` (boolean) | 文字列→boolean 変換 |
| `organizationCode` (標準属性) | string | `organizationCode` | そのまま転記 |
| `organizationName` (標準属性) | string | `organizationName` | そのまま転記 |

> **設計仕様値**: 上表の `custom:*` 属性名は本プロジェクトの設計仕様値として確定する。
> Okta テナント設定時に属性名が異なる場合は、Backend の `CzClaimsMapper` のマッピング定義のみを変更すれば吸収できる（他コンポーネントへの影響なし）。
> 開発環境では Auth Mock がこれらの属性名を直接使用するため、Okta 確定を待たずに開発・テスト可能。

---

## 5. ステータス × 権限ボタン制御マトリクス

### 5.1 ステータスキー定義（12状態）

```
sts_base_key = "000,010,011,100,110,111,200,210,211,900,910,911"
```

| キー | sts (状態) | ins (月次確定) | sum (集計) | 意味 |
|------|-----------|-------------|----------|------|
| 000 | 0:作成中 | 0:未確定 | 0:未集計 | 作成中・未確定・未集計 |
| 010 | 0:作成中 | 1:確定 | 0:未集計 | 作成中・確定済・未集計 |
| 011 | 0:作成中 | 1:確定 | 1:集計済 | 作成中・確定済・集計済 |
| 100 | 1:確認 | 0:未確定 | 0:未集計 | 確認済・未確定・未集計 |
| 110 | 1:確認 | 1:確定 | 0:未集計 | 確認済・確定済・未集計 |
| 111 | 1:確認 | 1:確定 | 1:集計済 | 確認済・確定済・集計済 |
| 200 | 2:確定 | 0:未確定 | 0:未集計 | 確定済・未確定・未集計 |
| 210 | 2:確定 | 1:確定 | 0:未集計 | 確定済・確定済・未集計 |
| 211 | 2:確定 | 1:確定 | 1:集計済 | 確定済・確定済・集計済 |
| 900 | 9:非表示 | 0:未確定 | 0:未集計 | 非表示状態 |
| 910 | 9:非表示 | 1:確定 | 0:未集計 | 非表示・確定済 |
| 911 | 9:非表示 | 1:確定 | 1:集計済 | 非表示・確定・集計済 |

### 5.2 担当者系列（`tab010.bit2 = true` 時）

値: **1**=有効, **0**=無効, **9**=非表示

| 操作 | 000 | 010 | 011 | 100 | 110 | 111 | 200 | 210 | 211 | 900 | 910 | 911 |
|------|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|
| 追加 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | **1** | 0 | 0 |
| コピー | **1** | 0 | 0 | **1** | 0 | 0 | **1** | 0 | 0 | 9 | 9 | 9 |
| 削除 | **1** | 0 | 0 | **1** | 0 | 0 | 0 | 0 | 0 | 9 | 9 | 9 |
| 更新 | **1** | 0 | 0 | **1** | 0 | 0 | 9 | 9 | 9 | 9 | 9 | 9 |
| 参照 | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |

### 5.3 管理者系列（`tab010.bit2 = false` 時）

| 操作 | 000 | 010 | 011 | 100 | 110 | 111 | 200 | 210 | 211 | 900 | 910 | 911 |
|------|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|
| 追加 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | **1** | **1** | **1** |
| コピー | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| 削除 | **1** | **1** | **1** | **1** | **1** | **1** | 0 | 0 | 0 | 9 | 9 | 9 |
| 更新 | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| 参照 | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| 状況更新 | 0 | 0 | 0 | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| 状況参照 | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |

### 5.4 マトリクスの読み方

**担当者（tan）系列の特徴:**
- 月次確定(ins=1)または集計済(sum=1)になると、コピー・削除・更新が**全て無効**
- 確定(sts=2)になると、削除・更新が**無効**
- 追加は非表示状態(sts=9)でのみ有効（新規登録画面専用）

**管理者（man）系列の特徴:**
- 月次確定・集計済でも、コピー・更新・参照は**全て有効のまま**
- 確定(sts=2)でも削除以外は操作可能
- 状況更新(man_j_upd)は確認(sts=1)以降のみ有効（ステータスを進める操作）

---

## 6. Backend 実装設計

### 6.1 パッケージ構成

```
com.example.czConsv.security/
├── filter/
│   └── AlbOidcAuthFilter.java       ← JWT解析・認証フィルター
├── model/
│   ├── CzPrincipal.java             ← 認証済みユーザー情報
│   ├── CzPermissions.java           ← 4層権限モデル
│   ├── TabPermission.java           ← TAB 010/011/012 ビット権限
│   ├── DataAuthority.java           ← 相対データアクセス権限
│   └── EmploymentType.java          ← 雇用形態 enum
├── service/
│   ├── CzSecurityContext.java        ← ThreadLocal による認証コンテキスト
│   └── OrganizationScopeResolver.java ← 組織階層によるデータスコープ解決
├── config/
│   └── SecurityConfig.java           ← Spring Security 設定
└── util/
    └── StatusMatrixResolver.java     ← 12状態ステータスマトリクス解決
```

### 6.1.1 CzPermissions セマンティックエイリアス

JWT クレームの `bitN` はトレーサビリティのため旧システムの命名を維持する。
コード上では `CzPermissions` がセマンティックな名前を提供し、
開発者が bit 番号を意識せずに権限判定できるようにする。

**Backend (Java)**:
```java
public class CzPermissions {
    private final TabPermission tab010;
    private final TabPermission tab011;
    private final TabPermission tab012;

    // --- TAB 010: 保守工数機能グループ ---
    /** bit0: 報告書作成担当グループ — 工数の入力・報告 */
    public boolean canReport()     { return tab010.bit0(); }
    /** bit1: 報告書管理担当グループ — 工数の入力、未確認/未集計の管理 */
    public boolean canManage()     { return tab010.bit1(); }
    /** bit2: 全管理グループ — 全データのフルアクセス */
    public boolean canFullAccess() { return tab010.bit2(); }
    /** bit2 → true: 12状態マトリクスの担当者系列(000-009)を使用, false: 管理者系列(900-911)を使用。
     *  全権管理者(bit2=true)は管理画面アクセスは持つが、ステータス操作は担当者系列で行う（レガシー踏襲） */
    public boolean useTanSeries()  { return tab010.bit2(); }

    // --- TAB 011: 拡張機能グループ ---
    /** bit0: 保守H時間出力機能の利用可否 */
    public boolean canExportHours()      { return tab011.bit0(); }
    /** bit1: FORM_010↔FORM_020 間の遷移リンク表示 */
    public boolean canNavigateForms()    { return tab011.bit1(); }

    // --- TAB 012: 状況管理グループ ---
    /** bit0: 状況一覧での期間条件入力の可否 */
    public boolean canInputPeriod()      { return tab012.bit0(); }
    /** bit1: 状況一覧での期間集計実行の可否 */
    public boolean canAggregate()        { return tab012.bit1(); }
}
```

**Frontend (TypeScript)**:
```typescript
// types/permissions.ts
interface CzPermissions {
  tab010: { bit0: boolean; bit1: boolean; bit2: boolean }
  tab011: { bit0: boolean; bit1: boolean }
  tab012: { bit0: boolean; bit1: boolean }

  // セマンティックエイリアス（computed で提供）
  readonly canReport: boolean       // tab010.bit0
  readonly canManage: boolean       // tab010.bit1
  readonly canFullAccess: boolean   // tab010.bit2
  readonly useTanSeries: boolean    // tab010.bit2
  readonly canExportHours: boolean  // tab011.bit0
  readonly canNavigateForms: boolean // tab011.bit1
  readonly canInputPeriod: boolean  // tab012.bit0
  readonly canAggregate: boolean    // tab012.bit1
}
```

**bitN ↔ セマンティック名 対応表**:

> **tab010 ビットの排他性**: 現行運用では tab010 は排他的に 1 ビットのみ設定されるが、データ構造はビットフラグとして維持する（将来の組み合わせ拡張に対応）。複数ビット同時設定時は上位ビット優先で判定する（bit2 > bit1 > bit0）。

| JWT クレーム | エイリアス | 権限名 | 用途 |
|---|---|---|---|
| `tab010.bit0` | `canReport` | 報告書作成担当 | 工数の入力・報告 |
| `tab010.bit1` | `canManage` | 報告書管理担当 | 未確認/未集計の管理 |
| `tab010.bit2` | `canFullAccess` / `useTanSeries` | 全管理 | フルアクセス / ステータスマトリクス系列選択 |
| `tab011.bit0` | `canExportHours` | 保守H時間出力 | Excel 出力機能 |
| `tab011.bit1` | `canNavigateForms` | 画面遷移リンク | FORM_010↔FORM_020 遷移 |
| `tab012.bit0` | `canInputPeriod` | 期間入力条件 | 状況一覧の期間条件入力 |
| `tab012.bit1` | `canAggregate` | 期間集計 | 状況一覧の期間集計実行 |

### 6.2 AlbOidcAuthFilter

```
リクエスト受信
  → X-Amzn-Oidc-Data ヘッダーから JWT 取得
  → JWT デコード・検証（iss, exp）
  → クレームから CzPrincipal 構築:
    - sub → userId
    - jinjiMode → JinjiMode
    - permissions → TabPermission (010/011/012)
    - dataAuthority → DataAuthority (201/202/211)
    - employmentType → EmploymentType enum
    - organizationCode → 所属組織
    - staffRole → スタッフ種別
  → CzSecurityContext に格納（ThreadLocal）
  → SecurityContextHolder に Authentication 設定
```

**代行モードの処理**:
```
リクエストに X-Delegation-Staff-Id ヘッダーがある場合:
  → CzPrincipal.employmentType が TYPE_SUBCONTRACT(3) であることを検証
  → CzPrincipal.canDelegate が true であることを検証
  → 代行先スタッフID の存在・代行可否を isAllowedStaff() で検証
    （mcz21_kanri_taisyo テーブルで kanritnt_esqid=現行ユーザー かつ kanritsy_esqid=tantoCode のレコード存在を確認）
  → 検証 OK → CzPrincipal.delegationStaffId に設定
  → 検証 NG → CZ-307 エラー（権限不足）
  → 以降のサービス層で登録者ID として delegationStaffId を使用
```

> spec #3 (core-api-design) で定義された `X-Delegation-Staff-Id` ヘッダーとの連携。
> 代行モードの UI は spec #7 (common-components) の StaffSearchDialog (purpose='delegation') で実装。

**開発環境の差異**:
- `DEV_AUTH_ENABLED=true` かつ `X-Dev-User-Id` ヘッダーがある場合、
  Auth Mock が発行した JWT と同等のクレームを構築
- `@Profile("dev")` + 環境変数ゲートの2層防御

### 6.3 OrganizationScopeResolver

```java
/**
 * 相対権限レベルとユーザー所属組織から、
 * アクセス可能な組織コードリストを解決する。
 *
 * @param authorityLevel 相対権限レベル (ZENSYA, HONBU, KYOKU, etc.)
 * @param userOrgCode    ユーザー所属組織コード
 * @return 許可組織コードリスト（SQL WHERE IN 句に使用）
 */
List<String> resolve(String authorityLevel, String userOrgCode);
```

**解決ロジック（セクション 2.4 の組織階層レベルに準拠）:**
- `ZENSYA` → `null`（フィルタなし = 全データアクセス）
- `EIGYOSHO` → ユーザー所属営業所配下の全組織
- `HONBU` → ユーザー所属本部配下の全組織
- `KYOKU` → ユーザー所属局配下の全組織
- `SHITSU` → ユーザー所属室配下の全組織
- `BU` → ユーザー所属部配下の全組織
- `KA` → ユーザー所属課のみ

組織階層は `mcz12_orgn_kr` テーブルから取得。

### 6.4 StatusMatrixResolver

```java
/**
 * 12状態ステータスマトリクスに基づき、
 * 指定ステータスキーと権限系列からボタンの表示状態を返す。
 *
 * @param statusKey   ステータスキー (000-911)
 * @param isTab010Bit2 TAB 010 bit2 (true: 担当者系列, false: 管理者系列)
 * @return Map<操作名, 状態(1/0/9)>
 */
Map<String, Integer> resolve(String statusKey, boolean isTab010Bit2);
```

### 6.5 ポリシーエンジンの不採用について

GAP-R06 で提案された汎用ポリシーエンジン（CASL / Casbin 等）は本仕様では採用しない。

**理由:**
- GAP-R06 の前提（「ビット文字列 `"110000"` の可読性が低い」）は、JWT クレームの JSON 化（`permissions.tab010.bit0: true`）により**既に解決済み**。開発者がビット演算を意識する場面は新システムには存在しない
- バックエンド（Casbin）とフロントエンド（CASL）で異なるポリシーライブラリを併用すると、同一権限ルールの二重管理が発生し、同期漏れリスクが増大する
- CZ の権限モデルは15アクター × 4層の固定構造であり、動的な権限定義変更の要件がない。`CzPermissions` / `StatusMatrixResolver` / `OrganizationScopeResolver` がドメイン特化のポリシー層として十分機能する

---

## 7. Auth Mock サーバー設計

### 7.1 アクター定義（15名完全版）

```javascript
const ACTORS = [
  // Layer 1: 人事モード (jinjiMode=true)
  { id: 'ACT-01', name: '報告担当者', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: null },
    employmentType: 0, orgCode: '100210', orgName: 'IT推進部 開発1課',
    staffRole: null },

  { id: 'ACT-05', name: '人事モードユーザー', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: null, upd: null },
    employmentType: 0, orgCode: '100220', orgName: '人事部 管理課',
    staffRole: null },

  { id: 'ACT-06', name: '正社員', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: null },
    employmentType: 0, orgCode: '100210', orgName: 'IT推進部 開発1課',
    staffRole: null },

  { id: 'ACT-07', name: '臨時職員1', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KA', ins: null, upd: null },
    employmentType: 1, orgCode: '100211', orgName: 'IT推進部 開発1課 第1G',
    staffRole: null },

  { id: 'ACT-08', name: '臨時職員2', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KA', ins: null, upd: null },
    employmentType: 2, orgCode: '100211', orgName: 'IT推進部 開発1課 第1G',
    staffRole: null },

  { id: 'ACT-09', name: '外部契約者', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KA', ins: null, upd: null },
    employmentType: 3, orgCode: '100211', orgName: 'IT推進部 開発1課 第1G',
    staffRole: null, canDelegate: true },

  // Layer 1: 管理モード (jinjiMode=false)
  { id: 'ACT-02', name: '報告管理者', jinjiMode: false,
    tab010: '010000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'HONBU', ins: 'KYOKU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: null },

  { id: 'ACT-03', name: '全権管理者', jinjiMode: false,
    tab010: '001000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'HONBU', ins: 'HONBU', upd: 'HONBU' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: null },

  { id: 'ACT-04', name: '管理モードユーザー', jinjiMode: false,
    tab010: '100000', tab011: '10', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: null },
    employmentType: 0, orgCode: '100210', orgName: 'IT推進部 開発1課',
    staffRole: null },

  { id: 'ACT-10', name: '全社スタッフ', jinjiMode: false,
    tab010: '001000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'ZENSYA', ins: 'ZENSYA', upd: 'ZENSYA' },
    employmentType: 0, orgCode: '100000', orgName: '全社',
    staffRole: 931 },

  { id: 'ACT-11', name: '事業スタッフ', jinjiMode: false,
    tab010: '010000', tab011: '11', tab012: '10',
    dataAuthority: { ref: 'HONBU', ins: 'HONBU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100100', orgName: '事業本部',
    staffRole: 932 },

  { id: 'ACT-12', name: '本社スタッフ', jinjiMode: false,
    tab010: '010000', tab011: '11', tab012: '10',
    dataAuthority: { ref: 'HONBU', ins: 'HONBU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100100', orgName: '本社管理',
    staffRole: 933 },

  { id: 'ACT-13', name: '局スタッフ', jinjiMode: false,
    tab010: '010000', tab011: '10', tab012: '10',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: 'KA' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: 934 },

  { id: 'ACT-14', name: '局スタッフ（総務管理者）', jinjiMode: false,
    tab010: '001000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: 935 },

  { id: 'ACT-15', name: '局スタッフ（営業）', jinjiMode: false,
    tab010: '010000', tab011: '10', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KA', upd: null },
    employmentType: 0, orgCode: '100300', orgName: '営業部',
    staffRole: 936 },
]
```

### 7.2 エンドポイント

> 06_devenv_infrastructure.md セクション 4.3 に準拠

| メソッド | パス | 機能 |
|---------|------|------|
| GET | `/` | アクター選択 UI（HTML 画面） |
| GET | `/health` | ヘルスチェック |
| GET | `/api/actors` | アクター一覧 JSON |
| POST | `/api/switch` | アクター切替 → JWT 発行（body: `{ actorId }`） |
| GET | `/api/current` | 現在のアクター情報 |
| GET | `/api/token` | 現在の JWT トークン |
| GET | `/.well-known/openid-configuration` | OIDC Discovery 模倣 |
| GET | `/oauth2/keys` | JWKS 模倣（JWT 検証用公開鍵） |

### 7.3 JWT 生成ロジック

```
POST /api/switch { actorId: "ACT-03" }
  → ACTORS から ACT-03 を取得
  → tab010 文字列 "001000" をビットオブジェクトに変換:
    { bit0: false, bit1: false, bit2: true }
  → JWT ペイロード構築（セクション 4.1 の構造）
  → HS256 署名
  → レスポンス: { token, actor, headers }
  → Set-Cookie: cz-auth-token=<JWT>
```

---

## 8. Frontend 実装設計

### 8.1 DevActorSwitcher コンポーネント

**表示条件**: `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH === 'true'` の場合のみ

```
┌─────────────────────────────────────────────┐
│ [ヘッダー] 保有資源管理  [開発環境]          │
│                          ┌────────────────┐ │
│                          │ 現在: ACT-01   │ │
│                          │ 報告担当者     │ │
│                          │ Mode: 人事     │ │
│                          │ ──────────────│ │
│                          │ ● ACT-01 報告  │ │
│                          │ ○ ACT-02 管理  │ │
│                          │ ○ ACT-03 全権  │ │
│                          │ ○ ACT-07 臨時  │ │
│                          │ ○ ACT-09 外部  │ │
│                          │ [切替]         │ │
│                          └────────────────┘ │
└─────────────────────────────────────────────┘
```

### 8.2 認証 Composable

```typescript
// composables/useAuth.ts
interface CzAuth {
  user: ComputedRef<CzPrincipal | null>
  isAuthenticated: ComputedRef<boolean>
  jinjiMode: ComputedRef<boolean>
  permissions: ComputedRef<CzPermissions>

  // セマンティックエイリアス（CzPermissions のエイリアスを直接公開）
  canReport: ComputedRef<boolean>        // tab010.bit0
  canManage: ComputedRef<boolean>        // tab010.bit1
  canFullAccess: ComputedRef<boolean>    // tab010.bit2
  canExportHours: ComputedRef<boolean>   // tab011.bit0
  canNavigateForms: ComputedRef<boolean> // tab011.bit1
  canInputPeriod: ComputedRef<boolean>   // tab012.bit0
  canAggregate: ComputedRef<boolean>     // tab012.bit1

  // ステータスマトリクス判定
  canOperate(operation: string, statusKey: string): boolean
  hasDataAccess(type: 'ref' | 'ins' | 'upd'): boolean
}
```

**使用例**:
```typescript
const auth = useAuth()

// ❌ bit 番号を直接参照（非推奨）
if (auth.permissions.value.tab010.bit0) { ... }

// ✅ セマンティックエイリアスを使用（推奨）
if (auth.canReport.value) { ... }
if (auth.canManage.value) { ... }
```

### 8.3 切替フロー

```
開発者が DevActorSwitcher で ACT-02 を選択
  → POST http://auth-mock:8180/api/switch { actorId: "ACT-02" }
  → JWT 取得（レスポンス body + Set-Cookie）
  → Pinia auth store に格納
  → Cookie に保存 (cz-auth-token)
  → ページリロード
  → 全 API リクエストに Authorization: Bearer <JWT> を付与
  → Backend が JWT から ACT-02 の4層権限を解決
```

---

## 9. 本番混入防止（三重安全策）

### 安全策 1: 環境変数ゲート

```
NUXT_PUBLIC_ENABLE_ACTOR_SWITCH=true  ← 開発/検証のみ
本番: この環境変数は設定しない（undefined → コンポーネント非表示）
```

### 安全策 2: Nuxt.js ビルド時 Tree Shaking

```typescript
// plugins/actor-switch.client.ts
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  if (config.public.enableActorSwitch !== 'true') {
    // プラグイン未登録 → コンポーネントコードがビルドに含まれない
    return
  }
  // DevActorSwitcher を登録
})
```

本番ビルドではアクター切替コード自体がバンドルから除外される。

### 安全策 3: CI パイプラインでの強制検証

```yaml
# ci-production.yml
- name: "CRITICAL: Verify no actor-switch in production build"
  run: |
    # フロントエンドビルド成果物の検査
    if grep -rl "ActorSwitch\|actor-switch\|ENABLE_ACTOR_SWITCH\|DevActorSwitcher" \
      frontend/.output/ 2>/dev/null; then
      echo "FATAL: Actor switch code found in production build!"
      exit 1
    fi

    # バックエンドのモックプロファイル検査
    if grep -rl "mock-auth\|MockAuthFilter\|actor-switch" \
      backend/build/libs/ 2>/dev/null; then
      echo "FATAL: Mock auth code found in production build!"
      exit 1
    fi
```

---

## 10. テスト要件

### 10.1 Backend テスト

| テスト種別 | 対象 | カバレッジ目標 |
|-----------|------|-------------|
| 単体テスト | AlbOidcAuthFilter — JWT パース・クレーム抽出 | 100% |
| 単体テスト | StatusMatrixResolver — 12状態×2系列 = 全パターン | 100% |
| 単体テスト | OrganizationScopeResolver — 全階層レベル | 100% |
| 単体テスト | CzPermissions — 4層権限判定ロジック | 100% |
| 統合テスト | 15アクター × 主要 API エンドポイント | 全アクター |
| パラメタライズドテスト | ステータスマトリクス 12状態 × 担当者/管理者 × 7操作 | 全組み合わせ |

### 10.2 Frontend テスト

| テスト種別 | 対象 |
|-----------|------|
| 単体テスト | useAuth composable — 権限判定ロジック |
| 単体テスト | DevActorSwitcher — アクター切替 UI |
| E2E テスト | 各アクターでのログイン → 画面表示 → ボタン表示/非表示確認 |
| ビルドテスト | `ENABLE_ACTOR_SWITCH` 未設定での本番ビルド → DevActorSwitcher 混入なし |

### 10.3 Auth Mock テスト

| テスト種別 | 対象 |
|-----------|------|
| 単体テスト | JWT 生成 — 全15アクターの正常なクレーム構造 |
| 単体テスト | tab010 文字列 → ビットオブジェクト変換 |
| 統合テスト | アクター切替 → Backend JWT 検証 → 正しい権限解決 |

### 受け入れ基準（Given-When-Then）

**AC-AUTH-01: JWT 検証成功**
- Given: Auth Mock から発行された有効な JWT がリクエストヘッダーに含まれている
- When: API エンドポイントにリクエストを送信する
- Then: AlbOidcAuthFilter が JWT を検証し、CzPrincipal がセキュリティコンテキストに設定される

**AC-AUTH-02: JWT 未設定時の拒否**
- Given: X-Amzn-Oidc-Data ヘッダーが存在しないリクエスト
- When: 認証が必要な API エンドポイントにリクエストを送信する
- Then: 401 Unauthorized が返される

**AC-AUTH-03: ビット権限によるタブ制御**
- Given: ACT-01（tab010 = "110000"）でログインしている
- When: CzPermissions.tab010 を取得する
- Then: bit0=true, bit1=true, bit2=false が返され、管理者系列ボタンが表示される

**AC-AUTH-04: 担当者/管理者系列の判定**
- Given: tab010.bit2 = true のユーザーでログインしている
- When: useTanSeries() を呼び出す
- Then: true が返され、担当者系列のステータスマトリクスが適用される

**AC-AUTH-05: ステータスマトリクス解決**
- Given: ステータスキー "010"（作成中・確定済・未集計）のレコード
- When: tab010.bit2=true（担当者系列）のユーザーがボタン状態を取得する
- Then: StatusMatrixResolver が担当者系列マトリクスから値 0（無効）を返す

**AC-AUTH-06: 代行モード設定**
- Given: ACT-09（外部契約者, canDelegate=true）でログインしている
- When: X-Delegation-Staff-Id ヘッダーに有効な正社員 ID を設定してリクエスト送信する
- Then: CzPrincipal.delegationStaffId に代行先 ID が設定され、登録者 ID として使用される

**AC-AUTH-07: 代行モード権限不足**
- Given: ACT-07（臨時職員1, canDelegate=false）でログインしている
- When: X-Delegation-Staff-Id ヘッダーを設定してリクエスト送信する
- Then: CZ-307（権限不足）エラーが返される

**AC-AUTH-08: 組織スコープ制御**
- Given: ACT-03（課長, relativeAuthority=SUBORDINATE）でログインしている
- When: 他課のデータを参照しようとする
- Then: OrganizationScopeResolver により自課配下のデータのみが返される

**AC-AUTH-09: JinjiMode 切替**
- Given: jinjiMode=true の JWT クレームを持つユーザー
- When: API リクエストを送信する
- Then: CzPrincipal.jinjiMode=true となり、人事モード用のデータアクセス範囲が適用される

**AC-AUTH-10: Auth Mock アクター切替**
- Given: 開発環境で Auth Mock サーバーが起動している
- When: DevActorSwitcher で ACT-05（係員B）を選択する
- Then: Auth Mock が ACT-05 の JWT を発行し、画面がリロードされ ACT-05 の権限でアクセスできる

**AC-AUTH-11: 本番環境での DevActorSwitcher 除外**
- Given: NUXT_PUBLIC_ENABLE_ACTOR_SWITCH が未設定（本番ビルド）
- When: アプリケーションを表示する
- Then: DevActorSwitcher コンポーネントがレンダリングされず、DOM に存在しない

---

## 11. 時間制御による権限（補足）

| ロールカテゴリ | 制御内容 | 新システム対応 |
|-------------|---------|-------------|
| 940 | 月初1日目のシステム利用可否 | Backend ミドルウェアで `LocalDate.now().getDayOfMonth() == 1` の場合に CZ-102（サービス時間外）を返す。staffRole 931〜935 の管理スタッフは制限対象外 |
| 941 | 月初2日目のシステム利用可否 | Backend ミドルウェアで `LocalDate.now().getDayOfMonth() == 2` の場合に CZ-102 を返す。staffRole 931〜935 の管理スタッフは制限対象外 |
| 937 | 局跨がり組織選択の可否 | **P2 対応**。現時点では未実装。将来的に JWT クレーム `crossBureauAccess` を追加 |
| 951 | 局跨がり役職管理の可否 | **P2 対応**。現時点では未実装。将来的に JWT クレーム `crossBureauRoleCtrl` を追加 |

---

## 12. 実装優先順

| Phase | 内容 | 依存 |
|-------|------|------|
| **Phase 1** | Auth Mock 15アクター定義 + JWT 生成 | なし |
| **Phase 2** | Backend AlbOidcAuthFilter + CzPrincipal + CzPermissions | Phase 1 |
| **Phase 3** | Backend OrganizationScopeResolver + StatusMatrixResolver | Phase 2 |
| **Phase 4** | Frontend useAuth composable + DevActorSwitcher | Phase 1 |
| **Phase 5** | 統合テスト（15アクター × API エンドポイント） | Phase 2-4 |
| **Phase 6** | CI production safety grep 検査 | Phase 4 |

# アクター定義書 - CZシステム（保有資源管理システム）

> **ソースコード根拠に基づく完全なアクター・権限分析**
> 分析対象: irpmng_czConsv

---

## 1. アクター一覧（総括）

本システムには、以下の **4層の権限モデル** が存在する。

```
Layer 1: アプリケーションモード（JinjiMode）    ← web.xml で決定
Layer 2: 機能権限（ビットベース TAB 010/011/012） ← セキュリティロールグループで決定
Layer 3: データアクセス権限（相対権限 201/202/211）← 組織階層で決定
Layer 4: 雇用形態（正社員/派遣/外部委託）        ← 人事マスタで決定
```

### 1.1 アクター一覧表

| アクターID | アクター名 | 判定ロジック（ソースコード根拠） | 説明 |
|-----------|-----------|-------------------------------|------|
| **ACT-01** | **報告担当者** | `canUseSbt010_0bit() = true` | 保守工数を入力・報告する担当者。報告書作成担当グループに所属 |
| **ACT-02** | **報告管理者** | `canUseSbt010_1bit() = true` | 報告内容を管理する責任者。報告書管理担当グループに所属 |
| **ACT-03** | **全権管理者** | `canUseSbt010_2bit() = true` | 全報告に対するフル権限を持つ管理者 |
| **ACT-04** | **管理モードユーザー** | `ApWebxmlParameter.isJinjiMode() = false` | `/czMgr/` 経由でアクセス。承認・確定・集計操作が可能 |
| **ACT-05** | **人事モードユーザー** | `ApWebxmlParameter.isJinjiMode() = true` | `/czEnt/` 経由でアクセス。報告登録が主な操作 |
| **ACT-06** | **正社員** | `getTemporaryStaffType() = 0` | 通常の社員。全機能にアクセス可能 |
| **ACT-07** | **臨時職員1** | `getTemporaryStaffType() = 1` (カテゴリ900) | 制限付きアクセス |
| **ACT-08** | **臨時職員2** | `getTemporaryStaffType() = 2` (カテゴリ901) | 制限付きアクセス。`isActOverRole_901()` で判定 |
| **ACT-09** | **外部契約者** | `getTemporaryStaffType() = 3` (カテゴリ902) | 最も制限されたアクセス。`isActOverRole_902()` で判定。代行モード時の登録者IDが特殊処理される |
| **ACT-10** | **全社スタッフ** | `isStaffRole_931() = true` | 全社横断のデータアクセス権限 |
| **ACT-11** | **事業スタッフ** | `isStaffRole_932() = true` | 事業本部レベルのデータアクセス |
| **ACT-12** | **本社スタッフ** | `isStaffRole_933() = true` | 本社レベルのデータアクセス |
| **ACT-13** | **局スタッフ** | `isStaffRole_934() = true` | 局レベルのデータアクセス |
| **ACT-14** | **局スタッフ（総務管理者）** | `isStaffRole_935() = true` | 局レベル + 総務管理権限 |
| **ACT-15** | **局スタッフ（営業）** | `isStaffRole_936() = true` | 局レベル + 営業部門権限 |

---

## 2. 権限判定ロジックの詳細（ソースコード根拠）

### 2.1 Layer 1: アプリケーションモード（JinjiMode）

**判定箇所:** `ApWebxmlParameter.isJinjiMode()`
**設定箇所:** `web.xml` の `<context-param name="JinjiMode">`

```
/czEnt/ (JinjiMode=true)  → 人事モード（保有資源報告登録システム）  CZAPP002
/czMgr/ (JinjiMode=false) → 管理モード（保有資源管理システム）      CZAPP001
```

**影響するUI要素（JSPでの分岐箇所）:**

| JSPファイル | 分岐内容 |
|------------|---------|
| `ap_header.jsp` (行81-94) | 管理モードのみ「戻る」ボタン表示。ヘッダー幅: 管理170px / 人事100px |
| `ap_main_frame.jsp` (行23-27) | システムタイトルGIF切替（GIF_TITLE_ENT / GIF_TITLE_MGR） |
| `main_menu_frame.jsp` (行14-22) | メニュー構成の切替 |
| `proxy.jsp` | プロキシログイン処理の分岐 |
| `concurrent.jsp` (行10, 23-27) | 同時編集通知の表示分岐 |
| `sysno_main.jsp` | システムNo選択ダイアログの動作分岐 |
| `statusStop.jsp` | 緊急停止画面のメッセージ分岐 |
| `ApParameter.getHelp_url()` | ヘルプURL（help_url_ent / help_url_mgr）の切替 |

**コントロールテーブル分岐:**
```java
// ApCtrlTBL内の処理
if(isJinjiMode()) {
    controlVO = findMCZControlVO("M1", yearMonth); // 人事モード → SYS_01
} else {
    controlVO = findMCZControlVO("M0", yearMonth); // 管理モード → SYS_00
}
```

---

### 2.2 Layer 2: 機能権限（ビットベース制御）

**判定箇所:** `SecurityRoleInfo.canUseSbtXXX_Xbit()`
**内部実装:** `isAvailableFunction(category, funcCode)` → ロールデータ文字列の指定位置が `'1'` かを判定

**ロールデータ構造（dummySecurity.xmlより）:**
```xml
<role category="010" dataType="function" priority="080">
  <roledata>110000</roledata>  <!-- ビット0=1, ビット1=1, ビット2=0 -->
</role>
```

#### TAB 010: 保守工数機能グループ（3ビット）

| ビット | メソッド | 権限名 | 機能説明 |
|-------|---------|--------|---------|
| 0 | `canUseSbt010_0bit()` | 報告書作成担当グループ | 工数の入力・報告。確認済み/集計済みの閲覧 |
| 1 | `canUseSbt010_1bit()` | 報告書管理担当グループ | 工数の入力。未確認/未集計の管理。条件付き操作 |
| 2 | `canUseSbt010_2bit()` | 全管理グループ | 全データのフルアクセス。ステータス制御の基準アクター |

**canUseSbt010_2bit() のUI制御への影響（InsertListJspBean.java）:**

```java
// 全管理グループかどうかで、ステータス制御マトリクスの系列が切り替わる
if(sec.canUseSbt010_2bit()) {
    // → isSts_tan_* 系列（担当者権限の制御マトリクス）を使用
} else {
    // → isSts_man_* 系列（管理者権限の制御マトリクス）を使用
}
```

#### TAB 011: 拡張機能グループ（2ビット）

| ビット | メソッド | 権限名 | 機能説明 |
|-------|---------|--------|---------|
| 0 | `canUseSbt011_0bit()` | 保守H時間出力 | 保守H時間出力機能の利用可否 |
| 1 | `canUseSbt011_1bit()` | 次画面遷移 | FORM_010↔FORM_020間の遷移リンク表示制御（`ApMenuControlJspBean.outNextPageLink()`） |

#### TAB 012: 状況管理グループ（2ビット）

| ビット | メソッド | 権限名 | 機能説明 |
|-------|---------|--------|---------|
| 0 | `canUseSbt012_0bit()` | 期間入力条件 | 状況一覧での期間条件入力の可否 |
| 1 | `canUseSbt012_1bit()` | 期間集計 | 状況一覧での期間集計実行の可否 |

**TAB 012 の使用箇所（InsertJokyoListJspBean.java）:**

```java
// 期間入力条件の表示制御
if(sec.canUseSbt012_0bit()) { /* 条件入力エリアを表示 */ }

// 期間集計ボタンの表示制御
if(sec.canUseSbt012_1bit()) { /* 集計ボタンを表示 */ }
```

---

### 2.3 Layer 3: データアクセス権限（相対権限モデル）

**判定箇所:** `SecurityRoleInfo.getRelativeAuthority()`, `MprSecurityInfo.getAllowedOrganizationCodes()`

| ロールコード | メソッド | 権限名 | 操作 |
|------------|---------|--------|------|
| 201 | `RERATIVE_AUTHORITY_REF_ROLE` | 相対権限（参照） | データ参照のみ |
| 202 | `RERATIVE_AUTHORITY_INS_ROLE` | 相対権限（登録） | データ登録が可能 |
| 211 | `RERATIVE_AUTHORITY_UPD_ROLE` | 相対権限（更新） | データ更新が可能 |

**組織階層レベルによるデータスコープ:**

| レベル定数 | 値 | 組織 | アクセス可能範囲 |
|----------|---|------|----------------|
| `RELATIVE_ZENSYA` | 9999999 | 全社 | 全組織のデータ |
| `RERATIVE_ZENSHA` | 255 | 全社（別定義） | 全組織のデータ |
| `RERATIVE_EIGYOSHO` | 127 | 営業所 | 営業所配下のデータ |
| `RERATIVE_HONBU` | 63 | 本部 | 本部配下のデータ |
| `RERATIVE_KYOKU` | 31 | 局 | 局配下のデータ |
| `RERATIVE_SHITSU` | 15 | 室 | 室配下のデータ |
| `RERATIVE_BU` | 7 | 部 | 部配下のデータ |
| `RERATIVE_KA` | 3 | 課 | 課配下のデータ |

**組織階層によるアクセス制御の実装:**

```java
// MprSecurityInfo.getAllowedOrganizations()
// ロールタイプに応じた組織解決:
// - AbsoluteAuthorityRole: 指定組織コードに直接アクセス
// - RelativeAuthorityRole: 組織階層を辿り、下位組織を含めてアクセス
// - CombineDataAuthorityRole: 上記2つのハイブリッド
```

---

### 2.4 Layer 4: 雇用形態による制御

**判定箇所:** `SecurityRoleInfo.getTemporaryStaffType()`, `MprTemporaryStaffInfo`

```java
// MprTemporaryStaffInfo.getTemporaryStaffType() の判定ロジック:
if (カテゴリ"900"のロールが存在) → return TYPE_TEMPORARY_1 (1)  // 臨時職員1
if (カテゴリ"901"のロールが存在) → return TYPE_TEMPORARY_2 (2)  // 臨時職員2
if (カテゴリ"902"のロールが存在) → return TYPE_SUBCONTRACT (3)  // 外部契約者
else                            → return TYPE_OFFICIAL (0)     // 正社員
```

**外部契約者（TYPE_SUBCONTRACT）の特殊処理:**

```java
// CurrentConditionInfo.getRegisterUserId() での分岐:
if (TYPE_SUBCONTRACT && isDaiko()) {
    return SelectedOfficialStaff のID;  // 代行元の正社員IDを返す
} else {
    return 通常のRegisterID;
}
```

**代行（Daiko）モード:**
- 外部契約者が正社員の代行として操作する場合に使用
- `CurrentConditionInfo.isDaiko()` で判定
- `MprTemporaryStaffInfo.getActForStaffList()` で代行可能な正社員リストを取得
- `MprTemporaryStaffInfo.isAllowedStaff(tantoCode)` で代行可否を検証

---

## 3. ステータス × 権限によるボタン制御マトリクス

### 3.1 ステータスキーの定義

**設定箇所:** `ApParameter.xml` の `sts_base_key`

```
sts_base_key = "000,010,011,100,110,111,200,210,211,900,910,911"
```

| Index | キー | ステータス(sts) | 月次確定(ins) | 集計(sum) | 意味 |
|-------|------|---------------|-------------|----------|------|
| 0 | 000 | 0:作成中 | 0:未確定 | 0:未集計 | 作成中・未確定・未集計 |
| 1 | 010 | 0:作成中 | 1:確定 | 0:未集計 | 作成中・確定済・未集計 |
| 2 | 011 | 0:作成中 | 1:確定 | 1:集計済 | 作成中・確定済・集計済 |
| 3 | 100 | 1:確認 | 0:未確定 | 0:未集計 | 確認済・未確定・未集計 |
| 4 | 110 | 1:確認 | 1:確定 | 0:未集計 | 確認済・確定済・未集計 |
| 5 | 111 | 1:確認 | 1:確定 | 1:集計済 | 確認済・確定済・集計済 |
| 6 | 200 | 2:確定 | 0:未確定 | 0:未集計 | 確定済・未確定・未集計 |
| 7 | 210 | 2:確定 | 1:確定 | 0:未集計 | 確定済・確定済・未集計 |
| 8 | 211 | 2:確定 | 1:確定 | 1:集計済 | 確定済・確定済・集計済 |
| 9 | 900 | 9:非表示 | 0:未確定 | 0:未集計 | 非表示状態 |
| 10 | 910 | 9:非表示 | 1:確定 | 0:未集計 | 非表示・確定済 |
| 11 | 911 | 9:非表示 | 1:確定 | 1:集計済 | 非表示・確定・集計済 |

### 3.2 担当者系列（`canUseSbt010_2bit() = true` 時に使用）

**値: 1=有効, 0=無効, 9=非表示**

| 操作 | 000 | 010 | 011 | 100 | 110 | 111 | 200 | 210 | 211 | 900 | 910 | 911 |
|------|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|
| **追加** (tan_ins_btn) | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | **1** | 0 | 0 |
| **コピー** (tan_cpy_btn) | **1** | 0 | 0 | **1** | 0 | 0 | **1** | 0 | 0 | 9 | 9 | 9 |
| **削除** (tan_del_btn) | **1** | 0 | 0 | **1** | 0 | 0 | 0 | 0 | 0 | 9 | 9 | 9 |
| **更新** (tan_upd_btn) | **1** | 0 | 0 | **1** | 0 | 0 | 9 | 9 | 9 | 9 | 9 | 9 |
| **参照** (tan_view) | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |

### 3.3 管理者系列（`canUseSbt010_2bit() = false` 時に使用）

| 操作 | 000 | 010 | 011 | 100 | 110 | 111 | 200 | 210 | 211 | 900 | 910 | 911 |
|------|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|
| **追加** (man_ins_btn) | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | **1** | **1** | **1** |
| **コピー** (man_cpy_btn) | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| **削除** (man_del_btn) | **1** | **1** | **1** | **1** | **1** | **1** | 0 | 0 | 0 | 9 | 9 | 9 |
| **更新** (man_upd_btn) | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| **参照** (man_view) | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| **状況更新** (man_j_upd) | 0 | 0 | 0 | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| **状況参照** (man_j_view) | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |

### 3.4 マトリクスの読み方

**担当者（tan）系列の特徴:**
- 月次確定(ins=1)または集計済(sum=1)になると、コピー・削除・更新が**全て無効**になる
- 確定(sts=2)になると、削除・更新が**無効**になる
- 追加は非表示状態(sts=9)でのみ有効（新規登録画面専用）

**管理者（man）系列の特徴:**
- 月次確定・集計済でも、コピー・更新・参照は**全て有効のまま**
- 確定(sts=2)でも削除以外は操作可能
- 状況更新(man_j_upd)は確認(sts=1)以降のみ有効（ステータスを進める操作）

---

## 4. 各アクターがアクセス可能なURL/機能の紐付け表

### 4.1 画面アクセス権限マトリクス

| 画面/機能 | URL Key | ACT-05 人事モード | ACT-04 管理モード | ACT-01 報告担当 | ACT-02 報告管理 | ACT-03 全権管理 | 判定ロジック |
|----------|---------|:---:|:---:|:---:|:---:|:---:|------------|
| **メインメニュー** | U_MENU_CONTROL | O | O | O | O | O | 全アクター共通 |
| **工数入力一覧** | U_INSERT_LIST_SEARCH | O | O | O | O | O | FORM_010 |
| **工数入力ソート** | U_INSERT_LIST_SORT | O | O | O | O | O | |
| **工数入力CRUD** | U_INSERT_LIST_MAINTENANCE | O | O | **制限付** | **制限付** | O | ステータスマトリクスで制御 |
| **工数AjaxSet** | U_INSERT_LIST_AJAXSET | O | O | O | O | O | |
| **プロジェクト工数** | U_INSERT_PROJECT_KOUSUU | O | O | O | O | O | ダイアログ |
| **翌月複製** | U_INSERT_NEXT_MONTH | O | O | **制限付** | **制限付** | O | ステータスマトリクスで制御 |
| **工数明細Excel出力** | U_INSERT_LIST_DETAILOUTPUT | O | O | O | O | O | |
| **工数状況一覧** | U_INSERTJOKYO_LIST_SEARCH | O | O | O | O | O | FORM_020 |
| **状況一覧ソート** | U_INSERTJOKYO_LIST_SORT | O | O | O | O | O | |
| **状況一覧CRUD** | U_INSERTJOKYO_LIST_MAINTENANCE | O | O | **制限付** | O | O | ステータスマトリクスで制御 |
| **状況一覧ページング** | U_INSERTJOKYO_LIST_PAGE | O | O | O | O | O | |
| **状況一覧期間条件** | - | O | O | - | - | O | `canUseSbt012_0bit()` |
| **状況一覧期間集計** | - | O | O | - | - | O | `canUseSbt012_1bit()` |
| **状況一覧出力** | U_INSERTJOKYO_LIST_OUTPUT | O | O | O | O | O | |
| **画面遷移リンク(010↔020)** | (ap_history内) | O | O | - | - | O | `canUseSbt011_1bit()` |
| **分類別半期推移** | U_HALF_SUII_INIT | O | O | O | O | O | FORM_030 |
| **半期推移検索** | U_HALF_SUII_SEARCH | O | O | O | O | O | |
| **半期推移ソート** | U_HALF_SUII_SORT | O | O | O | O | O | |
| **半期推移表示切替** | U_HALF_SUII_HIDDEN_SHOW | O | O | O | O | O | |
| **半期推移Sys/SubSys** | U_HALF_SUII_SYS_SUBSYS | O | O | O | O | O | |
| **半期推移工数/コスト** | U_HALF_SUII_KOUSUU_COST | O | O | O | O | O | |
| **半期推移MYシステム** | U_HALF_SUII_MY | O | O | O | O | O | |
| **半期推移Excel出力** | U_HALF_SUII_OUTPUT | O | O | O | O | O | |
| **SysNo別半期推移** | （同上 FORM_031） | O | O | O | O | O | タブ切替 |
| **MYシステム半期推移** | （同上 FORM_032） | O | O | O | O | O | タブ切替 |
| **分類別月別内訳** | U_MONTH_UTIWAKE_INIT | O | O | O | O | O | FORM_040 |
| **月別内訳検索** | U_MONTH_UTIWAKE_SEARCH | O | O | O | O | O | |
| **月別内訳ソート** | U_MONTH_UTIWAKE_SORT | O | O | O | O | O | |
| **月別内訳Excel出力** | U_MONTH_UTIWAKE_OUTPUT | O | O | O | O | O | |
| **月別内訳詳細出力** | U_MONTH_UTIWAKE_DETAIL_OUTPUT | O | O | O | O | O | |
| **SysNo別月別内訳** | （同上 FORM_041） | O | O | O | O | O | タブ切替 |
| **MYシステム月別内訳** | （同上 FORM_042） | O | O | O | O | O | タブ切替 |

### 4.2 ダイアログアクセス権限

| ダイアログ | URL Key | 全アクター | 備考 |
|----------|---------|:---:|------|
| 組織選択（単一） | U_ORG_INIT / U_ORG_SEARCH_ORG / U_ORG_SEARCH_NAME / U_ORG_OK_BUTTON / U_ORG_COMMIT | O | 組織階層に基づくデータフィルタリング |
| 組織選択（複数） | U_ORG_MULTI_INIT / U_ORG_MULTI_SEARCH_ORG / U_ORG_MULTI_SELECT / U_ORG_MULTI_COMMIT | O | |
| システムNo選択 | U_SYSNO_INIT / U_SYSNO_SEARCH_ORG / U_SYSNO_BUTTON / U_SYSNO_COMMIT | O | JinjiModeで動作が微妙に異なる |
| システムNo選択（複数） | U_SYSNO_MULTI_INIT / U_SYSNO_MULTI_SEARCH_ORG / U_SYSNO_MULTI_BUTTON / U_SYSNO_MULTI_COMMIT | O | |
| 担当者検索 | U_TNT_SEARCH_INIT / U_TNT_SEARCH_RETRIEVAL / U_TNT_SEARCH_BUSYO_SELECT / U_TNT_SEARCH_OK_BUTTON / U_TNT_SEARCH_COMMIT | O | Ajax通信。組織階層でフィルタ |
| セキュリティ（パスワード変更） | U_SECURITY_INIT / U_SECURITY_SELECT / U_SECURITY_CHANGE | O | |
| 担当者切替（代行） | U_SECURETANTO_SHOW / U_SECURETANTO_SEARCH / U_SECURETANTO_SETTANTO | O | 外部契約者の代行モード |

### 4.3 セキュリティ/セッション管理

| 機能 | URL Key | 説明 |
|------|---------|------|
| セッション維持 | U_SESSIONTRACKING | 自動リフレッシュ。全アクター |
| 初期化制御 | U_INIT / U_INIT_CONTROL | ログイン後のルーティング |
| 同時編集制御 | U_INIT_CONNCURRENT | 排他制御通知 |
| 緊急停止チェック | InitSecurityRoleProc内 | `isEmergencyStop()` で全画面ブロック |

---

## 5. 時間制御による権限

| メソッド | ロールカテゴリ | 制御内容 |
|---------|-------------|---------|
| `canUseInActualFirstDayOfMonth()` | 940 | 月初1日目のシステム利用可否 |
| `canUseInActualSecondDayOfMonth()` | 941 | 月初2日目のシステム利用可否 |

**用途:** 月次締め処理期間中のアクセス制御。バッチ処理中に一般ユーザーのアクセスを制限する目的。

---

## 6. 局跨がり権限

| メソッド/カテゴリ | ロールカテゴリ | 制御内容 |
|----------------|-------------|---------|
| `CATEGORY_KYOKUMATAGARI_ORG_SELECT` | 937 | 局をまたがった組織選択の可否 |
| `CATEGORY_KYOKUMATAGARI_ROLE_CTRL` | 951 | 局をまたがった役職管理の可否 |

---

## 7. 認証・セッション管理フロー

```
1. ブラウザ → /czEnt/ or /czMgr/
2. ApServlet → InitSecurityRoleProc 実行
   ├── LoginCond からユーザー情報取得
   ├── SecurityRoleInfo.createSecurityInfo() でロール情報構築
   ├── getCanUseUserProfile() で利用可能プロファイル取得
   ├── SAP使用モード(sapUse)によるプロファイルフィルタリング
   │   ├── sapUse="0" → 全プロファイル表示
   │   ├── sapUse="1" → SAP以外のプロファイルのみ
   │   └── sapUse="2" → SAPプロファイルのみ
   ├── ライセンスチェック (hasLicense)
   ├── 緊急停止チェック (isEmergencyStop)
   └── コントロールテーブルチェック (isSystemCanUseChkFromControlTable)
3. LoginProc 実行
   ├── CurrentConditionInfo 作成
   ├── SecurityInfo をセッションに格納
   └── ESQID（マルチウィンドウ識別子）生成
4. AccessLogProc 実行
   └── アクセスログ記録
5. → index_control.jsp → ap_main_frame.jsp（メイン画面）
```

---

## 8. データスコープ制御の実装パターン

```
ユーザーがデータ検索を実行
  → SecurityRoleInfo.getAllowedOrganizationCodes("920") で許可組織コード取得
    → ロールタイプに基づく組織解決:
       ├── AbsoluteAuthorityRole: 指定された組織コードに直接アクセス
       ├── RelativeAuthorityRole: 所属組織の階層を辿り、下位組織を包含
       └── CombineDataAuthorityRole: 上記2つのハイブリッド
  → 取得した組織コードリストをDAO検索条件に設定
  → SQL WHERE句に組織コードフィルタを適用
  → ユーザーに見える範囲のデータのみ返却
```

---

## 9. 複合権限の具体例

### 例1: 工数入力画面で「削除」ボタンを押す場合

```
Step 1: canUseSbt010_2bit() を確認
  → true の場合: isSts_tan_del_btn(sts, ins, sum) を使用
    → sts=0(作成中) かつ ins=0(未確定) かつ sum=0(未集計) → 削除可能
    → sts=1(確認) かつ ins=0(未確定) かつ sum=0(未集計) → 削除可能
    → sts=2(確定) → 削除不可
  → false の場合: isSts_man_del_btn(sts, ins, sum) を使用
    → sts=0 or sts=1（確定前） → 月次確定/集計に関わらず削除可能
    → sts=2（確定後） → 削除不可

Step 2: getTemporaryStaffType() を確認
  → TYPE_SUBCONTRACT(3) の場合、代行モードの追加チェック

Step 3: getRelativeAuthority() で対象データの組織がアクセス可能か確認
```

### 例2: 管理者が状況一覧で「確認→確定」にステータスを変更する場合

```
Step 1: ApWebxmlParameter.isJinjiMode() = false を確認（管理モード必須）
Step 2: canUseSbt010_1bit() or canUseSbt010_2bit() を確認（管理権限）
Step 3: isSts_man_j_upd(sts="1", ins, sum) を確認
  → sts=1(確認) 以上でないと更新不可（sts=0は不可）
Step 4: MCZ04CTRLMST のオンラインフラグを確認（バッチ実行中でないか）
```

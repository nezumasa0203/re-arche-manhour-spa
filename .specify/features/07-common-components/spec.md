# 共通コンポーネント: レイアウト・ナビゲーション・ダイアログ仕様

## 概要

全画面（FORM_010〜042）で共有するレイアウト、ナビゲーション、
ダイアログ（モーダル）コンポーネントの仕様。
現行 MPA の FRAMESET 入れ子構造（40+ フレーム）+ window.open() ポップアップを、
Nuxt.js 3 SPA の CSS Grid レイアウト + PrimeVue モーダルに移行する。

**対応 GAP**:
- GAP-N01: FRAMESET → CSS Grid/Flexbox
- GAP-N02: メインメニュー → SPA サイドナビ
- GAP-N05: パンくずリスト追加
- GAP-D01〜05: window.open() → SPA モーダル

### 移行元ソースコード参照

| コンポーネント | 踏襲元機能 | 移行元ファイル |
|---------------|-----------|---------------|
| StatusBadge | ステータス色定義 | `czResources/cssjs/czstyle.css`（ステータス色定義） |
| HoursInput | 時間入力マスク・自動変換 | `czResources/cssjs/TdMask.js`（マスク処理） |
| HoursInput | バイト長計算 | `czResources/cssjs/ApCheck.js`（chkChara 関数） |
| useStatusMatrix | 12状態マトリクス | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/proc/StatusKeyManager.java` |
| ConfirmDialog | CZ-5xx 確認ダイアログ | `czResources/cssjs/ApMessage.xml`（メッセージ定義） |
| OrganizationSearchDialog | 組織ツリー検索 | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/OrganizationSearchJspBean.java` |
| SubsystemSearchDialog | SS検索 | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/SubsystemSearchJspBean.java` |
| StaffSearchDialog | 職員検索 | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/StaffSearchJspBean.java` |

※ 全ファイルは `D:\PROJECT02\migration_source\migration_soource_irpmng_czConsv\` 配下

---

## 1. レイアウト構成

### 1.1 全体レイアウト (AppLayout)

```
┌──────────────────────────────────────────────────┐
│ [AppHeader]  h: 56px                              │
│ ロゴ | ページタイトル | ユーザー情報 | ヘルプ       │
├──────────┬───────────────────────────────────────┤
│ [Side    │ [NuxtPage]                             │
│  Nav]    │                                        │
│ w:220px  │ ページコンテンツ                        │
│          │                                        │
│          │                                        │
│ (折畳時  │                                        │
│  w:56px) │                                        │
│          │                                        │
└──────────┴───────────────────────────────────────┘
```

### 1.2 ファイル構成

```
layouts/default.vue (AppLayout)
├── components/layout/AppHeader.vue
├── components/layout/AppSideNav.vue
└── <slot /> (NuxtPage)

components/
├── layout/
│   ├── AppHeader.vue
│   ├── AppSideNav.vue
│   └── AppBreadcrumb.vue
├── common/
│   ├── OrganizationSearchDialog.vue   (DLG_001/002)
│   ├── SubsystemSearchDialog.vue      (DLG_003/004)
│   ├── StaffSearchDialog.vue          (DLG_005)
│   ├── ConfirmDialog.vue              (CZ-5xx 系確認)
│   ├── StatusBadge.vue                (ステータス色分け)
│   ├── HoursInput.vue                 (HH:MM 入力)
│   ├── MonthSelector.vue              (年月セレクタ)
│   └── MessageToast.vue               (操作結果通知)
└── dev/
    └── DevActorSwitcher.vue           (開発環境アクター切替)
```

---

## 2. AppHeader.vue — ヘッダー

### 2.1 レイアウト

```
┌──────────────────────────────────────────────────┐
│ [≡] 保有資源管理システム     山田太郎 | [?] | [⚙] │
└──────────────────────────────────────────────────┘
  ↑                             ↑       ↑     ↑
  ハンバーガー                ユーザー名 ヘルプ 設定
  (SideNav 開閉)
```

### 2.2 要素定義

| 要素 | コンポーネント | 動作 |
|------|-------------|------|
| ハンバーガー | PrimeVue Button (icon: pi-bars) | `sideNavCollapsed` トグル |
| アプリ名 | テキスト | "保有資源管理システム" |
| ユーザー名 | テキスト | `CzPrincipal.userName` 表示 |
| モード表示 | Badge | jinjiMode: "人事" (青) / "管理" (緑) |
| 代行表示 | Badge (warn) | 代行モード時: "代行中: {対象者名}" |
| ヘルプ | PrimeVue Button (icon: pi-question-circle) | ヘルプページ遷移 |
| 設定 | PrimeVue Button (icon: pi-cog) | 設定ページ遷移（全権管理者のみ） |

### 2.3 DevActorSwitcher（開発環境のみ）

```
┌──────────────────────────────────────────────────┐
│ [≡] 保有資源管理  [DEV: ACT-01 報告担当者 ▼]  ... │
└──────────────────────────────────────────────────┘
```

- `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH === 'true'` の場合のみ表示（spec #2 セクション 9 三重安全策準拠）
- PrimeVue Dropdown で 15 アクターを切替
- 切替時: `POST /api/switch` → JWT 再取得 → 全ストアリセット → ページリロード

---

## 3. AppSideNav.vue — サイドナビゲーション

### 3.1 レイアウト

```
展開時 (w: 220px):          折畳時 (w: 56px):
┌──────────────┐           ┌────┐
│ ≡ メニュー    │           │ ≡  │
├──────────────┤           ├────┤
│ 📝 工数入力   │           │ 📝 │
│ 📋 工数管理   │           │ 📋 │
│ 📊 分析       │           │ 📊 │
│ ⚙ 設定       │           │ ⚙  │
├──────────────┤           ├────┤
│              │           │    │
│              │           │    │
│ v1.0.0       │           │    │
└──────────────┘           └────┘
```

### 3.2 メニュー定義

| メニュー | アイコン | パス | 表示条件 | 対応画面 |
|---------|---------|------|---------|---------|
| 工数入力 | pi-pencil | `/work-hours` | `canReport` (tab010.bit0) | FORM_010 |
| 工数管理 | pi-list | `/work-status` | `canManage` (tab010.bit1) | FORM_020 |
| 分析 | pi-chart-bar | `/analytics` | `canNavigateForms` (tab011.bit1) | FORM_030-042 |
| 設定 | pi-cog | `/settings` | `canFullAccess` (tab010.bit2) | 管理画面 |

### 3.3 アクティブ状態

- 現在のルートパスに一致するメニュー項目にアクティブスタイル適用
- PrimeVue Menu の `class: 'active'` で左ボーダー + 背景色変更

### 3.4 レスポンシブ動作

| ブレークポイント | 動作 |
|-------------|------|
| >= 1280px | 展開状態 (220px)。ハンバーガーで折畳可能 |
| 960-1279px | 折畳状態 (56px)。ハンバーガーで展開可能 |
| < 960px | 非表示。ハンバーガーでオーバーレイ表示 |

折畳/展開状態は `localStorage` に保存し、次回アクセス時に復元。

---

## 4. OrganizationSearchDialog.vue — 組織選択モーダル (DLG_001/002)

### 4.1 ワイヤーフレーム

```
┌──────────────────────────────────────┐
│ 組織選択                      [×]    │
├──────────────────────────────────────┤
│ [🔍 組織名で検索...              ]   │
│                                      │
│ ┌──────────────────────────────────┐│
│ │ ▼ 全社                          ││
│ │   ▼ 情報システム本部             ││
│ │     ▶ IT推進部                   ││
│ │       ● IT推進部 開発1課         ││
│ │       ○ IT推進部 開発2課         ││
│ │       ○ IT推進部 運用課          ││
│ │     ▶ 基盤部                     ││
│ │   ▶ 営業本部                     ││
│ │   ▶ 管理本部                     ││
│ └──────────────────────────────────┘│
│                                      │
│              [選択] [キャンセル]      │
└──────────────────────────────────────┘
```

### 4.2 Props / Events

```typescript
interface Props {
  visible: boolean
  mode?: 'single' | 'multiple'   // 単一選択 / 複数選択
  initialValue?: string           // 初期選択組織コード
  scopeFilter?: string[]          // dataAuthority による組織スコープ制限
}

interface Events {
  'update:visible': (value: boolean) => void
  'selected': (org: OrganizationItem | OrganizationItem[]) => void
}

interface OrganizationItem {
  orgCode: string
  orgName: string
  parentOrgCode: string | null
  orgLevel: number
}
```

### 4.3 動作仕様

| 機能 | 実装 |
|------|------|
| ツリー表示 | PrimeVue Tree。`GET /masters/organizations/tree` で階層取得 |
| インクリメンタルサーチ | 入力 300ms デバウンス → ツリーノードのフィルタ + ハイライト |
| 組織スコープ | `scopeFilter` で表示対象を制限。`dataAuthority.ref` の範囲外はグレーアウト |
| 単一選択 | ノードクリックで選択 → [選択] で確定 |
| 複数選択 (DLG_002) | チェックボックス付きツリー。複数ノード選択可能 |
| 初期展開 | `initialValue` のノードまで自動展開 |

### 4.4 API

```
GET /api/v1/masters/organizations/tree
レスポンス:
{
  "data": [
    {
      "orgCode": "000000",
      "orgName": "全社",
      "children": [
        {
          "orgCode": "100000",
          "orgName": "情報システム本部",
          "children": [...]
        }
      ]
    }
  ]
}
```

---

## 5. SubsystemSearchDialog.vue — SS 選択モーダル (DLG_003/004)

### 5.1 ワイヤーフレーム

```
┌──────────────────────────────────────────────┐
│ サブシステム選択 (対象SS)              [×]    │
├──────────────────────────────────────────────┤
│ [🔍 システム名/SS名で検索...             ]   │
│                                              │
│ ┌────┬───────┬────────────┬──────────────┐  │
│ │ ◆  │ SYS No│ システム名  │ SS名         │  │
│ ├────┼───────┼────────────┼──────────────┤  │
│ │ ◆  │SYS001│ 基幹システム│ 会計モジュール│  │
│ │    │SYS001│ 基幹システム│ 人事モジュール│  │
│ │ ◆  │SYS002│ 営業システム│ CRM          │  │
│ │    │SYS003│ 人事システム│ 給与         │  │
│ │    │...   │ ...        │ ...          │  │
│ └────┴───────┴────────────┴──────────────┘  │
│                                              │
│ 全 150 件中 1-50 件  [<] 1 2 3 [>]          │
│                                              │
│              [選択] [キャンセル]              │
└──────────────────────────────────────────────┘
```

### 5.2 Props / Events

```typescript
interface Props {
  visible: boolean
  mode: 'target' | 'cause'       // 対象SS / 原因SS
  organizationCode?: string       // 組織コードでフィルタ
}

interface Events {
  'update:visible': (value: boolean) => void
  'selected': (ss: SubsystemItem) => void
}

interface SubsystemItem {
  systemNo: string
  systemName: string
  subsystemNo: string
  subsystemName: string
  sysKbn: number                  // 1 の場合 ◆ マーカー
}
```

### 5.3 動作仕様

| 機能 | 実装 |
|------|------|
| テーブル表示 | PrimeVue DataTable。行クリックで選択 |
| インクリメンタルサーチ | 入力 300ms デバウンス → `GET /masters/subsystems?keyword=xxx` |
| ◆ マーカー | `sysKbn === 1` の行に "◆" を表示（VAL_SYS_KBN_1 相当） |
| ページネーション | PrimeVue Paginator。50 件/ページ |
| モード表示 | ダイアログタイトルに "(対象SS)" or "(原因SS)" を表示 |

### 5.4 API

```
GET /api/v1/masters/subsystems
  ?organizationCode=100210
  &keyword=基幹
  &page=1
  &pageSize=50

レスポンス:
{
  "data": [...],
  "meta": { "totalCount": 150, "page": 1, "pageSize": 50 }
}
```

---

## 6. StaffSearchDialog.vue — 担当者選択モーダル (DLG_005)

### 6.1 ワイヤーフレーム

```
┌──────────────────────────────────────────────┐
│ 担当者選択                             [×]   │
├──────────────────────────────────────────────┤
│ [組織ツリー] | [検索]                         │
│                                              │
│ (組織ツリータブ)                              │
│ ┌────────────────────┬───────────────────┐  │
│ │ ▼ 情報システム本部  │ 鈴木 花子 (d10623)│  │
│ │   ▼ IT推進部       │ 田中 一郎 (d10624)│  │
│ │     ● 開発1課      │ 山田 太郎 (d10625)│  │
│ │     ○ 開発2課      │                   │  │
│ │     ○ 運用課       │                   │  │
│ └────────────────────┴───────────────────┘  │
│                                              │
│ (検索タブ)                                    │
│ 氏名: [________] 一致: [部分一致 ▼] [検索]   │
│ ┌───────┬────────┬──────────────────┐       │
│ │ 社員ID│ 氏名   │ 所属             │       │
│ ├───────┼────────┼──────────────────┤       │
│ │d10623 │鈴木花子│IT推進部 開発1課   │       │
│ └───────┴────────┴──────────────────┘       │
│                                              │
│              [選択] [キャンセル]              │
└──────────────────────────────────────────────┘
```

### 6.2 Props / Events

```typescript
interface Props {
  visible: boolean
  purpose?: 'delegation' | 'search'  // 代行用 / 一般検索
  organizationScope?: string[]        // 検索スコープ制限
}

interface Events {
  'update:visible': (value: boolean) => void
  'selected': (staff: StaffItem) => void
}

interface StaffItem {
  staffId: string
  staffName: string
  department: string
  organizationCode: string
}
```

### 6.3 動作仕様

| 機能 | 実装 |
|------|------|
| 組織ツリータブ | 左: PrimeVue Tree。ノード選択で右に所属担当者を表示 |
| 検索タブ | 氏名入力 + 一致タイプ (完全/部分) で検索 |
| 代行モード | `purpose === 'delegation'` の場合: 代行可能な担当者のみ表示 (`GET /delegation/available-staff`) |
| 一般検索 | `purpose === 'search'` の場合: 全担当者から検索 (`GET /masters/staff`) |
| 選択確定 | 行クリック → [選択] で確定 |

### 6.4 API

```
代行用:
GET /api/v1/delegation/available-staff
  ?organizationCode=100210

一般検索:
GET /api/v1/masters/staff
  ?organizationCode=100210
  &name=鈴木
  &matchType=partial
```

### ダイアログ共通状態表示パターン

| 状態 | 表示内容 | 適用コンポーネント |
|------|---------|-------------------|
| ローディング中 | ダイアログ内にスケルトンローダー（PrimeVue Skeleton）を表示。操作ボタンは disabled | 全検索ダイアログ |
| 検索結果0件 | 「該当するデータが見つかりません」メッセージを表示。[選択] ボタンは disabled | SubsystemSearchDialog, StaffSearchDialog |
| ツリー読込中 | ツリーエリアにスピナーを表示 | OrganizationSearchDialog |
| ツリーノードなし | 「組織データが存在しません」メッセージを表示 | OrganizationSearchDialog |
| API エラー | ダイアログ内に Toast エラー通知を表示。ダイアログは閉じない（リトライ可能） | 全検索ダイアログ |
| useApi ローディング | `useApi` の `loading` ref が true の間、呼び出し元コンポーネントにローディング表示 | 全 composable 利用箇所 |

---

## 7. ConfirmDialog.vue — 確認ダイアログ

### 7.1 ワイヤーフレーム

```
┌──────────────────────────────────┐
│ 確認                      [×]   │
├──────────────────────────────────┤
│                                  │
│ ⚠ 選択した 3 件のレコードを       │
│   削除します。よろしいですか？     │
│                                  │
│          [OK] [キャンセル]       │
└──────────────────────────────────┘
```

### 7.2 Props / Events

```typescript
interface Props {
  visible: boolean
  title?: string                    // デフォルト: "確認"
  message: string                   // CZ-5xx メッセージ
  messageCode?: string              // "CZ-506" など
  severity?: 'info' | 'warn'       // アイコン・色制御
  confirmLabel?: string             // デフォルト: "OK"
  cancelLabel?: string              // デフォルト: "キャンセル"
}

interface Events {
  'update:visible': (value: boolean) => void
  'confirm': () => void
  'cancel': () => void
}
```

### 7.3 確認ダイアログメッセージ一覧

| コード | メッセージ | トリガー | 画面 |
|--------|-----------|---------|------|
| CZ-505 | 「作成中」を全て「確認」に変更します。よろしいですか？ | 一括確認 | FORM_010 |
| CZ-506 | 選択したレコードを削除します。よろしいですか？ | レコード削除 | FORM_010 |
| CZ-507 | 選択レコードを「確認」に戻します。よろしいですか？ | 承認取消 | FORM_020 |
| CZ-508 | 選択レコードを「承認」に変更します。よろしいですか？ | 承認 | FORM_020 |
| CZ-516 | Excel出力します。時間がかかる場合があります | Excel出力 | 全画面 |
| CZ-518 | 「確認」を全て「作成中」に変更します。よろしいですか？ | 一括戻し | FORM_010 |
| CZ-509 | 「記入可能」に戻します。よろしいですか？ | 月次未確認 | FORM_020 |
| CZ-510 | 「登録確認」に変更します。よろしいですか？ | 月次確認 | FORM_020 |
| CZ-511 | 「データ承認」に変更します。よろしいですか？ | 月次集約 | FORM_020 |

---

## 8. StatusBadge.vue — ステータスバッジ

### 8.1 仕様

全画面共通のステータス表示コンポーネント。

```typescript
interface Props {
  status: number    // 0, 1, 2, 9
  size?: 'sm' | 'md'  // デフォルト: 'md'
}
```

### 8.2 色定義

| STATUS | ラベル | 背景色 | 文字色 | CSS変数 |
|--------|--------|--------|--------|---------|
| 0 | 作成中 | `#FBFBB6` | `#000` | `--cz-status-0` |
| 1 | 確認 | `#BDEAAD` | `#000` | `--cz-status-1` |
| 2 | 確定 | `#9DBDFE` | `#000` | `--cz-status-2` |
| 9 | 非表示 | `#5D5D5D` | `#FFF` | `--cz-status-9` |

### 8.3 スタイル

```css
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-align: center;
  min-width: 48px;
}
```

---

## 9. HoursInput.vue — HH:MM 入力コンポーネント

### 9.1 仕様

工数入力の共通コンポーネント。FORM_010 と FORM_020 で共用。

```typescript
interface Props {
  modelValue: string         // "03:30"
  readonly?: boolean
  placeholder?: string       // デフォルト: "HH:MM"
}

interface Events {
  'update:modelValue': (value: string) => void
  'validated': (result: ValidationResult) => void
}
```

### 9.2 入力補助 (BR-006)

| 入力値 | 自動変換結果 | ルール |
|--------|------------|--------|
| `""` (空) | バリデーションエラー | VR-008 必須チェック（空値 → "00:00" 自動変換は行わない） |
| `"3"` | `"03:00"` | 1桁 → 0H:00 |
| `"12"` | `"12:00"` | 2桁 → HH:00 |
| `"330"` | `"03:30"` | 3桁 → H:MM |
| `"0330"` | `"03:30"` | 4桁 → HH:MM |
| `"3:30"` | `"03:30"` | コロン付き → 正規化 |
| `"03:30"` | `"03:30"` | そのまま |

### 9.3 バリデーション

| ルール | 条件 | エラーコード |
|--------|------|------------|
| 必須 | 空値 (00:00 以外) | CZ-126 |
| 形式 | HH:MM 形式 | CZ-125 |
| 15分単位 | 分が 00/15/30/45 | CZ-147 |
| 最小値 | 0:15 以上 | CZ-129 |
| 最大値 | 24:00 以下 | CZ-146 |

### 9.4 パース関数

```typescript
function parseHours(input: string): { hours: number; minutes: number } | null {
  // "03:30" → { hours: 3, minutes: 30 }
  // "3" → { hours: 3, minutes: 0 }
  // "330" → { hours: 3, minutes: 30 }
  // 無効 → null
}

function formatHours(hours: number, minutes: number): string {
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}
```

---

## 10. MonthSelector.vue — 年月セレクタ

### 10.1 仕様

年月選択の共通コンポーネント。FORM_010, FORM_020 で共用。

```typescript
interface Props {
  modelValue: string          // "2025-02"
  range?: number              // ±N ヶ月 (デフォルト: 12)
  showNavButtons?: boolean    // << >> ボタン表示 (デフォルト: true)
}

interface Events {
  'update:modelValue': (value: string) => void
}
```

### 10.2 レイアウト

```
[<<] [2025年02月 ▼] [>>]
```

- PrimeVue Dropdown: 現在月 ± `range` ヶ月のオプション生成
- `<<` / `>>`: 前月/翌月ボタン
- 範囲外に達した場合はボタン無効化

---

## 11. MessageToast.vue — 操作結果通知

### 11.1 仕様

PrimeVue Toast をラップした共通通知コンポーネント。

```typescript
// composable: useMessage()
function useMessage() {
  return {
    success(message: string, code?: string): void    // 緑色、3秒後自動消去
    warn(message: string, code?: string): void       // 黄色、5秒後自動消去
    error(message: string, code?: string): void      // 赤色、手動消去
    info(message: string, code?: string): void       // 青色、3秒後自動消去
  }
}
```

### 11.2 CZ エラーコード連携

| コード範囲 | 種別 | Toast severity | 自動消去 |
|-----------|------|---------------|---------|
| CZ-000〜099 | 成功 | success | 3秒 |
| CZ-100〜299 | 警告（バリデーション） | warn | 5秒 |
| CZ-300〜499 | システムエラー | error | 手動 |
| CZ-500〜799 | 確認（ConfirmDialog 経由） | - | - |
| CZ-800〜999 | 情報 | info | 3秒 |

---

## 12. バイト長計算ユーティリティ

全画面で使用するバイト長計算。件名バリデーション（128バイト）等で利用。

### 12.1 `utils/byteLength.ts`

```typescript
/**
 * CZ 仕様のバイト長計算。
 * 全角: 2バイト、半角: 1バイト、半角カタカナ: 2バイト
 */
export function calculateByteLength(str: string): number {
  let bytes = 0
  for (const char of str) {
    const code = char.charCodeAt(0)
    if (code >= 0xFF61 && code <= 0xFF9F) {
      bytes += 2  // 半角カタカナ: 2バイト
    } else if (code > 0x7F) {
      bytes += 2  // 全角: 2バイト
    } else {
      bytes += 1  // 半角: 1バイト
    }
  }
  return bytes
}
```

---

## 13. ステータスマトリクスユーティリティ

### 13.1 `utils/statusMatrix.ts`

Frontend 用のステータスマトリクス解決。Backend `StatusMatrixResolver` と同一ロジック。

```typescript
export type Operation = 'add' | 'copy' | 'delete' | 'update' | 'view'
                       | 'statusUpdate' | 'statusView'

export const ENABLED = 1
export const DISABLED = 0
export const HIDDEN = 9

/**
 * ステータスキーと系列からボタン表示状態を返す。
 * @param statusKey "000"〜"911"
 * @param isTanSeries true: 担当者系列, false: 管理者系列
 */
export function resolveStatusMatrix(
  statusKey: string,
  isTanSeries: boolean
): Record<Operation, number>

/**
 * ステータスキーを構築。
 * @param recordStatus 0/1/2/9
 * @param getsujiKakutei 月次確認フラグ
 * @param dataSyuukei データ集約フラグ
 */
export function buildStatusKey(
  recordStatus: number,
  getsujiKakutei: boolean,
  dataSyuukei: boolean
): string {
  const sts = recordStatus === 9 ? '9' : recordStatus === 0 ? '0' : recordStatus === 1 ? '1' : '2'
  const ins = getsujiKakutei ? '1' : '0'
  const sum = dataSyuukei ? '1' : '0'
  return `${sts}${ins}${sum}`
}
```

---

## 14. API クライアント共通

### 14.1 `composables/useApi.ts`

```typescript
/**
 * API 呼出の共通 composable。
 * JWT 自動付与、エラーハンドリング、ローディング管理。
 */
export function useApi() {
  return {
    get<T>(url: string, params?: Record<string, any>): Promise<T>
    post<T>(url: string, body?: any): Promise<T>
    patch<T>(url: string, body?: any): Promise<T>
    delete<T>(url: string, body?: any): Promise<T>
  }
}
```

### 14.2 共通ヘッダー

```typescript
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${jwt}`,
  // 代行モード時:
  'X-Delegation-Staff-Id': delegationStaffId
}
```

### 14.3 エラーインターセプター

```typescript
// 共通エラー処理
function handleApiError(error: ApiError) {
  switch (error.status) {
    case 401: // 未認証 → ログインページへ
      navigateTo('/login')
      break
    case 403: // 権限不足 → Toast
      useMessage().error(error.data.error.message, error.data.error.code)
      break
    case 409: // 楽観的ロック競合 → Toast + リフレッシュ
      useMessage().warn('別ユーザーにより更新されました。最新データを取得します。')
      break
    default: // その他 → Toast
      useMessage().error(error.data?.error?.message || '通信エラーが発生しました')
  }
}
```

---

## 15. テーマ・デザイントークン

### 15.1 CSS カスタムプロパティ

Constitution で定義されたフラットデザイン（影なし・border基調）に準拠。

```css
:root {
  /* ステータスカラー */
  --cz-status-0: #FBFBB6;  /* 作成中 (黄) */
  --cz-status-1: #BDEAAD;  /* 確認 (緑) */
  --cz-status-2: #9DBDFE;  /* 確定 (青) */
  --cz-status-9: #5D5D5D;  /* 非表示 (灰) */

  /* レイアウト */
  --cz-header-height: 56px;
  --cz-sidenav-width: 220px;
  --cz-sidenav-collapsed-width: 56px;

  /* フラットデザイン */
  --cz-border-color: #DEE2E6;
  --cz-border-radius: 4px;
  --cz-surface-card: #FFFFFF;
  --cz-surface-hover: #F8F9FA;

  /* 文字色 */
  --cz-text-primary: #212529;
  --cz-text-secondary: #6C757D;
  --cz-text-error: #DC3545;
  --cz-text-success: #28A745;
}
```

### 15.2 PrimeVue テーマカスタマイズ

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  primevue: {
    options: {
      unstyled: false,
      theme: {
        preset: 'Lara',  // フラットデザイン基調のプリセット
        options: {
          darkModeSelector: false  // ダークモード無効
        }
      }
    }
  }
})
```

---

## 16. 受け入れ基準（Given-When-Then）

### AC-01: サイドナビゲーションの権限制御

**Given** `canReport=true, canManage=false` の権限でログインしている
**When** サイドナビゲーションを表示する
**Then** 「工数入力」メニューが表示され、「工数管理」メニューが非表示になる

### AC-02: サイドナビゲーションの折畳・展開

**Given** 画面幅が 1280px 以上でサイドナビが展開状態 (220px)
**When** ハンバーガーボタンをクリックする
**Then** サイドナビが 56px に折畳まれ、状態が localStorage に保存される

### AC-03: サイドナビゲーションのレスポンシブ

**Given** 画面幅が 960px 未満でサイドナビが非表示
**When** ハンバーガーボタンをクリックする
**Then** サイドナビがオーバーレイで表示される

### AC-04: 組織選択ダイアログの単一選択

**Given** OrganizationSearchDialog が `mode='single'` で開かれている
**When** ツリーからノード「IT推進部 開発1課」を選択し [選択] をクリックする
**Then** `selected` イベントが `{ orgCode, orgName, parentOrgCode, orgLevel }` で発火し、ダイアログが閉じる

### AC-05: 組織選択ダイアログのスコープフィルタ

**Given** `scopeFilter=['100000', '100100']` で OrganizationSearchDialog を開いている
**When** ツリーが表示される
**Then** スコープ外の組織ノードがグレーアウトされ、選択不可になる

### AC-06: 組織選択ダイアログのインクリメンタルサーチ

**Given** OrganizationSearchDialog が開かれている
**When** 検索ボックスに「推進」と入力して 300ms 待機する
**Then** 「IT推進部」を含むノードがフィルタ表示され、該当箇所がハイライトされる

### AC-07: SS 選択ダイアログの検索とページネーション

**Given** SubsystemSearchDialog が `mode='target'` で開かれている
**When** 検索ボックスに「基幹」と入力し検索結果が 100 件ある場合
**Then** 50 件/ページで表示され、ページネーションで2ページ目に遷移可能

### AC-08: SS 選択ダイアログの ◆ マーカー表示

**Given** SubsystemSearchDialog のテーブルが表示されている
**When** `sysKbn === 1` のレコードがある場合
**Then** 該当行の先頭列に「◆」マーカーが表示される

### AC-09: 担当者選択ダイアログの代行モード

**Given** StaffSearchDialog が `purpose='delegation'` で開かれている
**When** 組織ツリータブで組織を選択する
**Then** `GET /delegation/available-staff` が呼び出され、代行可能な担当者のみ表示される

### AC-10: 担当者選択ダイアログの一般検索

**Given** StaffSearchDialog の検索タブが表示されている
**When** 氏名「鈴木」を入力し、一致タイプ「部分一致」で [検索] をクリックする
**Then** 「鈴木」を含む担当者が一覧に表示される

### AC-11: 確認ダイアログの OK / キャンセル

**Given** ConfirmDialog が `message='選択したレコードを削除します。よろしいですか？', severity='warn'` で表示されている
**When** [OK] をクリックする
**Then** `confirm` イベントが発火し、ダイアログが閉じる

**Given** 同条件で ConfirmDialog が表示されている
**When** [キャンセル] をクリックする
**Then** `cancel` イベントが発火し、ダイアログが閉じ、操作は実行されない

### AC-12: ステータスバッジの色分け表示

**Given** StatusBadge に `status=0` を渡す
**When** コンポーネントがレンダリングされる
**Then** ラベル「作成中」が背景色 `#FBFBB6`（黄）で表示される

**Given** StatusBadge に `status=2` を渡す
**When** コンポーネントがレンダリングされる
**Then** ラベル「確定」が背景色 `#9DBDFE`（青）で表示される

### AC-13: HoursInput の自動変換

**Given** HoursInput に `readonly=false` でフォーカスしている
**When** 「330」と入力してフォーカスアウトする
**Then** 表示値が「03:30」に自動変換される

### AC-14: HoursInput の 15 分単位バリデーション

**Given** HoursInput にフォーカスしている
**When** 「03:20」と入力してフォーカスアウトする
**Then** バリデーションエラー CZ-147（15分単位）が表示される

### AC-15: HoursInput の範囲バリデーション

**Given** HoursInput にフォーカスしている
**When** 「25:00」と入力してフォーカスアウトする
**Then** バリデーションエラー CZ-146（最大値超過）が表示される

### AC-16: MonthSelector のナビゲーション

**Given** MonthSelector の現在値が「2025年02月」で `range=12`
**When** [>>] ボタンをクリックする
**Then** 値が「2025年03月」に変更され、`update:modelValue` イベントが発火する

### AC-17: MonthSelector の範囲制限

**Given** MonthSelector の現在値が「2026年02月」（range 上限月）
**When** [>>] ボタンを確認する
**Then** [>>] ボタンが無効化（disabled）されている

### AC-18: MessageToast のエラー表示

**Given** API 呼び出しで 403 エラーが返された
**When** エラーインターセプターが処理する
**Then** 赤色の Toast が表示され、手動消去が必要（自動消去されない）

### AC-19: MessageToast の成功表示

**Given** レコード保存が成功した
**When** `useMessage().success('保存しました', 'CZ-001')` が呼ばれる
**Then** 緑色の Toast が表示され、3 秒後に自動消去される

### AC-20: DevActorSwitcher（開発環境限定）

**Given** `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH === 'true'` の開発環境
**When** DevActorSwitcher のドロップダウンで「ACT-05 部門管理者」を選択する
**Then** `POST /api/switch` が送信され、JWT が再取得され、全ストアがリセットされ、ページがリロードされる

### AC-21: DevActorSwitcher の本番非表示

**Given** `NUXT_PUBLIC_ENABLE_ACTOR_SWITCH` が未設定（本番環境）
**When** AppHeader がレンダリングされる
**Then** DevActorSwitcher コンポーネントがバンドルに含まれず、DOM に出力されない

---

## 17. テスト要件

### 17.1 コンポーネント単体テスト (Vitest)

| コンポーネント | テスト内容 |
|-------------|----------|
| AppHeader | ユーザー名表示、モードバッジ、代行バッジ、DevActorSwitcher (dev のみ) |
| AppSideNav | メニュー表示条件（権限別）、アクティブ状態、折畳/展開 |
| OrganizationSearchDialog | ツリー表示、インクリメンタルサーチ、単一/複数選択、スコープフィルタ |
| SubsystemSearchDialog | テーブル表示、検索、◆マーカー、ページネーション、mode (target/cause) |
| StaffSearchDialog | 組織ツリータブ/検索タブ切替、代行用/一般検索の切替 |
| ConfirmDialog | メッセージ表示、OK/キャンセル操作、severity 別スタイル |
| StatusBadge | 4ステータスの色分け表示 |
| HoursInput | 自動変換 (全パターン)、バリデーション、15分単位チェック |
| MonthSelector | ±12ヶ月のオプション生成、<< >> ナビゲーション、範囲外無効化 |
| MessageToast | success/warn/error/info の表示、自動消去タイマー |

### 17.2 ユーティリティテスト (Vitest)

| ユーティリティ | テスト内容 |
|-------------|----------|
| calculateByteLength | 全角/半角/半角カタカナの組み合わせ、空文字列、絵文字 |
| resolveStatusMatrix | 12状態 × 2系列 × 7操作 = 168パターン |
| buildStatusKey | STATUS 0/1/2/9 × 月次確認 true/false × 集約 true/false = 16パターン（うち有効 12 キー: spec #8 セクション 6.1） |
| parseHours | 全自動変換パターン + 無効入力 |
| useApi | JWT 自動付与、エラーインターセプター (401/403/409/500) |

### 17.3 E2E テスト (Playwright)

| シナリオ | 内容 |
|---------|------|
| サイドナビ | メニュー遷移 → 各ページロード確認、権限でメニュー項目制限 |
| 組織選択 | ダイアログ表示 → ツリー展開 → 検索 → 選択確定 |
| SS選択 | ダイアログ表示 → 検索 → ◆マーカー確認 → ページ送り → 選択 |
| 担当者選択 | 組織ツリータブ → 選択、検索タブ → 部分一致検索 → 選択 |
| レスポンシブ | 1280px → 960px → 800px でサイドナビの展開/折畳/非表示 |
| DevActorSwitcher | (dev 環境) アクター切替 → JWT 再取得 → 権限変化確認 |

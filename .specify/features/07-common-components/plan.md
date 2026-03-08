# 共通コンポーネント 実装計画

## 概要

全画面（FORM_010〜042）で共有するレイアウト・ナビゲーション・ダイアログ・入力・表示コンポーネント群の実装計画。
現行 MPA の FRAMESET 入れ子構造（40+ フレーム）+ `window.open()` ポップアップを、
Nuxt.js 3 SPA の CSS Grid レイアウト + PrimeVue モーダルに移行する。

対象 GAP: GAP-N01（FRAMESET → CSS Grid）、GAP-N02（メインメニュー → SPA サイドナビ）、
GAP-N05（パンくず追加）、GAP-D01〜05（window.open() → SPA モーダル）。

---

## アーキテクチャ

### フロントエンド

#### コンポーネント一覧（11件）

| # | コンポーネント | ディレクトリ | 概要 |
|---|--------------|-------------|------|
| 1 | AppHeader.vue | components/layout/ | ヘッダー。アプリ名・ユーザー名・モード表示・代行表示 |
| 2 | AppSideNav.vue | components/layout/ | サイドナビ。権限別メニュー、折畳/展開（localStorage永続化）、レスポンシブ3段階 |
| 3 | AppBreadcrumb.vue | components/layout/ | パンくずナビゲーション。ルート定義に基づく自動生成 |
| 4 | StatusBadge.vue | components/common/ | 12状態ステータスの色分け表示。`buildStatusKey`（000〜911）連携 |
| 5 | HoursInput.vue | components/common/ | HH:MM 時間入力。1〜4桁自動変換（BR-006）、15分単位制約 |
| 6 | MonthSelector.vue | components/common/ | 年月セレクタ。±12ヶ月ドロップダウン、`<<`/`>>`ナビ |
| 7 | OrganizationSearchDialog.vue | components/common/ | 組織選択モーダル（DLG_001/002）。PrimeVue Tree |
| 8 | SubsystemSearchDialog.vue | components/common/ | SS選択モーダル（DLG_003/004）。PrimeVue DataTable |
| 9 | StaffSearchDialog.vue | components/common/ | 担当者選択モーダル（DLG_005）。2タブ構成 |
| 10 | ConfirmDialog.vue | components/common/ | 確認ダイアログ（CZ-500〜799） |
| 11 | MessageToast.vue | components/common/ | PrimeVue Toast ラッパー |

#### Composables（5件）

| # | Composable | 概要 |
|---|-----------|------|
| 1 | useStatusMatrix | 12状態 × 2系列のボタン制御解決 |
| 2 | useHoursFormat | HH:MM パース・変換・バリデーション |
| 3 | useConfirmDialog | CZ-5xx 確認ダイアログ管理（Promise ベース） |
| 4 | useMessage | Toast 通知管理（CZ コード範囲自動 severity 判定） |
| 5 | useApi | API クライアント共通（JWT 自動付与、エラーインターセプター） |

#### ユーティリティ（2件）

| # | ユーティリティ | 概要 |
|---|-------------|------|
| 1 | utils/statusMatrix.ts | `buildStatusKey()` + `resolveStatusMatrix()` |
| 2 | utils/byteLength.ts | CZ 仕様バイト長計算（全角2B、半角1B、半角カタカナ2B） |

### バックエンド

該当なし。API エンドポイントは spec #3 で定義済み。

### データベース

該当なし。

---

## 実装方針

### HoursInput — 時間入力自動変換

- 空文字列 → バリデーションエラー（VR-008）。`00:00` への自動変換は行わない
- 1桁（例: `3`）→ `03:00`
- 2桁（例: `12`）→ `12:00`
- 3桁（例: `330`）→ `03:30`
- 4桁（例: `0330`）→ `03:30`
- コロン付き（例: `3:30`）→ `03:30`
- バリデーション: 15分単位（CZ-147）、最小0:15（CZ-129）、最大24:00（CZ-146）
- `blur` イベントで自動変換+バリデーション実行

### StatusBadge — 12状態ステータス表示

- `buildStatusKey(recordStatus, getsujiKakutei, dataSyuukei)` で3桁キー構築
- status=9 は「緊急停止」として灰色背景・白文字
- 有効12状態: 000, 010, 100, 110, 200, 210, 211, 900, 910, 911, 001, 011

### レイアウト基盤

- CSS Grid で AppHeader + AppSideNav + NuxtPage
- ヘッダー 56px 固定、サイドナビ 220px / 折畳時 56px
- レスポンシブ 3段階: >= 1280px（展開）、960-1279px（折畳）、< 960px（オーバーレイ）

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | FRAMESET→CSS Grid、window.open()→PrimeVue Dialog、12状態マトリクス完全再現、HoursInput 変換ルール踏襲 |
| II. 仕様完全性 | ✅ | spec.md セクション 1〜16 で全コンポーネントの Props/Events/動作仕様を定義済み |
| IV. TDD | ✅ | buildStatusKey 16パターン、resolveStatusMatrix 168パターン、HoursInput 全変換パターン+境界値 |
| V. UX-First | ✅ | PrimeVue Lara プリセット、elevation="0" + border、Slate-700 テキスト、レスポンシブ3段階 |
| VI. Production Safety | ✅ | DevActorSwitcher の三重安全策（環境変数ゲート + Tree Shaking + CI grep） |
| IX. 最適技術選定 | ✅ | PrimeVue 4.x API 準拠、過度な抽象化回避 |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| StatusBadge: 12状態 + status=9 緊急停止 | 高 | buildStatusKey 全16パターンのパラメタライズドテスト |
| HoursInput: 複雑な自動変換アルゴリズム | 高 | テーブル駆動テストで全パターン検証。現行 Java ソースと照合 |
| SubsystemSearchDialog: 大規模データセット | 中 | サーバーサイドページネーション（50件/ページ）+ 300ms デバウンス |
| PrimeVue 4.x API 互換性 | 中 | 公式ドキュメントで破壊的変更確認、バージョン固定 |
| ステータスマトリクスの FE/BE 二重実装 | 中 | テスト結果照合 + E2E 統合検証 |

---

## 依存関係

### 依存先

| spec | 依存内容 |
|------|---------|
| #2 認証基盤 | CzPrincipal（権限・代行情報）、DevActorSwitcher の JWT 再取得 |
| #8 バリデーション・エラー体系 | CZ エラーコード体系、HoursInput バリデーションルール |

### 依存元

| spec | 依存内容 |
|------|---------|
| #4 工数入力 | HoursInput, StatusBadge, MonthSelector, ConfirmDialog, SubsystemSearchDialog |
| #5 工数一覧 | StatusBadge, MonthSelector, ConfirmDialog, StaffSearchDialog, useStatusMatrix |
| #6 分析画面 | AppBreadcrumb, SubsystemSearchDialog, OrganizationSearchDialog, MonthSelector |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| コンポーネント | 11 | レイアウト3 + 共通8 |
| Composables | 5 | useApi / useMessage / useHoursFormat / useConfirmDialog / useStatusMatrix |
| ユーティリティ | 2 | statusMatrix.ts / byteLength.ts |
| CSS/テーマ | 1 | デザイントークン + PrimeVue カスタマイズ |
| テスト（Vitest） | ~15 | StatusMatrix 168パターン + buildStatusKey 16 + HoursInput 15 + byteLength 10 + 各コンポーネント25 |
| テスト（Playwright E2E） | 6 | サイドナビ / 組織選択 / SS選択 / 担当者選択 / レスポンシブ / DevActorSwitcher |
| **合計** | **~40** | |

---

## 実装順序

| 順序 | 対象 | 理由 |
|------|------|------|
| 1 | ユーティリティ（statusMatrix.ts, byteLength.ts） | 依存なし。テストファースト |
| 2 | Composables（useApi, useMessage, useHoursFormat, useConfirmDialog, useStatusMatrix） | ユーティリティに依存 |
| 3 | テーマ・デザイントークン | 全コンポーネントの見た目に影響 |
| 4 | レイアウト基盤（AppHeader, AppSideNav, AppBreadcrumb） | 画面の骨格 |
| 5 | 基本表示（StatusBadge, MessageToast） | 単純な表示系 |
| 6 | 入力（HoursInput, MonthSelector） | バリデーションロジック含む |
| 7 | ダイアログ群（Organization/Subsystem/Staff SearchDialog, ConfirmDialog） | API 連携あり |

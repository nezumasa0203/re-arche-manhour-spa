# #3 Core API Design — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `03-core-api-design/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1/#2/#4-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| RESTful API 設計 | ✅ 合致 | constitution II「API スタイル: REST」。spec: リソース指向 REST 設計 |
| Doma 2 ORM | ✅ 合致 | constitution 技術スタック「Doma 2 + 2Way SQL」。spec: セクション 8 で repository/ (Doma 2 DAO), entity/ (Doma 2 Entity) を定義 |
| PostgreSQL 16 | ✅ 合致 | spec の SQL 構文（`SELECT FOR UPDATE`, `WHERE IN`）が PostgreSQL 準拠 |
| Docker-First | ✅ 合致 | constitution III「docker compose up で完結」。API はコンテナ上の Backend サービスとして動作 |
| TDD / テスト要件 | ✅ 合致 | constitution IV「ステータスマトリクス全パターンテスト」。spec セクション 9 で 168 パターンテスト明記 |
| エラーコード体系 | ✅ 合致 | constitution I「CZ-000〜999 踏襲」。spec セクション 1.3 で完全踏襲 |
| 4層権限モデル | ✅ 合致 | constitution VII「4層権限モデル」。spec セクション 5 でデータアクセス制御を完全実装 |
| 12状態ステータスマトリクス | ✅ 合致 | constitution I「12状態マトリクス完全再現必須」。spec セクション 4.2 で StatusMatrixResolver 使用 |
| Production Safety | ✅ 合致 | API 設計がプロファイル非依存。認証は AlbOidcAuthFilter に委譲（dev/prod 分離は #2 の管轄） |
| UX-First | ✅ 合致 | constitution V「操作フィードバック」。spec: エラーレスポンスに field/params を含め、Frontend で具体的フィードバック表示可能 |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| 88 Unit / 139 JSP → SPA | MPA 構成の詳細記述 | セクション概要「88 Unit / 139 JSP → 4 SPA ページ + 5 モーダル」| ✅ 合致 |
| トランザクションテーブル操作 | TCZ01 CRUD + MCZ04 制御 | 全 CRUD エンドポイント定義 | ✅ 合致 |
| 4層権限モデルの API 反映 | Layer 1-4 の構造定義 | セクション 5 でデータアクセス制御に反映 | ✅ 合致 |
| ステータス遷移 | STATUS 0→1→2 + 9 | セクション 3.6 (0→1), 3.8 (1→2), 3.9 (月次制御) | ✅ 合致 |
| 半期推移・月別内訳 | FORM_030-042 の集計構造 | セクション 2.4/2.5 + 3.10-3.12 で REST 化 | ✅ 合致 |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 15 アクター対応 | ✅ 合致 | セクション 9.2 で ACT-01/03/09/10/13 を例示した権限テスト定義 |
| TAB 010 bit2 → 系列切替 | ✅ 合致 | セクション 4.2「tab010.bit2 判定 → 担当者/管理者系列選択」 |
| 相対権限 (201/202/211) | ✅ 合致 | セクション 5.2 で dataAuthority.ref/ins/upd を操作別にチェック |
| 組織階層 7段階 | ✅ 合致 | セクション 5.1 で OrganizationScopeResolver が組織コードリスト解決 |
| 代行モード | ✅ 合致 | セクション 5.3 で X-Delegation-Staff-Id + isAllowedStaff() 定義 |
| 12状態ステータスマトリクス | ✅ 合致 | セクション 4.2 + 9.3「12状態 × 2系列 × 7操作 = 168 パターン」 |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 45 US → API 対応 | ✅ 合致 | セクション 2 の全エンドポイントに US-ID を紐付け済み |
| VR-001〜012, VR-014 | ✅ 合致 | セクション 3.2 バリデーション表に全ルール記載 |
| VR-013 (依頼者氏名 40文字) | ⚠️ 欠落 | セクション 3.2 POST バリデーション表に未記載 → **FIX-C02** |
| VR-015 (カテゴリ重複 CZ-132) | ⚠️ 欠落 | セクション 3.2 POST バリデーション表に未記載 → **FIX-C02** |
| BR-001〜007 | ✅ 合致 | セクション 4.1 で全ルールを API 実装に対応付け |
| エラーコード CZ-000〜999 | ✅ 合致 | セクション 1.3 でレンジ定義、各 VR/BR で個別コード参照 |

### 04_screen_transition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 18画面 + 10ダイアログ → SPA統合 | ✅ 合致 | 概要「4 SPA ページ + 5 モーダル」が screen_transition の統合提案と一致 |
| FORM_010 → work-hours API | ✅ 合致 | セクション 2.2 で 10 エンドポイント定義 |
| FORM_020 → work-status API | ✅ 合致 | セクション 2.3 で 8 エンドポイント定義 |
| FORM_030-032 → half-trends API | ✅ 合致 | セクション 2.4 で 4 エンドポイント定義（3階層ドリルダウン） |
| FORM_040-042 → monthly-breakdown API | ✅ 合致 | セクション 2.5 で 5 エンドポイント定義（4種 Excel 出力含む） |
| DLG_001-005 → masters API | ✅ 合致 | セクション 2.7 で 7 エンドポイント定義 |

### 05_gap_analysis.md との整合

| GAP-ID | 区分 | spec での対応 | 結果 |
|---|---|---|---|
| GAP-F10-01 | IMPROVE | Ajax インライン編集 → PATCH /work-hours/{id} | ✅ セクション 3.3 |
| GAP-F10-02 | IMPROVE | コピー機能 → POST /work-hours/copy | ✅ セクション 3.4 |
| GAP-F10-03 | IMPROVE | 一括確認 → POST /work-hours/batch-confirm | ✅ セクション 3.6 |
| GAP-F10-04 | ADD | 翌月転写 → POST /work-hours/transfer-next-month | ✅ セクション 3.5 |
| GAP-F10-09 | IMPROVE | ソート → sort パラメータ | ✅ セクション 3.1 |
| GAP-F20-01 | IMPROVE | ステータスフィルタ → statusFilter パラメータ | ✅ セクション 3.7 |
| GAP-F20-03 | IMPROVE | インライン工数編集 → PATCH /work-status/{id}/hours | ✅ セクション 2.3 |
| GAP-F30-01 | IMPROVE | 3階層ドリルダウン → categories/systems/subsystems | ✅ セクション 2.4 |
| GAP-F30-04 | ADD | コスト表示切替 → displayMode パラメータ | ✅ セクション 3.10 |
| GAP-E01 | IMPROVE | HSSF→XSSF | ✅ セクション 7「Apache POI (XSSF) で .xlsx 生成」|
| GAP-E02 | KEEP | 6テンプレート | ✅ セクション 7.1 で 6 テンプレート定義 |
| GAP-B01 | IMPROVE | Oracle→PostgreSQL | ✅ Doma 2 DAO + PostgreSQL 準拠 SQL |
| GAP-B04 | IMPROVE | API 設計 REST 化 | ✅ 全エンドポイント RESTful 設計 |
| GAP-D07 | IMPROVE | Excel .xls→.xlsx | ✅ セクション 7 で XSSF 使用 |

### 06_devenv_infrastructure.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| Backend コンテナ (Spring Boot, :8080) | ✅ 合致 | API は Backend コンテナ上で動作 |
| ORM 選択 | ℹ️ 注意 | 06_devenv セクション 1.2 に「Spring Data JPA + Hibernate」記載あり。**constitution で Doma 2 を確定済み**であり、spec セクション 8 も Doma 2 準拠。06_devenv の記載は分析時点の選択肢であり、constitution が権威。（#1, #2 と同一の既知事項）|
| Auth Mock 連携 | ✅ 合致 | セクション 1.1 の認証ヘッダー仕様が 06_devenv の ALB 模倣構成と整合 |
| DB 初期化 | ✅ 合致 | API が参照するテーブル群は #1 database-schema で定義済み |

---

## C. 他 Spec (#1, #2, #4-#10) との整合性

### 検出された不整合

| # | 不整合内容 | 重要度 | FIX-ID | 関連 |
|---|---|---|---|---|
| 1 | ~~セクション 2 に記載の 7 つの状態変更エンドポイントがセクション 3 に詳細定義なし~~ | ~~🔴 P0~~ | FIX-C01 ✅ | 内部不整合 + #4/#5 |
| 2 | ~~VR-013（依頼者氏名 40文字）と VR-015（カテゴリ重複 CZ-132）がセクション 3.2 の POST バリデーション表に欠落~~ | ~~🟡 P1~~ | FIX-C02 ✅ | 03_user_stories |
| 3 | ~~spec #2 FIX-A07 で定義済みのセマンティックエイリアスが未使用。旧命名 `canUseSbt012_0bit()` / raw bit 参照のまま~~ | ~~🟡 P1~~ | FIX-C03 ✅ | #2 auth-infrastructure |
| 4 | ~~セクション 3.6 のバッチエラーレスポンスに `recordId` があるが、セクション 1.3 の共通エラー構造に未定義~~ | ~~🟢 P2~~ | FIX-C04 ✅ | 内部不整合 + #8 |

### 不整合の詳細

#### FIX-C01: 7 つの状態変更エンドポイントに詳細定義なし

セクション 2 のエンドポイント一覧に記載されているが、セクション 3 に詳細（リクエストボディ、バリデーション、権限チェック、レスポンス）が存在しない:

| エンドポイント | US | 不足内容 |
|---|---|---|
| DELETE `/work-hours` | US-014 | リクエストボディ（ID 配列）、ステータスチェック（STATUS_0 のみ削除可）、権限チェック |
| POST `/work-hours/batch-revert` | US-016 | STATUS_1→0 の戻し条件、リクエスト/レスポンス形式 |
| PATCH `/work-status/{id}/hours` | US-026 | インライン工数編集のフィールド定義、バリデーション、権限チェック |
| POST `/work-status/revert` | US-025 | STATUS_2→1 の承認取消条件、リクエスト/レスポンス形式 |
| POST `/work-status/monthly-aggregate` | US-023 | DataSyuukeiFlg=1 の設定処理、権限（`canAggregate`）、リクエスト/レスポンス形式 |
| POST `/work-status/monthly-unconfirm` | US-021 | GetsujiKakuteiFlg=0 への戻し処理、権限チェック、リクエスト/レスポンス形式 |
| POST `/delegation/switch` | US-01C | 代行モード切替のリクエスト/レスポンス形式、`canDelegate` 権限検証フロー |

spec #4 (work-hours-input) は DELETE /work-hours の挙動を参照し、spec #5 (work-hours-status) は revert / monthly-aggregate / monthly-unconfirm の挙動を参照するため、実装時の曖昧さを排除する必要がある。

#### FIX-C02: VR-013 / VR-015 が POST バリデーション表に欠落

| ルール | 内容 | エラーコード | 現状 |
|---|---|---|---|
| VR-013 | 作業依頼者名は40文字以内 | chkChara 00000 | セクション 3.3 PATCH では `workRequesterName` フィールドに VR-013 を参照しているが、セクション 3.2 POST の表に欠落 |
| VR-015 | 同一サブシステム＋同一件名で異なるカテゴリは不可 | CZ-132 | セクション 3.2 POST、セクション 3.3 PATCH の両方に未記載 |

03_user_stories.md で定義された VR-015（カテゴリ重複チェック）は既存システムの重要なビジネスルールであり、POST / PATCH 両方で検証が必要。

#### FIX-C03: セマンティックエイリアスの未使用

spec #2 セクション 6.1.1 で `CzPermissions セマンティックエイリアス` が定義済み（FIX-A07）。
spec #3 の以下の箇所が旧命名のまま:

| 箇所 | 現在の記述 | セマンティックエイリアス |
|---|---|---|
| セクション 3.9 (月次確認 権限) | `canUseSbt012_0bit() = true` | `canInputPeriod()` |
| セクション 4.2 (ステータス制御フロー) | `tab010.bit2 判定` | `canFullAccess() 判定` (系列選択: `useTanSeries()`) |
| セクション 5.2 (承認/戻し) | `tab010.bit1 or tab010.bit2` | `canManage() or canFullAccess()` |
| セクション 5.2 (月次確認) | `tab012.bit0` | `canInputPeriod()` |
| セクション 5.2 (月次集約) | `tab012.bit1` | `canAggregate()` |

セマンティックエイリアスの使用により、開発者が bit 番号を意識せずに権限判定コードを理解でき、spec #2 との一貫性も確保される。

#### FIX-C04: バッチエラーレスポンスの共通構造未定義

セクション 1.3 の共通エラーレスポンス構造:
```json
{ "error": { "code": "CZ-126", "message": "...", "field": "workDate", "params": ["作業日"] } }
```

セクション 3.6 (batch-confirm) のエラーレスポンス:
```json
{ "error": { "code": "CZ-126", "message": "...", "recordId": 12345, "field": "workDate" } }
```

`recordId` フィールドはバッチ処理でエラー行を特定するために必要だが、セクション 1.3 の共通構造に含まれていない。spec #8 (validation-error-system) でエラー構造を統一定義する際に整合を取る必要がある。

### 整合している点

| 項目 | 結果 |
|---|---|
| spec #1 (database-schema) — 全テーブル名/カラム名が entity/ マッピングと一致 | ✅ |
| spec #1 (database-schema) — 楽観ロック updatedAt (CZ-101) の設計が一致 | ✅ |
| spec #2 (auth-infrastructure) — AlbOidcAuthFilter + CzPrincipal + CzSecurityContext の参照 | ✅ |
| spec #2 (auth-infrastructure) — StatusMatrixResolver の security/util 配置が一致 | ✅ |
| spec #2 (auth-infrastructure) — OrganizationScopeResolver の設計が一致 | ✅ |
| spec #2 (auth-infrastructure) — X-Delegation-Staff-Id ヘッダー名が一致 (FIX-A04 で追加) | ✅ |
| spec #4 (work-hours-input) — POST/PATCH/copy/transfer のエンドポイント設計参照 | ✅ |
| spec #5 (work-hours-status) — work-status API のエンドポイント設計参照 | ✅ |
| spec #6 (analysis-screens) — half-trends / monthly-breakdown の 3 階層ドリルダウン構造 | ✅ |
| spec #6 (analysis-screens) — displayMode (hours/cost) 切替パラメータ | ✅ |
| spec #7 (common-components) — API レスポンスの permissions 構造を Frontend で消費 | ✅ |
| spec #8 (validation-error-system) — CZ エラーコード体系 (CZ-000〜999) | ✅ |
| spec #9 (excel-export) — 6 テンプレートのエンドポイント定義 | ✅ |
| spec #10 (batch-processing) — バッチは API 経由でなく直接 DB 操作（分離設計） | ✅ |

---

## D. 旧システムとの仕様整合性・変更点まとめ

### KEEP（踏襲）— 変更なし

| # | 項目 | 旧 | 新 | 根拠 |
|---|---|---|---|---|
| 1 | CZ エラーコード体系 (CZ-000〜999) | 5レンジ分類 (成功/警告/システム/確認/情報) | 同一のレンジ分類 | GAP-B04 |
| 2 | バリデーションルール VR-001〜015 | 15ルール | 同一の15ルール (API バリデーション化) | 03_user_stories |
| 3 | ビジネスルール BR-001〜007 | 7ルール (サービス時間/15分単位/日次上限等) | 同一の7ルール | 03_user_stories |
| 4 | 12状態ステータスマトリクス | sts_base_key 000-911 × 2系列 | StatusMatrixResolver で同一マトリクス | GAP-R02 |
| 5 | 4層権限モデルによる操作可否 | Layer 1-4 でボタン表示制御 | 同一の4層モデル → API permissions レスポンス | GAP-R01 |
| 6 | 年度・半期ルール | 2014以前/2015特殊/2016以降 | FiscalYearResolver で同一ルール | BL-016〜018 |
| 7 | 禁止ワードチェック (12語) | HOSYU_SYUBETU_0 判定 | ValidationService で同一の12語チェック | BL-009 |
| 8 | バイト長計算 (全角2/半角1/半角カナ2) | chkChara 関数 | ValidationService で同一ルール | VR-006 |
| 9 | 15分単位工数入力 | 00/15/30/45 固定 | API バリデーションで同一制約 | BR-002 |
| 10 | MCZ04CTRLMST 月次制御 | GetsujiKakuteiFlg / DataSyuukeiFlg | 同一のフラグ制御 | BL-020〜022 |
| 11 | Excel 出力 6テンプレート | POI (HSSF) で生成 | POI (XSSF) で同一テンプレート構成 | GAP-E02 |
| 12 | 代行モード | isDaiko() + isAllowedStaff() | X-Delegation-Staff-Id + isAllowedStaff() | BL-027 |
| 13 | コピー機能 (STATUS→0, SEQNO→新規) | InsertListJspBean 内の処理 | POST /work-hours/copy で同一動作 | US-012 |
| 14 | 翌月転写 (カテゴリ存在確認) | InsertListJspBean 内の処理 | POST /work-hours/transfer-next-month で同一動作 | US-013 |
| 15 | 同時編集検知 | DBアクセス時の排他制御 | 楽観ロック updatedAt + MCZ04 SELECT FOR UPDATE | GAP-A05 |

### IMPROVE（改善）— 技術的改善

> 下表の「影響度」は移行作業の規模を示す指標であり、未解決の問題ではありません。

| # | 項目 | 旧 (Java MPA) | 新 (SPA + REST API) | 影響度 | GAP-ID |
|---|---|---|---|---|---|
| 1 | **API アーキテクチャ** | 88 Struts Unit/Action (アクション指向) | 35 REST エンドポイント (リソース指向) | 🔴 HIGH | GAP-B04 |
| 2 | **レスポンス形式** | 139 JSP (サーバーサイド HTML) | JSON レスポンス (data + meta 構造) | 🔴 HIGH | GAP-B04 |
| 3 | **データ送信** | フォーム POST (同期) | Ajax/Fetch API (非同期) | 🔴 HIGH | GAP-F10-01 |
| 4 | **インライン編集** | ページ全体リロード | PATCH リクエスト (フィールド単位) | 🟡 MID | GAP-F10-01 |
| 5 | **一括操作** | JSP 上のチェックボックス + ActionForward | POST /batch-confirm, /batch-revert (JSON 配列) | 🟡 MID | GAP-F10-03 |
| 6 | **ステータス制御** | InsertListJspBean 内のインライン判定 | StatusMatrixResolver 共通サービス | 🟡 MID | — |
| 7 | **年度半期解決** | JSP 内のインライン計算 | FiscalYearResolver 共通サービス | 🟢 LOW | — |
| 8 | **組織スコープ** | MprSecurityInfo.getAllowedOrganizationCodes() | OrganizationScopeResolver → SQL WHERE IN | 🟡 MID | — |
| 9 | **バリデーション** | Action 内 + JSP 内分散実装 | ValidationService 集約 (VR-001〜015 + 禁止ワード) | 🟡 MID | — |
| 10 | **Excel 出力** | POI HSSF (.xls) | POI XSSF (.xlsx) | 🟢 LOW | GAP-D07 |
| 11 | **同時編集検知** | DB 排他 (実装不統一) | 全 PATCH/POST に updatedAt 楽観ロック統一 | 🟡 MID | GAP-A05 |
| 12 | **ページング** | JSP 独自実装 | page/pageSize パラメータ + meta.totalCount | 🟢 LOW | GAP-F20-01 |
| 13 | **ソート** | サーバーサイド固定 | sort パラメータ (例: `workDate:asc,status:desc`) | 🟢 LOW | GAP-F10-09 |
| 14 | **ドリルダウン** | 画面遷移 (FORM_030→031→032) | 3階層 API (categories→systems→subsystems) | 🟡 MID | GAP-F30-01 |

### ADD（追加）

| # | 項目 | 詳細 | 根拠 |
|---|---|---|---|
| 1 | **REST API 層** | `/api/v1` ベースパス、バージョニング対応 | GAP-B04 |
| 2 | **共通 JSON レスポンス構造** | `data` + `meta` (totalCount, page, pageSize) | REST 設計標準 |
| 3 | **共通エラーレスポンス構造** | `error` (code, message, field, params) | REST 設計標準 |
| 4 | **楽観ロック統一** | 全更新 API で updatedAt ベース + 409 Conflict (CZ-101) | GAP-A05 |
| 5 | **StatusMatrixResolver** | 12状態×2系列のマトリクス解決を共通サービス化 | spec #2 |
| 6 | **FiscalYearResolver** | 年度半期の月リスト・基準日解決サービス | spec #3 |
| 7 | **OrganizationScopeResolver** | JWT 権限→許可組織コードリスト→SQL WHERE IN 生成 | spec #2 |
| 8 | **ValidationService** | VR-001〜015 + 禁止ワード + バイト長を集約 | spec #3 |
| 9 | **permissions レスポンス** | 各操作の可否を API レスポンスに含め、Frontend で即座に UI 反映 | UX-First |
| 10 | **monthControl レスポンス** | 月次制御状態をレスポンスに含め、Frontend でステータス表示 | UX-First |
| 11 | **displayMode パラメータ** | 半期推移/月別内訳で工数/コスト表示切替 | GAP-F30-04 |
| 12 | **filterType パラメータ** | 全部/指定/MY のフィルタリング | GAP-F30-05 |
| 13 | **Backend パッケージ設計** | controller/service/repository/entity/dto/security/config 7層構造 | constitution X |

### REMOVE（削除）

| # | 項目 | 旧の概要 | 削除理由 | GAP-ID |
|---|---|---|---|---|
| 1 | **88 Struts Unit/Action** | アクション指向の画面遷移制御 | REST API に置換 | GAP-B04 |
| 2 | **139 JSP ビュー** | サーバーサイド HTML レンダリング | JSON レスポンスに置換 (SPA が描画) | GAP-B04 |
| 3 | **ActionForward ディスパッチ** | `*.do` URL パターン + forward 定義 | HTTP メソッド + リソース URL に置換 | GAP-B04 |
| 4 | **セッションベース状態管理** | HttpSession にユーザー情報・画面状態格納 | JWT ステートレス + SPA クライアントステート | GAP-A02 |
| 5 | **ESQID** | マルチウィンドウ識別子 (URLパラメータ) | SPA 単一ウィンドウで不要 | — |
| 6 | **InsertListJspBean** | 工数入力画面のビューヘルパー (ステータス/権限/表示制御) | StatusMatrixResolver + permissions レスポンスに分離 | — |
| 7 | **サーバーサイドフォームレンダリング** | form タグ + hidden フィールドによるステート保持 | JSON リクエスト/レスポンスに置換 | — |
| 8 | **前月/翌月ページ submit** | form submit + ActionForward で月切替 | yearMonth クエリパラメータで非同期取得 | GAP-F10-05 |
| 9 | **SecurityRoleInfo セッション属性** | セッション格納のロール情報 | CzPrincipal (ThreadLocal) に置換 | GAP-A01 |

### 注意が必要な移行ポイント

| # | ポイント | 詳細 |
|---|---|---|
| 1 | **URL 設計の抜本変更** | 旧: `InsertList.do?action=save&esqid=...` → 新: `POST /api/v1/work-hours`。88 アクション URL の全面リデザイン |
| 2 | **レスポンス形式の変換** | JSP 変数バインディング → JSON フィールド。全画面項目の JSON マッピング設計が必要 |
| 3 | **ステータスマトリクスの API 表現** | 旧: JSP で直接ボタン表示/非表示。新: permissions レスポンスで可否フラグを返し、Frontend で解釈 |
| 4 | **バッチ処理との境界** | 旧: 同一アプリ内で直接呼び出し。新: #10 batch-processing は API 経由でなく直接 DB 操作する分離設計 |
| 5 | **Excel 出力のストリーミング** | 大量データ時のメモリ管理。XSSF はメモリ集約的なため、SXSSFWorkbook の採用を検討 |

---

## E. 推奨アクション

> **全4件完了** ✅（P0: 1件 / P1: 2件 / P2: 1件）

| ID | 優先度 | ステータス | アクション | 修正箇所 |
|---|---|---|---|---|
| FIX-C01 | P0 | ✅ 完了 | 7 つの状態変更エンドポイント（DELETE /work-hours, batch-revert, work-status/revert 等）の詳細定義をセクション 3 に追加 | spec セクション 3.15〜3.21 |
| FIX-C02 | P1 | ✅ 完了 | VR-013（依頼者氏名 40文字）と VR-015（カテゴリ重複 CZ-132）をセクション 3.2 POST バリデーション表に追加 | spec セクション 3.2 |
| FIX-C03 | P1 | ✅ 完了 | セクション 3.9 / 4.2 / 5.2 の旧命名（`canUseSbt012_0bit()`, `tab010.bit2`）をセマンティックエイリアス（`canInputPeriod()`, `canFullAccess()` 等）に更新 | spec セクション 3.9, 4.2, 5.2 |
| FIX-C04 | P2 | ✅ 完了 | セクション 1.3 共通エラー構造にバッチ操作向け拡張（`recordId` フィールド）を明記 | spec セクション 1.3 |

---

## F. 修正履歴

| 日時 | FIX-ID | 修正内容 | 修正者 |
|---|---|---|---|
| 2026-02-26 | FIX-C01 | セクション 3.15〜3.21 を新規追加。7つの状態変更エンドポイントの詳細定義: DELETE /work-hours（一括削除、STATUS_0 制約）、POST /work-hours/batch-revert（STATUS_1→0 一括戻し）、PATCH /work-status/{id}/hours（インライン工数編集 + VR-008/009/010/BR-007）、POST /work-status/revert（STATUS_2→1 承認取消）、POST /work-status/monthly-aggregate（DataSyuukeiFlg=1 + canAggregate 権限）、POST /work-status/monthly-unconfirm（GetsujiKakuteiFlg=0 + canInputPeriod 権限）、POST /delegation/switch（代行モード切替 + canDelegate 権限 + isAllowedStaff 検証） | Claude |
| 2026-02-26 | FIX-C02 | セクション 3.2 POST バリデーション表に VR-013（作業依頼者名 40文字以内、chkChara）と VR-015（同一サブシステム＋同一件名でのカテゴリ重複不可、CZ-132）を追加 | Claude |
| 2026-02-26 | FIX-C03 | セクション 3.9 の `canUseSbt012_0bit()` → `canInputPeriod()`、セクション 4.2 の `tab010.bit2 判定` → `canFullAccess() 判定 + useTanSeries`、セクション 5.2 の `tab010.bit1 or tab010.bit2` → `canManage() or canFullAccess()`、`tab012.bit0` → `canInputPeriod()`、`tab012.bit1` → `canAggregate()` に更新。spec #2 FIX-A07 で定義済みのセマンティックエイリアスと統一 | Claude |
| 2026-02-26 | FIX-C04 | セクション 1.3 に バッチ操作向け拡張エラーレスポンス構造を追加。`recordId` フィールドによるエラー発生行の特定を共通仕様として明記 | Claude |

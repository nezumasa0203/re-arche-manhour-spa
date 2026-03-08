# constitution.md 作成依頼プロンプト

> 以下のプロンプトをそのまま spec-kit に入力してください。

---

## プロンプト本文

```
/speckit.init

## プロジェクト概要

本プロジェクトは、既存の Java MPA システム「CZ（保有資源管理システム）」を
SPA + REST API にマイグレーション（移行）するプロジェクトです。
新規開発ではなく、稼働中のレガシーシステムの機能をモダン技術スタックで再構築します。

既存システムの分析は完了しており、以下の6つの分析ドキュメントが入力情報として存在します:

- 01_system_analysis.md    : 現行システム全体分析（技術スタック、ファイル構成、アーキテクチャ）
- 02_actor_definition.md   : アクター・権限定義（4層権限モデル、15アクター、ソースコード根拠付き）
- 03_user_stories.md       : ユーザーストーリー38件、ビジネスルール、エラーメッセージカタログ92件
- 04_screen_transition.md  : 画面一覧・遷移図・ワイヤーフレーム・SPA統合提案
- 05_gap_analysis.md       : 新旧差異比較表69件（KEEP/IMPROVE/ADD/REMOVE分類）、SPA追加機能8提案
- 06_devenv_infrastructure.md : Docker開発環境、認証モック、CIパイプライン、AWSインフラ定義

これらのドキュメントに記載された仕様・方針・技術選定をすべて尊重し、
本マイグレーションプロジェクトの constitution.md を作成してください。


## 技術スタック（確定済み）

| レイヤー       | 技術                              | 備考                          |
|---------------|-----------------------------------|-------------------------------|
| Frontend      | Nuxt.js 3 (TypeScript必須)        | SPA モード                    |
| Backend       | Spring Boot 3 (Java 21, Gradle)   | REST API                      |
| Batch         | Spring Batch 5                    | 既存13バッチSQL移行先         |
| Database      | PostgreSQL 16                     | Oracle からの移行             |
| Cache         | Redis 7                           | パラメータキャッシュ          |
| 認証           | ALB + Okta OIDC                   | JWT トークン                  |
| インフラ       | AWS ALB → ECS Fargate → RDS      | VPC内/閉域網                  |
| 開発環境       | Docker + .devcontainer            | compose up のみで完結         |
| 認証モック     | Express (Node.js)                 | ALB+Okta模倣, アクター切替UI |
| CI            | GitHub Actions (2系統)            | Production / Staging 分離     |
| テスト (BE)   | JUnit 5 + Testcontainers          |                               |
| テスト (FE)   | Vitest + Vue Test Utils + Playwright |                            |
| 仕様管理       | spec-kit                          | 仕様駆動開発                  |


## constitution.md に含めるべき原則（Core Principles）

以下の原則を必ず含めてください。順序と構成は最適化して構いません。

### 原則1: マイグレーション・ファースト（Migration-First）— 最重要原則

このプロジェクトは「新規開発」ではなく「既存システムの移行」です。
以下を constitution の最上位原則として定めてください:

- **現行仕様は正（Single Source of Truth）**: 01〜06の分析ドキュメントに記載された
  現行仕様（ビジネスルール、ステータス遷移、権限モデル、バリデーション等）は
  新システムでも100%踏襲する。変更する場合は差異管理表（05_gap_analysis.md）に記録し、
  業務部門の承認を必須とする
- **踏襲必須ビジネスロジック**: 05_gap_analysis.md セクション4に定義された28ルール
  （時間工数7件、ステータス制御6件、年度期間3件、入力制約7件、エラーメッセージ体系5件）は
  変更禁止。テストで100%カバーすること
- **12状態ステータスマトリクス**: sts_base_key（000〜911）の12状態 × 担当者/管理者系列の
  ボタン制御は完全再現必須。全パターンのユニットテストを実装すること
- **エラーメッセージ体系の保全**: CZ-000〜CZ-999 のエラーコード体系は踏襲。
  新システムのAPIレスポンスにも同一エラーコードを使用すること
- **差異管理の厳格化**: 現行仕様との差異が生じる場合、05_gap_analysis.md のフォーマット
  （差異ID / 区分 / 現行仕様 / 新仕様 / 変更理由 / 影響範囲）で必ず記録すること
- **SPA改善提案は段階的に**: 05_gap_analysis.md のADD項目（オートセーブ、非同期バリデーション、
  ダッシュボード化等）は Phase 4 以降で実装。Phase 1〜3は既存機能の移行に集中する
- **spec-kit 各機能の spec.md 作成時**: 03_user_stories.md のユーザーストーリーと
  ビジネスルールを正として参照し、仕様の出典を明記すること


### 原則2: Docker-First Development

06_devenv_infrastructure.md に定義された Docker 開発環境構成に準拠:
- 6コンテナ構成（frontend/backend/batch/auth-mock/db/redis）
- docker compose up のみで開発環境完結
- マルチステージビルド（dev/production）
- ヘルスチェックによる依存制御


### 原則3: TDD（テスト駆動開発）— 非交渉事項

t_wada 理論に基づく Red-Green-Refactor を厳格に遵守:
- テストファーストを徹底
- マイグレーション特有の追加要件:
  - **現行仕様の回帰テスト**: 各機能移行時、03_user_stories.md の
    ユーザーストーリーに対応するE2Eテストを先に作成
  - **ステータスマトリクステスト**: 12状態 × 2系列の全パターンをパラメタライズドテストで網羅
  - **バリデーションルールテスト**: 05_gap_analysis.md セクション4の全ルールをテスト化
  - **エラーメッセージテスト**: CZ-000〜CZ-999 の発生条件をテストで再現
- カバレッジ: 新規コード80%以上。踏襲必須ビジネスロジックは100%


### 原則4: Production Safety — 本番混入防止

06_devenv_infrastructure.md セクション5に定義された三重安全策に準拠:
- **安全策1**: 環境変数 NUXT_PUBLIC_ENABLE_ACTOR_SWITCH による条件付きビルド
- **安全策2**: Nuxt.js ビルド時 Tree Shaking でコード自体を除外
- **安全策3**: CI パイプラインでの grep 検査
- **守りと攻めの対称テスト**:
  - ci-production.yml: 本番ビルドで dev 機能が無効なことを検証
  - ci-staging.yml: 検証ビルドで dev 機能（アクター切替等）が正常動作を検証


### 原則5: CI/CD — 2系統パイプライン

06_devenv_infrastructure.md セクション6に定義された CI 構成に準拠:
- ci-production.yml: ENABLE_ACTOR_SWITCH 未設定、grep混入検査、ECR Push
- ci-staging.yml: ENABLE_ACTOR_SWITCH=true、アクター切替込みE2Eテスト
- ubuntu-latest を標準ランナーとする


### 原則6: 認証モックとアクター切替

06_devenv_infrastructure.md セクション4に定義された認証モック構成に準拠:
- Express 製 ALB+Okta OIDC モックサーバー（:8180）
- 8アクター定義（02_actor_definition.md の4層権限モデル完全再現）
- JWT発行 + OIDC Discovery + JWKS エンドポイント模倣
- フロントエンドの DevActorSwitcher コンポーネント


### 原則7: コード品質・保守性

- ESLint + Prettier (Frontend) / Checkstyle + SpotBugs (Backend)
- Conventional Commits
- PR レビュー必須
- 過度な抽象化を避ける YAGNI 原則


### 原則8: UX-First

04_screen_transition.md セクション7のSPA統合提案に準拠:
- 24画面 → 4ページ + 8モーダルへの統合
- 88 Unit → 約20 REST API への集約
- アクセシビリティ（WCAG 2.1 Level A）
- レスポンシブデザイン


## 開発ワークフロー

spec-kit の標準ワークフローに加え、マイグレーション固有のステップを含めること:

1. /speckit.specify — 仕様策定（03_user_stories.md から対象USを引用）
2. /speckit.clarify — 仕様明確化（現行コードとの照合）
3. /speckit.plan — 実装計画（05_gap_analysis.md の該当GAP-IDを参照）
4. /speckit.checklist — 仕様品質検証
5. /speckit.tasks — タスク分解（テストタスクを先に配置 = TDD）
6. /speckit.analyze — 整合性分析（constitution準拠チェック含む）
7. /speckit.implement — 実装（Red-Green-Refactor）


## 実装フェーズ（05_gap_analysis.md セクション7準拠）

| フェーズ | 内容 | 主要GAP-ID |
|---------|------|-----------|
| Phase 1 | 基盤構築（認証/DB/API基盤） | GAP-A01〜A08, GAP-B01〜B04 |
| Phase 2 | コア機能（工数入力/管理/ステータス制御） | GAP-F10-*, GAP-F20-*, GAP-R01〜R05 |
| Phase 3 | 分析・帳票（ダッシュボード/Excel/ダイアログ） | GAP-F30-*, GAP-E01〜E07, GAP-D01〜D06 |
| Phase 4 | 拡張機能（オートセーブ/非同期バリデーション等） | ENH-001〜ENH-008 |


## Governance

- 本 Constitution はすべての開発活動に優先する
- 原則の修正にはチーム合意と影響範囲のマイグレーション計画を必須とする
- 01〜06 の分析ドキュメントは constitution と同等の拘束力を持つ参照文書とする
- バージョニングはセマンティックバージョニングに従う


## 注意事項

- 既存の spec-kit-docker プロジェクトの constitution.md
  （D:\PROJECT\spec-kit-docker\.specify\memory\constitution.md）の
  フォーマットと構造を参考にしつつ、本マイグレーションプロジェクト固有の
  原則（Migration-First）を最上位に配置すること
- 技術スタックに Spring Batch を追加すること（既存 constitution にはない）
- 認証モックは Express ベース（既存は Spring Security DevFilter ベース）に変更されている点に注意
- 「01〜06分析ドキュメントへの参照」を constitution 内で明示すること
  （例: 「03_user_stories.md を正とする」「05_gap_analysis.md のフォーマットで記録する」等）
```

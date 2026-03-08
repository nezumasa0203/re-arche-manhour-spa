# CZ マイグレーション

保有資源管理システム（CZ）を Java MPA + Oracle から
Nuxt.js 3 SPA + Spring Boot REST API + PostgreSQL にマイグレーションするプロジェクト。

## Constitution（プロジェクト憲法）

すべての開発活動の原則・基準・ガバナンスルールは以下に定義されている。
**作業開始前に必ず読み込むこと。**

→ `.specify/memory/constitution.md`

## 移行元ソースコード

ローカル環境: `D:\PROJECT02\migration_source\migration_soource_irpmng_czConsv`
※ Codespaces 環境では移行元ソースは含まれません。必要に応じて参照してください。

## 分析ドキュメント

→ `analysis/01_system_analysis.md` 〜 `analysis/06_devenv_infrastructure.md`

## 開発環境

```bash
cp .env.example .env  # 初回のみ
docker compose up     # .env なしでもデフォルト値で起動可能
```

ポートを変更する場合は `.env` で上書き（`.env.example` 参照）。

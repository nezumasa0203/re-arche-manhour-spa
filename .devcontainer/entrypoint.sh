#!/bin/bash
# ============================================
# Workspace entrypoint
# spec-kit プロジェクトの自動初期化
# ============================================
set -e

WORKSPACE="/workspace"

# .specify/memory/constitution.md を持つディレクトリ = spec-kit プロジェクト
for constitution in "$WORKSPACE"/*/.specify/memory/constitution.md; do
  [ -f "$constitution" ] || continue

  project_dir="$(dirname "$(dirname "$(dirname "$constitution")")")"
  project_name="$(basename "$project_dir")"

  # .claude/commands/ が無ければ specify init を実行
  if [ ! -d "$project_dir/.claude/commands" ]; then
    echo "[spec-kit] Initializing: $project_name"
    cd "$project_dir"
    specify init --here --ai claude --force --no-git --ignore-agent-tools 2>&1 || \
      echo "[spec-kit] WARNING: specify init failed for $project_name. Run manually: cd $project_dir && specify init --here --ai claude --force --no-git"
    cd "$WORKSPACE"
  else
    echo "[spec-kit] Already initialized: $project_name"
  fi
done

echo "[workspace] Ready."

# 引数があればそれを実行、なければ待機
exec "$@"

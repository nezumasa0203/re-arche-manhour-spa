# ============================================
# Base — 共通ベース
# ============================================
FROM node:22-alpine AS base
WORKDIR /app

# ============================================
# Dev stage — ホットリロード開発用
# ============================================
FROM base AS dev
# node_modules は名前付きボリュームで管理
COPY package.json package-lock.json* ./
RUN npm install --legacy-peer-deps
# ソースはボリュームマウントされる
EXPOSE 3000
CMD ["npx", "nuxt", "dev", "--host", "0.0.0.0"]

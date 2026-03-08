# ============================================
# Auth Mock — Express ALB+Okta OIDC 模倣サーバー
# ============================================
FROM node:22-alpine
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY src ./src

EXPOSE 8180
CMD ["node", "src/index.js"]

# ============================================
# Workspace container — 開発用統合環境
# Node.js 22 + Python 3.12 + Java 21 + Gradle 8.12 + uv + spec-kit
# ============================================
FROM mcr.microsoft.com/devcontainers/base:1-ubuntu-24.04

ARG DEBIAN_FRONTEND=noninteractive

# ===== Java 21 (OpenJDK) =====
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jdk \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# ===== Gradle 8.12 =====
ARG GRADLE_VERSION=8.12
RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s "/opt/gradle-${GRADLE_VERSION}" /opt/gradle \
    && rm /tmp/gradle.zip

ENV GRADLE_HOME=/opt/gradle
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

# ===== Node.js 22 =====
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# ===== Python 3.12 (Ubuntu 24.04 built-in) + venv =====
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3-pip python3-venv \
    && rm -rf /var/lib/apt/lists/*

# ===== uv (fast Python package manager) =====
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv
COPY --from=ghcr.io/astral-sh/uv:latest /uvx /usr/local/bin/uvx

# ===== spec-kit (specify-cli) =====
USER vscode
ENV PATH="/home/vscode/.local/bin:${PATH}"
RUN uv tool install specify-cli --from "git+https://github.com/github/spec-kit.git" \
    || echo "INFO: spec-kit install skipped. Run manually: uv tool install specify-cli --from git+https://github.com/github/spec-kit.git"
USER root

# ===== エントリポイント (spec-kit 自動初期化) =====
COPY .devcontainer/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/entrypoint.sh && chmod +x /usr/local/bin/entrypoint.sh

# ===== 作業ディレクトリ =====
WORKDIR /workspace

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

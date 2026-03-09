const express = require('express')
const jwt = require('jsonwebtoken')
const cors = require('cors')

const app = express()
app.use(cors())
app.use(express.json())

const PORT = process.env.PORT || 8180
const JWT_SECRET = process.env.JWT_SECRET || 'dev-mock-secret-key-that-is-at-least-256-bits-long'
const JWT_ISSUER = process.env.JWT_ISSUER || 'https://mock-okta.example.com'

// ============================================
// TAB ビット文字列 → オブジェクト変換
// ============================================
function parseBits(bitStr) {
  const result = {}
  for (let i = 0; i < bitStr.length; i++) {
    result[`bit${i}`] = bitStr[i] === '1'
  }
  return result
}

// ============================================
// 15アクター定義（02_actor_definition.md 4層権限モデル完全準拠）
//
// Layer 1: JinjiMode (人事/管理モード)
// Layer 2: 機能権限 TAB 010/011/012 (ビットベース)
// Layer 3: データアクセス権限 (相対権限 201/202/211)
// Layer 4: 雇用形態 (0:正社員 / 1:臨時1 / 2:臨時2 / 3:外部契約者)
// ============================================
const ACTORS = [
  // --- 人事モード (jinjiMode=true) ---
  {
    id: 'ACT-01', name: '報告担当者', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: null },
    employmentType: 0, orgCode: '100210', orgName: 'IT推進部 開発1課',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-05', name: '人事モードユーザー', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: null, upd: null },
    employmentType: 0, orgCode: '100220', orgName: '人事部 管理課',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-06', name: '正社員', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: null },
    employmentType: 0, orgCode: '100210', orgName: 'IT推進部 開発1課',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-07', name: '臨時職員1', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KA', ins: null, upd: null },
    employmentType: 1, orgCode: '100211', orgName: 'IT推進部 開発1課 第1G',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-08', name: '臨時職員2', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KA', ins: null, upd: null },
    employmentType: 2, orgCode: '100211', orgName: 'IT推進部 開発1課 第1G',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-09', name: '外部契約者', jinjiMode: true,
    tab010: '100000', tab011: '00', tab012: '00',
    dataAuthority: { ref: 'KA', ins: null, upd: null },
    employmentType: 3, orgCode: '100211', orgName: 'IT推進部 開発1課 第1G',
    staffRole: null, canDelegate: true,
  },

  // --- 管理モード (jinjiMode=false) ---
  {
    id: 'ACT-02', name: '報告管理者', jinjiMode: false,
    tab010: '010000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'HONBU', ins: 'KYOKU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-03', name: '全権管理者', jinjiMode: false,
    tab010: '001000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'HONBU', ins: 'HONBU', upd: 'HONBU' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-04', name: '管理モードユーザー', jinjiMode: false,
    tab010: '100000', tab011: '10', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: null },
    employmentType: 0, orgCode: '100210', orgName: 'IT推進部 開発1課',
    staffRole: null, canDelegate: false,
  },
  {
    id: 'ACT-10', name: '全社スタッフ', jinjiMode: false,
    tab010: '001000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'ZENSYA', ins: 'ZENSYA', upd: 'ZENSYA' },
    employmentType: 0, orgCode: '100000', orgName: '全社',
    staffRole: 931, canDelegate: false,
  },
  {
    id: 'ACT-11', name: '事業スタッフ', jinjiMode: false,
    tab010: '010000', tab011: '11', tab012: '10',
    dataAuthority: { ref: 'HONBU', ins: 'HONBU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100100', orgName: '事業本部',
    staffRole: 932, canDelegate: false,
  },
  {
    id: 'ACT-12', name: '本社スタッフ', jinjiMode: false,
    tab010: '010000', tab011: '11', tab012: '10',
    dataAuthority: { ref: 'HONBU', ins: 'HONBU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100100', orgName: '本社管理',
    staffRole: 933, canDelegate: false,
  },
  {
    id: 'ACT-13', name: '局スタッフ', jinjiMode: false,
    tab010: '010000', tab011: '10', tab012: '10',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: 'KA' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: 934, canDelegate: false,
  },
  {
    id: 'ACT-14', name: '局スタッフ（総務管理者）', jinjiMode: false,
    tab010: '001000', tab011: '11', tab012: '11',
    dataAuthority: { ref: 'KYOKU', ins: 'KYOKU', upd: 'KYOKU' },
    employmentType: 0, orgCode: '100200', orgName: 'IT推進部',
    staffRole: 935, canDelegate: false,
  },
  {
    id: 'ACT-15', name: '局スタッフ（営業）', jinjiMode: false,
    tab010: '010000', tab011: '10', tab012: '00',
    dataAuthority: { ref: 'KYOKU', ins: 'KA', upd: null },
    employmentType: 0, orgCode: '100300', orgName: '営業部',
    staffRole: 936, canDelegate: false,
  },
]

// ============================================
// JWT ペイロード構築（4層権限モデル準拠）
// ============================================
function buildJwtPayload(actor) {
  return {
    sub: actor.id,
    name: actor.name,
    email: `${actor.id.toLowerCase().replace('-', '')}@example.com`,
    iss: JWT_ISSUER,

    // Layer 1: アプリケーションモード
    jinjiMode: actor.jinjiMode,

    // Layer 2: 機能権限（ビットベース）
    permissions: {
      tab010: parseBits(actor.tab010),
      tab011: parseBits(actor.tab011),
      tab012: parseBits(actor.tab012),
    },

    // Layer 3: データアクセス権限（相対権限）
    dataAuthority: actor.dataAuthority,

    // Layer 4: 雇用形態
    employmentType: actor.employmentType,

    // 追加属性
    organizationCode: actor.orgCode,
    organizationName: actor.orgName,
    staffRole: actor.staffRole,
    canDelegate: actor.canDelegate,
  }
}

// ============================================
// 雇用形態ラベル
// ============================================
const EMPLOYMENT_LABELS = ['正社員', '臨時職員1', '臨時職員2', '外部契約者']

// ============================================
// アクター選択 UI（GET /）
// ============================================
app.get('/', (_req, res) => {
  const actorRows = ACTORS.map((a) => {
    const modeBadge = a.jinjiMode
      ? '<span class="badge badge-jinji">人事</span>'
      : '<span class="badge badge-kanri">管理</span>'
    const empLabel = EMPLOYMENT_LABELS[a.employmentType] || `Type ${a.employmentType}`
    const delegateBadge = a.canDelegate
      ? '<span class="badge badge-delegate">委任可</span>'
      : ''
    const staffRoleStr = a.staffRole !== null ? a.staffRole : '-'
    const dataAuth = [
      `ref:${a.dataAuthority.ref || '-'}`,
      `ins:${a.dataAuthority.ins || '-'}`,
      `upd:${a.dataAuthority.upd || '-'}`,
    ].join(' / ')

    return `
      <tr class="actor-row" data-actor-id="${a.id}" onclick="switchActor('${a.id}')">
        <td class="col-id"><code>${a.id}</code></td>
        <td class="col-name">${a.name}</td>
        <td class="col-mode">${modeBadge}</td>
        <td class="col-emp">${empLabel}</td>
        <td class="col-org">${a.orgName}<br><code class="org-code">${a.orgCode}</code></td>
        <td class="col-tab"><code>${a.tab010}</code></td>
        <td class="col-tab"><code>${a.tab011}</code></td>
        <td class="col-tab"><code>${a.tab012}</code></td>
        <td class="col-data-auth"><code>${dataAuth}</code></td>
        <td class="col-staff">${staffRoleStr}</td>
        <td class="col-delegate">${delegateBadge}</td>
      </tr>`
  }).join('')

  const html = `<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CZ Auth Mock - Actor Selection</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      background: #f8fafc;
      color: #334155;
      line-height: 1.6;
    }
    .header {
      background: #1e293b;
      color: #f1f5f9;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .header h1 { font-size: 18px; font-weight: 600; }
    .header .subtitle { font-size: 13px; color: #94a3b8; }
    .container { max-width: 1400px; margin: 0 auto; padding: 24px; }

    /* Current actor panel */
    .current-panel {
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 20px 24px;
      margin-bottom: 24px;
      display: flex;
      align-items: flex-start;
      gap: 24px;
    }
    .current-panel .label { font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 600; letter-spacing: 0.05em; }
    .current-panel .value { font-size: 15px; font-weight: 500; margin-top: 2px; }
    .current-panel .section { min-width: 120px; }
    .current-panel .token-section { flex: 1; min-width: 0; }
    .current-panel .token-value {
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 11px;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      border-radius: 4px;
      padding: 8px 12px;
      word-break: break-all;
      max-height: 60px;
      overflow-y: auto;
      margin-top: 4px;
    }
    .no-actor { color: #94a3b8; font-style: italic; }

    /* Permissions display */
    .permissions-grid {
      display: flex;
      gap: 16px;
      margin-top: 8px;
    }
    .perm-block { font-size: 12px; }
    .perm-block .perm-label { font-weight: 600; color: #64748b; }
    .perm-bit { display: inline-block; width: 18px; height: 18px; line-height: 18px; text-align: center; font-size: 10px; font-weight: 700; border-radius: 3px; margin: 1px; }
    .perm-bit.on { background: #3b82f6; color: #fff; }
    .perm-bit.off { background: #e2e8f0; color: #94a3b8; }

    /* Actor table */
    .table-wrapper {
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      overflow-x: auto;
    }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    thead th {
      background: #f8fafc;
      border-bottom: 2px solid #e2e8f0;
      padding: 10px 12px;
      text-align: left;
      font-weight: 600;
      font-size: 12px;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      white-space: nowrap;
    }
    tbody td { padding: 10px 12px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
    .actor-row { cursor: pointer; transition: background 0.15s; }
    .actor-row:hover { background: #f0f9ff; }
    .actor-row.active { background: #eff6ff; border-left: 3px solid #3b82f6; }
    .col-id code { font-size: 12px; color: #475569; }
    .col-name { font-weight: 500; }
    .col-tab code { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 12px; letter-spacing: 1px; }
    .col-data-auth code { font-size: 11px; }
    .org-code { font-size: 11px; color: #94a3b8; }

    /* Badges */
    .badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.02em;
    }
    .badge-jinji { background: #dbeafe; color: #1d4ed8; }
    .badge-kanri { background: #fef3c7; color: #92400e; }
    .badge-delegate { background: #d1fae5; color: #065f46; }

    /* Section headers */
    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
    }
    .section-header h2 { font-size: 16px; font-weight: 600; }
    .actor-count { font-size: 13px; color: #64748b; }

    /* Status indicator */
    .status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; }
    .status-dot.active { background: #22c55e; }
    .status-dot.inactive { background: #94a3b8; }

    /* Copy button */
    .copy-btn {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      border-radius: 4px;
      padding: 4px 10px;
      font-size: 11px;
      color: #64748b;
      cursor: pointer;
      transition: all 0.15s;
    }
    .copy-btn:hover { background: #e2e8f0; color: #334155; }
  </style>
</head>
<body>
  <div class="header">
    <div>
      <h1>CZ Auth Mock - Actor Selection</h1>
      <div class="subtitle">ALB + Okta OIDC Mock Server / 4-Layer Permission Model / 15 Actors</div>
    </div>
    <div style="font-size: 12px; color: #64748b;">Port ${PORT}</div>
  </div>

  <div class="container">
    <!-- Current Actor Panel -->
    <div class="current-panel" id="currentPanel">
      <div class="section">
        <div class="label">Current Actor</div>
        <div class="value" id="currentActor"><span class="no-actor">None selected</span></div>
      </div>
      <div class="section">
        <div class="label">Mode</div>
        <div class="value" id="currentMode"><span class="no-actor">-</span></div>
      </div>
      <div class="section">
        <div class="label">Employment</div>
        <div class="value" id="currentEmployment"><span class="no-actor">-</span></div>
      </div>
      <div class="section">
        <div class="label">Organization</div>
        <div class="value" id="currentOrg"><span class="no-actor">-</span></div>
      </div>
      <div class="section">
        <div class="label">Permissions</div>
        <div id="currentPermissions"><span class="no-actor">-</span></div>
      </div>
      <div class="token-section">
        <div class="label">JWT Token <button class="copy-btn" onclick="copyToken()">Copy</button></div>
        <div class="token-value" id="currentToken"><span class="no-actor">No token generated</span></div>
      </div>
    </div>

    <!-- Actor Table -->
    <div class="section-header">
      <h2>Actors</h2>
      <span class="actor-count">${ACTORS.length} actors (click a row to switch)</span>
    </div>
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Mode</th>
            <th>Employment</th>
            <th>Organization</th>
            <th>TAB010</th>
            <th>TAB011</th>
            <th>TAB012</th>
            <th>Data Authority</th>
            <th>Staff Role</th>
            <th>Delegate</th>
          </tr>
        </thead>
        <tbody>
          ${actorRows}
        </tbody>
      </table>
    </div>
  </div>

  <script>
    const EMPLOYMENT_LABELS = ['正社員', '臨時職員1', '臨時職員2', '外部契約者'];
    let currentToken = null;

    function renderBits(label, bitStr) {
      const bits = bitStr.split('').map((b, i) =>
        '<span class="perm-bit ' + (b === '1' ? 'on' : 'off') + '">' + i + '</span>'
      ).join('');
      return '<div class="perm-block"><span class="perm-label">' + label + '</span> ' + bits + '</div>';
    }

    async function switchActor(actorId) {
      try {
        const res = await fetch('/api/switch', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ actorId }),
        });
        if (!res.ok) {
          const err = await res.json();
          alert('Error: ' + (err.error || 'Unknown error'));
          return;
        }
        const data = await res.json();
        currentToken = data.token;

        // Update current panel
        document.getElementById('currentActor').innerHTML =
          '<span class="status-dot active"></span><strong>' + data.actor.id + '</strong> ' + data.actor.name;
        document.getElementById('currentMode').innerHTML =
          data.actor.jinjiMode
            ? '<span class="badge badge-jinji">人事 (JinjiMode)</span>'
            : '<span class="badge badge-kanri">管理 (KanriMode)</span>';
        document.getElementById('currentEmployment').textContent =
          EMPLOYMENT_LABELS[data.actor.employmentType] || 'Type ' + data.actor.employmentType;
        document.getElementById('currentOrg').textContent = data.actor.orgName;
        document.getElementById('currentToken').textContent = data.token;

        // Decode JWT payload to show permissions
        const payload = JSON.parse(atob(data.token.split('.')[1]));
        const permsHtml = '<div class="permissions-grid">' +
          renderBits('TAB010', bitsToStr(payload.permissions.tab010)) +
          renderBits('TAB011', bitsToStr(payload.permissions.tab011)) +
          renderBits('TAB012', bitsToStr(payload.permissions.tab012)) +
          '</div>';
        document.getElementById('currentPermissions').innerHTML = permsHtml;

        // Highlight active row
        document.querySelectorAll('.actor-row').forEach(function(row) {
          row.classList.toggle('active', row.dataset.actorId === actorId);
        });
      } catch (err) {
        alert('Network error: ' + err.message);
      }
    }

    function bitsToStr(bitObj) {
      let s = '';
      for (let i = 0; ; i++) {
        const key = 'bit' + i;
        if (!(key in bitObj)) break;
        s += bitObj[key] ? '1' : '0';
      }
      return s;
    }

    function copyToken() {
      if (!currentToken) {
        alert('No token to copy. Select an actor first.');
        return;
      }
      navigator.clipboard.writeText(currentToken).then(function() {
        const btn = document.querySelector('.copy-btn');
        btn.textContent = 'Copied!';
        setTimeout(function() { btn.textContent = 'Copy'; }, 1500);
      });
    }
  </script>
</body>
</html>`

  res.type('html').send(html)
})

// ============================================
// ヘルスチェック
// ============================================
app.get('/health', (_req, res) => {
  res.json({ status: 'UP', service: 'auth-mock', actorCount: ACTORS.length })
})

// ============================================
// アクター一覧
// ============================================
app.get('/actors', (_req, res) => {
  res.json(ACTORS.map((a) => ({
    id: a.id,
    name: a.name,
    jinjiMode: a.jinjiMode,
    modeName: a.jinjiMode ? '人事' : '管理',
    employmentType: a.employmentType,
    orgName: a.orgName,
    staffRole: a.staffRole,
    tab010: a.tab010,
  })))
})

// ============================================
// JWT トークン発行（アクター切替用）
// ============================================
app.post('/token', (req, res) => {
  const { actorId } = req.body
  const actor = ACTORS.find((a) => a.id === actorId)
  if (!actor) {
    return res.status(400).json({ error: `Actor not found: ${actorId}` })
  }

  const payload = buildJwtPayload(actor)
  const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' })

  res.json({
    token,
    actor: {
      id: actor.id,
      name: actor.name,
      jinjiMode: actor.jinjiMode,
      modeName: actor.jinjiMode ? '人事' : '管理',
      employmentType: actor.employmentType,
      orgName: actor.orgName,
    },
    headers: {
      'X-Amzn-Oidc-Data': token,
      'X-Amzn-Oidc-Identity': actor.id,
      'X-Amzn-Oidc-Accesstoken': token,
    },
  })
})

// ============================================
// 現在のアクター情報（Cookie/Header から解決）
// ============================================
app.get('/api/current', (req, res) => {
  const authHeader = req.headers.authorization
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'No token provided' })
  }

  try {
    const token = authHeader.slice(7)
    const decoded = jwt.verify(token, JWT_SECRET)
    const actor = ACTORS.find((a) => a.id === decoded.sub)
    res.json({ decoded, actor: actor || null })
  } catch (err) {
    res.status(401).json({ error: 'Invalid token', message: err.message })
  }
})

// ============================================
// アクター切替（POST /api/switch）
// ============================================
app.post('/api/switch', (req, res) => {
  const { actorId } = req.body
  const actor = ACTORS.find((a) => a.id === actorId)
  if (!actor) {
    return res.status(400).json({ error: `Actor not found: ${actorId}` })
  }

  const payload = buildJwtPayload(actor)
  const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' })

  res.json({
    token,
    actor: {
      id: actor.id,
      name: actor.name,
      jinjiMode: actor.jinjiMode,
      modeName: actor.jinjiMode ? '人事' : '管理',
      employmentType: actor.employmentType,
      orgName: actor.orgName,
    },
  })
})

// ============================================
// OIDC Discovery エンドポイント（模倣）
// ============================================
app.get('/.well-known/openid-configuration', (_req, res) => {
  res.json({
    issuer: JWT_ISSUER,
    authorization_endpoint: `http://localhost:${PORT}/authorize`,
    token_endpoint: `http://localhost:${PORT}/token`,
    jwks_uri: `http://localhost:${PORT}/.well-known/jwks.json`,
    response_types_supported: ['code'],
    subject_types_supported: ['public'],
    id_token_signing_alg_values_supported: ['HS256'],
  })
})

// ============================================
// JWKS エンドポイント（模倣 — 開発用のためダミー）
// ============================================
app.get('/.well-known/jwks.json', (_req, res) => {
  res.json({ keys: [] })
})

// ============================================
// サーバー起動（テスト時は起動しない）
// ============================================
if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[auth-mock] ALB+Okta OIDC mock server running on port ${PORT}`)
    console.log(`[auth-mock] ${ACTORS.length} actors available (4-layer permission model)`)
    console.log('[auth-mock] Endpoints:')
    console.log(`  GET  /            (Actor Selection UI)`)
    console.log(`  GET  /health`)
    console.log(`  GET  /actors`)
    console.log(`  POST /token    { actorId: "ACT-01" }`)
    console.log(`  POST /api/switch { actorId: "ACT-01" }`)
    console.log(`  GET  /api/current (Authorization: Bearer <token>)`)
  })
}

// ============================================
// テスト用エクスポート
// ============================================
module.exports = { app, ACTORS, parseBits, buildJwtPayload }

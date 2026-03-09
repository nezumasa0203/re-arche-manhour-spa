import { describe, it, expect } from 'vitest'
import jwt from 'jsonwebtoken'
import { ACTORS, buildJwtPayload, parseBits } from '../index.js'

const JWT_SECRET = process.env.JWT_SECRET || 'dev-mock-secret-key-that-is-at-least-256-bits-long'
const JWT_ISSUER = process.env.JWT_ISSUER || 'https://mock-okta.example.com'

// ============================================
// JWT ペイロード構築テスト
// ============================================
describe('buildJwtPayload', () => {
  const actor03 = ACTORS.find((a) => a.id === 'ACT-03')

  it('should build payload for ACT-03 with all expected claims', () => {
    const payload = buildJwtPayload(actor03)

    // All required claims must be present
    expect(payload).toHaveProperty('sub', 'ACT-03')
    expect(payload).toHaveProperty('name', '全権管理者')
    expect(payload).toHaveProperty('email', 'act03@example.com')
    expect(payload).toHaveProperty('iss', JWT_ISSUER)
    expect(payload).toHaveProperty('jinjiMode', false)
    expect(payload).toHaveProperty('permissions')
    expect(payload).toHaveProperty('dataAuthority')
    expect(payload).toHaveProperty('employmentType', 0)
    expect(payload).toHaveProperty('organizationCode', '100200')
    expect(payload).toHaveProperty('organizationName', 'IT推進部')
    expect(payload).toHaveProperty('staffRole', null)
    expect(payload).toHaveProperty('canDelegate', false)
  })

  it('should include correct permissions structure with parsed bits', () => {
    const payload = buildJwtPayload(actor03)

    // ACT-03: tab010='001000', tab011='11', tab012='11'
    expect(payload.permissions).toEqual({
      tab010: parseBits('001000'),
      tab011: parseBits('11'),
      tab012: parseBits('11'),
    })
  })

  it('should correctly convert tab010 "001000" so bit2 is true', () => {
    const payload = buildJwtPayload(actor03)

    expect(payload.permissions.tab010.bit0).toBe(false)
    expect(payload.permissions.tab010.bit1).toBe(false)
    expect(payload.permissions.tab010.bit2).toBe(true)
    expect(payload.permissions.tab010.bit3).toBe(false)
    expect(payload.permissions.tab010.bit4).toBe(false)
    expect(payload.permissions.tab010.bit5).toBe(false)
  })

  it('should include correct dataAuthority for ACT-03', () => {
    const payload = buildJwtPayload(actor03)
    expect(payload.dataAuthority).toEqual({
      ref: 'HONBU', ins: 'HONBU', upd: 'HONBU',
    })
  })

  it('should generate correct email from actor ID', () => {
    // ACT-03 -> act03@example.com (lowercased, hyphen removed)
    const payload = buildJwtPayload(actor03)
    expect(payload.email).toBe('act03@example.com')

    // ACT-01 -> act01@example.com
    const act01 = ACTORS.find((a) => a.id === 'ACT-01')
    const payload01 = buildJwtPayload(act01)
    expect(payload01.email).toBe('act01@example.com')
  })

  it('should generate payload for every actor without throwing', () => {
    for (const actor of ACTORS) {
      expect(() => buildJwtPayload(actor)).not.toThrow()
    }
  })
})

// ============================================
// JWT トークン生成・検証テスト
// ============================================
describe('JWT token generation and verification', () => {
  const actor = ACTORS.find((a) => a.id === 'ACT-03')
  const payload = buildJwtPayload(actor)

  it('should produce a verifiable token with the dev secret', () => {
    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' })
    const decoded = jwt.verify(token, JWT_SECRET)

    expect(decoded.sub).toBe('ACT-03')
    expect(decoded.name).toBe('全権管理者')
    expect(decoded.iss).toBe(JWT_ISSUER)
  })

  it('should have correct expiration (approximately 24 hours)', () => {
    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' })
    const decoded = jwt.verify(token, JWT_SECRET)

    // exp should be roughly 24h from now (allow 10s tolerance)
    const now = Math.floor(Date.now() / 1000)
    const expectedExp = now + 24 * 60 * 60
    expect(decoded.exp).toBeGreaterThan(now)
    expect(decoded.exp).toBeLessThanOrEqual(expectedExp + 10)
    expect(decoded.exp).toBeGreaterThanOrEqual(expectedExp - 10)
  })

  it('should fail verification with a wrong secret', () => {
    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' })
    expect(() => jwt.verify(token, 'wrong-secret')).toThrow()
  })

  it('should preserve parseBits conversion in the decoded token', () => {
    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' })
    const decoded = jwt.verify(token, JWT_SECRET)

    // ACT-03 tab010='001000' -> bit2=true, rest false
    expect(decoded.permissions.tab010.bit2).toBe(true)
    expect(decoded.permissions.tab010.bit0).toBe(false)
    expect(decoded.permissions.tab010.bit1).toBe(false)
    expect(decoded.permissions.tab010.bit3).toBe(false)
    expect(decoded.permissions.tab010.bit4).toBe(false)
    expect(decoded.permissions.tab010.bit5).toBe(false)

    // tab011='11' -> bit0=true, bit1=true
    expect(decoded.permissions.tab011.bit0).toBe(true)
    expect(decoded.permissions.tab011.bit1).toBe(true)
  })

  it('should include jinjiMode, dataAuthority, employmentType in decoded token', () => {
    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' })
    const decoded = jwt.verify(token, JWT_SECRET)

    expect(decoded.jinjiMode).toBe(false)
    expect(decoded.dataAuthority).toEqual({ ref: 'HONBU', ins: 'HONBU', upd: 'HONBU' })
    expect(decoded.employmentType).toBe(0)
    expect(decoded.canDelegate).toBe(false)
  })
})

import { describe, it, expect, beforeAll } from 'vitest'
import request from 'supertest'
import jwt from 'jsonwebtoken'
import { app, ACTORS } from '../index.js'

const JWT_SECRET = process.env.JWT_SECRET || 'dev-mock-secret-key-that-is-at-least-256-bits-long'

// ============================================
// エンドポイントテスト
// ============================================

// ------------------------------------------
// GET / (Actor Selection UI)
// ------------------------------------------
describe('GET /', () => {
  it('should return HTML with actor selection UI', async () => {
    const res = await request(app).get('/')
    expect(res.status).toBe(200)
    expect(res.headers['content-type']).toMatch(/html/)
    expect(res.text).toContain('CZ Auth Mock')
    expect(res.text).toContain('Actor Selection')
  })

  it('should list all 15 actors in the HTML', async () => {
    const res = await request(app).get('/')
    for (const actor of ACTORS) {
      expect(res.text).toContain(actor.id)
      expect(res.text).toContain(actor.name)
    }
  })
})

// ------------------------------------------
// GET /health
// ------------------------------------------
describe('GET /health', () => {
  it('should return status UP', async () => {
    const res = await request(app).get('/health')
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('status', 'UP')
  })

  it('should return service name and actor count', async () => {
    const res = await request(app).get('/health')
    expect(res.body).toHaveProperty('service', 'auth-mock')
    expect(res.body).toHaveProperty('actorCount', 15)
  })
})

// ------------------------------------------
// GET /actors
// ------------------------------------------
describe('GET /actors', () => {
  it('should return all 15 actors', async () => {
    const res = await request(app).get('/actors')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(15)
  })

  it('should return actors with expected fields', async () => {
    const res = await request(app).get('/actors')
    const first = res.body[0]
    expect(first).toHaveProperty('id')
    expect(first).toHaveProperty('name')
    expect(first).toHaveProperty('jinjiMode')
    expect(first).toHaveProperty('modeName')
    expect(first).toHaveProperty('employmentType')
    expect(first).toHaveProperty('orgName')
    expect(first).toHaveProperty('staffRole')
    expect(first).toHaveProperty('tab010')
  })

  it('should have modeName "人事" for jinjiMode=true actors', async () => {
    const res = await request(app).get('/actors')
    const jinjiActors = res.body.filter((a) => a.jinjiMode === true)
    for (const actor of jinjiActors) {
      expect(actor.modeName).toBe('人事')
    }
  })

  it('should have modeName "管理" for jinjiMode=false actors', async () => {
    const res = await request(app).get('/actors')
    const kanriActors = res.body.filter((a) => a.jinjiMode === false)
    for (const actor of kanriActors) {
      expect(actor.modeName).toBe('管理')
    }
  })
})

// ------------------------------------------
// POST /api/switch
// ------------------------------------------
describe('POST /api/switch', () => {
  it('should return a token for a valid actorId', async () => {
    const res = await request(app)
      .post('/api/switch')
      .send({ actorId: 'ACT-01' })
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('token')
    expect(typeof res.body.token).toBe('string')
    expect(res.body.token.length).toBeGreaterThan(0)
  })

  it('should return actor info in the response', async () => {
    const res = await request(app)
      .post('/api/switch')
      .send({ actorId: 'ACT-01' })
    expect(res.body).toHaveProperty('actor')
    expect(res.body.actor).toHaveProperty('id', 'ACT-01')
    expect(res.body.actor).toHaveProperty('name', '報告担当者')
    expect(res.body.actor).toHaveProperty('jinjiMode', true)
    expect(res.body.actor).toHaveProperty('modeName', '人事')
  })

  it('should return 400 for an invalid actorId', async () => {
    const res = await request(app)
      .post('/api/switch')
      .send({ actorId: 'ACT-99' })
    expect(res.status).toBe(400)
    expect(res.body).toHaveProperty('error')
    expect(res.body.error).toContain('ACT-99')
  })

  it('should return 400 when actorId is missing', async () => {
    const res = await request(app)
      .post('/api/switch')
      .send({})
    expect(res.status).toBe(400)
  })

  it('should return a JWT that can be verified', async () => {
    const res = await request(app)
      .post('/api/switch')
      .send({ actorId: 'ACT-03' })
    const decoded = jwt.verify(res.body.token, JWT_SECRET)
    expect(decoded.sub).toBe('ACT-03')
    expect(decoded.name).toBe('全権管理者')
  })
})

// ------------------------------------------
// GET /api/current
// ------------------------------------------
describe('GET /api/current', () => {
  let validToken

  beforeAll(async () => {
    const res = await request(app)
      .post('/api/switch')
      .send({ actorId: 'ACT-03' })
    validToken = res.body.token
  })

  it('should return decoded actor with a valid Bearer token', async () => {
    const res = await request(app)
      .get('/api/current')
      .set('Authorization', `Bearer ${validToken}`)
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('decoded')
    expect(res.body.decoded).toHaveProperty('sub', 'ACT-03')
    expect(res.body).toHaveProperty('actor')
    expect(res.body.actor).toHaveProperty('id', 'ACT-03')
  })

  it('should return 401 without a token', async () => {
    const res = await request(app).get('/api/current')
    expect(res.status).toBe(401)
    expect(res.body).toHaveProperty('error', 'No token provided')
  })

  it('should return 401 with an invalid token', async () => {
    const res = await request(app)
      .get('/api/current')
      .set('Authorization', 'Bearer invalid.token.here')
    expect(res.status).toBe(401)
    expect(res.body).toHaveProperty('error', 'Invalid token')
  })

  it('should return 401 with a malformed Authorization header', async () => {
    const res = await request(app)
      .get('/api/current')
      .set('Authorization', 'Token something')
    expect(res.status).toBe(401)
    expect(res.body).toHaveProperty('error', 'No token provided')
  })
})

// ------------------------------------------
// OIDC Discovery endpoints
// ------------------------------------------
describe('GET /.well-known/openid-configuration', () => {
  it('should return valid OIDC configuration', async () => {
    const res = await request(app).get('/.well-known/openid-configuration')
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('issuer')
    expect(res.body).toHaveProperty('authorization_endpoint')
    expect(res.body).toHaveProperty('token_endpoint')
    expect(res.body).toHaveProperty('jwks_uri')
    expect(res.body).toHaveProperty('response_types_supported')
    expect(res.body).toHaveProperty('subject_types_supported')
    expect(res.body).toHaveProperty('id_token_signing_alg_values_supported')
  })

  it('should have HS256 in signing algorithms', async () => {
    const res = await request(app).get('/.well-known/openid-configuration')
    expect(res.body.id_token_signing_alg_values_supported).toContain('HS256')
  })
})

describe('GET /.well-known/jwks.json', () => {
  it('should return keys array (empty for dev mock)', async () => {
    const res = await request(app).get('/.well-known/jwks.json')
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('keys')
    expect(Array.isArray(res.body.keys)).toBe(true)
    expect(res.body.keys).toEqual([])
  })
})

// ------------------------------------------
// POST /token (legacy endpoint)
// ------------------------------------------
describe('POST /token', () => {
  it('should return a token and ALB headers for a valid actorId', async () => {
    const res = await request(app)
      .post('/token')
      .send({ actorId: 'ACT-01' })
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('token')
    expect(res.body).toHaveProperty('headers')
    expect(res.body.headers).toHaveProperty('X-Amzn-Oidc-Data')
    expect(res.body.headers).toHaveProperty('X-Amzn-Oidc-Identity', 'ACT-01')
    expect(res.body.headers).toHaveProperty('X-Amzn-Oidc-Accesstoken')
  })

  it('should return 400 for an invalid actorId', async () => {
    const res = await request(app)
      .post('/token')
      .send({ actorId: 'INVALID' })
    expect(res.status).toBe(400)
    expect(res.body).toHaveProperty('error')
  })
})

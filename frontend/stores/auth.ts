import { defineStore } from 'pinia'
import type { CzPrincipal, CzPermissions } from '~/types/auth'

const AUTH_COOKIE = 'cz-auth-token'

const defaultPermissions: CzPermissions = {
  jinjiMode: false,
  tab010: { bit0: false, bit1: false, bit2: false },
  tab011: { bit0: false, bit1: false },
  tab012: { bit0: false, bit1: false },
  dataAuthority: { ref: null, ins: null, upd: null },
  employmentType: 0,
  staffRole: null,
  canDelegate: false,
}

function decodeBase64Url(str: string): string {
  const base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  return atob(padded)
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    return JSON.parse(decodeBase64Url(parts[1]))
  } catch {
    return null
  }
}

function payloadToPrincipal(payload: Record<string, unknown>): CzPrincipal {
  const perms = payload.permissions as Record<string, unknown> | undefined
  const dataAuth = payload.dataAuthority as Record<string, unknown> | undefined

  return {
    userId: String(payload.sub ?? ''),
    userName: String(payload.name ?? ''),
    email: String(payload.email ?? ''),
    organizationCode: String(payload.organizationCode ?? ''),
    organizationName: String(payload.organizationName ?? ''),
    permissions: {
      jinjiMode: Boolean(payload.jinjiMode),
      tab010: {
        bit0: Boolean(perms?.tab010 && (perms.tab010 as Record<string, unknown>).bit0),
        bit1: Boolean(perms?.tab010 && (perms.tab010 as Record<string, unknown>).bit1),
        bit2: Boolean(perms?.tab010 && (perms.tab010 as Record<string, unknown>).bit2),
      },
      tab011: {
        bit0: Boolean(perms?.tab011 && (perms.tab011 as Record<string, unknown>).bit0),
        bit1: Boolean(perms?.tab011 && (perms.tab011 as Record<string, unknown>).bit1),
      },
      tab012: {
        bit0: Boolean(perms?.tab012 && (perms.tab012 as Record<string, unknown>).bit0),
        bit1: Boolean(perms?.tab012 && (perms.tab012 as Record<string, unknown>).bit1),
      },
      dataAuthority: {
        ref: (dataAuth?.ref as string) ?? null,
        ins: (dataAuth?.ins as string) ?? null,
        upd: (dataAuth?.upd as string) ?? null,
      },
      employmentType: Number(payload.employmentType ?? 0) as 0 | 1 | 2 | 3,
      staffRole: payload.staffRole != null ? Number(payload.staffRole) : null,
      canDelegate: Boolean(payload.canDelegate),
    },
    delegationStaffId: (payload.delegationStaffId as string) ?? null,
  }
}

export const useAuthStore = defineStore('auth', () => {
  const principal = ref<CzPrincipal | null>(null)
  const actorId = ref<string | null>(null)

  const isAuthenticated = computed(() => principal.value !== null)
  const permissions = computed(() => principal.value?.permissions ?? defaultPermissions)
  const jinjiMode = computed(() => permissions.value.jinjiMode)

  function initFromJwt(): void {
    const cookie = useCookie(AUTH_COOKIE)
    const token = cookie.value
    if (!token) {
      principal.value = null
      return
    }

    const payload = decodeJwtPayload(token)
    if (!payload) {
      principal.value = null
      return
    }

    principal.value = payloadToPrincipal(payload)
  }

  async function switchActor(newActorId: string): Promise<void> {
    const config = useRuntimeConfig()
    const res = await $fetch<{ token: string }>(
      `${config.public.authMockUrl}/api/switch`,
      {
        method: 'POST',
        body: { actorId: newActorId },
      },
    )

    const cookie = useCookie(AUTH_COOKIE)
    cookie.value = res.token
    actorId.value = newActorId
    initFromJwt()
  }

  function logout(): void {
    principal.value = null
    actorId.value = null
    const cookie = useCookie(AUTH_COOKIE)
    cookie.value = null
  }

  function $reset(): void {
    principal.value = null
    actorId.value = null
  }

  return {
    principal,
    actorId,
    isAuthenticated,
    permissions,
    jinjiMode,
    initFromJwt,
    switchActor,
    logout,
    $reset,
  }
})

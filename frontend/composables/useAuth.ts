import type { CzAuth, CzPermissions } from '~/types/auth'
import { useAuthStore } from '~/stores/auth'

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

export function useAuth(): CzAuth {
  const store = useAuthStore()

  return {
    user: computed(() => store.principal),
    isAuthenticated: computed(() => store.isAuthenticated),
    jinjiMode: computed(() => store.permissions?.jinjiMode ?? false),
    permissions: computed(() => store.permissions ?? defaultPermissions),

    // Tab010 semantic aliases
    canReport: computed(() => store.permissions?.tab010?.bit0 ?? false),
    canManage: computed(() => store.permissions?.tab010?.bit1 ?? false),
    canFullAccess: computed(() => store.permissions?.tab010?.bit2 ?? false),

    // Tab011 semantic aliases
    canExportHours: computed(() => store.permissions?.tab011?.bit0 ?? false),
    canNavigateForms: computed(() => store.permissions?.tab011?.bit1 ?? false),

    // Tab012 semantic aliases
    canInputPeriod: computed(() => store.permissions?.tab012?.bit0 ?? false),
    canAggregate: computed(() => store.permissions?.tab012?.bit1 ?? false),

    // TODO: Implement StatusMatrixResolver integration
    canOperate: (_operation: string, _statusKey: string): boolean => {
      return false
    },

    hasDataAccess: (type: 'ref' | 'ins' | 'upd'): boolean => {
      return store.permissions?.dataAuthority?.[type] != null
    },
  }
}

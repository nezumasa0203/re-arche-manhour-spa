/**
 * CZ Auth Type Definitions
 *
 * 4-layer permission model defined in 02_actor_definition.md:
 *   Layer 1: JinjiMode (boolean)
 *   Layer 2: Tab permissions (tab010, tab011, tab012) with bit-level flags
 *   Layer 3: DataAuthority (ref / ins / upd scope)
 *   Layer 4: EmploymentType (0-3)
 */

// ---------------------------------------------------------------------------
// Tab Permissions
// ---------------------------------------------------------------------------

/** Base permission bits for a tab (bit0, bit1) */
export interface TabPermission {
  bit0: boolean
  bit1: boolean
  bit2?: boolean // only tab010 has bit2
}

/** tab010 always carries bit2 (full-access flag) */
export interface Tab010Permission extends TabPermission {
  bit2: boolean
}

// ---------------------------------------------------------------------------
// Data Authority
// ---------------------------------------------------------------------------

/**
 * Data-authority scope per operation.
 * null = no access, string value = scope key (e.g. "ALL", "OWN", org-code).
 */
export interface DataAuthority {
  ref: string | null
  ins: string | null
  upd: string | null
}

// ---------------------------------------------------------------------------
// Employment Type
// ---------------------------------------------------------------------------

/** Employment type code (0: regular, 1: contract, 2: temporary, 3: outsource) */
export type EmploymentType = 0 | 1 | 2 | 3

// ---------------------------------------------------------------------------
// CZ Permissions (aggregate)
// ---------------------------------------------------------------------------

/**
 * Full permission object decoded from JWT claims.
 * Maps to the 4-layer permission model.
 */
export interface CzPermissions {
  /** Layer 1 – Jinji (HR) mode toggle */
  jinjiMode: boolean

  /** Layer 2 – Tab-level permission bits */
  tab010: Tab010Permission
  tab011: TabPermission
  tab012: TabPermission

  /** Layer 3 – Data authority scope per operation */
  dataAuthority: DataAuthority

  /** Layer 4 – Employment type code */
  employmentType: EmploymentType

  /** Staff role identifier (null when not applicable) */
  staffRole: number | null

  /** Whether the user can delegate operations to another staff */
  canDelegate: boolean
}

// ---------------------------------------------------------------------------
// CZ Principal (authenticated user)
// ---------------------------------------------------------------------------

/**
 * Authenticated user principal decoded from the JWT payload.
 * Corresponds to the `X-Amzn-Oidc-Data` header content.
 */
export interface CzPrincipal {
  userId: string
  userName: string
  email: string
  organizationCode: string
  organizationName: string
  permissions: CzPermissions
  /** Staff ID when operating under delegation */
  delegationStaffId?: string | null
}

// ---------------------------------------------------------------------------
// Actor Info (for DevActorSwitcher)
// ---------------------------------------------------------------------------

/**
 * Lightweight actor descriptor returned by Auth Mock GET /actors.
 * Used by DevActorSwitcher to display switchable actors.
 */
export interface ActorInfo {
  id: string
  name: string
  jinjiMode: boolean
  modeName: string
  employmentType: number
  orgName: string
  staffRole: number | null
  tab010: string
}

// ---------------------------------------------------------------------------
// Composable return type
// ---------------------------------------------------------------------------

import type { ComputedRef } from 'vue'

/**
 * Return type of the useAuth() composable.
 */
export interface CzAuth {
  user: ComputedRef<CzPrincipal | null>
  isAuthenticated: ComputedRef<boolean>

  /** Layer 1 shortcut */
  jinjiMode: ComputedRef<boolean>

  /** Full permissions object */
  permissions: ComputedRef<CzPermissions>

  // -- Semantic aliases (tab010) --
  canReport: ComputedRef<boolean>
  canManage: ComputedRef<boolean>
  canFullAccess: ComputedRef<boolean>

  // -- Semantic aliases (tab011) --
  canExportHours: ComputedRef<boolean>
  canNavigateForms: ComputedRef<boolean>

  // -- Semantic aliases (tab012) --
  canInputPeriod: ComputedRef<boolean>
  canAggregate: ComputedRef<boolean>

  /**
   * Check whether the current user can perform an operation
   * against a given status key. Delegates to the status matrix.
   */
  canOperate: (operation: string, statusKey: string) => boolean

  /**
   * Check whether the current user has data-authority access
   * for the specified operation type.
   */
  hasDataAccess: (type: 'ref' | 'ins' | 'upd') => boolean
}

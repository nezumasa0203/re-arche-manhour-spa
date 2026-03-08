<script setup lang="ts">
import type { ActorInfo } from '~/types/auth'

const store = useAuthStore()
const config = useRuntimeConfig()

const actors = ref<ActorInfo[]>([])
const loading = ref(false)
const open = ref(false)

const currentUser = computed(() => store.principal)
const currentActorId = computed(() => store.actorId)

async function fetchActors() {
  if (actors.value.length > 0) return
  try {
    actors.value = await $fetch<ActorInfo[]>(`${config.public.authMockUrl}/api/actors`)
  } catch (e) {
    console.error('Failed to fetch actors:', e)
  }
}

async function switchTo(id: string) {
  if (id === currentActorId.value) return
  loading.value = true
  try {
    await store.switchActor(id)
    window.location.reload()
  } catch (e) {
    console.error('Failed to switch actor:', e)
    loading.value = false
  }
}

function toggle() {
  open.value = !open.value
  if (open.value) fetchActors()
}

const jinjiActors = computed(() => actors.value.filter(a => a.jinjiMode))
const kanriActors = computed(() => actors.value.filter(a => !a.jinjiMode))

const empTypeLabel = (t: number) =>
  ['正社員', '臨時1', '臨時2', '外部'][t] ?? String(t)

const modeBadge = computed(() =>
  currentUser.value?.permissions?.jinjiMode ? '人事' : '管理',
)
</script>

<template>
  <div class="dev-actor-switcher">
    <button class="dev-actor-trigger" @click="toggle">
      <span class="dev-badge">DEV</span>
      <span v-if="currentUser" class="dev-actor-name">
        {{ currentActorId ?? '未選択' }}
      </span>
      <span v-else class="dev-actor-name">未認証</span>
    </button>

    <div v-if="open" class="dev-actor-panel">
      <div class="dev-actor-header">
        <strong>アクター切替</strong>
        <button class="dev-close" @click="open = false">&times;</button>
      </div>

      <div v-if="currentUser" class="dev-current">
        <div class="dev-current-id">{{ currentActorId }}</div>
        <div class="dev-current-name">{{ currentUser.userName }}</div>
        <div class="dev-current-mode">Mode: {{ modeBadge }}</div>
        <div class="dev-current-org">{{ currentUser.organizationName }}</div>
      </div>

      <div v-if="loading" class="dev-loading">切替中...</div>

      <template v-if="!loading">
        <div class="dev-group-label">人事モード</div>
        <button
          v-for="actor in jinjiActors"
          :key="actor.id"
          class="dev-actor-item"
          :class="{ active: actor.id === currentActorId }"
          @click="switchTo(actor.id)"
        >
          <span class="dev-actor-radio">{{ actor.id === currentActorId ? '●' : '○' }}</span>
          <span class="dev-actor-id">{{ actor.id }}</span>
          <span class="dev-actor-label">{{ actor.name }}</span>
          <span class="dev-actor-emp">{{ empTypeLabel(actor.employmentType) }}</span>
        </button>

        <div class="dev-group-label">管理モード</div>
        <button
          v-for="actor in kanriActors"
          :key="actor.id"
          class="dev-actor-item"
          :class="{ active: actor.id === currentActorId }"
          @click="switchTo(actor.id)"
        >
          <span class="dev-actor-radio">{{ actor.id === currentActorId ? '●' : '○' }}</span>
          <span class="dev-actor-id">{{ actor.id }}</span>
          <span class="dev-actor-label">{{ actor.name }}</span>
          <span v-if="actor.staffRole" class="dev-actor-staff">S{{ actor.staffRole }}</span>
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.dev-actor-switcher {
  position: fixed;
  bottom: 16px;
  right: 16px;
  z-index: 99999;
  font-family: sans-serif;
  font-size: 13px;
}

.dev-actor-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #1e293b;
  color: #f1f5f9;
  border: 1px solid #475569;
  border-radius: 6px;
  cursor: pointer;
}

.dev-badge {
  background: #f59e0b;
  color: #1e293b;
  font-weight: 700;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
}

.dev-actor-panel {
  position: absolute;
  bottom: 40px;
  right: 0;
  width: 280px;
  max-height: 480px;
  overflow-y: auto;
  background: #1e293b;
  border: 1px solid #475569;
  border-radius: 8px;
  padding: 8px;
  color: #e2e8f0;
}

.dev-actor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 4px 8px;
  border-bottom: 1px solid #334155;
  margin-bottom: 8px;
}

.dev-close {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 18px;
  cursor: pointer;
}

.dev-current {
  background: #334155;
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 8px;
}

.dev-current-id { font-weight: 700; color: #38bdf8; }
.dev-current-name { color: #e2e8f0; }
.dev-current-mode { color: #a5b4fc; font-size: 12px; }
.dev-current-org { color: #94a3b8; font-size: 11px; }

.dev-loading {
  text-align: center;
  padding: 16px;
  color: #94a3b8;
}

.dev-group-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  padding: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.dev-actor-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 5px 6px;
  background: none;
  border: none;
  border-radius: 4px;
  color: #cbd5e1;
  cursor: pointer;
  text-align: left;
}

.dev-actor-item:hover { background: #334155; }
.dev-actor-item.active { background: #1e3a5f; color: #38bdf8; }

.dev-actor-radio { width: 14px; flex-shrink: 0; }
.dev-actor-id { font-weight: 600; font-size: 11px; width: 50px; flex-shrink: 0; }
.dev-actor-label { flex: 1; }
.dev-actor-emp { font-size: 10px; color: #94a3b8; }
.dev-actor-staff { font-size: 10px; color: #a5b4fc; }
</style>

<template>
  <NuxtPage />
  <ConfirmDialog />
  <DevActorSwitcher v-if="showActorSwitch" />
</template>

<script setup lang="ts">
const config = useRuntimeConfig()
const showActorSwitch = config.public.enableActorSwitch === 'true'
</script>

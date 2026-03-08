export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  if (config.public.enableActorSwitch !== 'true') {
    return
  }

  const store = useAuthStore()
  store.initFromJwt()
})

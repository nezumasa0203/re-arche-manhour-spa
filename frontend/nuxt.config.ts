export default defineNuxtConfig({
  devtools: { enabled: true },

  typescript: {
    strict: true,
  },

  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080',
      authMockUrl: process.env.NUXT_PUBLIC_AUTH_MOCK_URL || 'http://localhost:8180',
      enableActorSwitch: process.env.NUXT_PUBLIC_ENABLE_ACTOR_SWITCH || '',
    },
  },

  // API プロキシ（開発時のみ）
  nitro: {
    devProxy: {
      '/api': {
        target: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  modules: ['@pinia/nuxt', '@primevue/nuxt-module'],

  primevue: {
    options: {
      theme: 'none',
    },
  },

  compatibilityDate: '2025-01-01',
})

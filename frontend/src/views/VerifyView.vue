<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const message = ref('驗證中…')
onMounted(async () => {
  try {
    const result = await http.post('/api/auth/verify', { token: route.query.token || '' })
    message.value = result.data.message
  } catch {
    message.value = '驗證連結無效或已過期'
  }
})
</script>

<template>
  <main class="verify-page">
    <h1>信箱驗證</h1>
    <p role="status">{{ message }}</p>
    <router-link to="/login">前往登入</router-link>
  </main>
</template>

<style scoped>
.verify-page { min-height: 100vh; display: grid; place-content: center; gap: 12px; text-align: center; background: var(--pale); }
</style>

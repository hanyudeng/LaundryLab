<script setup>
import { ref } from 'vue'
import http from '../api/http'

const email = ref('')
const password = ref('')
const message = ref('')
const error = ref('')
const busy = ref(false)

async function register() {
  message.value = ''
  error.value = ''
  busy.value = true
  try {
    const response = await http.post('/api/auth/register', { email: email.value, password: password.value })
    message.value = response.data.message
  } catch (cause) {
    error.value = cause.response?.status === 409 ? '此信箱已註冊' : '註冊失敗，請確認學校信箱及至少 8 字元密碼'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <form class="auth-card" @submit.prevent="register">
      <h1>學生註冊</h1>
      <label>學校信箱<input v-model.trim="email" type="email" autocomplete="email" required placeholder="學號@o365.fcu.edu.tw" /></label>
      <label>密碼<input v-model="password" type="password" autocomplete="new-password" minlength="8" required /></label>
      <p v-if="message" role="status">{{ message }}</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" :disabled="busy">{{ busy ? '送出中…' : '註冊並寄送驗證信' }}</button>
      <router-link to="/login">返回登入</router-link>
    </form>
  </main>
</template>

<style scoped>
.auth-page { min-height: 100vh; display: grid; place-items: center; background: var(--pale); padding: 24px; }
.auth-card { width: min(100%, 420px); display: grid; gap: 18px; background: white; padding: 32px; border-radius: 16px; box-shadow: var(--shadow); }
label { display: grid; gap: 8px; }
input { padding: 12px; border: 1px solid var(--line); border-radius: 8px; font: inherit; }
button { padding: 12px; border: 0; border-radius: 8px; background: var(--navy); color: white; cursor: pointer; }
.error { color: var(--red); }
</style>

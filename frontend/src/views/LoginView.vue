<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { roleHome, useAuthStore } from '../stores/auth'
import AppHeader from '../components/AppHeader.vue'

const router = useRouter()
const authStore = useAuthStore()

// 輸入框的值
const email = ref('')
const password = ref('')

// 密碼要不要顯示明碼
const showPassword = ref(false)

// 錯誤訊息，空字串代表沒有錯誤
const emailError = ref('')
const passwordError = ref('')
const submitError = ref('')
const submitting = ref(false)

function togglePassword() {
  showPassword.value = !showPassword.value
}

async function handleSubmit() {
  // 檢查有沒有填，沒填就塞錯誤訊息
  emailError.value = email.value.trim() ? '' : '請填寫學號信箱'
  passwordError.value = password.value.trim() ? '' : '請填寫密碼'

  // 有錯誤就不要往下跳轉
  if (emailError.value || passwordError.value) return

  submitError.value = ''
  submitting.value = true
  try {
    const user = await authStore.login(email.value, password.value)
    await router.push(roleHome[user.role] || '/login')
  } catch (error) {
    submitError.value = error.response?.status === 403
      ? '帳號尚未驗證或已停用'
      : '帳號或密碼不正確'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <AppHeader centered max-width="560px" />
    <div class="login-wrap">
      <div class="login-card">
        <div class="login-intro">
          <h1>登入</h1>
        </div>
        <form class="login-form" novalidate @submit.prevent="handleSubmit">
          <label for="login-email">帳號</label>
          <input
            id="login-email"
            v-model="email"
            type="email"
            placeholder="學號@o365.fcu.edu.tw"
          />
          <p v-if="emailError" class="field-error">⚠ {{ emailError }}</p>

          <label for="login-password">密碼</label>
          <div class="password-wrap">
            <input
              id="login-password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="請輸入密碼"
            />
            <!-- 根據目前是否顯示明碼，換成對應的眼睛圖示 -->
            <button
              type="button"
              class="password-toggle"
              :aria-label="showPassword ? '隱藏密碼' : '顯示密碼'"
              @click="togglePassword"
            >
              <span class="material-symbols-outlined">
                {{ showPassword ? 'visibility_off' : 'visibility' }}
              </span>
            </button>
          </div>
          <p v-if="passwordError" class="field-error">⚠ {{ passwordError }}</p>

          <div class="login-actions">
            <p v-if="submitError" class="field-error">{{ submitError }}</p>
            <button class="btn" type="submit" :disabled="submitting">{{ submitting ? '登入中…' : '登入' }}</button>
            <p><router-link to="/register">學生首次使用？註冊並驗證學校信箱</router-link></p>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 整個登入頁的外框，背景鋪滿 */
.login-page {
  min-height: 100vh;
  background: var(--pale);
}

/* header 下面放卡片的區域，固定上邊距、左右置中 */
.login-wrap {
  max-width: 560px;
  margin: 74px auto 60px;
  padding: 0 24px;
}

/* 中間白色卡片本體 */
.login-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 40px 30px;
  box-shadow: var(--shadow);
}

/* 卡片最上面「登入」標題區塊 */
.login-intro {
  text-align: center;
  margin-bottom: 32px;
}

.login-intro h1 {
  font-size: 25px;
  color: var(--navy);
  margin: 0;
}

/* 帳號/密碼欄位上方的文字（label） */
.login-form label {
  display: block;
  font-weight: 600;
  margin: 20px 0 6px;
  color: #40566a;
}

/* 輸入框本身的樣式 */
.login-form input {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 9px;
  padding: 12px;
  background: #fff;
  color: var(--text);
  font: inherit;
}

/* 密碼欄位的外層容器，用來定位右邊的眼睛按鈕 */
.password-wrap {
  position: relative;
}

.password-wrap input {
  padding-right: 48px;
}

/* 密碼欄位右邊「顯示/隱藏密碼」的眼睛按鈕 */
.password-toggle {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  border: 0;
  background: transparent;
  color: #71869a;
  padding: 7px;
  display: grid;
  place-items: center;
  cursor: pointer;
}

/* 欄位下方紅字錯誤提示 */
.field-error {
  color: var(--red);
  font-size: 13px;
  margin: 5px 0 0;
}

/* 最下面的登入按鈕 */
.login-actions {
  margin-top: 20px;
}

.login-actions .btn {
  width: 100%;
  border: 0;
  border-radius: 10px;
  background: var(--navy);
  color: #fff;
  padding: 12px;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
}
</style>

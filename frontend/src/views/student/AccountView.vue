<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { ref, onMounted } from 'vue'
import http from '../../api/http'

const router = useRouter()
const authStore = useAuthStore()
const line = ref({ linked: false, following: false, botUrl: '' })
const linkCode = ref('')
const lineError = ref('')
const showLine = ref(false)
async function loadLine() {
  try { line.value = (await http.get('/api/student/line')).data; lineError.value = '' }
  catch { lineError.value = '無法取得 LINE 綁定狀態' }
}
onMounted(loadLine)
async function createLinkCode() {
  try { linkCode.value = (await http.post('/api/student/line/link-code')).data.code; lineError.value = '' }
  catch { lineError.value = '無法建立綁定碼' }
}
async function unlinkLine() {
  try { await http.delete('/api/student/line'); linkCode.value = ''; await loadLine() }
  catch { lineError.value = '解除綁定失敗' }
}

// 登出：清掉 store 裡的登入狀態，跳回登入頁
async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="account-view">
    <div class="page-head">
      <h1>個人設定</h1>
    </div>

    <div class="account-card">
      <div class="profile">
        <div class="avatar">我</div>
        <div>
          <strong>{{ authStore.email || '尚未登入' }}</strong>
          <div class="sub">學生帳號</div>
        </div>
      </div>

      <button type="button" class="account-row" @click="router.push('/student/reminders')">
        <span class="row-label">
          <span class="material-symbols-outlined row-icon">history</span>
          訂閱紀錄
        </span>
        <span>›</span>
      </button>

      <button type="button" class="account-row" @click="router.push('/student/reports')">
        <span class="row-label">
          <span class="material-symbols-outlined row-icon">warning</span>
          問題回報
        </span>
        <span>›</span>
      </button>

      <button type="button" class="account-row" @click="showLine = !showLine; loadLine()">
        <span class="row-label">
          <span class="material-symbols-outlined row-icon">notifications</span>
          通知設定
        </span>
        <span>›</span>
      </button>
      <div v-if="showLine" class="line-settings">
        <p v-if="lineError" role="alert">{{ lineError }}</p>
        <p>LINE：{{ line.linked ? (line.following ? '已綁定並追蹤' : '已綁定，請追蹤官方帳號') : '尚未綁定' }}</p>
        <template v-if="!line.linked">
          <p>先加入 LINE 官方帳號，取得綁定碼後，在 LINE 對話傳送「link 空格 綁定碼」。綁定碼十分鐘內有效；完成後按「重新檢查」。你可以隨時解除綁定。</p>
          <a v-if="line.botUrl" :href="line.botUrl" target="_blank" rel="noopener noreferrer">開啟 LINE 官方帳號</a>
          <button type="button" @click="createLinkCode">取得綁定碼</button>
          <p v-if="linkCode"><code>link {{ linkCode }}</code></p>
          <button type="button" @click="loadLine">重新檢查</button>
        </template>
        <button v-else type="button" @click="unlinkLine">解除 LINE 綁定並取消提醒</button>
      </div>

      <button type="button" class="account-row logout" @click="handleLogout">
        <span class="row-label">
          <span class="material-symbols-outlined row-icon">logout</span>
          登出
        </span>
        <span>›</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.line-settings { padding: 20px; display: grid; gap: 10px; }
.line-settings button { justify-self: start; padding: 8px 12px; cursor: pointer; }
.line-settings code { overflow-wrap: anywhere; }
.page-head {
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 25px;
  color: var(--navy);
  margin: 0;
}

.account-card {
  max-width: 900px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow);
}

/* 頂部的頭像跟基本資料區塊 */
.profile {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: var(--sky);
}

.avatar {
  width: 55px;
  height: 55px;
  border-radius: 50%;
  background: #d7eafa;
  color: var(--navy);
  display: grid;
  place-items: center;
  font-size: 23px;
  font-weight: 700;
}

.profile .sub {
  font-size: 13px;
  color: var(--muted);
  margin-top: 2px;
}

/* 下面每一條可點擊的列表項目 */
.account-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border: 0;
  border-top: 1px solid var(--line);
  background: #fff;
  text-align: left;
  color: var(--text);
  cursor: pointer;
  font: inherit;
}

.account-row:hover {
  background: var(--pale);
}

.row-label {
  display: flex;
  align-items: center;
  gap: 12px;
}

.row-icon {
  color: #617f99;
}

/* 登出那一列文字跟圖示改成紅色，跟其他列區分 */
.account-row.logout {
  color: var(--red);
}

.account-row.logout .row-icon {
  color: var(--red);
}
</style>

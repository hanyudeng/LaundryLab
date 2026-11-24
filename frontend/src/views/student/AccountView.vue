<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 登出：清掉 store 裡的登入狀態，跳回登入頁
function handleLogout() {
  authStore.logout()
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
          <div class="sub">第一學生宿舍</div>
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

      <button type="button" class="account-row">
        <span class="row-label">
          <span class="material-symbols-outlined row-icon">notifications</span>
          通知設定
        </span>
        <span>›</span>
      </button>

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
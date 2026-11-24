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
      <p>管理員個人資料</p>
    </div>

    <div class="account-card">
      <div class="profile">
        <div class="avatar">王</div>
        <div>
          <strong>王經理</strong>
          <div class="sub">管理員帳號：{{ authStore.email || 'admin01' }}</div>
          <div class="sub">管理範圍：全部宿舍</div>
        </div>
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
.page-head {
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 25px;
  color: var(--navy);
  margin: 0;
}

.page-head p {
  margin: 3px 0 0;
  color: var(--muted);
  font-size: 13px;
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

/* 登出那一列 */
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

/* 登出文字跟圖示用紅色，跟其他列區分 */
.account-row.logout {
  color: var(--red);
}

.account-row.logout .row-icon {
  color: var(--red);
}
</style>
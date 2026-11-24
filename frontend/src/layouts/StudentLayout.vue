<script setup>
import AppHeader from '../components/AppHeader.vue'

// 底部導覽列的 4 個入口
const navItems = [
  { to: '/student/equipment', icon: 'local_laundry_service', label: '設備總覽' },
  { to: '/student/reminders', icon: 'notifications_active', label: '我的提醒' },
  { to: '/student/reports', icon: 'warning', label: '問題回報' },
  { to: '/student/account', icon: 'person', label: '個人設定' },
]
</script>

<template>
  <div class="student-layout">
    <AppHeader max-width="1160px" />
    <main class="content">
      <!-- 目前網址對應到的頁面內容會顯示在這裡 -->
      <router-view />
    </main>
    <nav class="bottom-nav">
  <div class="nav-inner">
    <RouterLink
      v-for="item in navItems"
      :key="item.to"
      :to="item.to"
      class="nav-item"
      active-class="active"
    >
      <span class="material-symbols-outlined">{{ item.icon }}</span>
      <span class="label">{{ item.label }}</span>
    </RouterLink>
  </div>
</nav>
  </div>
</template>

<style scoped>
/* 整個學生端外殼，底部留白讓內容不會被導覽列蓋住 */
.student-layout {
  min-height: 100vh;
  background: var(--pale);
  padding-bottom: 76px;
}

/* 中間內容區域，寬度比底部導覽列寬（1160px vs 760px），螢幕夠寬時卡片才能一排放更多張 */
.content {
  max-width: 1160px;
  margin: 0 auto;
  padding: 20px 24px;
}


/* 固定在畫面最下面的白色長條，寬度鋪滿整個畫面 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid var(--line);
  padding: 8px 24px;
}
/* 導覽列裡面實際放 4 個按鈕的區域，寬度跟上面內容對齊、置中 */
.nav-inner {
  max-width: 760px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
}

/* 導覽列單一個項目 */
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 7px 4px;
  border-radius: 12px;
  color: #768b9e;
  text-decoration: none;
  font-size: 12px;
}

/* 目前所在的那一頁，項目會變成這個樣式 */
.nav-item.active {
  background: var(--sky);
  color: var(--navy);
  font-weight: 700;
}
</style>
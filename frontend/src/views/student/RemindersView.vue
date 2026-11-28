<script setup>
import { computed, onMounted } from 'vue'
import MachineCard from '../../components/MachineCard.vue'
import { useMachinesStore } from '../../stores/machines'
import { useCountdown } from '../../composables/useCountdown'

const machinesStore = useMachinesStore()
onMounted(() => { machinesStore.loadMachines(); machinesStore.loadReminders() })
const { now } = useCountdown()

// 根據狀態決定卡片上要顯示的說明文字，跟設備總覽同一套邏輯
function detailText(m) {
  if (m.status === '使用中') {
    const remain = Math.max(0, m.end - now.value)
    const mm = Math.floor(remain / 60000)
    const ss = Math.floor((remain % 60000) / 1000)
    return `剩餘時間：約 ${mm}:${String(ss).padStart(2, '0')}`
  }
  if (m.status === '待確認') return `最後訊號：${m.signal}`
  return ''
}

// 只留下「已訂閱」的機台
const subscribedMachines = computed(() => {
  return machinesStore.machines
    .filter((m) => machinesStore.subscribed.has(m.cycleId))
    .map((m) => ({
      ...m,
      detail: detailText(m),
      bell: true,
    }))
})
</script>

<template>
  <div class="reminders-view">
    <div class="page-head">
      <h1>我的提醒</h1>
    </div>
    <p v-if="machinesStore.reminderError || machinesStore.error" role="alert">{{ machinesStore.reminderError || machinesStore.error }}</p>

    <div class="machine-list">
      <MachineCard
  v-for="m in subscribedMachines"
  :key="m.code"
  :machine="m"
>
  <template #action>
    <button
      type="button"
      class="bell on"
      aria-label="取消訂閱提醒"
    @click="machinesStore.toggleBell(m.cycleId)"
    >
      <span class="material-symbols-outlined">notifications_active</span>
    </button>
  </template>
</MachineCard>
    </div>

    <p v-if="subscribedMachines.length === 0" class="empty">目前沒有訂閱提醒的機台</p>
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

/* 單欄列表排版，跟設備總覽的多欄格線不同 */
.machine-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

/* 沒有訂閱任何機台時顯示的提示 */
.empty {
  text-align: center;
  color: var(--muted);
  padding: 30px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
}
/* 鈴鐺按鈕，這頁都是已訂閱狀態固定深藍色 */
.bell {
  border: 0;
  background: transparent;
  color: var(--navy);
  cursor: pointer;
  padding: 8px;
  display: grid;
  place-items: center;
}
</style>

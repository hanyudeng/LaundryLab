<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import FloorPlan from '../../components/FloorPlan.vue'
import { useMachinesStore } from '../../stores/machines'
import { useCountdown } from '../../composables/useCountdown'

const machinesStore = useMachinesStore()
let refreshTimer
onMounted(() => {
  machinesStore.loadMachines()
  refreshTimer = window.setInterval(() => { if (!document.hidden) machinesStore.loadMachines(true) }, 30000)
  machinesStore.loadReminders()
})
onUnmounted(() => window.clearInterval(refreshTimer))
const { now } = useCountdown()
// 下拉選單、篩選 chip 的選項
const dorms = computed(() => ['全部宿舍', ...machinesStore.dormitories.map((d) => d.name)])
const rooms = computed(() => ['全部洗衣間', ...machinesStore.rooms
  .filter((r) => selectedDorm.value === '全部宿舍' || machinesStore.dormitories.find((d) => d.id === r.dormitoryId)?.name === selectedDorm.value)
  .map((r) => r.name)])
const statusList = ['全部', '可用', '使用中', '待確認', '待檢查', '維修中']

// 目前選到的篩選條件
const selectedDorm = ref('全部宿舍')
const selectedRoom = ref('全部洗衣間')
const activeStatus = ref('全部')



// 根據狀態決定卡片上要顯示的說明文字
function detailText(m) {
  if (m.status === '使用中') {
    const remain = Math.max(0, m.end - now.value)
    const mm = Math.floor(remain / 60000)
    const ss = Math.floor((remain % 60000) / 1000)
    return `剩餘時間：約 ${mm}:${String(ss).padStart(2, '0')}`
  }
  if (m.status === '待確認') return `最後訊號：${m.signal}`
  if (m.status === '維修中') return '設備維修中'
  if (m.status === '待檢查') return '已有問題回報，請等候檢查'
  return '空間中，可立即使用'
}

// 套用篩選條件、加上顯示用的欄位、排序後的機台清單
const floorPlans = computed(() => machinesStore.rooms
  .filter((room) => selectedDorm.value === '全部宿舍' || machinesStore.dormitories.find((d) => d.id === room.dormitoryId)?.name === selectedDorm.value)
  .filter((room) => selectedRoom.value === '全部洗衣間' || room.name === selectedRoom.value)
  .map((room) => ({ ...room, dorm: machinesStore.dormitories.find((d) => d.id === room.dormitoryId)?.name,
    machines: machinesStore.machines.filter((m) => m.roomId === room.id).map((m) => ({
      ...m, detail: detailText(m), bell: ['使用中', '待確認'].includes(m.status) ? machinesStore.subscribed.has(m.cycleId) : null,
    })) })))

</script>

<template>
  <div class="equipment-view">
    <div class="page-head">
      <h1>設備總覽</h1>
      <div class="selectors">
        <select v-model="selectedDorm" class="select" @change="selectedRoom = '全部洗衣間'">
          <option v-for="d in dorms" :key="d">{{ d }}</option>
        </select>
        <select v-model="selectedRoom" class="select">
          <option v-for="r in rooms" :key="r">{{ r }}</option>
        </select>
      </div>
    </div>

    <div class="filters">
      <button
        v-for="s in statusList"
        :key="s"
        type="button"
        class="chip"
        :class="{ selected: activeStatus === s }"
        @click="activeStatus = s"
      >
        {{ s }}
             </button>
    </div>

    <p v-if="machinesStore.error" role="alert">{{ machinesStore.error }} <button type="button" @click="machinesStore.loadMachines()">重試</button></p>
    <p v-if="machinesStore.reminderError" role="alert">{{ machinesStore.reminderError }}</p>
    <p v-else-if="machinesStore.loading" role="status">載入設備中…</p>
    <div v-else class="floor-plans">
      <section v-for="room in floorPlans" :key="room.id" class="floor-section">
        <h2>{{ room.dorm }} · {{ room.name }}</h2>
        <FloorPlan :machines="room.machines" :entrance-side="room.entranceSide" :active-status="activeStatus">
          <template #action="{ machine }">
            <button v-if="machine.bell !== null" type="button" class="bell" :class="{ on: machine.bell }"
              :aria-label="machine.bell ? '取消訂閱提醒' : '訂閱提醒'" @click="machinesStore.toggleBell(machine.cycleId)">
              <span class="material-symbols-outlined">{{ machine.bell ? 'notifications_active' : 'notifications_off' }}</span>
            </button>
          </template>
        </FloorPlan>
      </section>
      <p v-if="floorPlans.length === 0" class="empty">這個宿舍目前沒有洗衣間</p>
    </div>

    <p class="hint">ⓘ 可用狀態僅供參考，實際仍以現場為準</p>
  </div>
</template>

<style scoped>
/* 標題跟右邊篩選下拉的排版 */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 25px;
  color: var(--navy);
  margin: 0;
}

.selectors {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.select {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 10px;
  padding: 10px 14px;
  color: var(--text);
  font: inherit;
}

/* 狀態篩選 chip 列，排序按鈕靠右放 */
.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.chip {
  border: 1px solid var(--line);
  background: #fff;
  color: #466077;
  border-radius: 999px;
  padding: 7px 15px;
  cursor: pointer;
  font: inherit;
}

.chip.selected {
  background: var(--navy);
  color: #fff;
  border-color: var(--navy);
}



/* 每個樓層保留自己的固定平面圖 */
.floor-plans { display: grid; gap: 24px; }
.floor-section h2 { color: var(--navy); font-size: 18px; margin: 0 0 10px; }

/* 沒有符合條件時顯示的提示 */
.empty {
  grid-column: 1 / -1;
  text-align: center;
  color: var(--muted);
  padding: 30px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
}

/* 最下面的小提示文字 */
.hint {
  background: var(--sky);
  color: #526c81;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  margin-top: 16px;
}
/* 鈴鐺按鈕，預設灰色 */
.bell {
  border: 0;
  background: transparent;
  color: #7790a5;
  cursor: pointer;
  padding: 8px;
  display: grid;
  place-items: center;
}

/* 已訂閱狀態下，鈴鐺變成深藍色 */
.bell.on {
  color: var(--navy);
}
</style>

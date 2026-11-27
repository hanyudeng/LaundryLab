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
})
onUnmounted(() => window.clearInterval(refreshTimer))
const { now } = useCountdown()

const dorms = computed(() => ['全部宿舍', ...machinesStore.dormitories.map((d) => d.name)])
const rooms = computed(() => ['全部洗衣間', ...machinesStore.rooms
  .filter((r) => selectedDorm.value === '全部宿舍' || machinesStore.dormitories.find((d) => d.id === r.dormitoryId)?.name === selectedDorm.value)
  .map((r) => r.name)])
const statusList = ['全部', '可用', '使用中', '待確認', '待檢查', '維修中']

const selectedDorm = ref('全部宿舍')
const selectedRoom = ref('全部洗衣間')
const activeStatus = ref('全部')
const searchText = ref('')
const actionError = ref('')
async function toggleMaintenance(machine) {
  const reason = window.prompt(machine.status === '維修中' ? '請填寫解除維修原因' : '請填寫設為維修中的原因')
  if (!reason?.trim()) return
  try { await machinesStore.toggleMaintenance(machine.id, reason.trim()); actionError.value = '' }
  catch { actionError.value = '更新維修狀態失敗' }
}

// 根據狀態決定卡片上要顯示的說明文字，跟學生端同一套邏輯
function detailText(m) {
  if (m.status === '使用中') {
    const remain = Math.max(0, m.end - now.value)
    const mm = Math.floor(remain / 60000)
    const ss = Math.floor((remain % 60000) / 1000)
    return `剩餘時間：約 ${mm}:${String(ss).padStart(2, '0')}`
  }
  if (m.status === '待確認') return `最後訊號：${m.signal}`
  if (m.status === '維修中') return '設備維修中'
  if (m.status === '待檢查') return '已有問題回報，請檢查處理'
  return '空間中，可立即使用'
}

// 套用篩選條件（含搜尋機台編號）、排序後的機台清單
const floorPlans = computed(() => machinesStore.rooms
  .filter((room) => selectedDorm.value === '全部宿舍' || machinesStore.dormitories.find((d) => d.id === room.dormitoryId)?.name === selectedDorm.value)
  .filter((room) => selectedRoom.value === '全部洗衣間' || room.name === selectedRoom.value)
  .map((room) => ({ ...room, dorm: machinesStore.dormitories.find((d) => d.id === room.dormitoryId)?.name,
    machines: machinesStore.machines.filter((m) => m.roomId === room.id).map((m) => ({ ...m, detail: detailText(m) })) })))

</script>

<template>
  <div class="equipment-view">
    <div class="page-head">
      <div>
        <h1>設備總覽</h1>
        <p>管理受指派宿舍的洗衣設備</p>
      </div>
      <div class="selectors">
        <select v-model="selectedDorm" class="select" @change="selectedRoom = '全部洗衣間'">
          <option v-for="d in dorms" :key="d">{{ d }}</option>
        </select>
        <select v-model="selectedRoom" class="select">
          <option v-for="r in rooms" :key="r">{{ r }}</option>
        </select>
        <label class="search-box">
          <span class="material-symbols-outlined">search</span>
          <input v-model="searchText" type="search" placeholder="搜尋機台編號" />
        </label>
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

    <p v-if="machinesStore.error || actionError" role="alert">{{ machinesStore.error || actionError }} <button type="button" @click="machinesStore.loadMachines()">重試</button></p>
    <p v-else-if="machinesStore.loading" role="status">載入設備中…</p>
    <div v-else class="floor-plans">
      <section v-for="room in floorPlans" :key="room.id" class="floor-section">
        <h2>{{ room.dorm }} · {{ room.name }}</h2>
        <FloorPlan :machines="room.machines" :entrance-side="room.entranceSide" :active-status="activeStatus" :search-text="searchText">
          <template #action="{ machine }">
            <button type="button" class="icon-action" :class="{ release: machine.status === '維修中' }"
              :aria-label="machine.status === '維修中' ? '解除維修' : '設為維修中'" @click="toggleMaintenance(machine)">
              <span class="material-symbols-outlined">build</span>
            </button>
          </template>
        </FloorPlan>
      </section>
      <p v-if="floorPlans.length === 0" class="empty">這個宿舍目前沒有洗衣間</p>
    </div>

    <p class="hint">設備狀態會依現場訊號更新。</p>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
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

/* 搜尋框，圖示跟輸入框放在同一個框框裡 */
.search-box {
  display: flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  padding: 0 11px;
}

.search-box .material-symbols-outlined {
  color: #6f879b;
  font-size: 21px;
}

.search-box input {
  border: 0;
  outline: 0;
  padding: 10px 0;
  color: var(--text);
  background: transparent;
  font: inherit;
  width: 150px;
}

.filters {
  display: flex;
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

.floor-plans { display: grid; gap: 24px; }
.floor-section h2 { color: var(--navy); font-size: 18px; margin: 0 0 10px; }

.empty {
  grid-column: 1 / -1;
  text-align: center;
  color: var(--muted);
  padding: 30px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
}

.hint {
  background: var(--sky);
  color: #526c81;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  margin-top: 16px;
}

/* 維修切換按鈕，預設灰色外框 */
.icon-action {
  width: 38px;
  height: 38px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  color: #7e91a2;
  display: grid;
  place-items: center;
  cursor: pointer;
}

/* 已經是維修中狀態，按鈕變紅色，代表按下去會解除維修 */
.icon-action.release {
  border-color: #f1c6c8;
  background: #fcebec;
  color: var(--red);
}
</style>

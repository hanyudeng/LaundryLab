<script setup>
import { ref, computed, onMounted } from 'vue'
import MachineCard from '../../components/MachineCard.vue'
import { useMachinesStore } from '../../stores/machines'
import { useCountdown } from '../../composables/useCountdown'

const machinesStore = useMachinesStore()
onMounted(() => {
  machinesStore.loadMachines()
})
const { now } = useCountdown()

const dorms = ['全部宿舍', '第一學生宿舍', '第二學生宿舍']
const rooms = ['全部洗衣間', 'A 棟洗衣間', 'B 棟洗衣間']
const statusList = ['全部', '可用', '使用中', '待確認', '維修中']

const selectedDorm = ref('全部宿舍')
const selectedRoom = ref('全部洗衣間')
const activeStatus = ref('全部')
const searchText = ref('')

// 狀態的排序優先順序，可用排最前面，維修中排最後面
const statusRank = { 可用: 0, 使用中: 1, 待確認: 2, 維修中: 3 }

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
  return '空間中，可立即使用'
}

// 套用篩選條件（含搜尋機台編號）、排序後的機台清單
const filteredMachines = computed(() => {
  return machinesStore.machines
    .filter((m) => activeStatus.value === '全部' || m.status === activeStatus.value)
    .filter((m) => selectedDorm.value === '全部宿舍' || m.dorm === selectedDorm.value)
    .filter((m) => selectedRoom.value === '全部洗衣間' || m.room === selectedRoom.value)
    .filter((m) => m.code.toLowerCase().includes(searchText.value.trim().toLowerCase()))
    .map((m) => ({
      ...m,
      detail: detailText(m),
    }))
    .sort((a, b) => statusRank[a.status] - statusRank[b.status] || a.code.localeCompare(b.code))
})
</script>

<template>
  <div class="equipment-view">
    <div class="page-head">
      <div>
        <h1>設備總覽</h1>
        <p>管理受指派宿舍的洗衣設備</p>
      </div>
      <div class="selectors">
        <select v-model="selectedDorm" class="select">
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

    <div class="machine-grid">
      <MachineCard v-for="m in filteredMachines" :key="m.code" :machine="m">
        <template #action>
          <button
            type="button"
            class="icon-action"
            :class="{ release: m.status === '維修中' }"
            :aria-label="m.status === '維修中' ? '解除維修' : '設為維修中'"
            @click="machinesStore.toggleMaintenance(m.code)"
          >
            <span class="material-symbols-outlined">build</span>
          </button>
        </template>
      </MachineCard>
      <p v-if="filteredMachines.length === 0" class="empty">目前沒有符合條件的設備</p>
    </div>

    <p class="hint">設備狀態會依現場訊號更新；維修設定僅供原型操作示意。</p>
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

.machine-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

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
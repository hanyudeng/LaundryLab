<script setup>
import { ref, computed } from 'vue'
import MachineCard from '../../components/MachineCard.vue'
import { useMachinesStore } from '../../stores/machines'
import { useCountdown } from '../../composables/useCountdown'

const machinesStore = useMachinesStore()
const { now } = useCountdown()
// 狀態的排序優先順序，可用排最前面，維修中排最後面
const statusRank = { 可用: 0, 使用中: 1, 待確認: 2, 維修中: 3 }

// 下拉選單、篩選 chip 的選項
const dorms = ['全部宿舍', '第一學生宿舍', '第二學生宿舍']
const rooms = ['全部洗衣間', 'A 棟洗衣間', 'B 棟洗衣間']
const statusList = ['全部', '可用', '使用中', '待確認', '維修中']

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
  return '空間中，可立即使用'
}

// 套用篩選條件、加上顯示用的欄位、排序後的機台清單
const filteredMachines = computed(() => {
  return machinesStore.machines
    .filter((m) => activeStatus.value === '全部' || m.status === activeStatus.value)
    .filter((m) => selectedDorm.value === '全部宿舍' || m.dorm === selectedDorm.value)
    .filter((m) => selectedRoom.value === '全部洗衣間' || m.room === selectedRoom.value)
    .map((m) => ({
      ...m,
      detail: detailText(m),
      // 只有使用中、待確認的機台才需要鈴鐺按鈕
      bell: ['使用中', '待確認'].includes(m.status) ? machinesStore.subscribed.has(m.code) : null,
    }))
    .sort((a, b) => {
      const group = statusRank[a.status] - statusRank[b.status]
      if (group !== 0) return group
      // 狀態一樣的情況下，使用中的機台依照剩餘時間排序
      if (a.status === '使用中' && b.status === '使用中') {
        return a.end - b.end
      }
      return a.code.localeCompare(b.code)
    })
})
</script>

<template>
  <div class="equipment-view">
    <div class="page-head">
      <h1>設備總覽</h1>
      <div class="selectors">
        <select v-model="selectedDorm" class="select">
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

    <div class="machine-grid">
      <MachineCard
        v-for="m in filteredMachines"
        :key="m.code"
        :machine="m"
        @toggle-bell="machinesStore.toggleBell"
      />
      <p v-if="filteredMachines.length === 0" class="empty">沒有符合條件的機台</p>
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



/* 機台卡片的格線排版 */
.machine-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

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
</style>
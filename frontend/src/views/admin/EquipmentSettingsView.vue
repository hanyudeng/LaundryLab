<script setup>
import { ref, computed } from 'vue'
import { useFacilitiesStore } from '../../stores/facilities'

const facilitiesStore = useFacilitiesStore()

// 搜尋機台編號、依宿舍/洗衣間篩選、啟用狀態篩選
const search = ref('')
const dormitoryFilter = ref('')
const roomFilter = ref('')
const statusFilter = ref('全部')

function roomsOf(dormitoryId) {
  return facilitiesStore.rooms.filter((r) => r.dormitoryId === dormitoryId)
}

function roomOf(roomId) {
  return facilitiesStore.rooms.find((r) => r.id === roomId) || null
}

function matchesStatus(item) {
  if (statusFilter.value === '啟用中') return item.isActive
  if (statusFilter.value === '已停用') return !item.isActive
  return true
}

// 某個洗衣間底下符合搜尋/啟用狀態篩選的機台
function machinesOfRoom(roomId) {
  let list = facilitiesStore.machines.filter((m) => m.roomId === roomId).filter(matchesStatus)
  const keyword = search.value.trim().toLowerCase()
  if (keyword) list = list.filter((m) => m.code.toLowerCase().includes(keyword))
  return list
}

// 有在搜尋、篩選洗衣間、或篩選啟用狀態時，隱藏沒有符合資料的宿舍／洗衣間，結果比較乾淨
const hasActiveQuery = computed(
  () => search.value.trim() !== '' || roomFilter.value !== '' || statusFilter.value !== '全部'
)

// 某個宿舍底下符合篩選條件的洗衣間
function visibleRoomsOf(dormitoryId) {
  let list = roomsOf(dormitoryId)
  if (roomFilter.value) list = list.filter((r) => r.id === Number(roomFilter.value))
  if (hasActiveQuery.value) list = list.filter((r) => machinesOfRoom(r.id).length > 0)
  return list
}

const visibleDormitories = computed(() => {
  let list = facilitiesStore.dormitories
  if (dormitoryFilter.value) list = list.filter((d) => d.id === Number(dormitoryFilter.value))
  if (hasActiveQuery.value) list = list.filter((d) => visibleRoomsOf(d.id).length > 0)
  return list
})

// 洗衣間篩選的選項：如果已經選了宿舍，只列出該宿舍底下的洗衣間
const roomFilterOptions = computed(() => {
  if (dormitoryFilter.value) return roomsOf(Number(dormitoryFilter.value))
  return facilitiesStore.rooms
})

// ------- 批次選取／啟用停用 -------

const selectedDormitoryIds = ref([])
const selectedRoomIds = ref([])
const selectedMachineIds = ref([])

function roomIdsOfDormitory(dormitoryId) {
  return roomsOf(dormitoryId).map((r) => r.id)
}

function machineIdsOfRoom(roomId) {
  return facilitiesStore.machines.filter((m) => m.roomId === roomId).map((m) => m.id)
}

function machineIdsOfDormitory(dormitoryId) {
  return roomIdsOfDormitory(dormitoryId).flatMap((roomId) => machineIdsOfRoom(roomId))
}

function addIds(targetRef, ids) {
  ids.forEach((id) => {
    if (!targetRef.value.includes(id)) targetRef.value.push(id)
  })
}

function removeIds(targetRef, ids) {
  targetRef.value = targetRef.value.filter((id) => !ids.includes(id))
}

function isDormitorySelected(id) {
  return selectedDormitoryIds.value.includes(id)
}

function isRoomSelected(id) {
  return selectedRoomIds.value.includes(id)
}

function isMachineSelected(id) {
  return selectedMachineIds.value.includes(id)
}

// 勾選宿舍＝連同它底下所有洗衣間、機台一起選取／取消
function toggleDormitorySelected(dormitory) {
  const roomIds = roomIdsOfDormitory(dormitory.id)
  const machineIds = machineIdsOfDormitory(dormitory.id)
  if (isDormitorySelected(dormitory.id)) {
    removeIds(selectedDormitoryIds, [dormitory.id])
    removeIds(selectedRoomIds, roomIds)
    removeIds(selectedMachineIds, machineIds)
  } else {
    addIds(selectedDormitoryIds, [dormitory.id])
    addIds(selectedRoomIds, roomIds)
    addIds(selectedMachineIds, machineIds)
  }
}

// 勾選洗衣間＝連同它底下所有機台一起選取／取消；取消時連帶取消父層宿舍的「整棟已選」狀態
function toggleRoomSelected(room) {
  const machineIds = machineIdsOfRoom(room.id)
  if (isRoomSelected(room.id)) {
    removeIds(selectedRoomIds, [room.id])
    removeIds(selectedMachineIds, machineIds)
    removeIds(selectedDormitoryIds, [room.dormitoryId])
  } else {
    addIds(selectedRoomIds, [room.id])
    addIds(selectedMachineIds, machineIds)
  }
}

// 勾選機台：取消時連帶取消父層洗衣間／宿舍的「整組已選」狀態
function toggleMachineSelected(machine) {
  if (isMachineSelected(machine.id)) {
    removeIds(selectedMachineIds, [machine.id])
    const room = roomOf(machine.roomId)
    if (room) {
      removeIds(selectedRoomIds, [room.id])
      removeIds(selectedDormitoryIds, [room.dormitoryId])
    }
  } else {
    addIds(selectedMachineIds, [machine.id])
  }
}

const selectedCount = computed(
  () => selectedDormitoryIds.value.length + selectedRoomIds.value.length + selectedMachineIds.value.length
)

function clearSelection() {
  selectedDormitoryIds.value = []
  selectedRoomIds.value = []
  selectedMachineIds.value = []
}

// 平常不顯示勾選框；按下「啟用」或「停用」才進入選取模式，勾選框才出現
const selectionMode = ref(null) // null｜'enable'｜'disable'

function enterSelectionMode(mode) {
  selectionMode.value = mode
  clearSelection()
}

function cancelSelectionMode() {
  selectionMode.value = null
  clearSelection()
}

// 勾完按「確認」先跳視窗列出會被影響的項目，使用者在視窗裡再按一次才真的套用
const showBatchConfirmModal = ref(false)

const selectionSummaryLines = computed(() => {
  const lines = []
  selectedDormitoryIds.value.forEach((id) => {
    const dormitory = facilitiesStore.dormitories.find((d) => d.id === id)
    if (dormitory) lines.push(`宿舍：${dormitory.name}`)
  })
  selectedRoomIds.value.forEach((id) => {
    const room = roomOf(id)
    if (room) lines.push(`洗衣間：${room.name}`)
  })
  selectedMachineIds.value.forEach((id) => {
    const machine = facilitiesStore.machines.find((m) => m.id === id)
    if (machine) lines.push(`機台：${machine.code}`)
  })
  return lines
})

function openBatchConfirm() {
  if (selectedCount.value === 0) return
  showBatchConfirmModal.value = true
}

function closeBatchConfirm() {
  showBatchConfirmModal.value = false
}

function applyBatchConfirm() {
  const isActive = selectionMode.value === 'enable'
  selectedDormitoryIds.value.forEach((id) => facilitiesStore.setDormitoryActive(id, isActive))
  selectedRoomIds.value.forEach((id) => facilitiesStore.setRoomActive(id, isActive))
  selectedMachineIds.value.forEach((id) => facilitiesStore.setMachineActive(id, isActive))
  showBatchConfirmModal.value = false
  selectionMode.value = null
  clearSelection()
}

// ------- 行內新增宿舍 -------

const showAddDormitoryRow = ref(false)
const newDormitoryName = ref('')

function openAddDormitoryRow() {
  showAddDormitoryRow.value = true
  newDormitoryName.value = ''
}

function cancelAddDormitory() {
  showAddDormitoryRow.value = false
  newDormitoryName.value = ''
}

function submitAddDormitory() {
  const name = newDormitoryName.value.trim()
  if (!name) return
  facilitiesStore.addDormitory(name)
  cancelAddDormitory()
}

// ------- 行內新增洗衣間（同一時間只開一個） -------

const addRoomForDormitoryId = ref(null)
const newRoomName = ref('')
const newRoomLocation = ref('')

function openAddRoomRow(dormitoryId) {
  addRoomForDormitoryId.value = dormitoryId
  newRoomName.value = ''
  newRoomLocation.value = ''
}

function cancelAddRoom() {
  addRoomForDormitoryId.value = null
}

function submitAddRoom(dormitoryId) {
  const name = newRoomName.value.trim()
  if (!name) return
  facilitiesStore.addRoom(dormitoryId, name, newRoomLocation.value.trim())
  cancelAddRoom()
}

// ------- 新增機台（右上角按鈕，宿舍／洗衣間只能下拉選現有的） -------

const showAddMachineModal = ref(false)
const addMachineDormitoryId = ref('')
const addMachineRoomId = ref('')
const addMachineCode = ref('')
const addMachineCycleMinutes = ref('')
const addMachineError = ref('')

const addMachineRoomOptions = computed(() => {
  if (!addMachineDormitoryId.value) return []
  return roomsOf(Number(addMachineDormitoryId.value))
})

function openAddMachineModal() {
  addMachineDormitoryId.value = dormitoryFilter.value || ''
  addMachineRoomId.value = ''
  addMachineCode.value = ''
  addMachineCycleMinutes.value = ''
  addMachineError.value = ''
  showAddMachineModal.value = true
}

function closeAddMachineModal() {
  showAddMachineModal.value = false
}

function submitAddMachine() {
  addMachineError.value = ''
  const roomId = Number(addMachineRoomId.value)
  const code = addMachineCode.value.trim()
  if (!roomId || !code) return

  const duplicate = facilitiesStore.machines.some((m) => m.roomId === roomId && m.code === code)
  if (duplicate) {
    addMachineError.value = '已有這個編號'
    return
  }

  const minutes = Number(addMachineCycleMinutes.value) || 0
  facilitiesStore.addMachine(roomId, code, minutes * 60)

  // 新增完直接把篩選切到這個洗衣間，馬上看到結果
  dormitoryFilter.value = addMachineDormitoryId.value
  roomFilter.value = addMachineRoomId.value

  closeAddMachineModal()
}

// ------- 編輯宿舍／洗衣間／機台（各自獨立，不互相牽動） -------

const editingItem = ref(null) // { level: 'dormitory'|'room'|'machine', id, roomId? } 或 null
const editName = ref('')
const editLocation = ref('')
const editCode = ref('')
const editCycleMinutes = ref('')
const editError = ref('')

const editModalTitle = computed(() => {
  if (!editingItem.value) return ''
  if (editingItem.value.level === 'dormitory') return '編輯宿舍'
  if (editingItem.value.level === 'room') return '編輯洗衣間'
  return '編輯機台'
})

function openEditDormitory(dormitory) {
  editingItem.value = { level: 'dormitory', id: dormitory.id }
  editName.value = dormitory.name
  editError.value = ''
}

function openEditRoom(room) {
  editingItem.value = { level: 'room', id: room.id }
  editName.value = room.name
  editLocation.value = room.location || ''
  editError.value = ''
}

function openEditMachine(machine) {
  editingItem.value = { level: 'machine', id: machine.id, roomId: machine.roomId }
  editCode.value = machine.code
  editCycleMinutes.value = String(Math.round(machine.defaultCycleSeconds / 60))
  editError.value = ''
}

function closeEditModal() {
  editingItem.value = null
}

function submitEdit() {
  const { level, id } = editingItem.value

  if (level === 'dormitory') {
    facilitiesStore.updateDormitory(id, { name: editName.value.trim() })
  } else if (level === 'room') {
    facilitiesStore.updateRoom(id, {
      name: editName.value.trim(),
      location: editLocation.value.trim(),
    })
  } else if (level === 'machine') {
    const code = editCode.value.trim()
    const duplicate = facilitiesStore.machines.some(
      (m) => m.roomId === editingItem.value.roomId && m.code === code && m.id !== id
    )
    if (duplicate) {
      editError.value = '已有這個編號'
      return
    }
    const minutes = Number(editCycleMinutes.value) || 0
    facilitiesStore.updateMachine(id, { code, defaultCycleSeconds: minutes * 60 })
  }

  closeEditModal()
}
</script>

<template>
  <div class="settings-view">
    <div class="page-head">
      <h1>設備總覽</h1>
    </div>

    <!-- 篩選、搜尋、新增機台靠右排 -->
    <div class="action-bar">
      <select v-model="dormitoryFilter" class="filter-select" @change="roomFilter = ''">
        <option value="">全部宿舍</option>
        <option v-for="d in facilitiesStore.dormitories" :key="d.id" :value="String(d.id)">
          {{ d.name }}
        </option>
      </select>

      <select v-model="roomFilter" class="filter-select">
        <option value="">全部洗衣間</option>
        <option v-for="r in roomFilterOptions" :key="r.id" :value="String(r.id)">
          {{ r.name }}
        </option>
      </select>

      <div class="search-box">
        <span class="material-symbols-outlined">search</span>
        <input v-model="search" placeholder="搜尋機台編號" />
      </div>

      <button type="button" class="add-btn" @click="openAddMachineModal">
        <span class="material-symbols-outlined">add</span>
        新增
      </button>
    </div>

    <!-- 啟用狀態篩選 chip 跟 啟用／停用操作同一排 -->
    <div class="filters">
      <button
        v-for="s in ['全部', '啟用中', '已停用']"
        :key="s"
        type="button"
        class="chip"
        :class="{ selected: statusFilter === s }"
        @click="statusFilter = s"
      >
        {{ s }}
      </button>

      <div class="status-actions">
        <template v-if="selectionMode">
          <span class="selected-count">已選取 {{ selectedCount }} 筆</span>
          <button
            type="button"
            class="btn"
            :class="selectionMode === 'enable' ? 'enable' : 'disable'"
            :disabled="selectedCount === 0"
            @click="openBatchConfirm"
          >
            確認{{ selectionMode === 'enable' ? '啟用' : '停用' }}
          </button>
          <button type="button" class="btn ghost" @click="cancelSelectionMode">取消</button>
        </template>
        <template v-else>
          <button type="button" class="btn enable" @click="enterSelectionMode('enable')">啟用</button>
          <button type="button" class="btn disable" @click="enterSelectionMode('disable')">停用</button>
        </template>
      </div>
    </div>

    <div class="dorm-list">
      <section v-for="d in visibleDormitories" :key="d.id" class="dorm-section">
        <!-- 宿舍是大標題 -->
        <div class="dorm-header">
          <div class="title-group">
            <input
              v-if="selectionMode"
              type="checkbox"
              class="row-check"
              :checked="isDormitorySelected(d.id)"
              @change="toggleDormitorySelected(d)"
            />
            <span class="dorm-name">{{ d.name }}</span>
            <span class="status-badge" :class="{ active: d.isActive }">
              {{ d.isActive ? '啟用中' : '已停用' }}
            </span>
          </div>
          <div class="row-actions">
            <button type="button" class="icon-btn" @click="openEditDormitory(d)">
              <span class="material-symbols-outlined">edit</span>
            </button>
          </div>
        </div>

        <!-- 洗衣間是副標題 -->
        <div v-for="r in visibleRoomsOf(d.id)" :key="r.id" class="room-block">
          <div class="room-header">
            <div class="title-group">
              <input
                v-if="selectionMode"
                type="checkbox"
                class="row-check"
                :checked="isRoomSelected(r.id)"
                @change="toggleRoomSelected(r)"
              />
              <span class="room-name">{{ r.name }}</span>
              <span class="status-badge small" :class="{ active: r.isActive }">
                {{ r.isActive ? '啟用中' : '已停用' }}
              </span>
            </div>
            <div class="row-actions">
              <button type="button" class="icon-btn" @click="openEditRoom(r)">
                <span class="material-symbols-outlined">edit</span>
              </button>
            </div>
          </div>

          <div class="machine-list">
            <div
              v-for="m in machinesOfRoom(r.id)"
              :key="m.id"
              class="machine-row"
              :class="{ inactive: !m.isActive }"
            >
              <div class="machine-info">
                <input
                  v-if="selectionMode"
                  type="checkbox"
                  class="row-check"
                  :checked="isMachineSelected(m.id)"
                  @change="toggleMachineSelected(m)"
                />
                <span class="machine-code">{{ m.code }}</span>
                <span class="status-badge" :class="{ active: m.isActive }">
                  {{ m.isActive ? '啟用中' : '已停用' }}
                </span>
              </div>
              <div class="row-actions">
                <button type="button" class="icon-btn" @click="openEditMachine(m)">
                  <span class="material-symbols-outlined">edit</span>
                </button>
              </div>
            </div>
            <p v-if="machinesOfRoom(r.id).length === 0" class="empty-hint">沒有符合條件的機台</p>
          </div>
        </div>

        <p v-if="visibleRoomsOf(d.id).length === 0" class="empty-hint">這個宿舍沒有符合條件的洗衣間</p>

        <!-- 行內新增洗衣間 -->
        <div v-if="addRoomForDormitoryId === d.id" class="inline-add-form">
          <input v-model="newRoomName" placeholder="洗衣間名稱" />
          <input v-model="newRoomLocation" placeholder="位置（選填）" />
          <div class="inline-add-actions">
            <button type="button" class="btn secondary" @click="cancelAddRoom">取消</button>
            <button type="button" class="btn" @click="submitAddRoom(d.id)">儲存</button>
          </div>
        </div>
        <button v-else type="button" class="inline-add-link" @click="openAddRoomRow(d.id)">
          ＋ 新增洗衣間
        </button>
      </section>

      <p v-if="visibleDormitories.length === 0" class="empty">目前沒有資料</p>

      <!-- 行內新增宿舍 -->
      <div v-if="showAddDormitoryRow" class="inline-add-form">
        <input v-model="newDormitoryName" placeholder="宿舍名稱" />
        <div class="inline-add-actions">
          <button type="button" class="btn secondary" @click="cancelAddDormitory">取消</button>
          <button type="button" class="btn" @click="submitAddDormitory">儲存</button>
        </div>
      </div>
      <button v-else type="button" class="inline-add-link" @click="openAddDormitoryRow">
        ＋ 新增宿舍
      </button>
    </div>

    <!-- 新增機台 Modal -->
    <Teleport to="body">
      <div v-if="showAddMachineModal" class="modal-overlay" @click.self="closeAddMachineModal">
        <div class="modal-card">
          <div class="modal-head">
            <h2>新增機台</h2>
            <button type="button" class="icon-btn" aria-label="關閉" @click="closeAddMachineModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form class="modal-form" @submit.prevent="submitAddMachine">
            <div class="field-group">
              <label>宿舍</label>
              <select v-model="addMachineDormitoryId" required @change="addMachineRoomId = ''">
                <option value="" disabled>請選擇宿舍</option>
                <option v-for="d in facilitiesStore.dormitories" :key="d.id" :value="String(d.id)">
                  {{ d.name }}
                </option>
              </select>
            </div>

            <div class="field-group">
              <label>洗衣間</label>
              <select v-model="addMachineRoomId" required :disabled="!addMachineDormitoryId">
                <option value="" disabled>請選擇洗衣間</option>
                <option v-for="r in addMachineRoomOptions" :key="r.id" :value="String(r.id)">
                  {{ r.name }}
                </option>
              </select>
            </div>

            <div class="field-group">
              <label>機台編號</label>
              <input v-model="addMachineCode" required placeholder="機台編號" />
              <p v-if="addMachineError" class="field-error">{{ addMachineError }}</p>
            </div>

            <div class="field-group">
              <label>預設洗程（分鐘）</label>
              <input v-model="addMachineCycleMinutes" type="number" required placeholder="分鐘" />
            </div>

            <div class="modal-actions">
              <button type="button" class="btn secondary" @click="closeAddMachineModal">取消</button>
              <button type="submit" class="btn">確認</button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- 編輯 Modal -->
    <Teleport to="body">
      <div v-if="editingItem" class="modal-overlay" @click.self="closeEditModal">
        <div class="modal-card">
          <div class="modal-head">
            <h2>{{ editModalTitle }}</h2>
            <button type="button" class="icon-btn" aria-label="關閉" @click="closeEditModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form class="modal-form" @submit.prevent="submitEdit">
            <div v-if="editingItem.level === 'dormitory'" class="field-group">
              <label>宿舍名稱</label>
              <input v-model="editName" required placeholder="宿舍名稱" />
            </div>

            <template v-else-if="editingItem.level === 'room'">
              <div class="field-group">
                <label>洗衣間名稱</label>
                <input v-model="editName" required placeholder="洗衣間名稱" />
              </div>
              <div class="field-group">
                <label>位置</label>
                <input v-model="editLocation" placeholder="位置（選填）" />
              </div>
            </template>

            <template v-else>
              <div class="field-group">
                <label>機台編號</label>
                <input v-model="editCode" required placeholder="機台編號" />
                <p v-if="editError" class="field-error">{{ editError }}</p>
              </div>
              <div class="field-group">
                <label>預設洗程（分鐘）</label>
                <input v-model="editCycleMinutes" type="number" required placeholder="分鐘" />
              </div>
            </template>

            <div class="modal-actions">
              <button type="button" class="btn secondary" @click="closeEditModal">取消</button>
              <button type="submit" class="btn">確認</button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- 批次啟用／停用確認 Modal -->
    <Teleport to="body">
      <div v-if="showBatchConfirmModal" class="modal-overlay" @click.self="closeBatchConfirm">
        <div class="modal-card">
          <div class="modal-head">
            <h2>確認{{ selectionMode === 'enable' ? '啟用' : '停用' }}</h2>
            <button type="button" class="icon-btn" aria-label="關閉" @click="closeBatchConfirm">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <p class="batch-confirm-intro">
            確定要{{ selectionMode === 'enable' ? '啟用' : '停用' }}以下 {{ selectedCount }} 筆項目嗎？
          </p>
          <ul class="batch-confirm-list">
            <li v-for="(line, i) in selectionSummaryLines" :key="i">{{ line }}</li>
          </ul>

          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeBatchConfirm">取消</button>
            <button
              type="button"
              class="btn"
              :class="selectionMode === 'enable' ? 'enable' : 'disable'"
              @click="applyBatchConfirm"
            >
              確認{{ selectionMode === 'enable' ? '啟用' : '停用' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page-head {
  margin-bottom: 16px;
}

.page-head h1 {
  font-size: 25px;
  color: var(--navy);
  margin: 0;
}

/* 篩選、搜尋、新增機台靠右排 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.selected-count {
  font-size: 14px;
  font-weight: 600;
  color: var(--navy);
  white-space: nowrap;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  border: 0;
  background: var(--navy);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-weight: 600;
  white-space: nowrap;
}

.search-box {
  width: 200px;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  padding: 0 12px;
}

.search-box .material-symbols-outlined {
  color: var(--muted);
  font-size: 19px;
}

.search-box input {
  flex: 1;
  width: 0;
  border: 0;
  padding: 9px 0;
  font: inherit;
  font-size: 13px;
  color: var(--text);
  background: transparent;
}

.filter-select {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  color: var(--text);
  padding: 9px 10px;
  font: inherit;
}

/* 啟用狀態篩選 chip 跟 啟用／停用操作同一排，操作靠右 */
.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.status-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
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

.dorm-list {
  display: grid;
  gap: 28px;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.row-check {
  width: 17px;
  height: 17px;
  cursor: pointer;
  flex: none;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: none;
}

/* 宿舍大標題 */
.dorm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--line);
  margin-bottom: 14px;
}

.dorm-name {
  font-size: 21px;
  font-weight: 700;
  color: var(--navy);
}

/* 洗衣間副標題，往右縮排一點跟宿舍區分 */
.room-block {
  margin: 0 0 16px 10px;
  padding-left: 14px;
  border-left: 2px solid var(--line);
}

.room-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.room-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.empty-hint {
  font-size: 13px;
  color: var(--muted);
}

.machine-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.machine-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 11px;
  padding: 10px 14px;
  box-shadow: var(--shadow);
}

.machine-row.inactive {
  opacity: 0.55;
}

.machine-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.machine-code {
  font-size: 15px;
  font-weight: 700;
  color: var(--navy);
}

/* 啟用/停用狀態標籤，共用在宿舍／洗衣間／機台上 */
.status-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fcebec;
  color: var(--red);
}

.status-badge.active {
  background: #e6f5ef;
  color: var(--green);
}

.status-badge.small {
  padding: 1px 7px;
}

/* 行內新增（宿舍／洗衣間）連結與表單 */
.inline-add-link {
  border: 1px dashed var(--line);
  background: transparent;
  color: var(--navy);
  border-radius: 10px;
  padding: 9px 14px;
  cursor: pointer;
  font: inherit;
  font-weight: 600;
  text-align: left;
}

.inline-add-form {
  display: grid;
  gap: 8px;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 12px 14px;
  background: #fff;
}

.inline-add-form input {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 8px 10px;
  font: inherit;
  color: var(--text);
}

.inline-add-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 圖示按鈕，拿掉外框底色，純圖示 */
.icon-btn {
  width: 36px;
  height: 36px;
  border: 0;
  background: transparent;
  color: #7e91a2;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.icon-btn .material-symbols-outlined {
  font-size: 17px;
  font-variation-settings: 'FILL' 0, 'wght' 200, 'GRAD' 0, 'opsz' 20;
}

.empty {
  text-align: center;
  color: var(--muted);
  padding: 30px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 30, 45, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 50;
}

.modal-card {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 420px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 20px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.modal-head h2 {
  font-size: 19px;
  color: var(--navy);
  margin: 0;
}

.modal-form {
  display: grid;
  gap: 16px;
}

.field-group {
  display: grid;
  gap: 8px;
}

.field-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}

.field-group input,
.field-group select {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 9px;
  padding: 10px 12px;
  background: #fff;
  color: var(--text);
  font: inherit;
}

.field-group select:disabled {
  background: var(--pale);
  color: var(--muted);
}

.field-error {
  font-size: 12px;
  color: var(--red);
}

.batch-confirm-intro {
  font-size: 14px;
  color: var(--text);
  margin: 0 0 10px;
}

.batch-confirm-list {
  list-style: none;
  margin: 0 0 16px;
  padding: 0;
  max-height: 220px;
  overflow-y: auto;
  display: grid;
  gap: 4px;
  font-size: 13px;
  color: var(--muted);
}

.batch-confirm-list li {
  border-bottom: 1px solid var(--line);
  padding: 4px 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn {
  border: 0;
  border-radius: 10px;
  background: var(--navy);
  color: #fff;
  padding: 10px 16px;
  cursor: pointer;
  font: inherit;
  font-weight: 600;
}

.btn.secondary {
  background: var(--sky);
  color: var(--navy);
}

/* 啟用＝白底藍字，停用＝藍底白字 */
.btn.enable {
  background: #fff;
  color: var(--navy);
  border: 1px solid var(--navy);
}

.btn.disable {
  background: var(--navy);
  color: #fff;
}

.btn.ghost {
  background: transparent;
  color: var(--muted);
  box-shadow: none;
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 螢幕較窄時，搜尋框跟篩選改成滿寬 */
@media (max-width: 640px) {
  .search-box,
  .filter-select {
    flex: 1 1 100%;
  }
}
</style>
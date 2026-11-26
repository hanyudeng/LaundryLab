<script setup>
import { ref, onMounted } from 'vue'
import { useAccountsStore } from '../../stores/accounts'
import { useFacilitiesStore } from '../../stores/facilities'

const accountsStore = useAccountsStore()
const facilitiesStore = useFacilitiesStore()
onMounted(() => { accountsStore.load(); facilitiesStore.load() })
const actionError = ref('')

function dormitoryName(id) {
  const dormitory = facilitiesStore.dormitories.find((d) => d.id === id)
  return dormitory ? dormitory.name : ''
}

function dormitoryNamesOf(supervisor) {
  return supervisor.dormitoryIds.map(dormitoryName).filter(Boolean)
}

// ------- 新增 -------

const showAddModal = ref(false)
const newEmail = ref('')
const newDormitoryIds = ref([])
const addError = ref('')

function openAddModal() {
  newEmail.value = ''
  newDormitoryIds.value = []
  addError.value = ''
  showAddModal.value = true
}

function closeAddModal() {
  showAddModal.value = false
}

function toggleNewDormitory(id) {
  const index = newDormitoryIds.value.indexOf(id)
  if (index === -1) newDormitoryIds.value.push(id)
  else newDormitoryIds.value.splice(index, 1)
}

async function submitAdd() {
  addError.value = ''
  const email = newEmail.value.trim().toLowerCase()
  if (!email) return

  const duplicate = accountsStore.supervisors.some((s) => s.email.toLowerCase() === email)
  if (duplicate) {
    addError.value = '已有該使用者'
    return
  }

  try { await accountsStore.addSupervisor(email, newDormitoryIds.value) }
  catch (error) { addError.value = error.message || '新增失敗'; return }
  closeAddModal()
}

// ------- 編輯宿舍指派 -------

const editingSupervisor = ref(null)
const editDormitoryIds = ref([])

function openEditModal(supervisor) {
  editingSupervisor.value = supervisor
  editDormitoryIds.value = [...supervisor.dormitoryIds]
}

function closeEditModal() {
  editingSupervisor.value = null
}

function toggleEditDormitory(id) {
  const index = editDormitoryIds.value.indexOf(id)
  if (index === -1) editDormitoryIds.value.push(id)
  else editDormitoryIds.value.splice(index, 1)
}

async function submitEdit() {
  try { await accountsStore.updateSupervisorDormitories(editingSupervisor.value.id, editDormitoryIds.value) }
  catch { actionError.value = '更新指派失敗'; return }
  closeEditModal()
}

// ------- 移除 -------

const removingSupervisor = ref(null)

function openRemoveConfirm(supervisor) {
  removingSupervisor.value = supervisor
}

function closeRemoveConfirm() {
  removingSupervisor.value = null
}

async function confirmRemove() {
  try { await accountsStore.removeSupervisor(removingSupervisor.value.id) }
  catch { actionError.value = '移除舍監失敗'; return }
  closeRemoveConfirm()
}
</script>

<template>
  <div class="settings-view">
    <p v-if="accountsStore.error || facilitiesStore.error || actionError" role="alert">{{ accountsStore.error || facilitiesStore.error || actionError }}</p>
    <div class="page-head">
      <h1>帳號管理</h1>
      <button type="button" class="add-btn" @click="openAddModal">
        <span class="material-symbols-outlined">add</span>
        新增
      </button>
    </div>

    <div class="supervisor-list">
      <div v-for="s in accountsStore.supervisors" :key="s.id" class="supervisor-row">
        <div class="supervisor-info">
          <span class="supervisor-email">{{ s.email }}</span>
          <div class="dormitory-chips">
            <span v-for="name in dormitoryNamesOf(s)" :key="name" class="chip-static">{{ name }}</span>
            <span v-if="dormitoryNamesOf(s).length === 0" class="empty-hint">尚未指派宿舍</span>
          </div>
        </div>
        <div class="row-actions">
          <button type="button" class="icon-btn" @click="openEditModal(s)">
            <span class="material-symbols-outlined">edit</span>
          </button>
          <button type="button" class="remove-link" @click="openRemoveConfirm(s)">移除</button>
        </div>
      </div>

      <p v-if="accountsStore.supervisors.length === 0" class="empty">目前沒有宿舍管理員</p>
    </div>

    <!-- 新增 Modal -->
    <Teleport to="body">
      <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
        <div class="modal-card">
          <div class="modal-head">
            <h2>新增宿舍管理員</h2>
            <button type="button" class="icon-btn" aria-label="關閉" @click="closeAddModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form class="modal-form" @submit.prevent="submitAdd">
            <div class="field-group">
              <label>學校信箱</label>
              <input v-model="newEmail" type="email" required placeholder="學號@o365.fcu.edu.tw" />
              <p v-if="addError" class="field-error">{{ addError }}</p>
            </div>

            <div class="field-group">
              <label>管理的宿舍（選填，之後也可以再編輯）</label>
              <div class="dormitory-check-list">
                <label v-for="d in facilitiesStore.dormitories" :key="d.id" class="dormitory-check-item">
                  <input
                    type="checkbox"
                    :checked="newDormitoryIds.includes(d.id)"
                    @change="toggleNewDormitory(d.id)"
                  />
                  {{ d.name }}
                </label>
              </div>
            </div>

            <div class="modal-actions">
              <button type="button" class="btn secondary" @click="closeAddModal">取消</button>
              <button type="submit" class="btn">確認</button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- 編輯宿舍指派 Modal -->
    <Teleport to="body">
      <div v-if="editingSupervisor" class="modal-overlay" @click.self="closeEditModal">
        <div class="modal-card">
          <div class="modal-head">
            <h2>編輯宿舍指派</h2>
            <button type="button" class="icon-btn" aria-label="關閉" @click="closeEditModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="modal-form">
            <p class="editing-email">{{ editingSupervisor.email }}</p>

            <div class="field-group">
              <label>管理的宿舍</label>
              <div class="dormitory-check-list">
                <label v-for="d in facilitiesStore.dormitories" :key="d.id" class="dormitory-check-item">
                  <input
                    type="checkbox"
                    :checked="editDormitoryIds.includes(d.id)"
                    @change="toggleEditDormitory(d.id)"
                  />
                  {{ d.name }}
                </label>
              </div>
            </div>

            <div class="modal-actions">
              <button type="button" class="btn secondary" @click="closeEditModal">取消</button>
              <button type="button" class="btn" @click="submitEdit">確認</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 移除確認 Modal -->
    <Teleport to="body">
      <div v-if="removingSupervisor" class="modal-overlay" @click.self="closeRemoveConfirm">
        <div class="modal-card">
          <div class="modal-head">
            <h2>移除宿舍管理員</h2>
            <button type="button" class="icon-btn" aria-label="關閉" @click="closeRemoveConfirm">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <p class="batch-confirm-intro">
            確定要移除「{{ removingSupervisor.email }}」嗎？移除後他管理的宿舍指派也會一併取消。
          </p>

          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeRemoveConfirm">取消</button>
            <button type="button" class="btn disable" @click="confirmRemove">確認移除</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 25px;
  color: var(--navy);
  margin: 0;
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
}

.supervisor-list {
  display: grid;
  gap: 10px;
}

.supervisor-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: var(--shadow);
}

.supervisor-info {
  display: grid;
  gap: 6px;
}

.supervisor-email {
  font-size: 15px;
  font-weight: 700;
  color: var(--navy);
}

.dormitory-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip-static {
  font-size: 12px;
  font-weight: 600;
  color: var(--navy);
  background: var(--sky);
  border-radius: 999px;
  padding: 3px 10px;
}

.empty-hint {
  font-size: 13px;
  color: var(--muted);
}

.empty {
  text-align: center;
  color: var(--muted);
  padding: 30px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: none;
}

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

.remove-link {
  border: 0;
  background: transparent;
  color: var(--red);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0 6px;
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

.editing-email {
  font-size: 14px;
  font-weight: 600;
  color: var(--navy);
  margin: 0;
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

.field-group > input {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 9px;
  padding: 10px 12px;
  background: #fff;
  color: var(--text);
  font: inherit;
}

.field-error {
  font-size: 12px;
  color: var(--red);
}

.dormitory-check-list {
  display: grid;
  gap: 8px;
  border: 1px solid var(--line);
  border-radius: 9px;
  padding: 10px 12px;
  max-height: 180px;
  overflow-y: auto;
}

.dormitory-check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text);
  cursor: pointer;
}

.batch-confirm-intro {
  font-size: 14px;
  color: var(--text);
  margin: 0 0 16px;
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

.btn.disable {
  background: var(--navy);
  color: #fff;
}
</style>

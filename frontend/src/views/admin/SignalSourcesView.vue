<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'

const machines = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const savingId = ref(null)
const testingId = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get('/api/admin/machines/signal-sources')
    machines.value = data.map(machine => ({ ...machine, selectedSource: machine.source, reason: '' }))
  } catch (e) {
    error.value = e.response?.status === 401 ? '登入已過期，請重新登入' : '無法載入機台訊號來源'
  } finally {
    loading.value = false
  }
}

async function save(machine) {
  error.value = ''
  notice.value = ''
  if (!machine.reason.trim()) {
    error.value = '請填寫變更原因'
    return
  }
  savingId.value = machine.id
  try {
    await http.put(`/api/admin/machines/${machine.id}/signal-source`, {
      source: machine.selectedSource,
      reason: machine.reason.trim(),
    })
    machine.source = machine.selectedSource
    machine.reason = ''
    notice.value = `${machine.machineCode} 的訊號來源已更新`
  } catch (e) {
    error.value = e.response?.status === 403 ? '只有系統管理員可以變更訊號來源' : '儲存失敗，請稍後再試'
  } finally {
    savingId.value = null
  }
}

async function sendTest(machine, type) {
  error.value = ''
  notice.value = ''
  testingId.value = machine.id
  try {
    const { data } = await http.post(`/api/admin/machines/${machine.id}/test-signal/${type}`)
    if (data.processStatus !== 'ACCEPTED') {
      error.value = data.reason === 'CYCLE_ALREADY_ACTIVE'
        ? '這台機器已有進行中的洗衣流程'
        : data.reason === 'NO_ACTIVE_CYCLE' ? '這台機器目前沒有進行中的洗衣流程' : `訊號遭拒絕：${data.reason}`
      return
    }
    notice.value = type === 'START'
      ? `${machine.machineCode} 已開始模擬洗衣。請用已綁定 LINE 的學生帳號到設備總覽訂閱提醒。`
      : `${machine.machineCode} 的模擬洗衣已結束。`
    await load()
  } catch (e) {
    error.value = e.response?.status === 409 ? '這台機器未設定為測試頁面來源' : '測試訊號送出失敗'
  } finally {
    testingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="signal-view">
    <div class="page-head">
      <div>
        <h1>訊號來源設定</h1>
        <p>設定每台機器接受的訊號來源；變更會留下操作原因與紀錄。</p>
      </div>
      <button type="button" class="refresh" @click="load">重新整理</button>
    </div>
    <p class="info">測試頁面產生模擬事件；IoT 開關代表現場設備訊號。來源不符的事件會被記錄並拒絕。</p>
    <p v-if="loading" role="status">載入中…</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <div v-if="!loading" class="machine-list">
      <article v-for="machine in machines" :key="machine.id" class="machine-card">
        <div class="machine-title">
          <strong>{{ machine.machineCode }}</strong>
          <span v-if="!machine.active" class="inactive">已停用</span>
        </div>
        <p>{{ machine.dormitoryName }} · {{ machine.roomName }}</p>
        <p class="last-signal">最後接收：{{ machine.lastSignalAt ? new Date(machine.lastSignalAt).toLocaleString('zh-TW') : '尚無訊號' }}</p>
        <label :for="`source-${machine.id}`">訊號來源</label>
        <select :id="`source-${machine.id}`" v-model="machine.selectedSource">
          <option value="TEST_PAGE">測試頁面（模擬）</option>
          <option value="IOT_SWITCH">IoT 開關</option>
        </select>
        <template v-if="machine.selectedSource !== machine.source">
          <label :for="`reason-${machine.id}`">變更原因</label>
          <input :id="`reason-${machine.id}`" v-model="machine.reason" maxlength="500" placeholder="請輸入變更原因" />
          <button type="button" :disabled="savingId === machine.id" @click="save(machine)">
            {{ savingId === machine.id ? '儲存中…' : '儲存設定' }}
          </button>
        </template>
        <div v-if="machine.source === 'TEST_PAGE' && machine.active" class="test-actions">
          <button v-if="!machine.cycleStatus" type="button" :disabled="testingId === machine.id" @click="sendTest(machine, 'START')">開始測試洗衣</button>
          <button v-else type="button" class="finish" :disabled="testingId === machine.id" @click="sendTest(machine, 'COMPLETE')">結束測試洗衣</button>
        </div>
      </article>
      <p v-if="machines.length === 0" class="empty">目前沒有機台</p>
    </div>
  </div>
</template>

<style scoped>
.page-head { display: flex; justify-content: space-between; gap: 16px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
h1 { color: var(--navy); font-size: 25px; margin: 0; }
.page-head p, .machine-card p { color: var(--muted); font-size: 13px; margin: 4px 0; }
.info { background: var(--sky); color: #526c81; padding: 12px 14px; border-radius: 10px; font-size: 13px; }
.machine-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(270px, 1fr)); gap: 14px; }
.machine-card { background: #fff; border: 1px solid var(--line); border-radius: 14px; padding: 18px; box-shadow: var(--shadow); display: grid; gap: 9px; }
.machine-title { display: flex; align-items: center; gap: 10px; color: var(--navy); }
.inactive { color: var(--red); font-size: 12px; }
.last-signal { margin-bottom: 6px !important; }
label { font-size: 13px; font-weight: 600; color: var(--text); }
select, input { width: 100%; box-sizing: border-box; padding: 10px; border: 1px solid var(--line); border-radius: 9px; background: #fff; font: inherit; color: var(--text); }
button { padding: 10px 14px; border: 0; border-radius: 9px; background: var(--navy); color: #fff; font: inherit; cursor: pointer; }
button:disabled { opacity: .6; cursor: wait; }
.refresh { background: #fff; color: var(--navy); border: 1px solid var(--line); }
.error { color: var(--red); }
.notice { color: #166534; }
.empty { color: var(--muted); }
.test-actions { border-top: 1px solid var(--line); padding-top: 12px; margin-top: 4px; }
.test-actions button { width: 100%; }
.test-actions .finish { background: #466077; }
</style>

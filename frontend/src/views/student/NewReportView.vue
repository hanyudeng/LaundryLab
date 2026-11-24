<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useReportsStore } from '../../stores/reports'

const router = useRouter()
const reportsStore = useReportsStore()

// 宿舍 → 洗衣間 → 機台的對應表，用來做連動下拉選單
const roomMap = {
  第一學生宿舍: ['A 棟洗衣間', 'B 棟洗衣間'],
  第二學生宿舍: ['C 棟洗衣間'],
}
const machineMap = {
  'A 棟洗衣間': ['A01', 'A02'],
  'B 棟洗衣間': ['B01', 'B02'],
  'C 棟洗衣間': ['C01', 'C02'],
}

const kinds = ['無法啟動', '洗衣中斷', '設備異音', '其他']

const dorm = ref('')
const room = ref('')
const machine = ref('')
const kind = ref('')
const desc = ref('')
const photoName = ref('')

// 根據選的宿舍/洗衣間，算出下一層下拉選單可以選的選項
const roomOptions = computed(() => roomMap[dorm.value] || [])
const machineOptions = computed(() => machineMap[room.value] || [])

// 換了宿舍，下面已選的洗衣間、機台要清空，避免選到不屬於這個宿舍的洗衣間
function onDormChange() {
  room.value = ''
  machine.value = ''
}

// 換了洗衣間，已選的機台要清空
function onRoomChange() {
  machine.value = ''
}

function onPhotoChange(e) {
  const file = e.target.files[0]
  photoName.value = file ? file.name : ''
}

// 每個欄位的錯誤訊息，用一個物件存，key 是欄位名稱
const errors = ref({})

function validate() {
  const next = {}
  if (!dorm.value) next.dorm = '請選擇宿舍'
  if (!room.value) next.room = '請選擇洗衣間'
  if (!machine.value) next.machine = '請選擇洗衣機'
  if (!kind.value) next.kind = '請選擇問題類型'
  if (!desc.value.trim()) next.desc = '請填寫問題說明'
  errors.value = next
  return Object.keys(next).length === 0
}

function handleSubmit() {
  if (!validate()) return

  reportsStore.addReport({
    dorm: dorm.value,
    room: room.value,
    machine: machine.value,
    kind: kind.value,
    desc: desc.value,
  })

  router.push('/student/reports')
}
</script>

<template>
  <div class="new-report-view">
    <div class="page-head">
      <button type="button" class="back" @click="router.push('/student/reports')">
        ‹ 返回問題回報
      </button>
    </div>
    <h1 class="new-report-title">新增問題回報</h1>

    <form class="form" novalidate @submit.prevent="handleSubmit">
      <label for="dorm">選擇宿舍</label>
      <select id="dorm" v-model="dorm" @change="onDormChange">
        <option value="">請選擇宿舍</option>
        <option v-for="d in Object.keys(roomMap)" :key="d">{{ d }}</option>
      </select>
      <p v-if="errors.dorm" class="field-error">⚠ {{ errors.dorm }}</p>

      <label for="room">選擇洗衣間</label>
      <select id="room" v-model="room" :disabled="!dorm" @change="onRoomChange">
        <option value="">請先選擇宿舍</option>
        <option v-for="r in roomOptions" :key="r">{{ r }}</option>
      </select>
      <p v-if="errors.room" class="field-error">⚠ {{ errors.room }}</p>

      <label for="machine">選擇洗衣機</label>
      <select id="machine" v-model="machine" :disabled="!room">
        <option value="">請先選擇洗衣間</option>
        <option v-for="m in machineOptions" :key="m">{{ m }}</option>
      </select>
      <p v-if="errors.machine" class="field-error">⚠ {{ errors.machine }}</p>

      <label for="kind">問題類型</label>
      <select id="kind" v-model="kind">
        <option value="">請選擇</option>
        <option v-for="k in kinds" :key="k">{{ k }}</option>
      </select>
      <p v-if="errors.kind" class="field-error">⚠ {{ errors.kind }}</p>

      <label for="desc">問題說明</label>
      <textarea id="desc" v-model="desc" placeholder="請簡單描述遇到的狀況"></textarea>
      <p v-if="errors.desc" class="field-error">⚠ {{ errors.desc }}</p>

      <label>附上照片（選填）</label>
      <label class="file-label">
  <span>{{ photoName || '附上照片' }}</span>
  <input type="file" accept="image/*" @change="onPhotoChange" />
</label>

      <div class="btn-row">
        <button class="btn" type="submit">送出回報</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.page-head {
  margin-bottom: 8px;
}

.back {
  border: 0;
  background: transparent;
  color: var(--blue);
  cursor: pointer;
  font-weight: 600;
  padding: 0;
  font-size: 14px;
}

.new-report-title {
  margin: 0 0 16px;
  font-size: 23px;
  color: var(--navy);
}

.form {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 22px;
  box-shadow: var(--shadow);
}

.form label {
  display: block;
  font-weight: 600;
  color: #40566a;
  margin: 14px 0 6px;
}

.form select,
.form textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 9px;
  padding: 11px 12px;
  background: #fff;
  color: var(--text);
  font: inherit;
}

/* 還沒選上一層的時候，下拉選單會被鎖住 */
.form select:disabled {
  background: var(--pale);
  color: var(--muted);
}

.form textarea {
  min-height: 110px;
  resize: vertical;
}

.field-error {
  color: var(--red);
  font-size: 13px;
  margin: 5px 0 0;
}

/* 照片上傳區塊，長得像個虛線框的按鈕 */
/* 照片上傳區塊，圖示在上、文字在下，置中 */
.file-label {
  display: grid;
  place-items: center;
  min-height: 120px;
  border: 1px dashed #aac4d9;
  border-radius: 9px;
  padding: 13px;
  color: #617b90;
  background: var(--pale);
  cursor: pointer;
  text-align: center;
}



/* 原生的檔案選擇按鈕藏起來，用上面那個框框代替 */
.file-label input {
  display: none;
}

.btn-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
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
</style>
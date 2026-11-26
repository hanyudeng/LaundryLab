<script setup>
import { ref, computed, onMounted } from 'vue'
import { useReportsStore } from '../../stores/reports'
import { useAuthStore } from '../../stores/auth'

const reportsStore = useReportsStore()
const authStore = useAuthStore()
onMounted(() => reportsStore.loadReports())
const updateError = ref('')

// 目前選的案件 id，null 代表顯示列表、有值就顯示詳情
const activeId = ref(null)

const statusList = ['全部', '未處理', '處理中', '已完成']
const activeStatus = ref('全部')

// 套用狀態篩選後的列表
const filteredReports = computed(() => {
  return reportsStore.reports.filter(
    (r) => activeStatus.value === '全部' || r.status === activeStatus.value,
  )
})

// 根據 activeId 找出目前要顯示詳情的那一筆
const activeReport = computed(() => reportsStore.reports.find((r) => r.id === activeId.value))

// 詳情表單目前填的值
const formStatus = ref('')
const formNote = ref('')

// 點進一筆案件，把表單帶入目前的狀態跟備註
function openDetail(report) {
  activeId.value = report.id
  formStatus.value = report.status
  formNote.value = report.note
}

function backToList() {
  activeId.value = null
}

// 按下更新案件時，把表單內容存回 store，回到列表
async function handleUpdate() {
  updateError.value = ''
  try {
    await reportsStore.updateReport(activeId.value, {
    status: formStatus.value,
    note: formNote.value,
    })
    activeId.value = null
  } catch {
    updateError.value = '更新失敗，請稍後重試'
  }
}
</script>

<template>
  <div class="reports-view">
    <!-- 沒有選中案件時顯示列表 -->
    <template v-if="!activeReport">
      <div class="page-head">
        <div>
          <h1>回報處理</h1>
          <p>查看受指派宿舍的學生回報</p>
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

      <p v-if="reportsStore.error" role="alert">{{ reportsStore.error }} <button type="button" @click="reportsStore.loadReports()">重試</button></p>
      <p v-else-if="reportsStore.loading" role="status">載入回報中…</p>
      <div v-else class="report-list">
        <article
          v-for="r in filteredReports"
          :key="r.id"
          class="report-card"
          @click="openDetail(r)"
        >
          <div
            class="report-icon"
            :class="{
              pending: r.status === '未處理',
              running: r.status === '處理中',
              done: r.status === '已完成',
            }"
          >
            <span class="material-symbols-outlined">
              {{ r.status === '已完成' ? 'task_alt' : r.status === '處理中' ? 'schedule' : 'warning' }}
            </span>
          </div>
          <div class="report-main">
            <div class="report-title">
              <span class="report-kind">{{ r.kind }}</span>
              <span class="report-machine">{{ r.machine }}</span>
              <span
                class="report-state"
                :class="{
                  pending: r.status === '未處理',
                  running: r.status === '處理中',
                  done: r.status === '已完成',
                }"
              >
                {{ r.status }}
              </span>
            </div>
            <div class="report-meta">{{ r.room }}　・　{{ r.reporter }}　・　{{ r.time }}</div>
          </div>
          <span class="material-symbols-outlined chevron">chevron_right</span>
        </article>

        <p v-if="filteredReports.length === 0" class="empty">目前沒有符合條件的回報案件</p>
      </div>
    </template>

    <!-- 有選中案件時顯示兩欄詳情 -->
    <template v-else>
      <div class="page-head">
        <button type="button" class="back" @click="backToList">‹ 返回回報列表</button>
        <span
          class="report-state"
          :class="{
            pending: activeReport.status === '未處理',
            running: activeReport.status === '處理中',
            done: activeReport.status === '已完成',
          }"
        >
          {{ activeReport.status }}
        </span>
      </div>

      <div class="case-layout">
        <section class="panel">
          <h2>回報內容</h2>
          <dl class="case-info">
            <dt>回報人</dt>
            <dd>{{ activeReport.reporter }}</dd>
            <dt>機台位置</dt>
            <dd>{{ activeReport.dorm }}・{{ activeReport.room }}・{{ activeReport.machine }}</dd>
            <dt>問題類型</dt>
            <dd>{{ activeReport.kind }}</dd>
            <dt>原始說明</dt>
            <dd>{{ activeReport.desc }}</dd>
            <dt>回報時間</dt>
            <dd>{{ activeReport.time }}</dd>
          </dl>
        </section>

        <section class="panel">
          <h2>處理案件</h2>
          <form class="case-form" @submit.prevent="handleUpdate">
            <label for="case-status">案件狀態</label>
            <select id="case-status" v-model="formStatus">
              <option>未處理</option>
              <option>處理中</option>
              <option>已完成</option>
            </select>

            <label for="case-note">處理說明</label>
            <textarea
              id="case-note"
              v-model="formNote"
              placeholder="填寫檢查結果或處理方式"
            ></textarea>

            <div class="sub">處理人：{{ authStore.email }}</div>
            <p v-if="updateError" role="alert">{{ updateError }}</p>

            <div class="btn-row">
              <button class="btn" type="submit">更新案件</button>
            </div>
          </form>
        </section>
      </div>
    </template>
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

.page-head p {
  margin: 3px 0 0;
  color: var(--muted);
  font-size: 13px;
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

.report-list {
  display: grid;
  gap: 12px;
}

/* 單一筆回報卡片，整張可以點擊 */
.report-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 13px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow);
  cursor: pointer;
}

.report-card:hover {
  border-color: #a9c7dc;
}

/* 左邊狀態圖示圓角方塊 */
.report-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--sky);
  color: var(--blue);
  display: grid;
  place-items: center;
  flex: none;
}

.report-icon.pending {
  background: #fff3d8;
  color: var(--yellow);
}

.report-icon.running {
  background: #e7f1fb;
  color: #286da7;
}

.report-icon.done {
  background: #e6f5ef;
  color: var(--green);
}

.report-main {
  flex: 1;
  min-width: 0;
}

.report-title {
  font-weight: 700;
  color: var(--navy);
  display: flex;
  align-items: center;
  gap: 8px;
}

.report-meta {
  font-size: 13px;
  color: var(--muted);
  margin-top: 4px;
}

/* 狀態小標籤，列表跟詳情頁共用 */
.report-state {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--sky);
  color: var(--blue);
  white-space: nowrap;
}

.report-state.pending {
  background: #fff3d8;
  color: var(--yellow);
}

.report-state.running {
  background: #e7f1fb;
  color: #286da7;
}

.report-state.done {
  background: #e6f5ef;
  color: var(--green);
}

.chevron {
  color: #7890a5;
}

.empty {
  text-align: center;
  color: var(--muted);
  padding: 30px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
}

/* 案件詳情：左右兩欄排版，左邊內容寬一點 */
.case-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(290px, 0.75fr);
  gap: 18px;
}

.panel {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  box-shadow: var(--shadow);
  padding: 20px;
}

.panel h2 {
  font-size: 17px;
  color: var(--navy);
  margin: 0 0 16px;
}

/* 回報內容那個左欄的定義清單 */
.case-info {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 12px 16px;
}

.case-info dt {
  color: var(--muted);
  font-size: 13px;
}

.case-info dd {
  margin: 0;
  font-weight: 500;
}

.case-form label {
  display: block;
  font-weight: 600;
  color: #40566a;
  margin: 14px 0 6px;
}

.case-form select,
.case-form textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 9px;
  padding: 11px 12px;
  background: #fff;
  color: var(--text);
  font: inherit;
}

.case-form textarea {
  min-height: 120px;
  resize: vertical;
}

.case-form .sub {
  font-size: 13px;
  color: var(--muted);
  margin-top: 12px;
}

.btn-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useReportsStore } from '../../stores/reports'

const reportsStore = useReportsStore()
onMounted(() => reportsStore.loadReports())
const router = useRouter()

// 目前展開看時間軸的回報 id，null 代表都沒展開
const expandedId = ref(null)

function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id
}

const steps = ['未處理', '處理中', '已完成']

// 算出這筆回報目前走到時間軸的第幾步
function currentStep(status) {
  return steps.indexOf(status)
}

// 每個步驟底下要顯示的說明文字
function stepNotes(report) {
  return [
    `回報時間：${report.time}`,
    report.status === '未處理'
      ? '等待處理'
      : report.status === '處理中'
        ? '案件正在處理中'
        : '已完成處理',
    report.status === '已完成' ? '案件已完成' : '等待處理完成',
  ]
}
</script>

<template>
  <div class="reports-view">
    <div class="page-head">
      <h1>問題回報</h1>
      <button type="button" class="btn" @click="router.push('/student/reports/new')">
        ＋ 新增回報
      </button>
    </div>

    <p v-if="reportsStore.error" role="alert">{{ reportsStore.error }} <button type="button" @click="reportsStore.loadReports()">重試</button></p>
    <p v-else-if="reportsStore.loading" role="status">載入回報中…</p>
    <div v-else class="report-list">
      <article
        v-for="r in reportsStore.reports"
        :key="r.id"
        class="report"
        :class="{ expanded: expandedId === r.id }"
      >
        <button type="button" class="report-toggle" @click="toggleExpand(r.id)">
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
            <div class="report-title">{{ r.machine }}｜{{ r.kind }}</div>
            <div class="report-meta">{{ r.dorm }} {{ r.room }} · {{ r.time }}</div>
          </div>
          <span
            class="badge"
            :class="{
              pending: r.status === '未處理',
              running: r.status === '處理中',
              done: r.status === '已完成',
            }"
          >
            {{ r.status }}
          </span>
          <span class="chevron">›</span>
        </button>

        <div v-if="expandedId === r.id" class="report-detail">
          <div class="timeline">
            <div
              v-for="(step, i) in steps"
              :key="step"
              class="timeline-step"
              :class="{ done: i < currentStep(r.status), current: i === currentStep(r.status) }"
            >
              <span class="timeline-mark"></span>
              <div>
                <div class="timeline-title">{{ step }}</div>
                <div class="timeline-note">{{ stepNotes(r)[i] }}</div>
              </div>
            </div>
          </div>
          <div class="report-meta">問題說明：{{ r.desc }}</div>
        </div>
      </article>

      <p v-if="reportsStore.reports.length === 0" class="empty">目前還沒有回報紀錄</p>
    </div>
  </div>
</template>

<style scoped>
/* 標題跟新增按鈕的排版 */
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

.report-list {
  display: grid;
  gap: 12px;
}

.report {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 13px;
  box-shadow: var(--shadow);
  overflow: hidden;
}

/* 整個卡片頭部當成一顆可以點擊的按鈕 */
.report-toggle {
  width: 100%;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  padding: 16px;
  cursor: pointer;
  font: inherit;
  color: inherit;
}

/* 左邊狀態圖示方塊，固定 42x42px 正方形，顏色隨狀態變化 */
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
}

.report-meta {
  font-size: 13px;
  color: var(--muted);
  margin-top: 5px;
}

.badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--sky);
  color: var(--blue);
  white-space: nowrap;
}

.badge.pending {
  background: #fff3d8;
  color: var(--yellow);
}

.badge.running {
  background: #e7f1fb;
  color: #286da7;
}

.badge.done {
  background: #e6f5ef;
  color: var(--green);
}

.chevron {
  color: #6d8498;
  font-size: 22px;
  transition: transform 0.2s;
}

.report.expanded .chevron {
  transform: rotate(90deg);
}

/* 展開後的時間軸區塊 */
.report-detail {
  border-top: 1px solid var(--line);
  padding: 16px;
}

.timeline-step {
  display: grid;
  grid-template-columns: 24px 1fr;
  gap: 10px;
  min-height: 46px;
  color: #8a9aaa;
  position: relative;
}

/* 時間軸上的圓點 */
.timeline-mark {
  position: relative;
  width: 18px;
  height: 18px;
  border: 2px solid #c9d6e1;
  border-radius: 50%;
  margin-top: 2px;
  background: #fff;
}

/* 圓點之間的連接線，最後一個步驟不用畫 */
.timeline-step:not(:last-child) .timeline-mark::after {
  content: '';
  position: absolute;
  left: 6px;
  top: 16px;
  height: 30px;
  border-left: 2px solid #d9e3eb;
}

.timeline-step.done,
.timeline-step.current {
  color: var(--navy);
}

.timeline-step.done .timeline-mark,
.timeline-step.current .timeline-mark {
  border-color: var(--blue);
  background: #dceefa;
}

/* 已完成的步驟在圓點裡加一個打勾符號 */
.timeline-step.done .timeline-mark::before {
  content: '✓';
  position: absolute;
  left: 2px;
  top: -3px;
  font-size: 13px;
  color: var(--navy);
}

.timeline-title {
  font-weight: 600;
}

.timeline-note {
  font-size: 13px;
  color: var(--muted);
  margin-top: 2px;
}

.empty {
  text-align: center;
  color: var(--muted);
  padding: 30px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
}
</style>

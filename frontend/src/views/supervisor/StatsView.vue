<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMachinesStore } from '../../stores/machines'
import { useReportsStore } from '../../stores/reports'

const router = useRouter()
const machinesStore = useMachinesStore()
const reportsStore = useReportsStore()

const machineStatusDefs = [
  { status: '可用', icon: 'check_circle' },
  { status: '使用中', icon: 'local_laundry_service' },
  { status: '待確認', icon: 'schedule' },
  { status: '維修中', icon: 'build' },
]

const reportStatusDefs = [
  { status: '未處理', icon: 'warning' },
  { status: '處理中', icon: 'schedule' },
  { status: '已完成', icon: 'task_alt' },
]

// 狀態文字對應圖示底色的 class
const statusColorMap = {
  可用: 'available',
  使用中: 'running',
  待確認: 'pending',
  維修中: 'maintenance',
  未處理: 'pending',
  處理中: 'running',
  已完成: 'done',
}

function statusIconClass(status) {
  return statusColorMap[status]
}

// 依狀態算出機台數量
const machineCounts = computed(() => {
  return machineStatusDefs.map((def) => ({
    ...def,
    count: machinesStore.machines.filter((m) => m.status === def.status).length,
  }))
})

// 依狀態算出回報案件數量
const reportCounts = computed(() => {
  return reportStatusDefs.map((def) => ({
    ...def,
    count: reportsStore.reports.filter((r) => r.status === def.status).length,
  }))
})

function goToEquipment() {
  router.push('/supervisor/equipment')
}

function goToReports() {
  router.push('/supervisor/reports')
}

// 假資料：熱門時段，8 個時段的使用人次
const popularHours = [
  { time: '08', value: 16 },
  { time: '10', value: 27 },
  { time: '12', value: 42 },
  { time: '14', value: 60 },
  { time: '16', value: 78 },
  { time: '18', value: 91 },
  { time: '20', value: 77 },
  { time: '22', value: 53 },
]
const peakValue = Math.max(...popularHours.map((h) => h.value))

// 假資料：各機台使用率
const usageRates = [
  { code: 'A01', percent: 72 },
  { code: 'A02', percent: 88 },
  { code: 'A03', percent: 61 },
  { code: 'B01', percent: 43 },
  { code: 'B02', percent: 18 },
]
</script>

<template>
  <div class="stats-view">
    <div class="page-head">
      <h1>宿舍統計</h1>
      <p>第一學生宿舍・全部洗衣間</p>
    </div>

    <div class="stats-block">
      <h2 class="stats-title">設備狀態</h2>
      <div class="stats-grid">
        <button
          v-for="m in machineCounts"
          :key="m.status"
          type="button"
          class="stat-card"
          @click="goToEquipment"
        >
          <span class="stat-icon" :class="statusIconClass(m.status)">
            <span class="material-symbols-outlined">{{ m.icon }}</span>
          </span>
          <span>
            <span class="stat-label">{{ m.status }}</span><br />
            <span class="stat-number">{{ m.count }}</span>
            <span class="stat-label"> 台</span>
          </span>
        </button>
      </div>
    </div>

    <div class="stats-block">
      <h2 class="stats-title">回報案件</h2>
      <div class="stats-grid reports">
        <button
          v-for="r in reportCounts"
          :key="r.status"
          type="button"
          class="stat-card"
          @click="goToReports"
        >
          <span class="stat-icon" :class="statusIconClass(r.status)">
            <span class="material-symbols-outlined">{{ r.icon }}</span>
          </span>
          <span>
            <span class="stat-label">{{ r.status }}</span><br />
            <span class="stat-number">{{ r.count }}</span>
            <span class="stat-label"> 筆</span>
          </span>
        </button>
      </div>
    </div>

    <div class="stats-block">
      <h2 class="stats-title">使用情形</h2>
      <div class="chart-grid">
        <section class="chart-panel">
          <h3>熱門時段</h3>
          <p class="chart-caption">假資料・設備使用人次</p>
          <div class="popular-chart">
            <div v-for="h in popularHours" :key="h.time" class="popular-column">
              <span class="popular-value">{{ h.value }}</span>
              <div
                class="popular-bar"
                :class="{ busy: h.value >= peakValue * 0.82 }"
                :style="{ height: Math.max(8, (h.value / peakValue) * 100) + '%' }"
              ></div>
              <span class="popular-time">{{ h.time }}</span>
            </div>
          </div>
          <p class="chart-note">時段為原型示意，數據尚未串接實際使用紀錄。</p>
        </section>

        <section class="chart-panel">
          <h3>機台使用率</h3>
          <p class="chart-caption">假資料・依機台估算</p>
          <div class="usage-list">
            <div v-for="u in usageRates" :key="u.code" class="usage-row">
              <span class="usage-code">{{ u.code }}</span>
              <div class="usage-track">
                <div class="usage-fill" :style="{ width: u.percent + '%' }"></div>
              </div>
              <span class="usage-percent">{{ u.percent }}%</span>
            </div>
          </div>
          <p class="chart-note">使用率為原型假資料。</p>
        </section>
      </div>
    </div>

    <p class="hint">點選數字卡片，可直接查看對應的設備或案件。</p>
  </div>
</template>

<style scoped>
.page-head {
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

.stats-block {
  margin: 0 0 30px;
}

.stats-title {
  font-size: 17px;
  color: var(--navy);
  margin: 0 0 12px;
}

/* 設備狀態 4 格一排 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

/* 回報案件只有 3 種狀態，改成 3 格一排 */
.stats-grid.reports {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stat-card {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  box-shadow: var(--shadow);
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  cursor: pointer;
  color: inherit;
  font: inherit;
}

.stat-card:hover {
  border-color: #9abbd3;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  background: var(--sky);
  color: var(--blue);
  display: grid;
  place-items: center;
  flex: none;
}

.stat-icon.available {
  background: #e6f5ef;
  color: var(--green);
}

.stat-icon.running {
  background: #e7f1fb;
  color: #286da7;
}

.stat-icon.pending {
  background: #fff3d8;
  color: var(--yellow);
}

.stat-icon.maintenance {
  background: #fcebec;
  color: var(--red);
}

.stat-icon.done {
  background: #e6f5ef;
  color: var(--green);
}

.stat-label {
  font-size: 13px;
  color: var(--muted);
}

.stat-number {
  font-size: 27px;
  font-weight: 700;
  color: var(--navy);
  line-height: 1.15;
}

/* 兩個圖表並排，左邊（熱門時段）比右邊（使用率）寬一點 */
.chart-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 14px;
}

.chart-panel {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  box-shadow: var(--shadow);
  padding: 18px;
}

.chart-panel h3 {
  font-size: 16px;
  color: var(--navy);
  margin: 0;
}

.chart-caption {
  font-size: 12px;
  color: var(--muted);
  margin: 3px 0 17px;
}

/* 長條圖容器，固定高度 175px，8 欄對應 8 個時段 */
.popular-chart {
  height: 175px;
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  align-items: end;
  gap: 10px;
  border-bottom: 1px solid var(--line);
  padding: 0 5px;
}

.popular-column {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
}

.popular-value {
  font-size: 10px;
  color: var(--muted);
}

/* 單一根長條，高度用行內樣式動態控制（看 template 的 :style） */
.popular-bar {
  width: min(100%, 34px);
  min-height: 4px;
  border-radius: 7px 7px 0 0;
  background: #9ed8ec;
}

/* 人潮比較多的時段（超過最高值的 82%）顏色加深 */
.popular-bar.busy {
  background: #56b6d4;
}

.popular-time {
  font-size: 10px;
  color: var(--muted);
}

.usage-list {
  display: grid;
  gap: 13px;
}

/* 每一列：機台編號（固定 42px）、進度條（吃剩下空間）、百分比（固定 38px） */
.usage-row {
  display: grid;
  grid-template-columns: 42px 1fr 38px;
  gap: 9px;
  align-items: center;
  font-size: 12px;
}

.usage-code {
  font-weight: 700;
  color: var(--navy);
}

.usage-track {
  height: 9px;
  background: #edf3f7;
  border-radius: 99px;
  overflow: hidden;
}

.usage-fill {
  height: 100%;
  border-radius: 99px;
  background: #72bfd6;
}

.usage-percent {
  text-align: right;
  color: var(--muted);
}

.chart-note {
  font-size: 11px;
  color: var(--muted);
  margin: 12px 0 0;
}

.hint {
  background: var(--sky);
  color: #526c81;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  margin-top: 16px;
}
/* 螢幕寬度小於 700px 時，設備狀態、回報案件都改成 2 格一排 */
@media (max-width: 700px) {
  .stats-grid,
  .stats-grid.reports {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
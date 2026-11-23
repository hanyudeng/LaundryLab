<script setup>
defineProps({
  machine: {
    type: Object,
    required: true,
  },
})

// 狀態對應的 badge 顏色 class
const statusClassMap = {
  可用: 'available',
  使用中: 'running',
  待確認: 'pending',
  維修中: 'maintenance',
}
</script>

<template>
  <article class="machine-card">
    <div class="washer">
      <span class="material-symbols-outlined">local_laundry_service</span>
    </div>
    <div class="machine-info">
      <div class="machine-line">
        <span class="code">{{ machine.code }}</span>
        <span class="badge" :class="statusClassMap[machine.status]">{{ machine.status }}</span>
      </div>
      <div class="sub">{{ machine.detail }}</div>
    </div>
    <div class="action">
      <!-- 插槽：外面套用這個元件時可以塞自己的按鈕進來（鈴鐺或維修切換），這個元件本身不管塞什麼 -->
      <slot name="action"></slot>
    </div>
  </article>
</template>

<style scoped>
/* 整張卡片，分成「圖示／文字資訊／插槽」三欄 */
.machine-card {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) 42px;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 16px;
  box-shadow: var(--shadow);
  min-height: 112px;
}

/* 左邊洗衣機圖示那一欄 */
.washer {
  display: grid;
  place-items: center;
  color: var(--navy);
}

.washer .material-symbols-outlined {
  font-size: 46px;
}

/* 中間文字資訊那一欄 */
.machine-info {
  min-width: 0;
}

/* 機台編號跟狀態 badge 同一行 */
.machine-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 機台編號文字，像 A01 */
.code {
  font-size: 19px;
  font-weight: 700;
  color: var(--navy);
}

/* 狀態 badge 的基本樣式，顏色預設是藍色 */
.badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--sky);
  color: var(--blue);
}

/* 以下四個是依照狀態覆蓋掉 badge 的顏色 */
.badge.available {
  background: #e6f5ef;
  color: var(--green);
}

.badge.running {
  background: #e7f1fb;
  color: #286da7;
}

.badge.pending {
  background: #fff3d8;
  color: var(--yellow);
}

.badge.maintenance {
  background: #fcebec;
  color: var(--red);
}

/* 底下那行說明文字（剩餘時間／最後訊號之類的） */
.sub {
  font-size: 13px;
  color: var(--muted);
  margin-top: 5px;
}

/* 右邊插槽區域，固定寬度對齊格線，裡面放什麼由外面決定 */
.action {
  display: grid;
  place-items: center;
}
</style>
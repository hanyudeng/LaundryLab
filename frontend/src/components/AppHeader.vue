<script setup>
</script>

<template>
  <header class="app-header">
    <div class="brand">校園生活服務</div>
    <div class="brand-title">宿舍洗衣服務</div>
    <!-- 跟原型一樣的六層波浪，顏色從淺藍漸層到接近白色 -->
    <svg class="wave" viewBox="0 0 1440 100" preserveAspectRatio="none">
      <path
        fill="#afd4ec"
        d="M0 32 C130 3 194 62 330 34 C468 8 522 11 663 42 C800 72 866 70 1002 38 C1146 4 1261 15 1440 42 L1440 100 L0 100Z"
      />
      <path
        fill="#c4e1f2"
        d="M0 40 C130 12 200 68 336 41 C474 16 530 18 668 49 C805 78 872 77 1008 46 C1148 14 1266 22 1440 50 L1440 100 L0 100Z"
      />
      <path
        fill="#d2e8f4"
        d="M0 48 C130 20 205 75 342 49 C478 24 535 25 674 56 C811 84 878 84 1013 54 C1152 23 1270 30 1440 58 L1440 100 L0 100Z"
      />
      <path
        fill="#e0eef7"
        d="M0 56 C131 28 210 82 348 57 C483 32 540 33 679 63 C815 90 882 91 1017 62 C1156 32 1274 38 1440 66 L1440 100 L0 100Z"
      />
      <path
        fill="#eff7fb"
        d="M0 64 C132 36 215 89 354 65 C488 41 545 42 684 70 C820 96 886 97 1021 70 C1160 41 1278 46 1440 74 L1440 100 L0 100Z"
      />
      <path
        fill="var(--pale)"
        d="M0 72 C133 44 220 95 360 73 C494 50 550 51 689 78 C825 99 890 99 1025 78 C1164 50 1282 54 1440 82 L1440 100 L0 100Z"
      />
    </svg>
  </header>
</template>

<style scoped>
/* 深藍色頂部橫條本體 */
.app-header {
  position: relative;
  height: 140px;
  background: var(--header);
  color: #fff;
  padding: 28px 24px;
  text-align: center;
}

/* 上面那行小字「校園生活服務」 */
.brand {
  font-size: 14px;
  opacity: 0.8;
  letter-spacing: 0.08em;
}

/* 下面那行大字「宿舍洗衣服務」 */
.brand-title {
  font-size: 25px;
  font-weight: 700;
  margin-top: 2px;
}

/* 波浪 svg 貼齊 header 底部，蓋在上面 */
.wave {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 54px;
  display: block;
}
</style>
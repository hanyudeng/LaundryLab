<script setup>
import { computed } from 'vue'
import MachineCard from './MachineCard.vue'

const props = defineProps({
  machines: { type: Array, required: true },
  entranceSide: { type: String, default: 'RIGHT' },
  activeStatus: { type: String, default: '全部' },
  searchText: { type: String, default: '' },
})

const placed = computed(() => [...props.machines].sort((a, b) => a.id - b.id).map((machine, index) => ({
  ...machine,
  x: machine.layoutX ?? (index % 3 === 0 ? 18 : index % 3 === 1 ? 50 : 82),
  y: machine.layoutY ?? (index < 3 ? 31 : 76 + Math.floor((index - 3) / 3) * 20),
  muted: (props.activeStatus !== '全部' && props.activeStatus !== machine.status)
    || !machine.code.toLowerCase().includes(props.searchText.trim().toLowerCase()),
})))

const planHeight = computed(() => placed.value.some((m) => m.y > 85) ? '660px' : '440px')
</script>

<template>
  <div class="plan-scroll">
    <div class="floor-plan" :style="{ height: planHeight }" role="group" aria-label="洗衣間機台位置圖">
      <div class="windows"><span>窗戶</span><span>窗戶</span></div>
      <div class="entrance" :class="entranceSide.toLowerCase()"><span>入口</span></div>
      <div v-for="machine in placed" v-show="!machine.muted" :key="machine.id" class="machine-slot"
        :style="{ left: machine.x + '%', top: machine.y + '%' }">
        <MachineCard :machine="machine">
          <template #action><slot name="action" :machine="machine" /></template>
        </MachineCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.plan-scroll { overflow-x: auto; padding-bottom: 4px; }
.floor-plan { position: relative; min-width: 980px; border: 3px solid #9eb9c8; border-radius: 18px; background: #f7fbfd; box-shadow: var(--shadow); }
.windows { position: absolute; top: -3px; left: 12%; right: 20%; display: flex; justify-content: space-between; pointer-events: none; }
.windows span { width: 145px; text-align: center; background: #e8f7fb; border: 2px solid #84bfcf; border-top: 0; border-radius: 0 0 9px 9px; color: #366277; font-size: 13px; padding: 4px; }
.entrance { position: absolute; z-index: 2; background: #f7fbfd; color: #345c70; font-weight: 700; font-size: 14px; text-align: center; }
.entrance.right { right: -3px; top: 43%; height: 70px; border-left: 3px solid #7ea5b8; writing-mode: vertical-rl; padding: 8px 0; }
.entrance.left { left: -3px; top: 43%; height: 70px; border-right: 3px solid #7ea5b8; writing-mode: vertical-rl; padding: 8px 0; }
.entrance.top { top: -3px; left: 43%; width: 90px; border-bottom: 3px solid #7ea5b8; padding: 5px; }
.entrance.bottom { bottom: -3px; left: 43%; width: 90px; border-top: 3px solid #7ea5b8; padding: 5px; }
.machine-slot { position: absolute; transform: translate(-50%, -50%); z-index: 1; width: 270px; }
</style>

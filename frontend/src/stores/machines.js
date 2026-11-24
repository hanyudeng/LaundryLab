import { defineStore } from 'pinia'
import { fetchDormitories, fetchRooms, fetchMachines } from '../api/machines'

// 後端狀態值對應到我們畫面上用的中文
const statusMap = {
  AVAILABLE: '可用',
  RUNNING: '使用中',
  PENDING_CONFIRMATION: '待確認',
  MAINTENANCE: '維修中',
}

export const useMachinesStore = defineStore('machines', {
  state: () => ({
    machines: [],
    // 已訂閱提醒的「循環編號」（後端還沒做訂閱 API，先留在本機）
    subscribed: new Set(),
    loading: false,
  }),
  actions: {
    // 依序打「宿舍→洗衣間→機台」三層 API，整理成一份扁平的機台清單
    async loadMachines() {
      this.loading = true
      try {
        const dormitories = await fetchDormitories()
        const allMachines = []

        for (const dormitory of dormitories) {
          const rooms = await fetchRooms(dormitory.id)

          for (const room of rooms) {
            const machines = await fetchMachines(room.id)

            for (const m of machines) {
              allMachines.push({
                code: m.machineCode,
                status: statusMap[m.status] || m.status,
                dorm: dormitory.name,
                room: room.name,
                signal: m.lastSignalAt,
                end: m.estimatedEndAt ? new Date(m.estimatedEndAt).getTime() : null,
                cycleId: m.cycleId,
              })
            }
          }
        }

        this.machines = allMachines
      } finally {
        this.loading = false
      }
    },
    // 按鈴鐺時切換訂閱狀態（後端還沒做訂閱 API，先留在本機）
    toggleBell(cycleId) {
      if (this.subscribed.has(cycleId)) {
        this.subscribed.delete(cycleId)
      } else {
        this.subscribed.add(cycleId)
      }
    },
    // 切換機台的維修狀態（後端還沒做管理操作 API，先留在本機）
    toggleMaintenance(code) {
      const machine = this.machines.find((m) => m.code === code)
      if (!machine) return
      machine.status = machine.status === '維修中' ? '可用' : '維修中'
    },
  },
})
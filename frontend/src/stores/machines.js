import { defineStore } from 'pinia'
import { fetchDormitories, fetchRooms, fetchMachines } from '../api/machines'
import http from '../api/http'

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
    dormitories: [],
    rooms: [],
    error: '',
    // 已訂閱提醒的循環編號，由後端載入。
    subscribed: new Set(),
    reminderError: '',
    loading: false,
  }),
  actions: {
    // 依序打「宿舍→洗衣間→機台」三層 API，整理成一份扁平的機台清單
    async loadMachines() {
      this.loading = true
      this.error = ''
      try {
        const dormitories = await fetchDormitories()
        const allMachines = []
        const allRooms = []

        for (const dormitory of dormitories) {
          const rooms = await fetchRooms(dormitory.id)
          allRooms.push(...rooms.map((room) => ({ ...room, dormitoryId: dormitory.id })))

          for (const room of rooms) {
            const machines = await fetchMachines(room.id)

            for (const m of machines) {
              allMachines.push({
                id: m.id,
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
        this.dormitories = dormitories
        this.rooms = allRooms
      } catch (error) {
        this.error = '無法取得設備資料，請稍後重試'
      } finally {
        this.loading = false
      }
    },
    // 按鈴鐺時呼叫提醒 API。
    async loadReminders() {
      try {
        const { data } = await http.get('/api/student/reminders')
        this.subscribed = new Set(data.map((item) => item.cycleId))
        this.reminderError = ''
      } catch { this.reminderError = '無法取得提醒資料' }
    },
    async toggleBell(cycleId) {
      try {
        if (this.subscribed.has(cycleId)) await http.delete(`/api/student/reminders/${cycleId}`)
        else await http.post(`/api/student/reminders/${cycleId}`)
        await this.loadReminders()
      } catch (error) {
        this.reminderError = error.response?.status === 409
          ? '請先到個人設定綁定並追蹤 LINE 官方帳號' : '提醒設定失敗，請稍後重試'
      }
    },
    // 舍監維修操作由後端檢查指派範圍並留存稽核紀錄。
    async toggleMaintenance(id, reason) {
      const machine = this.machines.find((m) => m.id === id)
      if (!machine) return
      await http.put(`/api/supervisor/machines/${id}/maintenance`, {
        status: machine.status === '維修中' ? 'NORMAL' : 'MAINTENANCE', reason,
      })
      await this.loadMachines()
    },
  },
})

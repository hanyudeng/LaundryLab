import { defineStore } from 'pinia'

export const useMachinesStore = defineStore('machines', {
  state: () => ({
    // 假資料，之後要換成打後端 API
    machines: [
      { code: 'A01', status: '可用', dorm: '第一學生宿舍', room: 'A 棟洗衣間', signal: null, end: null },
      { code: 'A02', status: '使用中', dorm: '第一學生宿舍', room: 'A 棟洗衣間', signal: null, end: Date.now() + 27 * 60000 },
      { code: 'A03', status: '使用中', dorm: '第一學生宿舍', room: 'A 棟洗衣間', signal: null, end: Date.now() + 8 * 60000 },
      { code: 'B01', status: '待確認', dorm: '第一學生宿舍', room: 'B 棟洗衣間', signal: '今天 09:17', end: null },
      { code: 'B02', status: '維修中', dorm: '第一學生宿舍', room: 'B 棟洗衣間', signal: null, end: null },
    ],
    // 已訂閱提醒的機台編號
    subscribed: new Set(),
  }),
  actions: {
    // 按鈴鐺時切換訂閱狀態，EquipmentView、RemindersView 都會呼叫這個
    toggleBell(code) {
      if (this.subscribed.has(code)) {
        this.subscribed.delete(code)
      } else {
        this.subscribed.add(code)
      }
    },
    // 切換機台的維修狀態（維修中 ↔ 可用），舍監端用
    toggleMaintenance(code) {
      const machine = this.machines.find((m) => m.code === code)
      if (!machine) return
      machine.status = machine.status === '維修中' ? '可用' : '維修中'
    },
  },
})
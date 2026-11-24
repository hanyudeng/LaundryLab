import { defineStore } from 'pinia'

export const useMachinesStore = defineStore('machines', {
  state: () => ({
    // 假資料，之後要換成打後端 API（cycleId 代表目前這次洗衣循環的編號，沒有循環的機台是 null）
    machines: [
      { code: 'A01', status: '可用', dorm: '第一學生宿舍', room: 'A 棟洗衣間', signal: null, end: null, cycleId: null },
      { code: 'A02', status: '使用中', dorm: '第一學生宿舍', room: 'A 棟洗衣間', signal: null, end: Date.now() + 27 * 60000, cycleId: 101 },
      { code: 'A03', status: '使用中', dorm: '第一學生宿舍', room: 'A 棟洗衣間', signal: null, end: Date.now() + 8 * 60000, cycleId: 102 },
      { code: 'B01', status: '待確認', dorm: '第一學生宿舍', room: 'B 棟洗衣間', signal: '今天 09:17', end: null, cycleId: 103 },
      { code: 'B02', status: '維修中', dorm: '第一學生宿舍', room: 'B 棟洗衣間', signal: null, end: null, cycleId: null },
    ],
    // 已訂閱提醒的「循環編號」，不是機台編號——這次循環結束、換下一輪，舊的訂閱就自動對不上了
    subscribed: new Set(),
  }),
  actions: {
    // 按鈴鐺時切換訂閱狀態，傳進來的是 cycleId
    toggleBell(cycleId) {
      if (this.subscribed.has(cycleId)) {
        this.subscribed.delete(cycleId)
      } else {
        this.subscribed.add(cycleId)
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
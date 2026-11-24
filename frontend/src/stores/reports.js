import { defineStore } from 'pinia'

export const useReportsStore = defineStore('reports', {
  state: () => ({
    // 假資料，之後要換成打後端 API
    reports: [
      {
        id: 1,
        dorm: '第一學生宿舍',
        room: 'A 棟洗衣間',
        machine: 'A02',
        kind: '無法啟動',
        status: '未處理',
        time: '今天 10:24',
        desc: '按下啟動後沒有反應。',
      },
      {
        id: 2,
        dorm: '第一學生宿舍',
        room: 'B 棟洗衣間',
        machine: 'B01',
        kind: '狀態異常',
        status: '處理中',
        time: '昨天 21:08',
        desc: '洗衣機顯示待確認。',
      },
    ],
  }),
  actions: {
    // 新增一筆回報，放在清單最前面
    addReport(report) {
      this.reports.unshift({
        id: Date.now(),
        status: '未處理',
        time: '剛剛',
        ...report,
      })
    },
  },
})
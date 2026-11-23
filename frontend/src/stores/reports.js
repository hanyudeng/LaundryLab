import { defineStore } from 'pinia'

export const useReportsStore = defineStore('reports', {
  state: () => ({
    // 假資料，之後要換成打後端 API
    reports: [
      {
        id: 1,
        reporter: '林同學',
        dorm: '第一學生宿舍',
        room: 'A 棟洗衣間',
        machine: 'A02',
        kind: '無法啟動',
        status: '未處理',
        time: '今天 10:24',
        desc: '按下啟動後沒有反應。',
        handler: '',
        note: '',
      },
      {
        id: 2,
        reporter: '張同學',
        dorm: '第一學生宿舍',
        room: 'B 棟洗衣間',
        machine: 'B01',
        kind: '狀態異常',
        status: '處理中',
        time: '昨天 21:08',
        desc: '洗衣機顯示待確認。',
        handler: '陳怡君',
        note: '已先暫停使用，安排檢查。',
      },
    ],
  }),
  actions: {
    // 新增一筆回報，放在清單最前面，學生端用
    addReport(report) {
      this.reports.unshift({
        id: Date.now(),
        reporter: '你',
        status: '未處理',
        time: '剛剛',
        handler: '',
        note: '',
        ...report,
      })
    },
    // 更新案件狀態跟處理備註，舍監端用
    updateReport(id, { status, note, handler }) {
      const report = this.reports.find((r) => r.id === id)
      if (!report) return
      report.status = status
      report.note = note
      report.handler = handler
    },
  },
})
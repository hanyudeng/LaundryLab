import { defineStore } from 'pinia'
import http from '../api/http'

const kinds = { FAULT: '設備故障', STATUS_MISMATCH: '狀態顯示錯誤', OTHER: '其他' }
const statuses = { OPEN: '未處理', IN_PROGRESS: '處理中', RESOLVED: '已完成' }
const statusCodes = Object.fromEntries(Object.entries(statuses).map(([key, value]) => [value, key]))

export const useReportsStore = defineStore('reports', {
  state: () => ({ reports: [], loading: false, error: '' }),
  actions: {
    async loadReports() {
      this.loading = true
      this.error = ''
      try {
        const { data } = await http.get('/api/reports')
        this.reports = data.map((r) => ({
          id: r.id, reporter: r.reporter, dorm: r.dorm, room: r.room,
          machine: r.machine, kind: kinds[r.issueType] || r.issueType,
          status: statuses[r.status] || r.status,
          time: new Date(typeof r.createdAt === 'string' && !r.createdAt.endsWith('Z') ? `${r.createdAt}Z` : r.createdAt).toLocaleString(), desc: r.description,
          handler: r.handler || '', note: r.note || '',
        }))
      } catch {
        this.error = '無法取得回報資料'
      } finally {
        this.loading = false
      }
    },
    async addReport(report) {
      await http.post('/api/reports', report)
      await this.loadReports()
    },
    async updateReport(id, { status, note }) {
      await http.put(`/api/reports/${id}`, { status: statusCodes[status], resolution: note })
      await this.loadReports()
    },
  },
})

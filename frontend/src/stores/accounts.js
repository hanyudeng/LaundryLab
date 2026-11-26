import { defineStore } from 'pinia'
import http from '../api/http'

export const useAccountsStore = defineStore('accounts', {
  state: () => ({ supervisors: [], users: [], error: '' }),
  actions: {
    async load() {
      this.error = ''
      try {
        const { data } = await http.get('/api/admin/users')
        const byId = new Map()
        for (const row of data) {
          if (!byId.has(row.id)) byId.set(row.id, { id: row.id, email: row.email, role: row.role, dormitoryIds: [] })
          if (row.dormitoryId != null) byId.get(row.id).dormitoryIds.push(row.dormitoryId)
        }
        this.users = [...byId.values()]
        this.supervisors = this.users.filter((u) => u.role === 'SUPERVISOR')
      } catch {
        this.error = '無法取得帳號資料'
      }
    },
    async addSupervisor(email, dormitoryIds) {
      const user = this.users.find((u) => u.email.toLowerCase() === email.toLowerCase())
      if (!user) throw new Error('請先讓該使用者註冊並驗證信箱')
      if (user.role !== 'STUDENT') throw new Error('該使用者不是學生帳號')
      await http.put(`/api/admin/users/${user.id}/role`, { role: 'SUPERVISOR' })
      for (const id of dormitoryIds) await http.put(`/api/admin/supervisors/${user.id}/dormitories/${id}`)
      await this.load()
    },
    async updateSupervisorDormitories(id, dormitoryIds) {
      const current = this.supervisors.find((u) => u.id === id)?.dormitoryIds || []
      for (const dormitoryId of current.filter((n) => !dormitoryIds.includes(n)))
        await http.delete(`/api/admin/supervisors/${id}/dormitories/${dormitoryId}`)
      for (const dormitoryId of dormitoryIds.filter((n) => !current.includes(n)))
        await http.put(`/api/admin/supervisors/${id}/dormitories/${dormitoryId}`)
      await this.load()
    },
    async removeSupervisor(id) {
      await http.put(`/api/admin/users/${id}/role`, { role: 'STUDENT' })
      await this.load()
    },
  },
})

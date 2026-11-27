import { defineStore } from 'pinia'
import http from '../api/http'

export const roleHome = {
  STUDENT: '/student/equipment',
  SUPERVISOR: '/supervisor/equipment',
  ADMIN: '/admin/signal-sources',
}

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: null, checked: false }),
  getters: {
    isLoggedIn: (state) => !!state.user,
    email: (state) => state.user?.email || '',
    role: (state) => state.user?.role || '',
  },
  actions: {
    async restore() {
      if (this.checked) return
      try {
        this.user = (await http.get('/api/auth/me')).data
      } catch {
        this.user = null
      } finally {
        this.checked = true
      }
    },
    async login(email, password) {
      this.user = (await http.post('/api/auth/login', { email, password })).data
      this.checked = true
      return this.user
    },
    async logout() {
      await http.post('/api/auth/logout')
      this.user = null
      this.checked = true
    },
  },
})

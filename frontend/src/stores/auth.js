import { defineStore } from 'pinia'

// 讀取瀏覽器裡存的登入資訊，重新整理頁面時用來恢復登入狀態
function loadSavedAuth() {
  try {
    const saved = localStorage.getItem('laundryAuth')
    return saved ? JSON.parse(saved) : null
  } catch {
    return null
  }
}

const savedAuth = loadSavedAuth()

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isLoggedIn: savedAuth?.isLoggedIn ?? false,
    email: savedAuth?.email ?? '',
  }),
  actions: {
    login(email) {
      this.isLoggedIn = true
      this.email = email
      localStorage.setItem('laundryAuth', JSON.stringify({ isLoggedIn: true, email }))
    },
    logout() {
      this.isLoggedIn = false
      this.email = ''
      localStorage.removeItem('laundryAuth')
    },
  },
})
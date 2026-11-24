import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isLoggedIn: false,
    email: '',
  }),
  actions: {
    login(email) {
      this.isLoggedIn = true
      this.email = email
    },
    logout() {
      this.isLoggedIn = false
      this.email = ''
    },
  },
})
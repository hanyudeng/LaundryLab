import { defineStore } from 'pinia'

export const useAccountsStore = defineStore('accounts', {
  state: () => ({
    supervisors: [
      { id: 1, email: 'a1234567@o365.fcu.edu.tw', dormitoryIds: [1] },
      { id: 2, email: 'b7654321@o365.fcu.edu.tw', dormitoryIds: [2] },
    ],
  }),
  actions: {
    addSupervisor(email, dormitoryIds) {
      const id = Date.now()
      this.supervisors.push({ id, email, dormitoryIds: [...dormitoryIds] })
      return id
    },
    updateSupervisorDormitories(id, dormitoryIds) {
      const supervisor = this.supervisors.find((s) => s.id === id)
      if (!supervisor) return
      supervisor.dormitoryIds = [...dormitoryIds]
    },
    removeSupervisor(id) {
      this.supervisors = this.supervisors.filter((s) => s.id !== id)
    },
  },
})
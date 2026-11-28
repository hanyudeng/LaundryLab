import { defineStore } from 'pinia'
import http from '../api/http'

export const useFacilitiesStore = defineStore('facilities', {
  state: () => ({ dormitories: [], rooms: [], machines: [], error: '' }),
  actions: {
    async load() {
      this.error = ''
      try {
        const { data } = await http.get('/api/admin/facilities')
        this.dormitories = data.dormitories
        this.rooms = data.rooms
        this.machines = data.machines
      } catch {
        this.error = '無法取得設備設定'
      }
    },
    async create(type, payload) {
      await http.post(`/api/admin/facilities/${type}`, payload)
      await this.load()
    },
    async update(type, id, payload) {
      await http.put(`/api/admin/facilities/${type}/${id}`, payload)
      await this.load()
    },
    addDormitory(name) { return this.create('dormitories', { name }) },
    updateDormitory(id, payload) { return this.update('dormitories', id, payload) },
    setDormitoryActive(id, isActive) { return this.update('dormitories', id, { isActive }) },
    addRoom(dormitoryId, name, location) { return this.create('rooms', { dormitoryId, name, location }) },
    updateRoom(id, payload) { return this.update('rooms', id, payload) },
    setRoomActive(id, isActive) { return this.update('rooms', id, { isActive }) },
    addMachine(roomId, code, defaultCycleSeconds) { return this.create('machines', { roomId, code, defaultCycleSeconds }) },
    updateMachine(id, payload) { return this.update('machines', id, payload) },
    setMachineActive(id, isActive) { return this.update('machines', id, { isActive }) },
  },
})

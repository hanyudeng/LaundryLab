import { defineStore } from 'pinia'

export const useFacilitiesStore = defineStore('facilities', {
  state: () => ({
    dormitories: [
      { id: 1, name: '第一學生宿舍', isActive: true },
      { id: 2, name: '第二學生宿舍', isActive: true },
    ],
    rooms: [
      { id: 1, dormitoryId: 1, name: 'A 棟洗衣間', location: '一樓東側', isActive: true },
      { id: 2, dormitoryId: 1, name: 'B 棟洗衣間', location: '一樓西側', isActive: true },
      { id: 3, dormitoryId: 2, name: 'C 棟洗衣間', location: '二樓', isActive: true },
    ],
    machines: [
      { id: 1, roomId: 1, code: 'A01', defaultCycleSeconds: 2700, isActive: true },
      { id: 2, roomId: 1, code: 'A02', defaultCycleSeconds: 2700, isActive: true },
      { id: 3, roomId: 2, code: 'B01', defaultCycleSeconds: 3000, isActive: true },
      { id: 4, roomId: 2, code: 'B02', defaultCycleSeconds: 3000, isActive: true },
      { id: 5, roomId: 3, code: 'C01', defaultCycleSeconds: 2700, isActive: true },
    ],
  }),
  actions: {
    addDormitory(name) {
      const id = Date.now()
      this.dormitories.push({ id, name, isActive: true })
      return id
    },
    updateDormitory(id, { name }) {
      const dormitory = this.dormitories.find((d) => d.id === id)
      if (!dormitory) return
      dormitory.name = name
    },
    setDormitoryActive(id, isActive) {
      const dormitory = this.dormitories.find((d) => d.id === id)
      if (dormitory) dormitory.isActive = isActive
    },
    addRoom(dormitoryId, name, location) {
      const id = Date.now()
      this.rooms.push({ id, dormitoryId, name, location, isActive: true })
      return id
    },
    updateRoom(id, { name, location }) {
      const room = this.rooms.find((r) => r.id === id)
      if (!room) return
      room.name = name
      room.location = location
    },
    setRoomActive(id, isActive) {
      const room = this.rooms.find((r) => r.id === id)
      if (room) room.isActive = isActive
    },
    addMachine(roomId, code, defaultCycleSeconds) {
      const id = Date.now()
      this.machines.push({ id, roomId, code, defaultCycleSeconds, isActive: true })
      return id
    },
    updateMachine(id, { code, defaultCycleSeconds }) {
      const machine = this.machines.find((m) => m.id === id)
      if (!machine) return
      machine.code = code
      machine.defaultCycleSeconds = defaultCycleSeconds
    },
    setMachineActive(id, isActive) {
      const machine = this.machines.find((m) => m.id === id)
      if (machine) machine.isActive = isActive
    },
  },
})
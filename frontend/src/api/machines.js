import http from './http'

// 查詢所有宿舍
export function fetchDormitories() {
  return http.get('/api/dormitories').then((res) => res.data)
}

// 查詢某個宿舍底下的洗衣間
export function fetchRooms(dormitoryId) {
  return http.get(`/api/dormitories/${dormitoryId}/rooms`).then((res) => res.data)
}

// 查詢某個洗衣間底下的機台
export function fetchMachines(roomId) {
  return http.get(`/api/rooms/${roomId}/machines`).then((res) => res.data)
}

// 查詢單一機台
export function fetchMachine(machineId) {
  return http.get(`/api/machines/${machineId}`).then((res) => res.data)
}
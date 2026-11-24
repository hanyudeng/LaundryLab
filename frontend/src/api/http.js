import axios from 'axios'

// 開發時透過 Vite 代理轉發到後端，所以不用寫後端的完整網址
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
})

export default http
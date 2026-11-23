import { ref, onMounted, onUnmounted } from 'vue'

// 每秒更新一次的「現在時間」，給需要顯示倒數的頁面共用
export function useCountdown() {
  const now = ref(Date.now())
  let timer = null

  onMounted(() => {
    timer = setInterval(() => {
      now.value = Date.now()
    }, 1000)
  })

  onUnmounted(() => {
    clearInterval(timer)
  })

  return { now }
}
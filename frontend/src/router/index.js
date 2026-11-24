import { createRouter, createWebHistory } from 'vue-router'
import StudentLayout from '../layouts/StudentLayout.vue'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登入', public: true },
  },
  {
    path: '/student',
    component: StudentLayout,
    children: [
      {
        path: 'equipment',
        name: 'student-equipment',
        component: () => import('../views/student/EquipmentView.vue'),
        meta: { title: '設備總覽' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
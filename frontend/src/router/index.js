import { createRouter, createWebHistory } from 'vue-router'
import StudentLayout from '../layouts/StudentLayout.vue'
import SupervisorLayout from '../layouts/SupervisorLayout.vue'
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
      },      {
        path: 'reminders',
        name: 'student-reminders',
        component: () => import('../views/student/RemindersView.vue'),
        meta: { title: '我的提醒' },
      },
            {
        path: 'reports',
        name: 'student-reports',
        component: () => import('../views/student/ReportsView.vue'),
        meta: { title: '問題回報' },
      },
            {
        path: 'reports/new',
        name: 'student-new-report',
        component: () => import('../views/student/NewReportView.vue'),
        meta: { title: '新增問題回報' },
      },
                 {
        path: 'account',
        name: 'student-account',
        component: () => import('../views/student/AccountView.vue'),
        meta: { title: '個人設定' },
      },
      
    ],
  },
{
    path: '/supervisor',
    component: SupervisorLayout,
    children: [
      {
        path: 'equipment',
        name: 'supervisor-equipment',
        component: () => import('../views/supervisor/EquipmentView.vue'),
        meta: { title: '設備總覽' },
      },
            {
        path: 'reports',
        name: 'supervisor-reports',
        component: () => import('../views/supervisor/ReportsView.vue'),
        meta: { title: '回報處理' },
      },
    ],
  },
]
const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
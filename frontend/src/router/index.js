import { createRouter, createWebHistory } from 'vue-router'
import StudentLayout from '../layouts/StudentLayout.vue'
import SupervisorLayout from '../layouts/SupervisorLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { roleHome, useAuthStore } from '../stores/auth'

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
    path: '/register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true },
  },
  {
    path: '/verify',
    component: () => import('../views/VerifyView.vue'),
    meta: { public: true },
  },
  {
    path: '/student',
    component: StudentLayout,
    meta: { role: 'STUDENT' },
    redirect: '/student/equipment',
    children: [
      {
        path: 'equipment',
        name: 'student-equipment',
        component: () => import('../views/student/EquipmentView.vue'),
        meta: { title: '設備總覽' },
      },
      {
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
    meta: { role: 'SUPERVISOR' },
    redirect: '/supervisor/equipment',
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
      {
        path: 'account',
        name: 'supervisor-account',
        component: () => import('../views/supervisor/AccountView.vue'),
        meta: { title: '個人設定' },
      },
      {
        path: 'stats',
        name: 'supervisor-stats',
        component: () => import('../views/supervisor/StatsView.vue'),
        meta: { title: '宿舍統計' },
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { role: 'ADMIN' },
    redirect: '/admin/account',
    children: [
              {
        path: 'equipment',
        name: 'admin-equipment',
        component: () => import('../views/admin/EquipmentSettingsView.vue'),
        meta: { title: '設備設定' },
      },
      {
        path: 'account',
        name: 'admin-account',
        component: () => import('../views/admin/AccountView.vue'),
        meta: { title: '個人設定' },
      },
            {
        path: 'accounts',
        name: 'admin-accounts',
        component: () => import('../views/admin/AccountsView.vue'),
        meta: { title: '帳號管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.restore()
  if (to.meta.public) return auth.isLoggedIn && to.path === '/login' ? roleHome[auth.role] : true
  if (!auth.isLoggedIn) return '/login'
  if (to.meta.role && auth.role !== to.meta.role) return roleHome[auth.role]
  return true
})

export default router

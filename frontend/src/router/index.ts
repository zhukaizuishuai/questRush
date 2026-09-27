import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/login/LoginView.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('@/views/register/RegisterView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/components/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/home/HomeView.vue') },
      { path: 'category', name: 'category', component: () => import('@/views/question/CategoryListView.vue') },
      { path: 'question', name: 'questionList', component: () => import('@/views/question/QuestionListView.vue') },
      { path: 'question/:id', name: 'questionDetail', component: () => import('@/views/question/QuestionDetailView.vue') },
      { path: 'practice', name: 'practice', component: () => import('@/views/practice/PracticeView.vue'), meta: { requiresAuth: true } },
      { path: 'wrong', name: 'wrongBook', component: () => import('@/views/practice/WrongBookView.vue'), meta: { requiresAuth: true } },
      { path: 'review', name: 'reviewQueue', component: () => import('@/views/practice/ReviewQueueView.vue'), meta: { requiresAuth: true } },
      { path: 'stats', name: 'stats', component: () => import('@/views/practice/StatsView.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/user/ProfileView.vue'), meta: { requiresAuth: true } },
      { path: 'vip', name: 'vip', component: () => import('@/views/user/VipView.vue') },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/user/FavoritesView.vue'), meta: { requiresAuth: true } },
      { path: 'notes', name: 'notes', component: () => import('@/views/user/NotesView.vue'), meta: { requiresAuth: true } },
      { path: 'orders', name: 'orders', component: () => import('@/views/user/OrdersView.vue'), meta: { requiresAuth: true } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'adminDashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'users', name: 'adminUsers', component: () => import('@/views/admin/UserManageView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'categories', name: 'adminCategories', component: () => import('@/views/admin/CategoryManageView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'questions', name: 'adminQuestions', component: () => import('@/views/admin/QuestionManageView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'question-edit/:id?', name: 'adminQuestionEdit', component: () => import('@/views/admin/QuestionEditView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 全局前置守卫：体验层权限（真正的权限判定全在服务端）
router.beforeEach((to) => {
  const userStore = useUserStore()

  // 已登录用户访问登录/注册页 → 跳首页
  if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    return { path: '/' }
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin) {
    if (!userStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 管理员路由普通用户跳首页
    if (!userStore.isAdmin) {
      return { path: '/' }
    }
  }
  return true
})

export default router

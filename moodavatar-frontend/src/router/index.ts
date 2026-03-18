import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/',          redirect: '/dashboard' },
    { path: '/login',            component: () => import('../views/LoginView.vue'),           meta: { guest: true } },
    { path: '/register',         component: () => import('../views/RegisterView.vue'),        meta: { guest: true } },
    { path: '/forgot-password',  component: () => import('../views/ForgotPasswordView.vue'),  meta: { guest: true } },
    { path: '/reset-password',   component: () => import('../views/ResetPasswordView.vue'),   meta: { guest: true } },
    { path: '/verify-email',     component: () => import('../views/VerifyEmailView.vue') },
    { path: '/dashboard', component: () => import('../views/DashboardView.vue'), meta: { auth: true } },
    { path: '/profile',   component: () => import('../views/ProfileView.vue'),   meta: { auth: true } },
    { path: '/friends',   component: () => import('../views/FriendsView.vue'),   meta: { auth: true } },
    { path: '/admin',     component: () => import('../views/admin/AdminView.vue'), meta: { admin: true } },
    { path: '/u/:username',    component: () => import('../views/PublicProfileView.vue') },
    { path: '/room/:username', component: () => import('../views/RoomView.vue'), meta: { auth: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth  && !auth.isLoggedIn)          return '/login'
  if (to.meta.guest && auth.isLoggedIn)            return '/dashboard'
  if (to.meta.admin && auth.user?.role !== 'ADMIN') return '/dashboard'
})

export default router

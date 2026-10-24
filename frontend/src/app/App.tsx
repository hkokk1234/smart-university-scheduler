import { lazy, Suspense, useEffect, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation, Link } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { useSession } from "@/features/auth/sessionStore"
import { usePreferences } from "@/features/settings/preferencesStore"
import { AppLayout } from '@/components/layout/AppLayout'
import { EmptyState, LoadingState } from "@/components/shared/RequestStates"
import { Button } from '@/components/ui/button'

const LandingPage =lazy(() => import('@/features/landing/LandingPage'))
const AuthPage =lazy(() => import('@/features/auth/AuthPage'))
const DashboardPage =lazy(() => import('@/features/dashboard/DashboardPage'))
const CatalogPage =lazy(() => import('@/features/catalog/CatalogPage'))
const CourseDetailsPage =lazy(() => import('@/features/catalog/CourseDetailsPage'))
const BuilderPage =lazy(() => import('@/features/planner/BuilderPage'))
const ResultsPage =lazy(() => import('@/features/planner/ResultsPage'))
const SavedSchedulesPage =lazy(() => import('@/features/schedules/SavedSchedulesPage'))
<<<<<<< HEAD
const NotificationsPage =lazy(() => import('@/features/notifications/NotificationsPage'))
const RepairSchedulePage =lazy(() => import('@/features/schedules/RepairSchedulePage'))
const AdminPage =lazy(() => import('@/features/admin/AdminPage'))
const CatalogImportPage =lazy(() => import('@/features/admin/CatalogImportPage'))
=======
const AdminPage =lazy(() => import('@/features/admin/AdminPage'))
>>>>>>> origin/main
const SettingsPage =lazy(() => import('@/features/settings/SettingsPage'))
const CopilotPage =lazy(() => import('@/features/copilot/CopilotPage'))
const DigitalTwinPage =lazy(() => import('@/features/digital-twin/DigitalTwinPage'))

function ProtectedRoute({ children, admin =false }: { children: ReactNode; admin?: boolean }) {
  const user =useSession((state) => state.user)
  const location =useLocation()
  if (!user) return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />
  if (admin && user.role !=='ADMIN') return <EmptyState title="Administrator access required" description="Your account does not have access to catalog administration." action={<Button asChild><Link to="/">Return to overview</Link></Button>} />
  return children
}

function HomeLayout() {
  const user =useSession((state) => state.user)
  const location =useLocation()
  if (!user && location.pathname !== '/') return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />
  return user ? <AppLayout /> : <LandingPage />
}

export function App() {
  const theme =usePreferences((state) => state.theme)
  const token =useSession((state) => state.token)
  const expiry =useSession((state) => state.expiresAt)
  const client =useQueryClient()
  useEffect(() => {
    const system =window.matchMedia('(prefers-color-scheme: dark)')
    const update =() => document.documentElement.classList.toggle('dark', theme ==='dark' || (theme ==='system' && system.matches))
    update(); system.addEventListener('change', update)
    return () => system.removeEventListener('change', update)
  }, [theme])
  useEffect(() => {
    if (!token) { client.removeQueries({ queryKey: ['private'] }); return }
    if (!expiry) return
    const remaining =expiry - Date.now()
    if (remaining <=0) { useSession.getState().clear(); return }
    const timer =setTimeout(() => useSession.getState().clear(), remaining)
    return () => clearTimeout(timer)
  }, [token, expiry, client])
  return <Suspense fallback={<div className="mx-auto max-w-6xl p-8"><LoadingState /></div>}><Routes>
    <Route path="/login" element={<AuthPage key="login" mode="login" />} />
    <Route path="/register" element={<AuthPage key="register" mode="register" />} />
    <Route path="/" element={<HomeLayout />}>
      <Route index element={<DashboardPage />} />
      <Route path="catalog" element={<ProtectedRoute><CatalogPage /></ProtectedRoute>} />
      <Route path="catalog/:courseId" element={<ProtectedRoute><CourseDetailsPage /></ProtectedRoute>} />
      <Route path="builder" element={<ProtectedRoute><BuilderPage /></ProtectedRoute>} />
      <Route path="results" element={<ProtectedRoute><ResultsPage /></ProtectedRoute>} />
      <Route path="saved" element={<ProtectedRoute><SavedSchedulesPage /></ProtectedRoute>} />
<<<<<<< HEAD
      <Route path="notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
      <Route path="saved/:scheduleId/repair" element={<ProtectedRoute><RepairSchedulePage /></ProtectedRoute>} />
      <Route path="admin" element={<ProtectedRoute admin><AdminPage /></ProtectedRoute>} />
      <Route path="admin/import" element={<ProtectedRoute admin><CatalogImportPage /></ProtectedRoute>} />
=======
      <Route path="admin" element={<ProtectedRoute admin><AdminPage /></ProtectedRoute>} />
>>>>>>> origin/main
      <Route path="digital-twin" element={<ProtectedRoute admin><DigitalTwinPage /></ProtectedRoute>} />
      <Route path="settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
      <Route path="copilot" element={<ProtectedRoute><CopilotPage /></ProtectedRoute>} />
      <Route path="*" element={<EmptyState title="This page isn’t in your timetable" description="The address may have changed.Head back to your overview to continue planning." action={<Button asChild><Link to="/">Back to overview</Link></Button>} />} />
    </Route>
  </Routes></Suspense>
}

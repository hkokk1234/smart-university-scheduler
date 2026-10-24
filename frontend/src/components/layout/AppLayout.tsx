import { Suspense, useEffect, useState } from 'react'
import { LoadingState } from "@/components/shared/RequestStates"
import { NavLink, Outlet, useLocation, Link } from 'react-router'
import { BookOpen, CalendarDays, GraduationCap, LayoutDashboard, LogOut, Menu, Search, Settings, ShieldCheck, Sparkles, Bookmark, ArrowUpRight } from 'lucide-react'
import { useSession } from "@/features/auth/sessionStore"
import { usePlanner } from "@/features/planner/plannerStore"
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { mergeClassNames } from "@/lib/classNames"
<<<<<<< HEAD
import { NotificationBell } from '@/features/notifications/NotificationBell'
=======
>>>>>>> origin/main

const items =[
  { to: '/', label: 'Overview', icon: LayoutDashboard },
  { to: '/catalog', label: 'Course catalog', icon: BookOpen },
  { to: '/builder', label: 'Schedule builder', icon: CalendarDays },
  { to: '/results', label: 'Generated results', icon: Sparkles },
  { to: '/saved', label: 'Saved schedules', icon: Bookmark },
<<<<<<< HEAD
  { to: '/notifications', label: 'Timetable updates', icon: CalendarDays },
=======
>>>>>>> origin/main
  { to: '/copilot', label: 'AI Academic Copilot', icon: Sparkles },
  { to: '/digital-twin', label: 'Campus Digital Twin', icon: LayoutDashboard, admin: true },
]
export function AppLayout() {
  const [menuOpen, setMenuOpen] =useState(false)
  const location =useLocation()
  const user =useSession((state) => state.user)
  const selectedCount =usePlanner((state) => state.courseIds.length)
  const current =[...items, { to: '/admin', label: 'Administration' }, { to: '/settings', label: 'Settings' }].find((item) => item.to ==='/' ? location.pathname ==='/' : location.pathname.startsWith(item.to))
  useEffect(() => {
    document.title =`${current?.label ?? 'Workspace'} · Smart University Scheduler`
  }, [current?.label])
  function signOut() {
    usePlanner.getState().setOwner(null)
    useSession.getState().clear()
  }
  const sidebar =<div className="flex h-full flex-col">
    <Link to="/" onClick={() => setMenuOpen(false)} className="flex items-center gap-3 px-3 py-5"><span className="flex size-10 items-center justify-center rounded-xl bg-primary text-primary-foreground"><GraduationCap size={23} /></span><span className="text-sm font-semibold leading-tight">Smart University<span className="block text-xs font-normal text-muted-foreground">Scheduler workspace</span></span></Link>
    <p className="mb-3 mt-7 px-4 text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground">Your workspace</p>
    <nav aria-label="Main navigation" className="space-y-1">{items.filter(item => !('admin' in item) || !item.admin || user?.role ==='ADMIN').map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} end={to ==='/'} onClick={() => setMenuOpen(false)} className={({ isActive }) => mergeClassNames("flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-colors", isActive ? 'bg-primary/10 text-primary' : "text-muted-foreground hover:bg-muted hover:text-foreground")}><Icon size={18} />{label}{to ==='/builder' && selectedCount > 0 && <span className="ml-auto rounded-md bg-primary/10 px-1.5 py-0.5 text-xs">{selectedCount}</span>}</NavLink>)}</nav>
    <div className="mt-7 space-y-1 border-t pt-5">{user?.role ==='ADMIN' && <NavLink to="/admin" onClick={() => setMenuOpen(false)} className={({ isActive }) => mergeClassNames("flex items-center gap-3 rounded-xl px-4 py-3 text-sm", isActive ? 'bg-primary/10 font-medium text-primary' : "text-muted-foreground hover:bg-muted")}><ShieldCheck size={18} />Administration</NavLink>}<NavLink to="/settings" onClick={() => setMenuOpen(false)} className={({ isActive }) => mergeClassNames("flex items-center gap-3 rounded-xl px-4 py-3 text-sm", isActive ? 'bg-primary/10 font-medium text-primary' : "text-muted-foreground hover:bg-muted")}><Settings size={18} />Settings</NavLink></div>
    <div className="mt-auto pt-8"><div className="rounded-2xl border bg-background p-4"><p className="text-sm font-semibold">A week that works for you.</p><p className="mt-2 text-xs leading-relaxed text-muted-foreground">Compare your options before committing to a schedule.</p><Link to="/builder" onClick={() => setMenuOpen(false)} className="mt-4 inline-flex items-center gap-2 text-xs font-semibold text-primary">Open builder <ArrowUpRight size={14} /></Link></div><button onClick={signOut} className="mt-4 flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm text-muted-foreground hover:bg-muted"><LogOut size={17} />Sign out</button></div>
  </div>
  return <div className="min-h-dvh bg-background">
    <a href="#main-content" className="fixed left-4 top-4 z-[100] -translate-y-24 rounded-lg bg-primary px-4 py-2 text-primary-foreground focus:translate-y-0">Skip to content</a>
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r bg-card px-4 pb-4 lg:block">{sidebar}</aside>
    <Dialog open={menuOpen} onOpenChange={setMenuOpen} title="Your workspace" description="Navigate Smart University Scheduler" className="left-0 top-0 h-dvh max-h-dvh w-72 max-w-[90vw] translate-x-0 translate-y-0 rounded-none">{sidebar}</Dialog>
    <div className="lg:pl-64">
      <header className="sticky top-0 z-20 flex h-20 items-center justify-between gap-3 border-b bg-card/95 px-5 backdrop-blur-sm sm:px-8">
        <div className="flex items-center gap-3"><Button variant="ghost" size="sm" aria-label="Open navigation" onClick={() => setMenuOpen(true)} className="lg:hidden"><Menu size={20} /></Button><div className="text-sm"><span className="hidden text-muted-foreground sm:inline">Workspace <span className="mx-2 text-border">/</span></span><span className="font-medium">{current?.label ?? 'Course details'}</span></div></div>
<<<<<<< HEAD
        <div className="flex items-center gap-4"><NotificationBell /><Link to="/catalog" className="rounded-lg p-2 text-muted-foreground hover:bg-muted" aria-label="Search courses"><Search size={19} /></Link><span className="hidden h-7 w-px bg-border sm:block" /><div className="hidden text-right sm:block"><p className="text-xs font-semibold">{user?.displayName}</p><p className="mt-0.5 text-[11px] text-muted-foreground">{user?.role ==='ADMIN' ? 'Administrator' : 'Student account'}</p></div><span className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary" aria-label={user?.displayName}>{user?.displayName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('')}</span></div>
=======
        <div className="flex items-center gap-4"><Link to="/catalog" className="rounded-lg p-2 text-muted-foreground hover:bg-muted" aria-label="Search courses"><Search size={19} /></Link><span className="hidden h-7 w-px bg-border sm:block" /><div className="hidden text-right sm:block"><p className="text-xs font-semibold">{user?.displayName}</p><p className="mt-0.5 text-[11px] text-muted-foreground">{user?.role ==='ADMIN' ? 'Administrator' : 'Student account'}</p></div><span className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary" aria-label={user?.displayName}>{user?.displayName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('')}</span></div>
>>>>>>> origin/main
      </header>
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-[1600px] space-y-7 p-5 outline-none sm:p-8 lg:p-9"><Suspense fallback={<LoadingState />}><Outlet /></Suspense></main>
      <footer className="mx-auto flex max-w-[1600px] flex-wrap justify-between gap-2 px-8 pb-6 text-[11px] text-muted-foreground"><span>Smart University Scheduler</span><span>Your courses.Your time.Your plan.</span></footer>
    </div>
  </div>
}

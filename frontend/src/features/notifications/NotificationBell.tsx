import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { Bell } from 'lucide-react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, streamNotifications } from '@/lib/api'
import { useSession } from '@/features/auth/sessionStore'
import { usePlanner } from '@/features/planner/plannerStore'

function pause(milliseconds: number, signal: AbortSignal) {
  return new Promise<void>(resolve => {
    const finish =() => { clearTimeout(timer); signal.removeEventListener('abort', finish); resolve() }
    const timer =setTimeout(finish, milliseconds)
    signal.addEventListener('abort', finish, { once: true })
    if (signal.aborted) finish()
  })
}

export function NotificationBell() {
  const user =useSession(state => state.user)
  const token =useSession(state => state.token)
  const [live, setLive] =useState(false)
  const client =useQueryClient()
  const query =useQuery({ queryKey: ['private', user?.id, 'notifications', 'badge'], enabled: !!user,
    queryFn: ({ signal }) => api.notifications(0, 1, signal), refetchInterval: 30_000 })
  const latestChange =query.data?.items.content[0]?.change.id
  useEffect(() => {
    if (!latestChange || !user) return
    // Polling also refreshes affected plans after a dropped signal or a server restart.
    void client.invalidateQueries({ queryKey: ['course'] })
    void client.invalidateQueries({ queryKey: ['private', user.id, 'schedules'] })
    usePlanner.getState().setResults(null)
  }, [latestChange, user, client])
  useEffect(() => {
    if (!token || !user) return
    const controller =new AbortController()
    const refreshInbox =() => void client.invalidateQueries({ queryKey: ['private', user.id, 'notifications'] })
    const refreshCatalog =(clearResults: boolean) => {
      void client.invalidateQueries({ queryKey: ['courses'] })
      void client.invalidateQueries({ queryKey: ['course'] })
      void client.invalidateQueries({ queryKey: ['private', user.id, 'schedules'] })
      void client.invalidateQueries({ queryKey: ['private', user.id, 'section-history'] })
      void client.invalidateQueries({ queryKey: ['private', user.id, 'digital-twin'] })
      if (clearResults) usePlanner.getState().setResults(null)
    }
    async function connect() {
      let delay =1000
      while (!controller.signal.aborted) {
        try {
          await streamNotifications(controller.signal, event => {
            if (controller.signal.aborted) return
            if (event.name ==='ready') { setLive(true); delay =1000; refreshInbox(); refreshCatalog(false) }
            if (event.name ==='catalog-changed') { refreshInbox(); refreshCatalog(true) }
            if (event.name ==='inbox-changed') refreshInbox()
          })
        } catch { /* Persistent inbox polling remains available during reconnects. */ }
        if (controller.signal.aborted) return
        setLive(false)
        await pause(delay, controller.signal)
        delay =Math.min(30_000, delay * 2)
      }
    }
    void connect()
    return () => controller.abort()
  }, [token, user, client])
  const count =query.data?.unreadCount ?? 0
  return <Link to="/notifications" className="relative rounded-lg p-2 text-muted-foreground hover:bg-muted" aria-label={`Notifications${count ? `, ${count} unread` : ''}`} title={live ? 'Live updates connected' : 'Live updates reconnecting; inbox refreshes every 30 seconds'}>
    <Bell size={19} /><span aria-hidden="true" className={`absolute bottom-1 left-1 size-1.5 rounded-full ${live ? 'bg-emerald-500' : 'bg-amber-500'}`} />
    {count > 0 && <span className="absolute -right-1 -top-1 min-w-4 rounded-full bg-primary px-1 text-center text-[10px] text-primary-foreground">{count > 99 ? '99+' : count}</span>}
  </Link>
}

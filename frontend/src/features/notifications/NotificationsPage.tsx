import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { useSession } from '@/features/auth/sessionStore'
import { PageHeading } from '@/components/shared/PageHeading'
import { EmptyState, ErrorState, LoadingState } from '@/components/shared/RequestStates'
import { Pagination } from '@/components/shared/Pagination'
import { Card, Badge } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ChangeDetails } from './ChangeDetails'

export default function NotificationsPage() {
  const user =useSession(state => state.user)
  const [page, setPage] =useState(0)
  const client =useQueryClient()
  const key =['private', user?.id, 'notifications']
  const query =useQuery({ queryKey: [...key, page], queryFn: ({ signal }) => api.notifications(page, 20, signal), refetchInterval: 30_000 })
  const read =useMutation({ mutationFn: api.readNotification, onSuccess: () => void client.invalidateQueries({ queryKey: key }) })
  const readAll =useMutation({ mutationFn: api.readAllNotifications, onSuccess: () => void client.invalidateQueries({ queryKey: key }) })
  return <>
    <PageHeading eyebrow="Stay in sync" title="Timetable updates" description="Changes to sections included in your saved schedules. Updates remain here while you are offline." action={<Button variant="outline" disabled={!query.data?.unreadCount || readAll.isPending} onClick={() => readAll.mutate()}>Mark all as read</Button>} />
    <p className="text-sm text-muted-foreground">{query.data?.unreadCount ?? 0} unread updates. Review affected schedules for new conflicts.</p>
    {(read.error || readAll.error) && <ErrorState error={(read.error || readAll.error)!} />}
    {query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : !query.data.items.content.length ? <EmptyState title="You're up to date" description="When an administrator changes a section in one of your saved plans, its before and after details will appear here." /> : query.data.items.content.map(notification => <Card key={notification.id} className="space-y-4 p-5"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-semibold">{notification.change.courseCode} · {notification.change.courseTitle}</h2>{!notification.readAt && <Badge>Unread</Badge>}</div>{!notification.readAt && <Button variant="outline" size="sm" disabled={read.isPending} onClick={() => read.mutate(notification.id)}>Mark as read</Button>}</div><ChangeDetails change={notification.change} /><div className="flex flex-wrap gap-3"><Button asChild variant="outline" size="sm"><Link to="/saved">Review saved schedules</Link></Button><Button asChild variant="ghost" size="sm"><Link to={'/catalog/' + notification.change.courseId}>View current course</Link></Button></div></Card>)}
    {query.data && <Pagination page={page} totalPages={query.data.items.totalPages} totalElements={query.data.items.totalElements} change={setPage} />}
  </>
}

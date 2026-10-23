import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Bookmark, CalendarDays, Clock, Plus, Trash2, ArrowUpRight } from 'lucide-react'
import { api } from '@/lib/api'
import type { SavedSchedule } from '@/lib/types'
import { useSchedules, useSemesters } from "@/lib/catalogQueries"
import { useSession } from "@/features/auth/sessionStore"
import { calculateScheduleStats } from "@/features/planner/scheduleCalculations"
import { WeeklyCalendar } from '@/features/planner/WeeklyCalendar'
import { ConflictNotice } from '@/features/planner/ConflictNotice'
import { PageHeading } from '@/components/shared/PageHeading'
import { ErrorState, LoadingState, EmptyState } from "@/components/shared/RequestStates"
import { Pagination } from '@/components/shared/Pagination'
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Card, Badge } from '@/components/ui/card'

function entries(schedule: SavedSchedule) { return schedule.sections.map(({ section }) => ({ section, courseCode: section.courseCode, courseTitle: section.courseTitle })) }
export default function SavedSchedulesPage() {
  const [page, setPage] =useState(0)
  const [preview, setPreview] =useState<SavedSchedule | null>(null)
  const [removing, setRemoving] =useState<SavedSchedule | null>(null)
  const [notice, setNotice] =useState('')
  const user =useSession((state) => state.user)
  const query =useSchedules(page)
  const semesters =useSemesters()
  const client =useQueryClient()
  const deletion =useMutation({ mutationFn: (id: string) => api.deleteSchedule(id), onSuccess: () => { setRemoving(null); setNotice("Schedule deleted.Its courses and sections are unchanged."); void client.invalidateQueries({ queryKey: ['private', user?.id, 'schedules'] }); if (query.data?.content.length ===1 && page > 0) setPage(page - 1) } })
  return <>
    <PageHeading eyebrow="Keep your possibilities" title="Saved schedules" description="Your private collection of plans.Revisit a week,compare your choices,and keep the options you like." action={<Button asChild><Link to="/builder"><Plus size={16} />Create a schedule</Link></Button>} />
    {notice && <p role="status" className="rounded-xl bg-secondary p-4 text-sm text-primary">{notice}</p>}
    {query.isPending ? <LoadingState label="Loading your saved plans…" /> : query.isError ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : !query.data.content.length ? <EmptyState title="There’s room for your first plan" description="Save a selection from the builder or a generated option.Only you can view your saved schedules." action={<Button asChild><Link to="/builder">Build your first schedule</Link></Button>} /> : <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{query.data.content.map((schedule) => { const stats =calculateScheduleStats(entries(schedule)); return <Card key={schedule.id} className="p-6"><div className="flex items-start justify-between"><span className="rounded-xl bg-secondary p-3 text-primary"><Bookmark size={21} /></span><Button variant="ghost" size="sm" aria-label={'Delete ' + schedule.name} onClick={() => { deletion.reset(); setRemoving(schedule) }}><Trash2 size={16} className="text-muted-foreground" /></Button></div><h2 className="mt-5 text-lg font-semibold">{schedule.name}</h2><p className="mt-1 text-xs text-muted-foreground">{semesters.data?.find((semester) => semester.id ===schedule.semesterId)?.name ?? 'Semester plan'}</p><div className="mt-4 flex flex-wrap gap-1.5">{[...new Set(schedule.sections.map(({ section }) => section.courseCode))].map((code) => <Badge key={code}>{code}</Badge>)}{!schedule.sections.length && <Badge>Empty draft</Badge>}</div><div className="mt-5 flex gap-4 border-t pt-4 text-xs text-muted-foreground"><span className="flex items-center gap-1.5"><CalendarDays size={13} />{stats.days} days</span><span className="flex items-center gap-1.5"><Clock size={13} />{stats.hours.toFixed(1)}h weekly</span></div><div className="mt-5 flex flex-wrap items-center justify-between gap-2"><span className="text-[10px] text-muted-foreground">Saved {new Date(schedule.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</span>{schedule.sections.length > 0 && <Button asChild variant="outline" size="sm"><Link to={'/saved/' + schedule.id + '/repair'}>Repair</Link></Button>}<Button variant="outline" size="sm" onClick={() => setPreview(schedule)}>View plan<ArrowUpRight size={14} /></Button></div></Card> })}</div>}
    {query.data && <Pagination page={page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} change={setPage} />}
    <Dialog open={!!preview} onOpenChange={(open) => { if (!open) setPreview(null) }} title={preview?.name ?? 'Saved schedule'} description="This schedule attends all meetings of each selected section.Published section changes may update these meeting times." className="max-w-5xl">{preview && <div className="space-y-5"><ConflictNotice entries={entries(preview)} /><WeeklyCalendar entries={entries(preview)} timeZone={semesters.data?.find((semester) => semester.id ===preview.semesterId)?.timeZone} /></div>}</Dialog>
    <Dialog open={!!removing} onOpenChange={(open) => { if (!open && !deletion.isPending) setRemoving(null) }} title="Delete this schedule?" description={`“${removing?.name ?? ''}” will be removed from your saved plans. This action cannot be undone.`}>{deletion.isError && <ErrorState error={deletion.error} />}<div className="mt-5 flex justify-end gap-3"><Button variant="outline" disabled={deletion.isPending} onClick={() => setRemoving(null)}>Keep schedule</Button><Button variant="destructive" disabled={deletion.isPending} onClick={() => removing && deletion.mutate(removing.id)}>{deletion.isPending ? 'Deleting…' : 'Delete schedule'}</Button></div></Dialog>
  </>
}

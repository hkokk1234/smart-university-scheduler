import { useState, type FormEvent } from 'react'
<<<<<<< HEAD
import { Link } from 'react-router'
=======
>>>>>>> origin/main
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, ShieldCheck, Search } from 'lucide-react'
import { api } from '@/lib/api'
import type { Section } from '@/lib/types'
import { useCourses, useCourse } from "@/lib/catalogQueries"
import { useDebouncedValue } from "@/lib/useDebouncedValue"
import { useSession } from "@/features/auth/sessionStore"
import { usePlanner } from "@/features/planner/plannerStore"
import { SectionEditor } from './SectionEditor'
<<<<<<< HEAD
import { SectionHistory } from './SectionHistory'
=======
>>>>>>> origin/main
import { PageHeading } from '@/components/shared/PageHeading'
import { SemesterSelect } from '@/components/shared/SemesterSelect'
import { LoadingState, ErrorState, EmptyState } from "@/components/shared/RequestStates"
import { Pagination } from '@/components/shared/Pagination'
import { Button } from '@/components/ui/button'
import { Card, Badge } from '@/components/ui/card'
import { Input, Select } from '@/components/ui/input'
import { Dialog } from '@/components/ui/dialog'

export default function AdminPage() {
  const semesterId =usePlanner((state) => state.semesterId)
  const user =useSession((state) => state.user)
  const [search, setSearch] =useState('')
  const [page, setPage] =useState(0)
  const [courseId, setCourseId] =useState('')
  const [editing, setEditing] =useState<Section | null>(null)
<<<<<<< HEAD
  const [history, setHistory] =useState<Section | null>(null)
=======
>>>>>>> origin/main
  const [notice, setNotice] =useState('')
  const q =useDebouncedValue(search)
  const courses =useCourses({ semesterId: semesterId || undefined, q, page, size: 20 })
  const detail =useCourse(courseId || undefined)
  const rooms =useQuery({ queryKey: ['private', user?.id, 'rooms'], queryFn: ({ signal }) => api.rooms(signal) })
  const instructors =useQuery({ queryKey: ['private', user?.id, 'instructors'], queryFn: ({ signal }) => api.instructors(signal) })
  const client =useQueryClient()
  const creation =useMutation({ mutationFn: api.createCourse, onSuccess: () => { void client.invalidateQueries({ queryKey: ['courses'] }); setNotice("Course created successfully.Sections can be published through the university catalog import."); setCourseId('') } })
  function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form =new FormData(event.currentTarget)
    creation.mutate({ semesterId, code: String(form.get('code')).trim(), title: String(form.get('title')).trim(), credits: Number(form.get('credits')) })
  }
  return <>
    <PageHeading eyebrow="Catalog management" title="Administration" description="Publish course information and keep section meeting times up to date.Changes are protected by administrator authorization." action={<SemesterSelect />} />
<<<<<<< HEAD
    <Card className="flex flex-wrap items-center justify-between gap-4 p-5"><div><h2 className="font-semibold">Import your university timetable</h2><p className="mt-1 text-sm text-muted-foreground">Upload PDF or Excel, map columns and correct rows before publishing courses and sections.</p></div><Button asChild><Link to="/admin/import">Import PDF / Excel</Link></Button></Card>
    <p className="flex items-center gap-2 rounded-xl bg-secondary px-4 py-3 text-xs text-primary"><ShieldCheck size={16} />Administrative changes are authorized by the server.Section edits require the current version.</p>
    {notice && <p role="status" className="rounded-xl border bg-card p-4 text-sm text-primary">{notice}</p>}
    <Dialog open={!!history} onOpenChange={open => { if (!open) setHistory(null) }} title={history ? `Section ${history.code} · Change history` : 'Change history'} description="Recorded section changes with their original before and after details." className="max-w-3xl">{history && <SectionHistory key={history.id} sectionId={history.id} />}</Dialog>
    <div className="grid items-start gap-6 xl:grid-cols-[340px_minmax(0,1fr)]"><Card className="p-6"><h2 className="text-lg font-semibold">Create a course</h2><p className="mt-2 text-xs leading-relaxed text-muted-foreground">Add a course to the selected semester.Section groups and new sections remain managed by your catalog import.</p><form onSubmit={create} className="mt-6 space-y-4"><div><label htmlFor="course-code" className="mb-2 block text-xs font-medium">Course code</label><Input id="course-code" name="code" placeholder="Course code" required maxLength={32} /></div><div><label htmlFor="course-title" className="mb-2 block text-xs font-medium">Course title</label><Input id="course-title" name="title" placeholder="Course title" required maxLength={200} /></div><div><label htmlFor="course-credits" className="mb-2 block text-xs font-medium">Credits</label><Input id="course-credits" name="credits" type="number" min="0.1" max="60" step="0.1" required placeholder="Number of credits" /></div>{creation.isError && <ErrorState error={creation.error} />}<Button className="w-full" type="submit" disabled={!semesterId || creation.isPending}><Plus size={16} />{creation.isPending ? 'Creating…' : 'Create course'}</Button>{!semesterId && <p className="text-xs text-muted-foreground">A published semester is required before adding courses.</p>}</form></Card>
    <div className="space-y-5"><Card className="p-6"><h2 className="text-lg font-semibold">Sections and meetings</h2><p className="mt-2 text-sm text-muted-foreground">Find a course to manage its published section alternatives.</p><div className="relative mt-5"><Search size={17} className="absolute left-3 top-3.5 text-muted-foreground" /><Input aria-label="Find a course to administer" className="pl-10" placeholder="Search the catalog…" value={search} maxLength={100} onChange={(event) => { setSearch(event.target.value); setPage(0); setCourseId('') }} /></div>{courses.isPending ? <LoadingState label="Loading courses…" /> : courses.isError ? <ErrorState error={courses.error} retry={() => void courses.refetch()} /> : <><Select aria-label="Choose course to manage" className="mt-4" value={courses.data.content.some((course) => course.id ===courseId) ? courseId : ''} onChange={(event) => setCourseId(event.target.value)}><option value="">Choose a course</option>{courses.data.content.map((course) => <option key={course.id} value={course.id}>{course.code} · {course.title}</option>)}</Select><div className="mt-5"><Pagination page={page} totalPages={courses.data.totalPages} totalElements={courses.data.totalElements} change={(value) => { setPage(value); setCourseId('') }} /></div></>}</Card>
    {rooms.isError && <ErrorState error={rooms.error} retry={() => void rooms.refetch()} />}{instructors.isError && <ErrorState error={instructors.error} retry={() => void instructors.refetch()} />}
    {courseId && (detail.isPending ? <LoadingState label="Loading section groups…" /> : detail.isError ? <ErrorState error={detail.error} retry={() => void detail.refetch()} /> : detail.data && (!detail.data.sectionGroups.length ? <EmptyState title="No sections published" description="This course does not yet have section groups.Add groups and alternatives through your catalog import before editing meetings here." /> : detail.data.sectionGroups.map(({ group, sections }) => <Card key={group.id} className="p-5"><div className="mb-4 flex gap-3"><h3 className="text-sm font-semibold">{group.name}</h3><Badge>{group.type.toLowerCase()}</Badge></div><div className="divide-y">{sections.map((section) => <div key={section.id} className="flex flex-wrap items-center justify-between gap-4 py-4"><div><p className="text-sm font-medium">Section {section.code}</p><p className="mt-1 text-xs text-muted-foreground">{section.meetings.length} meetings · {section.capacity} capacity · Version {section.version}</p></div><Button size="sm" variant="outline" onClick={() => setHistory(section)}>History</Button><Button size="sm" variant="outline" disabled={!rooms.data || !instructors.data} onClick={() => setEditing(section)}><Pencil size={14} />Edit section</Button></div>)}</div></Card>)))}</div></div>
=======
    <p className="flex items-center gap-2 rounded-xl bg-secondary px-4 py-3 text-xs text-primary"><ShieldCheck size={16} />Administrative changes are authorized by the server.Section edits require the current version.</p>
    {notice && <p role="status" className="rounded-xl border bg-card p-4 text-sm text-primary">{notice}</p>}
    <div className="grid items-start gap-6 xl:grid-cols-[340px_minmax(0,1fr)]"><Card className="p-6"><h2 className="text-lg font-semibold">Create a course</h2><p className="mt-2 text-xs leading-relaxed text-muted-foreground">Add a course to the selected semester.Section groups and new sections remain managed by your catalog import.</p><form onSubmit={create} className="mt-6 space-y-4"><div><label htmlFor="course-code" className="mb-2 block text-xs font-medium">Course code</label><Input id="course-code" name="code" placeholder="Course code" required maxLength={32} /></div><div><label htmlFor="course-title" className="mb-2 block text-xs font-medium">Course title</label><Input id="course-title" name="title" placeholder="Course title" required maxLength={200} /></div><div><label htmlFor="course-credits" className="mb-2 block text-xs font-medium">Credits</label><Input id="course-credits" name="credits" type="number" min="0.1" max="60" step="0.1" required placeholder="Number of credits" /></div>{creation.isError && <ErrorState error={creation.error} />}<Button className="w-full" type="submit" disabled={!semesterId || creation.isPending}><Plus size={16} />{creation.isPending ? 'Creating…' : 'Create course'}</Button>{!semesterId && <p className="text-xs text-muted-foreground">A published semester is required before adding courses.</p>}</form></Card>
    <div className="space-y-5"><Card className="p-6"><h2 className="text-lg font-semibold">Sections and meetings</h2><p className="mt-2 text-sm text-muted-foreground">Find a course to manage its published section alternatives.</p><div className="relative mt-5"><Search size={17} className="absolute left-3 top-3.5 text-muted-foreground" /><Input aria-label="Find a course to administer" className="pl-10" placeholder="Search the catalog…" value={search} maxLength={100} onChange={(event) => { setSearch(event.target.value); setPage(0); setCourseId('') }} /></div>{courses.isPending ? <LoadingState label="Loading courses…" /> : courses.isError ? <ErrorState error={courses.error} retry={() => void courses.refetch()} /> : <><Select aria-label="Choose course to manage" className="mt-4" value={courses.data.content.some((course) => course.id ===courseId) ? courseId : ''} onChange={(event) => setCourseId(event.target.value)}><option value="">Choose a course</option>{courses.data.content.map((course) => <option key={course.id} value={course.id}>{course.code} · {course.title}</option>)}</Select><div className="mt-5"><Pagination page={page} totalPages={courses.data.totalPages} totalElements={courses.data.totalElements} change={(value) => { setPage(value); setCourseId('') }} /></div></>}</Card>
    {rooms.isError && <ErrorState error={rooms.error} retry={() => void rooms.refetch()} />}{instructors.isError && <ErrorState error={instructors.error} retry={() => void instructors.refetch()} />}
    {courseId && (detail.isPending ? <LoadingState label="Loading section groups…" /> : detail.isError ? <ErrorState error={detail.error} retry={() => void detail.refetch()} /> : detail.data && (!detail.data.sectionGroups.length ? <EmptyState title="No sections published" description="This course does not yet have section groups.Add groups and alternatives through your catalog import before editing meetings here." /> : detail.data.sectionGroups.map(({ group, sections }) => <Card key={group.id} className="p-5"><div className="mb-4 flex gap-3"><h3 className="text-sm font-semibold">{group.name}</h3><Badge>{group.type.toLowerCase()}</Badge></div><div className="divide-y">{sections.map((section) => <div key={section.id} className="flex flex-wrap items-center justify-between gap-4 py-4"><div><p className="text-sm font-medium">Section {section.code}</p><p className="mt-1 text-xs text-muted-foreground">{section.meetings.length} meetings · {section.capacity} capacity · Version {section.version}</p></div><Button size="sm" variant="outline" disabled={!rooms.data || !instructors.data} onClick={() => setEditing(section)}><Pencil size={14} />Edit section</Button></div>)}</div></Card>)))}</div></div>
>>>>>>> origin/main
    <Dialog open={!!editing} onOpenChange={(open) => { if (!open) setEditing(null) }} title={editing ? 'Edit section ' + editing.code : 'Edit section'} description="Update published details and the full weekly meeting list." className="max-w-3xl">{editing && rooms.data && instructors.data && <SectionEditor key={editing.id + ':' + editing.version} section={editing} rooms={rooms.data} instructors={instructors.data} close={() => setEditing(null)} saved={() => { setEditing(null); setNotice("Section changes saved.Its schedules will use the updated meeting list.") }} reload={() => { setEditing(null); void detail.refetch(); setNotice("Reloaded current details.Open the section again to apply your changes.") }} />}</Dialog>
  </>
}

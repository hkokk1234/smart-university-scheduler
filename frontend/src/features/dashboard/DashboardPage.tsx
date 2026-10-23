import { Link } from 'react-router'
import { ArrowRight,ArrowUpRight,BookOpen,Bookmark,CalendarDays,Clock,Plus,Sparkles } from 'lucide-react'
import { useSession } from "@/features/auth/sessionStore"
import { usePlanner } from "@/features/planner/plannerStore"
import { buildCalendarSections,calculateScheduleStats } from "@/features/planner/scheduleCalculations"
import { useCourses,useSchedules,useSelectedCourses,useSemesters } from "@/lib/catalogQueries"
import { PageHeading } from '@/components/shared/PageHeading'
import { SemesterSelect } from '@/components/shared/SemesterSelect'
import { ErrorState,EmptyState } from "@/components/shared/RequestStates"
import { Button } from '@/components/ui/button'
import { Card,Badge,Skeleton } from '@/components/ui/card'

export default function DashboardPage() {
  const user =useSession((state) => state.user)
  const semesterId =usePlanner((state) => state.semesterId)
  const ids =usePlanner((state) => state.courseIds)
  const chosen =usePlanner((state) => state.sectionIds)
  const courses =useCourses({ semesterId: semesterId || undefined,size: 1 })
  const saved =useSchedules(0,3)
  const details =useSelectedCourses()
  const semesters =useSemesters()
  const active =semesters.data?.find((semester) => semester.id ===semesterId)
  const entries =buildCalendarSections(details.flatMap((query) => query.data ? [query.data] : []),chosen)
  const stats =calculateScheduleStats(entries)
  const metrics =[
    { label: 'Courses offered',value: courses.data?.totalElements,pending: courses.isPending,icon: BookOpen,note: 'In the selected semester' },
    { label: 'Courses in your draft',value: ids.length,pending: false,icon: CalendarDays,note: 'Ready to compare' },
    { label: 'Saved schedules',value: saved.data?.totalElements,pending: saved.isPending,icon: Bookmark,note: 'Private to your account' },
    { label: 'Weekly meeting hours',value: stats.hours.toFixed(1),pending: details.some((query) => query.isPending),icon: Clock,note: 'From your current choices' },
  ]
  return <>
    <PageHeading title={`Hello,${user?.displayName.split(' ')[0] ?? 'there'}.`} description="A fresh look at your semester.Let’s make your next week a good one." action={<SemesterSelect />} />
    <div className="relative overflow-hidden rounded-2xl bg-[#481721] p-7 text-white sm:p-9"><div className="relative z-10 max-w-2xl"><Badge className="border border-white/15 bg-white/10 text-white/85">{active?.name ?? 'Your planning workspace'}</Badge><h2 className="mt-5 text-2xl font-medium leading-snug tracking-tight sm:text-3xl">Less juggling.<br />More room for your semester.</h2><p className="mt-3 max-w-md text-sm leading-relaxed text-white/70">Bring your courses together,spot the overlaps,and find a timetable that feels right.</p><Button asChild className="mt-6 bg-[#f3e7dc] text-[#481721] hover:bg-white"><Link to={ids.length ? '/builder' : '/catalog'}>{ids.length ? 'Continue your draft' : 'Explore the course catalog'}<ArrowRight size={16} /></Link></Button></div><div aria-hidden="true" className="pointer-events-none absolute -right-16 -top-10 size-80 rounded-full border-[45px] border-white/5 sm:right-2" /><div aria-hidden="true" className="pointer-events-none absolute bottom-8 right-12 hidden rotate-[-8deg] rounded-2xl border border-white/10 bg-white/5 p-8 backdrop-blur-sm xl:block"><CalendarDays size={80} className="text-[#edc1c0]/80" /></div></div>
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{metrics.map(({ label,value,pending,icon: Icon,note }) => <Card key={label} className="p-5"><div className="flex items-center justify-between"><p className="text-xs text-muted-foreground">{label}</p><span className="rounded-lg bg-secondary p-2 text-primary"><Icon size={16} /></span></div>{pending ? <Skeleton className="my-3 h-9 w-16" /> : <p className="my-3 text-3xl font-semibold tracking-tight">{value ?? '—'}</p>}<p className="text-[11px] text-muted-foreground">{note}</p></Card>)}</div>
    {courses.isError && <ErrorState error={courses.error} retry={() => void courses.refetch()} />}{semesters.isError && <ErrorState error={semesters.error} retry={() => void semesters.refetch()} />}
    <div className="grid items-start gap-6 xl:grid-cols-[1.25fr_1fr]"><Card className="p-6"><div className="mb-5 flex items-center justify-between"><h2 className="text-base font-semibold">Your week in progress</h2><Link to="/builder" className="inline-flex items-center gap-1 text-xs font-medium text-primary">Open builder<ArrowUpRight size={14} /></Link></div>{entries.length ? <div className="space-y-3">{entries.flatMap((entry) => entry.section.meetings.map((meeting) => <div key={meeting.id} className="flex items-center gap-4 rounded-xl bg-background p-3.5"><span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-secondary text-[10px] font-bold text-primary">{meeting.weekday.slice(0,3)}</span><div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold">{entry.courseCode} · {entry.courseTitle}</p><p className="mt-1 text-xs text-muted-foreground">Section {entry.section.code} · {meeting.room?.code ?? 'Room to be assigned'}</p></div><span className="text-xs text-muted-foreground">{meeting.startTime.slice(0,5)}<span className="block">{meeting.endTime.slice(0,5)}</span></span></div>)).slice(0,5)}</div> : <EmptyState title="Your timetable is a blank canvas" description="Choose courses and section alternatives to preview your weekly meetings here." action={<Button asChild variant="outline"><Link to="/catalog"><Plus size={14} />Add your first course</Link></Button>} />}</Card>
    <div className="space-y-5"><Card className="p-6"><div className="mb-5 flex items-center justify-between"><h2 className="text-base font-semibold">Recently saved</h2><Link to="/saved" className="inline-flex items-center gap-1 text-xs font-medium text-primary">View all<ArrowUpRight size={14} /></Link></div>{saved.isPending ? <Skeleton className="h-24" /> : saved.isError ? <ErrorState error={saved.error} retry={() => void saved.refetch()} /> : !saved.data.content.length ? <p className="py-4 text-sm leading-relaxed text-muted-foreground">Your favorite options will appear here once you save a schedule.</p> : <div className="space-y-3">{saved.data.content.map((schedule) => <Link key={schedule.id} to="/saved" className="flex items-center gap-3 rounded-xl bg-background p-3.5 hover:bg-secondary"><span className="rounded-lg bg-card p-2 text-primary"><Bookmark size={16} /></span><span className="flex-1"><span className="block text-sm font-medium">{schedule.name}</span><span className="mt-1 block text-[11px] text-muted-foreground">{schedule.sections.length} sections · {new Date(schedule.createdAt).toLocaleDateString()}</span></span><ArrowUpRight size={15} className="text-muted-foreground" /></Link>)}</div>}</Card><Card className="border-primary/15 bg-secondary/60 p-6"><Sparkles className="mb-4 text-primary" size={24} /><h2 className="text-base font-semibold">There’s more than one good week.</h2><p className="mt-2 text-sm leading-relaxed text-muted-foreground">Generate combinations from your selected courses,then compare days on campus and gaps between meetings.</p><Button asChild variant="outline" className="mt-5"><Link to="/builder">Compare schedule options<ArrowRight size={15} /></Link></Button></Card></div></div>
  </>
}

import { Link, useParams } from 'react-router'
import { ArrowLeft, Check, Plus, Clock, MapPin, UserRound } from 'lucide-react'
import { useCourse } from "@/lib/catalogQueries"
import { usePlanner } from "@/features/planner/plannerStore"
import { PageHeading } from '@/components/shared/PageHeading'
import { LoadingState, ErrorState, EmptyState } from "@/components/shared/RequestStates"
import { Button } from '@/components/ui/button'
import { Badge, Card } from '@/components/ui/card'

export default function CourseDetailsPage() {
  const { courseId } =useParams()
  const query =useCourse(courseId)
  const selected =usePlanner((state) => state.courseIds)
  if (query.isPending) return <LoadingState label="Loading course and section details…" />
  if (query.isError) return <ErrorState error={query.error} retry={() => void query.refetch()} />
  const { course, sectionGroups } =query.data
  function add() { if (usePlanner.getState().semesterId !==course.semesterId) usePlanner.getState().setSemester(course.semesterId); usePlanner.getState().addCourse(course.id) }
  return <>
    <Link to="/catalog" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-primary"><ArrowLeft size={15} />Back to catalog</Link>
    <PageHeading eyebrow={course.code} title={course.title} description={`${course.credits} credits · Choosing a section includes every meeting listed below.`} action={<Button disabled={selected.includes(course.id) || selected.length >=8} onClick={add}>{selected.includes(course.id) ? <><Check size={16} />In your builder</> : <><Plus size={16} />Add to builder</>}</Button>} />
    {!sectionGroups.length ? <EmptyState title="Sections aren’t published yet" description="This course is in the catalog,but there are no section alternatives available.Check back after your administrator publishes them." /> : sectionGroups.map(({ group, sections }) => <section key={group.id} className="space-y-4"><div className="flex flex-wrap items-center gap-3"><h2 className="text-lg font-semibold">{group.name}</h2><Badge>{group.type.toLowerCase()}</Badge><span className="ml-auto text-xs text-muted-foreground">{sections.length} alternative{sections.length ===1 ? '' : 's'}</span></div><div className="grid gap-4 xl:grid-cols-2">{sections.map((section) => <Card key={section.id} className="p-5"><div className="mb-4 flex justify-between"><h3 className="text-sm font-semibold">Section {section.code}</h3><Badge>{section.capacity} capacity</Badge></div><div className="space-y-3">{section.meetings.map((meeting) => <div key={meeting.id} className="rounded-xl bg-background p-3.5 text-sm"><p className="flex items-center gap-2 font-medium"><Clock size={14} className="text-primary" />{meeting.weekday.charAt(0) + meeting.weekday.slice(1).toLowerCase()} · {meeting.startTime.slice(0, 5)}–{meeting.endTime.slice(0, 5)}</p><div className="mt-2 flex flex-wrap gap-x-5 gap-y-2 text-xs text-muted-foreground"><span className="inline-flex items-center gap-1.5"><UserRound size={12} />{meeting.instructor?.displayName ?? 'Instructor to be assigned'}</span><span className="inline-flex items-center gap-1.5"><MapPin size={12} />{meeting.room ? `${meeting.room.building} · ${meeting.room.code}` : 'Room to be assigned'}</span></div></div>)}</div></Card>)}</div></section>)}
    <Card className="flex flex-wrap items-center justify-between gap-4 border-primary/15 bg-secondary/60 p-5"><div><p className="text-sm font-semibold">Ready to see how this fits your week?</p><p className="mt-1 text-xs text-muted-foreground">The builder lets you compare alternatives and spot conflicts.</p></div><Button asChild variant="outline"><Link to="/builder">Open schedule builder</Link></Button></Card>
  </>
}

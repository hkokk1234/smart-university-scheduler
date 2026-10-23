import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { BookOpen, Plus, Sparkles, Trash2, RefreshCw, X } from 'lucide-react'
import { useSelectedCourses, useSemesters } from "@/lib/catalogQueries"
import { api } from '@/lib/api'
import { usePlanner } from "./plannerStore"
import { buildCalendarSections, findMeetingConflicts, calculateScheduleStats } from "./scheduleCalculations"
import { WeeklyCalendar } from './WeeklyCalendar'
import { ConflictNotice } from './ConflictNotice'
import { SaveScheduleDialog } from '@/features/schedules/SaveScheduleDialog'
import { usePreferences } from "@/features/settings/preferencesStore"
import { PageHeading } from '@/components/shared/PageHeading'
import { SemesterSelect } from '@/components/shared/SemesterSelect'
import { LoadingState, EmptyState, ErrorState } from "@/components/shared/RequestStates"
import { Button } from '@/components/ui/button'
import { Badge, Card } from '@/components/ui/card'
import { mergeClassNames } from "@/lib/classNames"

export default function BuilderPage() {
  const ids =usePlanner((state) => state.courseIds)
  const chosen =usePlanner((state) => state.sectionIds)
  const semesterId =usePlanner((state) => state.semesterId)
  const queries =useSelectedCourses()
  const semesters =useSemesters()
  const [generating, setGenerating] =useState(false)
  const [error, setError] =useState('')
  const activeRequest =useRef<AbortController | null>(null)
  const navigate =useNavigate()
  useEffect(() => () => activeRequest.current?.abort(), [])
  const details =queries.flatMap((query) => query.data ? [query.data] : [])
  const entries =buildCalendarSections(details, chosen)
  const conflicts =findMeetingConflicts(entries)
  const stats =calculateScheduleStats(entries)
  const groupCount =details.reduce((count, course) => count + course.sectionGroups.length, 0)
  const incomplete =queries.some((query) => query.isPending || query.isError)
  const unpublished =details.some((course) => !course.sectionGroups.length || course.sectionGroups.some((group) => !group.sections.length))
  async function generate() {
    setError(''); setGenerating(true)
    activeRequest.current?.abort()
    const controller =new AbortController()
    activeRequest.current =controller
    try {
      const preferences =usePreferences.getState()
      const hasValidTimeWindow =preferences.earliestStart && preferences.latestEnd && preferences.earliestStart < preferences.latestEnd
      const result =await api.generate(semesterId, ids, {
        ...(hasValidTimeWindow ? { preferredStart: preferences.earliestStart, preferredEnd: preferences.latestEnd } : {}),
        preferredFreeDays: preferences.preferredFreeDays,
        maximumCampusDays: preferences.maximumCampusDays,
        preferredPeriod: preferences.preferredPeriod,
        weights: { gaps: preferences.gapImportance, campusDays: 1, preferredTimes: 1, freeDays: 1, balance: 0.2 },
      }, controller.signal)
      if (controller.signal.aborted) return
      usePlanner.getState().setResults({ status: result.status, complete: result.metrics.complete, explored: result.metrics.nodesVisited,
        executionMillis: result.metrics.executionMillis, schedules: result.schedules.map((schedule, index) => ({
          id: 'rank-' + index, optimizationScore: schedule.optimizationScore, scoreExplanation: schedule.scoreExplanation,
          sections: schedule.sections.map((section) => ({ section, courseCode: section.courseCode, courseTitle: section.courseTitle })),
        })) })
      navigate('/results')
    } catch (failure) {
      if (!controller.signal.aborted) setError(failure instanceof Error ? failure.message : 'The schedule search failed.')
    } finally { if (!controller.signal.aborted) setGenerating(false) }
  }
  return <>
    <PageHeading eyebrow="Design your week" title="Schedule builder" description="Start with your courses.Choose an alternative for each group,or let us find combinations without meeting conflicts." action={<SemesterSelect />} />
    <div className="flex flex-wrap items-center gap-3 text-xs"><Badge><span className="mr-1 rounded-full bg-primary px-1.5 text-primary-foreground">1</span>{ids.length} courses selected</Badge><Badge><span className="mr-1 rounded-full bg-primary px-1.5 text-primary-foreground">2</span>{entries.length} of {groupCount} groups chosen</Badge><Badge><span className="mr-1 rounded-full bg-primary px-1.5 text-primary-foreground">3</span>Compare and save</Badge></div>
    {!ids.length ? <EmptyState title="Your week starts with a course" description="Browse the catalog and add the courses you want to take.Your selections will be kept on this device while you plan." action={<Button asChild><Link to="/catalog"><Plus size={16} />Find your courses</Link></Button>} /> : <>
      <div className="flex flex-wrap justify-between gap-3"><div className="flex gap-2"><Button asChild variant="outline" size="sm"><Link to="/catalog"><Plus size={14} />Add courses</Link></Button><Button variant="ghost" size="sm" onClick={() => queries.forEach((query) => void query.refetch())} disabled={generating}><RefreshCw size={14} />Refresh</Button></div><Button variant="ghost" size="sm" onClick={() => usePlanner.getState().clearDraft()} disabled={generating}><Trash2 size={14} />Clear draft</Button></div>
      <div className="grid items-start gap-6 xl:grid-cols-[340px_minmax(0,1fr)]">
        <div className="space-y-4">{queries.map((query, index) => query.isPending ? <LoadingState key={ids[index]} label="Loading course…" /> : query.isError ? <Card key={ids[index]} className="space-y-3 p-4"><ErrorState error={query.error} retry={() => void query.refetch()} /><Button variant="outline" size="sm" onClick={() => usePlanner.getState().removeCourse(ids[index])}>Remove unavailable course</Button></Card> : query.data && <Card key={query.data.course.id} className="p-5"><div className="mb-4 flex justify-between gap-3"><div><p className="text-xs font-semibold text-primary">{query.data.course.code}</p><h2 className="mt-1 text-sm font-semibold">{query.data.course.title}</h2></div><Button variant="ghost" size="sm" aria-label={'Remove ' + query.data.course.code} disabled={generating} onClick={() => usePlanner.getState().removeCourse(query.data!.course.id, query.data!.sectionGroups.map(({ group }) => group.id))}><X size={15} /></Button></div>{!query.data.sectionGroups.length && <p className="text-xs text-muted-foreground">No sections are published for this course yet.</p>}<div className="space-y-5">{query.data.sectionGroups.map(({ group, sections }) => <fieldset key={group.id} disabled={generating}><legend className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">{group.name}</legend><div className="space-y-2">{sections.map((section) => <label key={section.id} className={mergeClassNames("flex cursor-pointer items-start gap-3 rounded-xl border p-3 text-xs transition-colors", chosen[group.id] ===section.id ? 'border-primary/40 bg-secondary/60' : 'hover:bg-muted')}><input type="radio" name={group.id} value={section.id} checked={chosen[group.id] ===section.id} onChange={() => usePlanner.getState().selectSection(group.id, section.id)} className="mt-0.5 accent-primary" /><span><span className="font-semibold">Section {section.code}</span>{section.meetings.map((meeting) => <span key={meeting.id} className="mt-1 block text-[11px] text-muted-foreground">{meeting.weekday.slice(0, 3)} {meeting.startTime.slice(0, 5)}–{meeting.endTime.slice(0, 5)}</span>)}</span></label>)}{chosen[group.id] && <button type="button" onClick={() => usePlanner.getState().selectSection(group.id, null)} className="px-1 text-[11px] text-muted-foreground underline underline-offset-2">Clear choice</button>}</div></fieldset>)}</div></Card>)}</div>
        <div className="min-w-0 space-y-5"><div className="grid grid-cols-3 gap-3">{[{ label: 'Meeting hours', value: stats.hours.toFixed(1) }, { label: 'Days on campus', value: stats.days }, { label: 'Conflicts', value: conflicts.length }].map((metric) => <Card key={metric.label} className="p-4"><p className="text-[11px] text-muted-foreground">{metric.label}</p><p className="mt-2 text-2xl font-semibold">{metric.value}</p></Card>)}</div><ConflictNotice entries={entries} /><WeeklyCalendar entries={entries} timeZone={semesters.data?.find((semester) => semester.id ===semesterId)?.timeZone} />
        <Card className="space-y-4 p-5"><div className="flex items-start gap-3"><span className="rounded-xl bg-secondary p-2.5 text-primary"><Sparkles size={19} /></span><div><h2 className="text-sm font-semibold">Explore what’s possible</h2><p className="mt-1 text-xs leading-relaxed text-muted-foreground">Find up to five ranked conflict free options.Each option includes one alternative from every group of your selected courses.</p></div></div>{unpublished && <p role="status" className="text-xs text-destructive">One or more courses have no published sections.Remove them or ask your administrator to publish alternatives.</p>}{error && <ErrorState error={new Error(error)} />}<div className="flex flex-wrap justify-end gap-3"><SaveScheduleDialog entries={entries} semesterId={semesterId} disabled={incomplete || generating || conflicts.length > 0 || !entries.length} /><Button onClick={generate} disabled={generating || incomplete || unpublished || !details.length}><Sparkles size={16} />{generating ? 'Comparing options…' : 'Generate options'}</Button></div><p className="text-[11px] text-muted-foreground">Your manual choices are a preview.Generation compares all alternatives.Saving a partial selection creates a draft.</p></Card></div>
      </div>
    </>}
    {!ids.length && <p className="flex items-center gap-2 text-xs text-muted-foreground"><BookOpen size={14} />A course may include separate lecture and laboratory groups.</p>}
  </>
}

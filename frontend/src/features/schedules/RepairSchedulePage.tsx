import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { RefreshCw } from 'lucide-react'
import { api } from '@/lib/api'
import type { RepairResult, SavedSchedule, Section } from '@/lib/types'
import { useSession } from '@/features/auth/sessionStore'
import { usePreferences } from '@/features/settings/preferencesStore'
import { useSemesters } from '@/lib/catalogQueries'
import { WeeklyCalendar } from '@/features/planner/WeeklyCalendar'
import { ConflictNotice } from '@/features/planner/ConflictNotice'
import { SaveScheduleDialog } from './SaveScheduleDialog'
import { PageHeading } from '@/components/shared/PageHeading'
import { ErrorState, LoadingState, EmptyState } from '@/components/shared/RequestStates'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { mergeClassNames } from '@/lib/classNames'

function entries(sections: Section[]) {
  return sections.map(section => ({ section, courseCode: section.courseCode, courseTitle: section.courseTitle }))
}
function meetingTimes(section: Section) {
  return section.meetings.map(meeting => `${meeting.weekday.slice(0, 3)} ${meeting.startTime.slice(0, 5)}–${meeting.endTime.slice(0, 5)}`).join(', ')
}

export default function RepairSchedulePage() {
  const { scheduleId } =useParams()
  const user =useSession(state => state.user)
  const query =useQuery({
    queryKey: ['private', user?.id, 'schedules', 'detail', scheduleId],
    queryFn: ({ signal }) => api.schedule(scheduleId!, signal), enabled: !!scheduleId && !!user,
  })
  if (query.isPending) return <LoadingState label="Loading schedule…" />
  if (query.isError) return <ErrorState error={query.error} retry={() => void query.refetch()} />
  return <RepairWorkspace key={query.data.id} original={query.data} />
}

function RepairWorkspace({ original }: { original: SavedSchedule }) {
  const [excluded, setExcluded] =useState<string[]>([])
  const [result, setResult] =useState<RepairResult | null>(null)
  const [index, setIndex] =useState(0)
  const [busy, setBusy] =useState(false)
  const [error, setError] =useState<Error | null>(null)
  const activeRequest =useRef<AbortController | null>(null)
  const semesters =useSemesters()
  useEffect(() => () => activeRequest.current?.abort(), [])
  const selected =result?.schedules[index]
  const originalEntries =entries(original.sections.map(entry => entry.section))
  const timeZone =semesters.data?.find(semester => semester.id ===original.semesterId)?.timeZone

  async function repair() {
    activeRequest.current?.abort()
    const controller =new AbortController()
    activeRequest.current =controller
    setBusy(true); setError(null); setResult(null); setIndex(0)
    const preferences =usePreferences.getState()
    const validWindow =preferences.earliestStart && preferences.latestEnd && preferences.earliestStart < preferences.latestEnd
    try {
      const response =await api.repairSchedule(original.id, excluded, {
        ...(validWindow ? { preferredStart: preferences.earliestStart, preferredEnd: preferences.latestEnd } : {}),
        preferredFreeDays: preferences.preferredFreeDays, maximumCampusDays: preferences.maximumCampusDays,
        preferredPeriod: preferences.preferredPeriod,
        weights: { gaps: preferences.gapImportance, campusDays: 1, preferredTimes: 1, freeDays: 1, balance: 0.2 },
      }, controller.signal)
      if (!controller.signal.aborted) setResult(response)
    } catch (failure) {
      if (!controller.signal.aborted) setError(failure instanceof Error ? failure : new Error('The repair search failed.'))
    } finally { if (!controller.signal.aborted) setBusy(false) }
  }
  function toggleExcluded(id: string) {
    setExcluded(current => current.includes(id) ? current.filter(value => value !==id) : [...current, id])
    setResult(null); setError(null); setIndex(0)
  }
  return <>
    <Link to="/saved" className="text-sm text-muted-foreground">← Saved schedules</Link>
    <PageHeading eyebrow="Keep your week familiar" title="Repair your schedule" description={`Find alternatives for “${original.name}” with the fewest section changes. Your current preferences break ties.`} />
    {!original.sections.length ? <EmptyState title="This draft is empty" description="Save a complete schedule before looking for repairs." /> : <>
      <Card className="space-y-4 p-5">
        <h2 className="font-semibold">Has a section been cancelled or become unavailable?</h2>
        <p className="text-sm text-muted-foreground">Select sections to exclude. Leave all unchecked to repair conflicts using the latest published meeting times. Complete schedules with one section per required group are supported.</p>
        <fieldset disabled={busy} className="grid gap-3 md:grid-cols-2">
          <legend className="sr-only">Sections to exclude from repair</legend>
          {original.sections.map(({ section }) => <label key={section.id} className="flex cursor-pointer items-start gap-3 rounded-xl border p-3 text-sm">
            <input type="checkbox" checked={excluded.includes(section.id)} onChange={() => toggleExcluded(section.id)} className="mt-1 accent-primary" />
            <span><span className="font-medium">{section.courseCode} · Section {section.code}</span><span className="mt-1 block text-xs text-muted-foreground">{meetingTimes(section)}</span></span>
          </label>)}
        </fieldset>
        {error && <ErrorState error={error} />}
        <div className="flex flex-wrap justify-end gap-3">
          {busy && <Button variant="outline" onClick={() => { activeRequest.current?.abort(); setBusy(false) }}>Cancel search</Button>}
          <Button disabled={busy} onClick={repair}><RefreshCw size={16} />{busy ? 'Finding repairs…' : 'Find repairs'}</Button>
        </div>
      </Card>
      {!result && <div className="space-y-4"><h2 className="font-semibold">Current schedule</h2><ConflictNotice entries={originalEntries} /><WeeklyCalendar entries={originalEntries} timeZone={timeZone} /></div>}
      {result && <div className="space-y-5" aria-live="polite">
        {!result.metrics.complete && <p role="status" className="rounded-xl border bg-secondary/50 p-4 text-sm">The search stopped before completion. Any options below are conflict free, but the minimum number of changes has not been proved. Try again or reduce the selection.</p>}
        {!selected ? <EmptyState title={result.status ==='UNSAT' ? 'No repair fits these restrictions' : 'No repair found before the search stopped'} description={result.status ==='UNSAT' ? 'No valid combination exists with these exclusions. Allow more sections or change your course selection.' : 'This does not prove a repair is impossible. Try searching again.'} /> : <>
          <div className="flex flex-wrap gap-3">{result.schedules.map((option, optionIndex) => <button type="button" key={optionIndex} aria-pressed={index ===optionIndex} onClick={() => setIndex(optionIndex)} className={mergeClassNames('rounded-xl border bg-card px-5 py-3 text-left text-sm', index ===optionIndex && 'border-primary ring-1 ring-primary')}>
            <span className="block font-semibold">Option {optionIndex + 1}</span><span>{option.changedSectionCount} section {option.changedSectionCount ===1 ? 'change' : 'changes'}</span>
          </button>)}</div>
          <Card className="space-y-4 p-5">
            <h2 className="font-semibold">{selected.changedSectionCount ===0 ? 'Your current section choices still work' : `${selected.changedSectionCount} section ${selected.changedSectionCount ===1 ? 'change' : 'changes'}`}</h2>
            {index ===0 && result.metrics.complete && <p className="text-sm text-primary">The complete search confirms this option needs the fewest section changes.</p>}
            {selected.changes.length > 0 && <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-2">Course</th><th className="p-2">From</th><th className="p-2">To</th></tr></thead><tbody>{selected.changes.map(change => <tr key={change.groupId} className="border-b"><th className="p-2 font-medium">{change.after.courseCode}</th><td className="p-2">Section {change.before.code}<span className="block text-xs text-muted-foreground">{meetingTimes(change.before)}</span></td><td className="p-2">Section {change.after.code}<span className="block text-xs text-muted-foreground">{meetingTimes(change.after)}</span></td></tr>)}</tbody></table></div>}
            <p className="text-xs text-muted-foreground">Changes count replaced sections, not individual meetings. Times shown are the latest published times. Save your preferred repair as a new schedule.</p>
            <div className="flex justify-end"><SaveScheduleDialog key={index} entries={entries(selected.schedule.sections)} semesterId={original.semesterId} /></div>
          </Card>
          <WeeklyCalendar entries={entries(selected.schedule.sections)} timeZone={timeZone} />
        </>}
      </div>}
    </>}
  </>
}

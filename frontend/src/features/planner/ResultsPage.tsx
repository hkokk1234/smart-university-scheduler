import { useState } from 'react'
import { Link } from 'react-router'
import { usePlanner } from "./plannerStore"
import { calculateScheduleStats } from "./scheduleCalculations"
import { WeeklyCalendar } from './WeeklyCalendar'
import { useSemesters } from "@/lib/catalogQueries"
import { SaveScheduleDialog } from '@/features/schedules/SaveScheduleDialog'
import { PageHeading } from '@/components/shared/PageHeading'
import { EmptyState } from "@/components/shared/RequestStates"
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { mergeClassNames } from "@/lib/classNames"

export default function ResultsPage() {
  const result =usePlanner((state) => state.results)
  const semesterId =usePlanner((state) => state.semesterId)
  const semesters =useSemesters()
  const [index, setIndex] =useState(0)
  if (!result) return <EmptyState title="Let’s find your options" description="Add courses and generate schedules from the live catalog.Results are kept for this session." action={<Button asChild><Link to="/builder">Open builder</Link></Button>} />
  const selected =result.schedules[Math.min(index, result.schedules.length - 1)]
  return <>
    <Link to="/builder" className="text-sm text-muted-foreground">← Back to builder</Link>
    <PageHeading eyebrow="Make it your own" title="Your schedule options" description={`${result.schedules.length} valid options, ranked by normalized weighted preference penalties. Lower scores are better.`} action={selected && <SaveScheduleDialog entries={selected.sections} semesterId={semesterId} />} />
    <p className="text-xs text-muted-foreground">{result.status} · {result.explored} search nodes · {result.executionMillis} ms</p>
    {!result.complete && <p role="status" className="rounded-xl border bg-secondary/50 p-4 text-sm">The search timed out or was cancelled.Returned schedules are valid; their optimality is not yet proved.</p>}
    {!selected ? <EmptyState title={result.status ==='UNSAT' ? 'These courses cannot fit together' : 'No option found before the search stopped'} description={result.status ==='UNSAT' ? "The complete search found no assignment satisfying every required group and restriction.Adjust your courses or preferences." : "This is an incomplete search,rather than proof that no schedule exists.Try a smaller selection."} action={<Button asChild variant="outline"><Link to="/builder">Adjust selection</Link></Button>} /> :
      <div className="grid items-start gap-6 xl:grid-cols-[280px_minmax(0,1fr)]">
        <div className="space-y-3">{result.schedules.map((schedule, option) => {
          const stats =calculateScheduleStats(schedule.sections)
          return <button key={schedule.id} onClick={() => setIndex(option)} aria-pressed={option ===index} className={mergeClassNames("w-full rounded-2xl border bg-card p-5 text-left", option ===index && "border-primary ring-1 ring-primary")}>
            <h2 className="text-sm font-semibold">Option {option + 1}</h2>
            <p className="mt-2 text-sm">Score: {schedule.optimizationScore?.toFixed(2)}</p>
            <p className="mt-2 text-xs text-muted-foreground">{stats.days} campus days · {stats.gaps.toFixed(1)}h breaks</p>
          </button>
        })}</div>
        <div className="min-w-0 space-y-5">
          <WeeklyCalendar entries={selected.sections} timeZone={semesters.data?.find((semester) => semester.id ===semesterId)?.timeZone} />
          <Card className="p-5"><h2 className="mb-4 text-sm font-semibold">Score explanation</h2><ul className="space-y-2 text-xs">{selected.scoreExplanation?.map((part) => <li key={part.criterion}>{part.explanation}: {(part.penalty * 100).toFixed(1)}% × {part.weight} ={part.contribution.toFixed(3)}</li>)}</ul></Card>
          <Card className="p-5"><h2 className="mb-4 text-sm font-semibold">Included sections</h2><ul className="space-y-3 text-xs">{selected.sections.map((entry) => <li key={entry.section.id}><strong>{entry.courseCode}</strong> — {entry.courseTitle} · Section {entry.section.code}</li>)}</ul></Card>
        </div>
      </div>}
  </>
}

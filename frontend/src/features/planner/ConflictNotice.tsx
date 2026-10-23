import { AlertTriangle, CircleCheck } from 'lucide-react'
import { findMeetingConflicts } from "./scheduleCalculations"
import type { CalendarSection } from '@/lib/types'
export function ConflictNotice({ entries }: { entries: CalendarSection[] }) {
  const conflicts =findMeetingConflicts(entries)
  if (!entries.length) return null
  if (!conflicts.length) return <div role="status" className="flex items-center gap-3 rounded-xl border border-primary/15 bg-secondary/60 px-4 py-3 text-sm text-primary"><CircleCheck size={18} />No meeting conflicts in this selection.</div>
  return <div role="alert" className="rounded-xl border border-destructive/20 bg-destructive/5 p-4"><p className="flex items-center gap-2 text-sm font-semibold text-destructive"><AlertTriangle size={18} />{conflicts.length} timetable conflict{conflicts.length ===1 ? '' : 's'}</p><ul className="mt-3 space-y-2 text-xs text-muted-foreground">{conflicts.slice(0, 8).map((conflict) => <li key={conflict.firstMeetingId + conflict.secondMeetingId}><strong>{conflict.first.courseCode} {conflict.first.section.code}</strong> overlaps <strong>{conflict.second.courseCode} {conflict.second.section.code}</strong> · {conflict.weekday.toLowerCase()} {conflict.startTime.slice(0, 5)}–{conflict.endTime.slice(0, 5)}</li>)}</ul>{conflicts.length > 8 && <p className="mt-2 text-xs">And {conflicts.length - 8} more overlaps.</p>}</div>
}

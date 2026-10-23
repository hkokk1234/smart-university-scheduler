import type { SectionChange, SectionSnapshot } from '@/lib/types'

function Snapshot({ value }: { value: SectionSnapshot }) {
  return <div className="space-y-2 text-xs"><p>Section {value.code} · Capacity {value.capacity}</p><ul className="space-y-2">{value.meetings.map((meeting, index) => <li key={index} className="rounded-lg border p-3"><span className="font-medium">{meeting.weekday.slice(0, 3)} {meeting.startTime.slice(0, 5)}–{meeting.endTime.slice(0, 5)}</span><span className="mt-1 block text-muted-foreground">{meeting.room ?? 'Room unassigned'} · {meeting.instructor ?? 'Instructor unassigned'}</span></li>)}</ul></div>
}
export function ChangeDetails({ change }: { change: SectionChange }) {
  return <div className="space-y-4"><p className="text-xs text-muted-foreground">Changed by {change.actorName} · {new Date(change.createdAt).toLocaleString()}</p><div className="grid gap-4 sm:grid-cols-2"><div><h3 className="mb-3 text-sm font-semibold">Before</h3><Snapshot value={change.before} /></div><div><h3 className="mb-3 text-sm font-semibold">After</h3><Snapshot value={change.after} /></div></div></div>
}

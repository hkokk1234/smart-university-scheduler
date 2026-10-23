import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { TwinChange, TwinComparison, Weekday } from '@/lib/types'
import { usePlanner } from "@/features/planner/plannerStore"
import { useSession } from "@/features/auth/sessionStore"
import { PageHeading } from '@/components/shared/PageHeading'
import { SemesterSelect } from '@/components/shared/SemesterSelect'
import { LoadingState, ErrorState, EmptyState } from "@/components/shared/RequestStates"
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input, Select } from '@/components/ui/input'

const days: Weekday[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
export default function DigitalTwinPage() {
  const semester = usePlanner(s => s.semesterId)
  const user = useSession(s => s.user)
  const query = useQuery({ queryKey: ['private', user?.id, 'digital-twin', semester], enabled: !!semester,
    queryFn: ({ signal }) => api.simulateCampus(semester, [], signal) })
  return <div className="space-y-6"><PageHeading eyebrow="Campus planning" title="Campus Digital Twin" description="Test room and timetable changes before putting them into practice." action={<SemesterSelect />} />
    <Card className="p-5 text-sm text-muted-foreground">Read only simulation of all offered sections,including alternatives.Estimated attendance defaults to section capacity,not actual enrolment.Room utilization uses the complete 168 hour week; daily peaks include meetings without an assigned room.</Card>
    {!semester ? <EmptyState title="Choose a semester" description="Select the catalog to simulate." /> : query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : <Scenario key={semester + ':' + query.dataUpdatedAt} initial={query.data} />}
  </div>
}
function Scenario({ initial }: { initial: TwinComparison }) {
  const [changes, setChanges] = useState<TwinChange[]>([])
  const [result, setResult] = useState(initial)
  const [meetingId, setMeetingId] = useState(initial.meetings[0]?.id ?? '')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const meeting = initial.meetings.find(m => m.id === meetingId)
  async function simulate(next: TwinChange[]) {
    setPending(true); setError(null)
    try { const data = await api.simulateCampus(initial.semesterId, next); setResult(data); setChanges(next) }
    catch (e) { setError(e instanceof Error ? e : new Error('Simulation failed.')) }
    finally { setPending(false) }
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const change: TwinChange = { meetingId, weekday: form.get('weekday') as Weekday, startTime: String(form.get('start')), endTime: String(form.get('end')), roomId: String(form.get('room')) || null, attendance: Number(form.get('attendance')) }
    void simulate([...changes.filter(c => c.meetingId !== meetingId), change])
  }
  const report = result.scenario
  return <div className="space-y-6">
    <div className="grid gap-4 sm:grid-cols-3">{[
      ['Occupied room hours', initial.baseline.occupiedRoomHours.toFixed(1), report.occupiedRoomHours.toFixed(1)],
      ['Planning alerts', initial.baseline.alerts.length, report.alerts.length],
      ['Unassigned meetings', initial.baseline.unassignedMeetings, report.unassignedMeetings],
    ].map(([label, before, after]) => <Card key={label} className="p-5"><p className="text-sm text-muted-foreground">{label}</p><p className="mt-2 text-2xl font-semibold">{after}</p><p className="text-xs text-muted-foreground">Baseline: {before}</p></Card>)}</div>
    <Card className="p-6"><h2 className="mb-4 text-lg font-semibold">What if scenario</h2>
      {!meeting ? <EmptyState title="No meetings published" description="Publish sections before simulating campus usage." /> : <>
        <label className="block text-sm">Meeting<Select value={meetingId} disabled={pending} onChange={e => setMeetingId(e.target.value)}>{initial.meetings.map(m => <option key={m.id} value={m.id}>{m.label} · {m.weekday} {m.startTime.slice(0,5)}</option>)}</Select></label>
        <form key={meetingId} onSubmit={submit} className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <label className="text-sm">Day<Select name="weekday" defaultValue={meeting.weekday}>{days.map(day => <option key={day}>{day}</option>)}</Select></label>
          <label className="text-sm">Start<Input name="start" type="time" step="1" required defaultValue={meeting.startTime} /></label>
          <label className="text-sm">End<Input name="end" type="time" step="1" required defaultValue={meeting.endTime} /></label>
          <label className="text-sm">Room<Select name="room" defaultValue={meeting.roomId ?? ''}><option value="">Unassigned</option>{initial.rooms.map(room => <option key={room.id} value={room.id}>{room.building} {room.code} ({room.capacity})</option>)}</Select></label>
          <label className="text-sm">Estimated attendees<Input name="attendance" type="number" min="0" max="10000" required defaultValue={meeting.attendance} /></label>
          <div className="flex flex-wrap gap-2 sm:col-span-2 lg:col-span-5"><Button disabled={pending}>{pending ? 'Simulating…' : 'Simulate change'}</Button><Button type="button" variant="outline" disabled={pending || !changes.length} onClick={() => void simulate([])}>Reset scenario</Button><span className="self-center text-sm text-muted-foreground">{changes.length} changes · nothing is saved</span></div>
        </form>
      </>}
      {changes.length > 0 && <ul className="mt-4 space-y-2 text-sm">{changes.map(c => <li key={c.meetingId} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted p-3"><span>{initial.meetings.find(m => m.id === c.meetingId)?.label} · {c.weekday} {c.startTime.slice(0,5)}–{c.endTime.slice(0,5)} · {initial.rooms.find(r => r.id === c.roomId)?.code ?? 'Unassigned'} · {c.attendance} attendees</span><Button variant="ghost" disabled={pending} onClick={() => void simulate(changes.filter(other => other.meetingId !== c.meetingId))}>Remove</Button></li>)}</ul>}
      {error && <div className="mt-4" role="alert"><ErrorState error={error} /></div>}
    </Card>
    <Card className="p-6"><h2 className="mb-4 text-lg font-semibold">Daily peak estimated attendance</h2><div className="space-y-3">{report.peaks.map((peak, i) => <div key={peak.weekday}><div className="mb-1 flex justify-between gap-2 text-sm"><span>{peak.weekday}</span><span>{initial.baseline.peaks[i].estimatedAttendance} → {peak.estimatedAttendance} attendees{peak.peakTime ? ` at ${peak.peakTime.slice(0,5)}` : ''} · {peak.simultaneousMeetings} concurrent meetings</span></div><div className="h-2 rounded bg-muted"><div className="h-2 rounded bg-primary" style={{ width: `${peak.estimatedAttendance / Math.max(1, ...report.peaks.map(p => p.estimatedAttendance)) * 100}%` }} /></div></div>)}</div></Card>
    <Card className="overflow-x-auto p-6"><h2 className="mb-4 text-lg font-semibold">Room usage</h2><table className="w-full text-left text-sm"><thead><tr><th className="p-2">Room / capacity</th><th className="p-2">Baseline hours</th><th className="p-2">Scenario hours</th><th className="p-2">Weekly utilization</th></tr></thead><tbody>{report.rooms.map((usage, i) => <tr key={usage.room.id} className="border-t"><td className="p-2">{usage.room.building} {usage.room.code} / {usage.room.capacity}</td><td className="p-2">{initial.baseline.rooms[i].occupiedHours.toFixed(1)}</td><td className="p-2">{usage.occupiedHours.toFixed(1)}</td><td className="p-2">{usage.utilizationPercent.toFixed(1)}%</td></tr>)}</tbody></table></Card>
    <Card className="p-6"><h2 className="mb-4 text-lg font-semibold">Scenario alerts</h2>{report.alerts.length ? <ul className="space-y-2">{report.alerts.map((alert, i) => <li key={i} className="rounded-lg border border-destructive/30 p-3 text-sm">{alert.message}</li>)}</ul> : <p className="text-sm text-muted-foreground">No room conflicts or capacity violations detected.</p>}</Card>
  </div>
}

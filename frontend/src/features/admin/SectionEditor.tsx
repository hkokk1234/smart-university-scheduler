import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { api, ApiError } from '@/lib/api'
import type { Instructor, MeetingInput, Room, Section, Weekday } from '@/lib/types'
import { weekdays, timeToSeconds } from "@/features/planner/scheduleCalculations"
import { Button } from '@/components/ui/button'
import { Input, Select } from '@/components/ui/input'
import { ErrorState } from "@/components/shared/RequestStates"
import { usePlanner } from "@/features/planner/plannerStore"

export function SectionEditor({ section, rooms, instructors, close, saved, reload }: { section: Section; rooms: Room[]; instructors: Instructor[]; close: () => void; saved: () => void; reload: () => void }) {
  const [meetings, setMeetings] =useState<MeetingInput[]>(section.meetings.map((meeting) => ({ weekday: meeting.weekday, startTime: meeting.startTime.slice(0, 8), endTime: meeting.endTime.slice(0, 8), roomId: meeting.room?.id ?? null, instructorId: meeting.instructor?.id ?? null })))
  const [validation, setValidation] =useState('')
  const client =useQueryClient()
  const mutation =useMutation({ mutationFn: (body: { code: string; capacity: number }) => api.updateSection(section.id, { ...body, version: section.version, meetings }),
    onSuccess: () => { void client.invalidateQueries({ queryKey: ['courses'] }); void client.invalidateQueries({ queryKey: ['course'] }); void client.invalidateQueries({ queryKey: ['private'] }); usePlanner.getState().setResults(null); saved() } })
  function update(index: number, patch: Partial<MeetingInput>) { setMeetings((rows) => rows.map((row, position) => position ===index ? { ...row, ...patch } : row)) }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setValidation('')
    if (!meetings.length || meetings.some((meeting) => !meeting.startTime || !meeting.endTime || timeToSeconds(meeting.startTime) >=timeToSeconds(meeting.endTime))) { setValidation('Each meeting needs a start time before its end time.'); return }
    const form =new FormData(event.currentTarget)
    mutation.mutate({ code: String(form.get('code')).trim(), capacity: Number(form.get('capacity')) })
  }
  return <form onSubmit={submit} className="space-y-6"><div className="grid gap-4 sm:grid-cols-2"><div><label htmlFor="section-code" className="mb-2 block text-sm font-medium">Section code</label><Input id="section-code" name="code" defaultValue={section.code} required maxLength={32} /></div><div><label htmlFor="section-capacity" className="mb-2 block text-sm font-medium">Capacity</label><Input id="section-capacity" name="capacity" type="number" min={1} max={2147483647} defaultValue={section.capacity} required /></div></div>
    <div className="flex items-center justify-between"><h3 className="text-sm font-semibold">Weekly meetings</h3><Button type="button" variant="outline" size="sm" disabled={meetings.length >=50 || mutation.isPending} onClick={() => setMeetings([...meetings, { weekday: 'MONDAY', startTime: '', endTime: '', roomId: null, instructorId: null }])}><Plus size={14} />Add meeting</Button></div>
    <div className="space-y-4">{meetings.map((meeting, index) => <fieldset key={index} disabled={mutation.isPending} className="rounded-xl border bg-background p-4"><legend className="px-1 text-xs font-medium">Meeting {index + 1}</legend><div className="grid gap-3 sm:grid-cols-3"><label className="text-xs">Weekday<Select value={meeting.weekday} className="mt-2" onChange={(event) => update(index, { weekday: event.target.value as Weekday })}>{weekdays.map((day) => <option key={day} value={day}>{day.toLowerCase()}</option>)}</Select></label><label className="text-xs">Start time<Input type="time" step="1" className="mt-2" required value={meeting.startTime} onChange={(event) => update(index, { startTime: event.target.value })} /></label><label className="text-xs">End time<Input type="time" step="1" className="mt-2" required value={meeting.endTime} onChange={(event) => update(index, { endTime: event.target.value })} /></label></div><div className="mt-3 grid gap-3 sm:grid-cols-2"><label className="text-xs">Instructor<Select className="mt-2" value={meeting.instructorId ?? ''} onChange={(event) => update(index, { instructorId: event.target.value || null })}><option value="">Not assigned</option>{instructors.map((instructor) => <option key={instructor.id} value={instructor.id}>{instructor.displayName}</option>)}</Select></label><label className="text-xs">Room<Select className="mt-2" value={meeting.roomId ?? ''} onChange={(event) => update(index, { roomId: event.target.value || null })}><option value="">Not assigned</option>{rooms.map((room) => <option key={room.id} value={room.id}>{room.building} Β· {room.code}</option>)}</Select></label></div><Button type="button" variant="ghost" size="sm" className="mt-3 text-destructive" disabled={meetings.length <=1} onClick={() => setMeetings(meetings.filter((_, position) => position !==index))}><Trash2 size={13} />Remove meeting</Button></fieldset>)}</div>
    {validation && <p role="alert" className="text-sm text-destructive">{validation}</p>}{mutation.isError && <ErrorState error={mutation.error} />}{mutation.error instanceof ApiError && mutation.error.status ===409 && <Button type="button" variant="outline" onClick={reload}>Reload the current section</Button>}
    <div className="flex justify-end gap-3 border-t pt-5"><Button type="button" variant="outline" disabled={mutation.isPending} onClick={close}>Cancel</Button><Button type="submit" disabled={mutation.isPending || !meetings.length}>{mutation.isPending ? 'Saving changesβ€¦' : 'Save section changes'}</Button></div><p className="text-xs text-muted-foreground">This replaces the full meeting list.Existing saved schedules will use the published changes.</p>
  </form>
}


import { useRef, useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import { CalendarDays, Clock, MapPin, UserRound } from 'lucide-react'
import type { EventInput } from '@fullcalendar/core'
import type { CalendarSection, Meeting } from '@/lib/types'
import { usePreferences } from "@/features/settings/preferencesStore"
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Select } from '@/components/ui/input'
import { Dialog } from '@/components/ui/dialog'
import { mergeClassNames } from "@/lib/classNames"
import { findMeetingConflicts, weekdays } from "./scheduleCalculations"

const palette =[
  { background: '#e5f2eb', foreground: '#205b42' }, { background: '#e9edfb', foreground: '#3d4d89' },
  { background: '#fcf0d6', foreground: '#7b561a' }, { background: '#f3e8f8', foreground: '#744286' },
  { background: '#e1f2f5', foreground: '#216578' }, { background: '#fce8e0', foreground: '#8d4a31' },
]
function weekDate(day =0) { const date =new Date(); date.setDate(date.getDate() - (date.getDay() + 6) % 7 + day); return date }
export function WeeklyCalendar({ entries, timeZone }: { entries: CalendarSection[]; timeZone?: string }) {
  const calendar =useRef<FullCalendar>(null)
  const [view, setView] =useState<'timeGridWeek' | 'timeGridDay'>('timeGridWeek')
  const [day, setDay] =useState(0)
  const [detail, setDetail] =useState<{ entry: CalendarSection; meeting: Meeting } | null>(null)
  const weekends =usePreferences((state) => state.weekends)
  const compact =usePreferences((state) => state.compactCalendar)
  const conflictIds =new Set(findMeetingConflicts(entries).flatMap((conflict) => [conflict.firstMeetingId, conflict.secondMeetingId]))
  const courses =[...new Set(entries.map((entry) => entry.courseCode))]
  const startHours =entries.flatMap((entry) => entry.section.meetings.map((meeting) => Number(meeting.startTime.split(':')[0])))
  const minimumHour =Math.min(6, ...startHours)
  const scrollHour =Math.min(8, ...startHours)
  const hasWeekend =entries.some((entry) => entry.section.meetings.some((meeting) => meeting.weekday ==='SATURDAY' || meeting.weekday ==='SUNDAY'))
  const events: EventInput[] =entries.flatMap((entry) => entry.section.meetings.map((meeting) => {
    const color =palette[courses.indexOf(entry.courseCode) % palette.length]
    const conflict =conflictIds.has(meeting.id)
    return { id: entry.section.id + meeting.id, title: entry.courseCode + ' · ' + entry.section.code,
      daysOfWeek: [(weekdays.indexOf(meeting.weekday) + 1) % 7], startTime: meeting.startTime, endTime: meeting.endTime,
      backgroundColor: conflict ? '#fee2e2' : color.background, textColor: conflict ? '#991b1b' : color.foreground,
      borderColor: conflict ? '#ef4444' : color.background, extendedProps: { entry, meeting } }
  }))
  function switchView(next: typeof view) { setView(next); calendar.current?.getApi().changeView(next, weekDate(day)) }
  return <Card className="overflow-hidden">
    <div className="flex flex-wrap items-center justify-between gap-3 border-b p-5"><div><h2 className="flex items-center gap-2 text-sm font-semibold"><CalendarDays size={17} className="text-primary" />Weekly timetable</h2><p className="mt-1 text-xs text-muted-foreground">Recurring meetings{timeZone ? ` · ${timeZone}` : ''} · Select a meeting for details</p></div><div className="flex gap-2"><Button variant={view ==='timeGridWeek' ? 'secondary' : 'ghost'} size="sm" aria-pressed={view ==='timeGridWeek'} onClick={() => switchView('timeGridWeek')}>Week</Button><Button variant={view ==='timeGridDay' ? 'secondary' : 'ghost'} size="sm" aria-pressed={view ==='timeGridDay'} onClick={() => switchView('timeGridDay')}>Day</Button>{view ==='timeGridDay' && <Select aria-label="Calendar day" className="h-9 w-32" value={day} onChange={(event) => { const value =Number(event.target.value); setDay(value); calendar.current?.getApi().gotoDate(weekDate(value)) }}>{weekdays.map((weekday, index) => <option key={weekday} value={index}>{weekday.charAt(0) + weekday.slice(1).toLowerCase()}</option>)}</Select>}</div></div>
    <div className="overflow-x-auto p-3 sm:p-5"><div className={mergeClassNames('calendar-shell', compact && 'compact', view ==='timeGridWeek' && 'min-w-[620px]')}>
      <FullCalendar ref={calendar} plugins={[timeGridPlugin, interactionPlugin]} initialView="timeGridWeek" initialDate={weekDate()}
        headerToolbar={false} firstDay={1} weekends={weekends || hasWeekend} allDaySlot={false} height={compact ? 510 : 620}
        slotMinTime={String(minimumHour).padStart(2, '0') + ':00:00'} slotMaxTime="24:00:00" scrollTime={String(scrollHour).padStart(2, '0') + ':00:00'} slotDuration="00:30:00"
        dayHeaderFormat={{ weekday: 'short' }} slotLabelFormat={{ hour: 'numeric', minute: '2-digit', hour12: false }}
        events={events} editable={false} eventOverlap={false} slotEventOverlap={false} nowIndicator={false}
        eventContent={(argument) => {
          const entry =argument.event.extendedProps.entry as CalendarSection
          const meeting =argument.event.extendedProps.meeting as Meeting
          return <button type="button" className="h-full w-full text-left" onClick={() => setDetail({ entry, meeting })} aria-label={`${entry.courseCode} section ${entry.section.code}, ${meeting.weekday}, ${meeting.startTime.slice(0, 5)} to ${meeting.endTime.slice(0, 5)}`}><span className="block text-[10px] opacity-80">{meeting.startTime.slice(0, 5)}–{meeting.endTime.slice(0, 5)}</span><span className="mt-0.5 block text-xs font-semibold">{entry.courseCode} · {entry.section.code}</span><span className="mt-0.5 block truncate text-[10px]">{meeting.room?.code ?? 'Room TBA'}</span>{conflictIds.has(meeting.id) && <span className="block text-[9px] font-bold">Conflict</span>}</button>
        }} />
    </div></div>
    <div className="flex flex-wrap gap-x-5 gap-y-2 border-t px-5 py-4">{courses.length ? courses.map((code, index) => <span key={code} className="inline-flex items-center gap-2 text-xs text-muted-foreground"><span className="size-2.5 rounded-sm" style={{ background: palette[index % palette.length].foreground }} />{code}</span>) : <p className="text-xs text-muted-foreground">Choose section alternatives to see your week take shape.</p>}{hasWeekend && !weekends && <p className="w-full text-xs text-muted-foreground">Weekend meetings are shown automatically so no required meeting is hidden.</p>}</div>
    <Dialog open={!!detail} onOpenChange={(open) => { if (!open) setDetail(null) }} title={detail ? detail.entry.courseCode + ' · Section ' + detail.entry.section.code : 'Meeting'} description={detail?.entry.courseTitle}>
      {detail && <div className="space-y-4 text-sm"><p className="flex items-center gap-3"><Clock className="text-primary" size={18} />{detail.meeting.weekday.toLowerCase()} · {detail.meeting.startTime.slice(0, 5)}–{detail.meeting.endTime.slice(0, 5)}</p><p className="flex items-center gap-3"><UserRound className="text-primary" size={18} />{detail.meeting.instructor?.displayName ?? 'Instructor to be assigned'}</p><p className="flex items-center gap-3"><MapPin className="text-primary" size={18} />{detail.meeting.room ? `${detail.meeting.room.building} · ${detail.meeting.room.code}` : 'Room to be assigned'}</p><p className="border-t pt-4 text-xs text-muted-foreground">This is a recurring weekly meeting.Choosing this section includes all of its meetings.</p></div>}
    </Dialog>
  </Card>
}

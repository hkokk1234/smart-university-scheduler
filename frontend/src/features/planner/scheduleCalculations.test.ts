import { describe, expect, it } from 'vitest'
import { findMeetingConflicts, meetingsOverlap, calculateScheduleStats, timeToSeconds } from "./scheduleCalculations"
import type { CalendarSection, Meeting, Weekday } from '@/lib/types'

let sequence =0
function meeting(weekday: Weekday, startTime: string, endTime: string): Meeting {
  return { id: 'meeting-' + ++sequence, sectionId: 'test', weekday, startTime, endTime, instructor: null, room: null }
}
function entry(meetings: Meeting[]): CalendarSection {
  return { courseCode: 'TEST', courseTitle: 'Test', section: {
    id: 'section-' + ++sequence, sectionGroupId: 'group', semesterId: 'semester', courseId: 'course',
    courseCode: 'TEST', courseTitle: 'Test', code: 'A', capacity: 20, version: 0, meetings,
  } }
}
describe('weekly calendar calculations', () => {
  it('allows touching intervals', () => {
    expect(meetingsOverlap(meeting('MONDAY', '09:00', '10:00'), meeting('MONDAY', '10:00', '11:00'))).toBe(false)
  })
  it('keeps weekdays distinct', () => {
    expect(meetingsOverlap(meeting('MONDAY', '09:00', '10:00'), meeting('TUESDAY', '09:00', '10:00'))).toBe(false)
  })
  it('checks every meeting and reports the overlap interval', () => {
    const conflicts =findMeetingConflicts([entry([meeting('MONDAY', '09:00', '10:00'), meeting('WEDNESDAY', '09:00', '11:00')]), entry([meeting('WEDNESDAY', '10:00', '12:00')])])
    expect(conflicts).toHaveLength(1)
    expect(conflicts[0].startTime).toBe('10:00')
    expect(conflicts[0].endTime).toBe('11:00')
  })
  it('detects conflicts within the same section', () => {
    expect(findMeetingConflicts([entry([meeting('MONDAY', '09:00', '11:00'), meeting('MONDAY', '10:00', '12:00')])])).toHaveLength(1)
  })
  it('includes Sunday in weekly totals', () => {
    expect(calculateScheduleStats([entry([meeting('SUNDAY', '09:00', '11:00')])])).toEqual({ days: 1, hours: 2, gaps: 0 })
  })
  it('measures gaps without counting overlap as negative time', () => {
    expect(calculateScheduleStats([entry([meeting('MONDAY', '09:00', '11:00'), meeting('MONDAY', '10:00', '12:00'), meeting('MONDAY', '13:00', '14:00')])]).gaps).toBe(1)
  })
  it('preserves seconds and fractional seconds', () => {
    expect(timeToSeconds('09:30:15.5')).toBe(34215.5)
  })
})

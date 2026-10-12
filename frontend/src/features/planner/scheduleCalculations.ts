import type { CalendarSection, CourseDetail, Meeting, Weekday } from '@/lib/types'

export const weekdays: Weekday[] =['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
// Convert clock values to seconds while preserving fractional seconds.
export function timeToSeconds(time: string) {
  const [hours, minutes, remainingSeconds ='0'] =time.split(':')
  return Number(hours) * 3600 + Number(minutes) * 60 + Number(remainingSeconds)
}
// Touching boundaries are allowed because the first meeting has already ended.
export function meetingsOverlap(first: Meeting, second: Meeting) {
  return first.weekday ===second.weekday && timeToSeconds(first.startTime) < timeToSeconds(second.endTime) && timeToSeconds(second.startTime) < timeToSeconds(first.endTime)
}
export interface MeetingConflict { first: CalendarSection; second: CalendarSection; firstMeetingId: string; secondMeetingId: string; weekday: Weekday; startTime: string; endTime: string }
export function findMeetingConflicts(entries: CalendarSection[]): MeetingConflict[] {
  const scheduledMeetings =entries.flatMap((entry) => entry.section.meetings.map((meeting) => ({ entry, meeting })))
  const conflicts: MeetingConflict[] =[]
  // Compare each pair once including meetings in the same section.
  for (let firstIndex =0; firstIndex < scheduledMeetings.length; firstIndex++) for (let secondIndex =firstIndex + 1; secondIndex < scheduledMeetings.length; secondIndex++) {
    const firstMeeting =scheduledMeetings[firstIndex], secondMeeting =scheduledMeetings[secondIndex]
    if (meetingsOverlap(firstMeeting.meeting, secondMeeting.meeting)) conflicts.push({ first: firstMeeting.entry, second: secondMeeting.entry, firstMeetingId: firstMeeting.meeting.id, secondMeetingId: secondMeeting.meeting.id, weekday: firstMeeting.meeting.weekday,
      startTime: timeToSeconds(firstMeeting.meeting.startTime) >=timeToSeconds(secondMeeting.meeting.startTime) ? firstMeeting.meeting.startTime : secondMeeting.meeting.startTime,
      endTime: timeToSeconds(firstMeeting.meeting.endTime) <=timeToSeconds(secondMeeting.meeting.endTime) ? firstMeeting.meeting.endTime : secondMeeting.meeting.endTime })
  }
  return conflicts
}
export function calculateScheduleStats(entries: CalendarSection[]) {
  const meetings =entries.flatMap((entry) => entry.section.meetings)
  let gaps =0
  for (const weekday of weekdays) {
    const dailyMeetings =meetings.filter((meeting) => meeting.weekday ===weekday).sort((firstMeeting, secondMeeting) => timeToSeconds(firstMeeting.startTime) - timeToSeconds(secondMeeting.startTime))
    // Use the latest end time so overlapping meetings cannot create negative gaps.
    let latestEndTime =0
    for (const meeting of dailyMeetings) {
      if (latestEndTime) gaps +=Math.max(0, timeToSeconds(meeting.startTime) - latestEndTime)
      latestEndTime =Math.max(latestEndTime, timeToSeconds(meeting.endTime))
    }
  }
  return { days: new Set(meetings.map((meeting) => meeting.weekday)).size, hours: meetings.reduce((total, meeting) => total + (timeToSeconds(meeting.endTime) - timeToSeconds(meeting.startTime)) / 3600, 0), gaps: gaps / 3600 }
}
export function buildCalendarSections(details: CourseDetail[], selectedSectionIds: Record<string, string>): CalendarSection[] {
  return details.flatMap(({ course, sectionGroups }) => sectionGroups.flatMap(({ group, sections }) => {
    const section =sections.find((section) => section.id ===selectedSectionIds[group.id])
    return section ? [{ section, courseCode: course.code, courseTitle: course.title }] : []
  }))
}

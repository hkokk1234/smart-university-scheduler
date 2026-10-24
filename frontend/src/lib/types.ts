export type UUID =string
<<<<<<< HEAD
export interface SectionSnapshot {
  code: string; capacity: number
  meetings: { weekday: Weekday; startTime: string; endTime: string; roomId: UUID | null; room: string | null; instructorId: UUID | null; instructor: string | null }[]
}
export interface SectionChange {
  id: UUID; sectionId: UUID; courseId: UUID; courseCode: string; courseTitle: string
  actorName: string; createdAt: string; before: SectionSnapshot; after: SectionSnapshot
}
export interface ChangeNotification { id: UUID; readAt: string | null; change: SectionChange }
export interface NotificationInbox { items: Page<ChangeNotification>; unreadCount: number }
export interface ImportTable { name: string; rows: string[][] }
export interface ImportPreview { tables: ImportTable[]; warnings: string[] }
export interface ImportRow {
  courseCode: string; title: string; credits: string; groupCode: string; groupType: string
  sectionCode: string; capacity: string; weekday: string; startTime: string; endTime: string
}
export interface ImportReport {
  valid: boolean; errors: { rowIndex: number; fields: Partial<Record<keyof ImportRow, string>> }[]
  courseCount: number; sectionCount: number; meetingCount: number
}
export interface ImportSummary { coursesCreated: number; groupsCreated: number; sectionsCreated: number; meetingsCreated: number }
=======
>>>>>>> origin/main
export type Role ='STUDENT' | 'ADMIN'
export type Weekday ='MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
export type PreferredPeriod ='ANY' | 'MORNING' | 'AFTERNOON'
export interface GenerationPreferences {
  preferredStart?: string
  preferredEnd?: string
  preferredFreeDays: Weekday[]
  maximumCampusDays: number | null
  preferredPeriod: PreferredPeriod
  weights: { gaps: number; campusDays: number; preferredTimes: number; freeDays: number; balance: number }
}
export interface User { id: UUID; email: string; displayName: string; createdAt: string; role: Role }
export interface AuthResponse { accessToken: string; tokenType: 'Bearer'; expiresIn: number; user: User }
export interface Semester { id: UUID; code: string; name: string; startsOn: string; endsOn: string; timeZone: string }
export interface Course { id: UUID; semesterId: UUID; code: string; title: string; credits: number }
export interface Instructor { id: UUID; staffCode: string; displayName: string; email: string | null }
export interface Room { id: UUID; building: string; code: string; capacity: number }
export interface Meeting { id: UUID; sectionId: UUID; weekday: Weekday; startTime: string; endTime: string; instructor: Instructor | null; room: Room | null }
export interface Section { id: UUID; sectionGroupId: UUID; semesterId: UUID; code: string; capacity: number; version: number; courseId: UUID; courseCode: string; courseTitle: string; meetings: Meeting[] }
export interface SectionGroup { id: UUID; courseId: UUID; semesterId: UUID; code: string; name: string; type: 'LECTURE' | 'LABORATORY' | 'TUTORIAL' | 'SEMINAR' }
export interface CourseDetail { course: Course; sectionGroups: { group: SectionGroup; sections: Section[] }[] }
export interface SavedSchedule { id: UUID; userId: UUID; semesterId: UUID; name: string; createdAt: string; sections: { id: UUID; savedScheduleId: UUID; section: Section }[] }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface CourseFilters { semesterId?: UUID; q?: string; code?: string; page?: number; size?: number; sort?: string }
export interface ApiErrorBody { timestamp?: string; status: number; code: string; message: string; path?: string; fieldErrors?: Record<string, string> }
export interface MeetingInput { weekday: Weekday; startTime: string; endTime: string; instructorId: UUID | null; roomId: UUID | null }
export interface SectionUpdate { code: string; capacity: number; version: number; meetings: MeetingInput[] }
export interface CalendarSection { section: Section; courseCode: string; courseTitle: string }
export interface ScorePart { criterion: string; penalty: number; weight: number; contribution: number; explanation: string }
export interface GeneratedSchedule { id: string; sections: CalendarSection[]; optimizationScore?: number; scoreExplanation?: ScorePart[] }
export interface GenerationResult { schedules: GeneratedSchedule[]; complete: boolean; explored: number; status?: 'SOLVED' | 'UNSAT' | 'TIMEOUT' | 'CANCELLED'; executionMillis?: number }
export interface ServerGeneration { status: 'SOLVED' | 'UNSAT' | 'TIMEOUT' | 'CANCELLED'; schedules: { selectedSectionIds: UUID[]; sections: Section[]; weeklyMeetings: Meeting[]; optimizationScore: number; scoreExplanation: ScorePart[] }[]; metrics: { nodesVisited: number; backtracks: number; domainPruned: number; solutionsFound: number; executionMillis: number; complete: boolean } }
export interface CopilotTurn { role: 'USER' | 'ASSISTANT'; content: string }
<<<<<<< HEAD
export interface RepairResult {
  originalScheduleId: UUID
  status: ServerGeneration['status']
  metrics: ServerGeneration['metrics']
  schedules: {
    schedule: ServerGeneration['schedules'][number]
    changedSectionCount: number
    changes: { groupId: UUID; before: Section; after: Section }[]
  }[]
}
=======
>>>>>>> origin/main
export interface CopilotReply { answer: string; recommendedCourses: Course[]; scheduleCheck: ServerGeneration | null; catalogTruncated: boolean }
export interface TwinSlot { id: string; label: string; weekday: Weekday; startTime: string; endTime: string; roomId: string | null; attendance: number }
export interface TwinChange { meetingId: string; weekday: Weekday; startTime: string; endTime: string; roomId: string | null; attendance: number }
export interface TwinReport { rooms: { room: Room; occupiedHours: number; utilizationPercent: number }[]; peaks: { weekday: Weekday; simultaneousMeetings: number; estimatedAttendance: number; peakTime: string | null }[]; alerts: { type: string; meetingId: string; otherMeetingId: string | null; message: string }[]; unassignedMeetings: number; occupiedRoomHours: number }
export interface TwinComparison { semesterId: string; rooms: Room[]; meetings: TwinSlot[]; baseline: TwinReport; scenario: TwinReport }

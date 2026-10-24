import { useSession } from "@/features/auth/sessionStore"
<<<<<<< HEAD
import { consumeEventStream, type StreamEvent } from './eventStream'
=======
>>>>>>> origin/main
import type { ApiErrorBody, AuthResponse, CourseDetail, CourseFilters, Course, GenerationPreferences, Instructor, Page, Room, SavedSchedule, Section, SectionUpdate, Semester, ServerGeneration, CopilotReply, CopilotTurn } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly fields: Record<string, string>
  constructor(status: number, message: string, fields: Record<string, string> ={}) {
    super(message)
    this.name ='ApiError'
    this.status =status
    this.fields =fields
  }
}
const root =(import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
async function request<T>(path: string, options: RequestInit & { private?: boolean } ={}): Promise<T> {
  const { private: authenticated, ...init } =options
  const headers =new Headers(init.headers)
  headers.set('Accept', 'application/json')
<<<<<<< HEAD
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
=======
  if (init.body) headers.set('Content-Type', 'application/json')
>>>>>>> origin/main
  if (authenticated) {
    const token =useSession.getState().token
    if (!token) throw new ApiError(401, 'Please sign in to continue.')
    headers.set('Authorization', `Bearer ${token}`)
  }
  let response: Response
  try {
    const timeout =AbortSignal.timeout(15_000)
    const signal =init.signal ? AbortSignal.any([init.signal, timeout]) : timeout
    response =await fetch(root + path, { ...init, headers, credentials: 'omit', signal })
  } catch (error) {
    if (error instanceof DOMException && error.name ==='AbortError' && init.signal?.aborted) throw error
    if (error instanceof DOMException && error.name ==='TimeoutError') throw new ApiError(0, "The request took too long.Please try again.")
    throw new ApiError(0, "We couldn’t reach the server.Check your connection and that the backend is running.")
  }
  if (response.status ===204) return undefined as T
  const body: unknown =await response.json().catch(() => null)
  if (!response.ok) {
    if (authenticated && response.status ===401) useSession.getState().clear()
    const details =body && typeof body ==='object' ? body as Partial<ApiErrorBody> : null
    throw new ApiError(response.status, typeof details?.message ==='string' ? details.message : `The request failed (HTTP ${response.status}).`, details?.fieldErrors ?? {})
  }
  if (body ===null) throw new ApiError(response.status, 'The server returned an unexpected response.')
  return body as T
}
function params(values: Record<string, string | number | undefined>) {
  const query =new URLSearchParams()
  for (const [key, value] of Object.entries(values)) if (value !==undefined && value !=='') query.set(key, String(value))
  return query.size ? '?' + query.toString() : ''
}
export const api ={
<<<<<<< HEAD
  notifications: (page =0, size =20, signal?: AbortSignal) => request<import('./types').NotificationInbox>('/notifications' + params({ page, size }), { private: true, signal }),
  readNotification: (id: string) => request<void>('/notifications/' + encodeURIComponent(id) + '/read', { method: 'POST', private: true }),
  readAllNotifications: () => request<void>('/notifications/read-all', { method: 'POST', private: true }),
  sectionHistory: (id: string, page =0, signal?: AbortSignal) => request<Page<import('./types').SectionChange>>('/admin/sections/' + encodeURIComponent(id) + '/history' + params({ page, size: 10 }), { private: true, signal }),
  previewImport: (file: File, signal?: AbortSignal) => {
    const body =new FormData(); body.append('file', file)
    return request<import('./types').ImportPreview>('/admin/catalog-import/preview', { method: 'POST', private: true, body, signal })
  },
  validateImport: (semesterId: string, rows: import('./types').ImportRow[], signal?: AbortSignal) => request<import('./types').ImportReport>('/admin/catalog-import/validate', { method: 'POST', private: true, signal, body: JSON.stringify({ semesterId, rows }) }),
  commitImport: (semesterId: string, rows: import('./types').ImportRow[], signal?: AbortSignal) => request<import('./types').ImportSummary>('/admin/catalog-import/commit', { method: 'POST', private: true, signal, body: JSON.stringify({ semesterId, rows }) }),
=======
>>>>>>> origin/main
  simulateCampus: (semesterId: string, changes: import('./types').TwinChange[], signal?: AbortSignal) => request<import('./types').TwinComparison>('/admin/digital-twin/simulate', { method: 'POST', private: true, signal, body: JSON.stringify({ semesterId, changes }) }),
  copilotStatus: (signal?: AbortSignal) => request<{ available: boolean }>('/copilot/status', { private: true, signal }),
  copilotChat: (payload: { semesterId: string; message: string; courseIds: string[]; history: CopilotTurn[]; preferences: GenerationPreferences }, signal?: AbortSignal) => request<CopilotReply>('/copilot/chat', { method: 'POST', private: true, signal, body: JSON.stringify(payload) }),
  login: (email: string, password: string) => request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (email: string, displayName: string, password: string) => request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify({ email, displayName, password }) }),
  semesters: (signal?: AbortSignal) => request<Semester[]>('/semesters', { signal }),
  courses: (filters: CourseFilters, signal?: AbortSignal) => request<Page<Course>>('/courses' + params({ ...filters }), { signal }),
  course: (id: string, signal?: AbortSignal) => request<CourseDetail>('/courses/' + encodeURIComponent(id), { signal }),
  section: (id: string, signal?: AbortSignal) => request<Section>('/sections/' + encodeURIComponent(id), { signal }),
  schedules: (page =0, size =12, signal?: AbortSignal) => request<Page<SavedSchedule>>('/schedules' + params({ page, size }), { signal, private: true }),
<<<<<<< HEAD
  schedule: (id: string, signal?: AbortSignal) => request<SavedSchedule>('/schedules/' + encodeURIComponent(id), { signal, private: true }),
  repairSchedule: (id: string, excludedSectionIds: string[], preferences: GenerationPreferences, signal?: AbortSignal) => request<import('./types').RepairResult>('/schedules/' + encodeURIComponent(id) + '/repair', { method: 'POST', private: true, signal, body: JSON.stringify({ excludedSectionIds, maxResults: 5, preferences }) }),
=======
>>>>>>> origin/main
  generate: (semesterId: string, courseIds: string[], preferences: GenerationPreferences, signal?: AbortSignal) => request<ServerGeneration>('/schedules/generate', { method: 'POST', private: true, signal, body: JSON.stringify({ semesterId, courseIds, maxResults: 5, preferences }) }),
  saveSchedule: (semesterId: string, name: string, sectionIds: string[]) => request<SavedSchedule>('/schedules', { method: 'POST', private: true, body: JSON.stringify({ semesterId, name, sectionIds }) }),
  deleteSchedule: (id: string) => request<void>('/schedules/' + encodeURIComponent(id), { method: 'DELETE', private: true }),
  createCourse: (course: { semesterId: string; code: string; title: string; credits: number }) => request<Course>('/admin/courses', { method: 'POST', private: true, body: JSON.stringify(course) }),
  updateSection: (id: string, section: SectionUpdate) => request<Section>('/admin/sections/' + encodeURIComponent(id), { method: 'PUT', private: true, body: JSON.stringify(section) }),
  rooms: (signal?: AbortSignal) => request<Room[]>('/admin/rooms', { private: true, signal }),
  instructors: (signal?: AbortSignal) => request<Instructor[]>('/admin/instructors', { private: true, signal }),
}
<<<<<<< HEAD

export async function streamNotifications(signal: AbortSignal, onEvent: (event: StreamEvent) => void) {
  const token =useSession.getState().token
  if (!token) throw new ApiError(401, 'Please sign in to continue.')
  const deadline =AbortSignal.any([signal, AbortSignal.timeout(75_000)])
  const response =await fetch(root + '/notifications/stream', {
    headers: { Accept: 'text/event-stream', Authorization: `Bearer ${token}` }, credentials: 'omit', cache: 'no-store', signal: deadline,
  })
  if (!response.ok) {
    if (response.status ===401) useSession.getState().clear()
    throw new ApiError(response.status, 'Live updates could not connect.')
  }
  await consumeEventStream(response, deadline, onEvent)
}
=======
>>>>>>> origin/main

import { useQueries, useQuery } from '@tanstack/react-query'
import { api } from './api'
import type { CourseFilters } from './types'
import { useSession } from "@/features/auth/sessionStore"
import { usePlanner } from "@/features/planner/plannerStore"
import { ApiError } from './api'

export function useSemesters() { return useQuery({ queryKey: ['semesters'], queryFn: ({ signal }) => api.semesters(signal), staleTime: 300_000 }) }
export function useCourses(filters: CourseFilters) { return useQuery({ queryKey: ['courses', filters], queryFn: ({ signal }) => api.courses(filters, signal) }) }
export function useCourse(id: string | undefined) { return useQuery({ queryKey: ['course', id], queryFn: ({ signal }) => { if (!id) throw new ApiError(400, 'A course identifier is required.'); return api.course(id, signal) }, enabled: !!id }) }
export function useSelectedCourses() {
  const ids =usePlanner((state) => state.courseIds)
  return useQueries({ queries: ids.map((id) => ({ queryKey: ['course', id], queryFn: ({ signal }: { signal: AbortSignal }) => api.course(id, signal) })) })
}
export function useSchedules(page =0, size =12) {
  const user =useSession((state) => state.user)
  return useQuery({ queryKey: ['private', user?.id, 'schedules', page, size], queryFn: ({ signal }) => api.schedules(page, size, signal), enabled: !!user })
}

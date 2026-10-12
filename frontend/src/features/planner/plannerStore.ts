import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { GenerationResult, UUID } from '@/lib/types'

interface PlannerState {
  ownerId: UUID | null
  semesterId: UUID | ''
  courseIds: UUID[]
  sectionIds: Record<UUID, UUID>
  results: GenerationResult | null
  setOwner: (id: UUID | null) => void
  setSemester: (id: UUID) => void
  addCourse: (id: UUID) => void
  removeCourse: (id: UUID, groupIds?: UUID[]) => void
  selectSection: (groupId: UUID, sectionId: UUID | null) => void
  setResults: (result: GenerationResult | null) => void
  clearDraft: () => void
}
export const usePlanner =create<PlannerState>()(persist((set) => ({
  ownerId: null, semesterId: '', courseIds: [], sectionIds: {}, results: null,
  // Clear the previous draft when a different user signs in.
  setOwner: (ownerId) => set((state) => state.ownerId ===ownerId ? {} : { ownerId, courseIds: [], sectionIds: {}, results: null }),
  setSemester: (semesterId) => set((state) => state.semesterId ===semesterId ? {} : { semesterId, courseIds: [], sectionIds: {}, results: null }),
  addCourse: (id) => set((state) => state.courseIds.includes(id) || state.courseIds.length >=8 ? {} : { courseIds: [...state.courseIds, id], results: null }),
  removeCourse: (id, groupIds =[]) => set((state) => ({ courseIds: state.courseIds.filter((course) => course !==id), sectionIds: Object.fromEntries(Object.entries(state.sectionIds).filter(([group]) => !groupIds.includes(group))), results: null })),
  selectSection: (groupId, sectionId) => set((state) => {
    const selectedSections ={ ...state.sectionIds }
    if (sectionId) selectedSections[groupId] =sectionId
    else delete selectedSections[groupId]
    return { sectionIds: selectedSections, results: null }
  }),
  setResults: (results) => set({ results }),
  clearDraft: () => set({ courseIds: [], sectionIds: {}, results: null }),
}), { name: 'sus-planner-draft', version: 1, partialize: ({ ownerId, semesterId, courseIds, sectionIds }) => ({ ownerId, semesterId, courseIds, sectionIds }) }))

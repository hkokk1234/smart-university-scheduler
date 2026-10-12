import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { PreferredPeriod, Weekday } from '@/lib/types'

export type Theme ='light' | 'dark' | 'system'
export type { PreferredPeriod }
interface Preferences {
  theme: Theme
  weekends: boolean
  compactCalendar: boolean
  earliestStart: string
  latestEnd: string
  preferredFreeDays: Weekday[]
  maximumCampusDays: number | null
  preferredPeriod: PreferredPeriod
  gapImportance: number
  setTheme: (theme: Theme) => void
  setWeekends: (value: boolean) => void
  setCompactCalendar: (value: boolean) => void
  setEarliestStart: (value: string) => void
  setLatestEnd: (value: string) => void
  togglePreferredFreeDay: (value: Weekday) => void
  setMaximumCampusDays: (value: number | null) => void
  setPreferredPeriod: (value: PreferredPeriod) => void
  setGapImportance: (value: number) => void
  reset: () => void
}
const defaults ={
  theme: 'system' as Theme, weekends: false, compactCalendar: false,
  earliestStart: '08:00', latestEnd: '18:00', preferredFreeDays: [] as Weekday[],
  maximumCampusDays: 5 as number | null, preferredPeriod: 'ANY' as PreferredPeriod, gapImportance: 1,
}
export const usePreferences =create<Preferences>()(persist((set) => ({
  ...defaults,
  setTheme: (theme) => set({ theme }),
  setWeekends: (weekends) => set({ weekends }),
  setCompactCalendar: (compactCalendar) => set({ compactCalendar }),
  setEarliestStart: (earliestStart) => set({ earliestStart }),
  setLatestEnd: (latestEnd) => set({ latestEnd }),
  togglePreferredFreeDay: (day) => set((state) => ({
    preferredFreeDays: state.preferredFreeDays.includes(day)
      ? state.preferredFreeDays.filter((value) => value !==day)
      : [...state.preferredFreeDays, day],
  })),
  setMaximumCampusDays: (maximumCampusDays) => set({ maximumCampusDays }),
  setPreferredPeriod: (preferredPeriod) => set({ preferredPeriod }),
  setGapImportance: (gapImportance) => set({ gapImportance }),
  reset: () => set(defaults),
}), {
  name: 'sus-preferences',
  version: 1,
  partialize: ({ theme, weekends, compactCalendar, earliestStart, latestEnd, preferredFreeDays, maximumCampusDays, preferredPeriod, gapImportance }) => ({
    theme, weekends, compactCalendar, earliestStart, latestEnd, preferredFreeDays, maximumCampusDays, preferredPeriod, gapImportance,
  }),
}))

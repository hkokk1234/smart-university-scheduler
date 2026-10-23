import { Laptop, Moon, Sun, UserRound, ShieldCheck } from 'lucide-react'
import { usePreferences, type PreferredPeriod, type Theme } from "./preferencesStore"
import { useSession } from "@/features/auth/sessionStore"
import { PageHeading } from '@/components/shared/PageHeading'
import { Card, Badge } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { HealthCheck } from '@/features/health/HealthCheck'
import { mergeClassNames } from "@/lib/classNames"
import type { Weekday } from '@/lib/types'

const weekdays: { value: Weekday; label: string }[] =[
  { value: 'MONDAY', label: 'Monday' }, { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' }, { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' }, { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
]

export default function SettingsPage() {
  const user =useSession((state) => state.user)
  const expires =useSession((state) => state.expiresAt)
  const theme =usePreferences((state) => state.theme)
  const weekends =usePreferences((state) => state.weekends)
  const compact =usePreferences((state) => state.compactCalendar)
  const earliestStart =usePreferences((state) => state.earliestStart)
  const latestEnd =usePreferences((state) => state.latestEnd)
  const preferredFreeDays =usePreferences((state) => state.preferredFreeDays)
  const maximumCampusDays =usePreferences((state) => state.maximumCampusDays)
  const preferredPeriod =usePreferences((state) => state.preferredPeriod)
  const gapImportance =usePreferences((state) => state.gapImportance)
  const timeWindowValid =Boolean(earliestStart && latestEnd && earliestStart < latestEnd)
  const timeWindowEmpty =!earliestStart && !latestEnd
  const themes: { value: Theme; title: string; icon: typeof Sun }[] =[
    { value: 'light', title: 'Light', icon: Sun },
    { value: 'dark', title: 'Dark', icon: Moon },
    { value: 'system', title: 'System', icon: Laptop },
  ]
  const periods: { value: PreferredPeriod; label: string }[] =[
    { value: 'ANY', label: 'No preference' },
    { value: 'MORNING', label: 'Morning (08:00–12:00)' },
    { value: 'AFTERNOON', label: 'Afternoon (12:00–18:00)' },
  ]

  return <>
    <PageHeading eyebrow="A workspace that fits" title="Settings" description="Display choices and solver preferences are saved only in this browser." />
    <div className="grid items-start gap-6 xl:grid-cols-[1.4fr_1fr]">
      <div className="space-y-6">
        <Card className="p-6">
          <h2 className="text-lg font-semibold">Appearance</h2>
          <p className="mt-2 text-sm text-muted-foreground">Choose a theme for your workspace.</p>
          <fieldset className="mt-5 grid grid-cols-3 gap-3">
            <legend className="sr-only">Color theme</legend>
            {themes.map(({ value, title, icon: Icon }) => <label key={value} className={mergeClassNames("flex cursor-pointer flex-col items-center gap-3 rounded-xl border p-5 text-sm", theme ===value ? 'border-primary bg-secondary/50 text-primary' : 'hover:bg-muted')}>
              <Icon size={22} />
              <span className="flex items-center gap-2">
                <input type="radio" name="theme" className="accent-primary" checked={theme ===value} onChange={() => usePreferences.getState().setTheme(value)} />
                {title}
              </span>
            </label>)}
          </fieldset>
        </Card>
        <Card className="p-6">
          <h2 className="text-lg font-semibold">Calendar display</h2>
          <div className="mt-5 space-y-5">
            <label className="flex items-start justify-between gap-5">
              <span><span className="block text-sm font-medium">Show weekends</span><span className="mt-1 block text-xs leading-relaxed text-muted-foreground">Include Saturday and Sunday columns.Required weekend meetings are always visible.</span></span>
              <input type="checkbox" checked={weekends} onChange={(event) => usePreferences.getState().setWeekends(event.target.checked)} className="mt-1 size-4 shrink-0 accent-primary" />
            </label>
            <label className="flex items-start justify-between gap-5">
              <span><span className="block text-sm font-medium">Compact timetable</span><span className="mt-1 block text-xs text-muted-foreground">Use smaller time slots to see more of the week.</span></span>
              <input type="checkbox" checked={compact} onChange={(event) => usePreferences.getState().setCompactCalendar(event.target.checked)} className="mt-1 size-4 accent-primary" />
            </label>
          </div>
          <div className="mt-6 border-t pt-5"><Button variant="outline" onClick={() => usePreferences.getState().reset()}>Reset all preferences</Button></div>
        </Card>
        <Card className="space-y-5 p-6">
          <div>
            <h2 className="text-lg font-semibold">Schedule preferences</h2>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">These are soft ranking preferences,not hard restrictions.A class outside your preferred times can still appear if it avoids all conflicts and unavailable times.</p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-2 text-sm font-medium" htmlFor="earliest-start">Earliest preferred start
              <input id="earliest-start" type="time" value={earliestStart} onChange={(event) => usePreferences.getState().setEarliestStart(event.target.value)} className="block w-full rounded-lg border bg-background px-3 py-2 text-sm" />
            </label>
            <label className="space-y-2 text-sm font-medium" htmlFor="latest-end">Latest preferred end
              <input id="latest-end" type="time" value={latestEnd} onChange={(event) => usePreferences.getState().setLatestEnd(event.target.value)} className="block w-full rounded-lg border bg-background px-3 py-2 text-sm" />
            </label>
          </div>
          {!timeWindowValid && !timeWindowEmpty && <p role="alert" className="text-xs text-destructive">Choose both times and make the latest end later than the earliest start,or clear both to disable this preference.</p>}
          <label className="block space-y-2 text-sm font-medium" htmlFor="preferred-period">Preferred class period
            <select id="preferred-period" value={preferredPeriod} onChange={(event) => {
              const selection =periods.find((period) => period.value ===event.target.value)
              if (selection) usePreferences.getState().setPreferredPeriod(selection.value)
            }} className="block w-full rounded-lg border bg-background px-3 py-2 text-sm">
              {periods.map((period) => <option key={period.value} value={period.value}>{period.label}</option>)}
            </select>
          </label>
          <fieldset>
            <legend className="text-sm font-medium">Preferred free days</legend>
            <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
              {weekdays.map(({ value, label }) => <label key={value} className="flex items-center gap-2 text-xs">
                <input type="checkbox" checked={preferredFreeDays.includes(value)} onChange={() => usePreferences.getState().togglePreferredFreeDay(value)} className="size-4 accent-primary" />
                {label}
              </label>)}
            </div>
          </fieldset>
          <label className="block space-y-2 text-sm font-medium" htmlFor="maximum-campus-days">Preferred maximum campus days per week
            <select id="maximum-campus-days" value={maximumCampusDays ?? ''} onChange={(event) => usePreferences.getState().setMaximumCampusDays(event.target.value ? Number(event.target.value) : null)} className="block w-full rounded-lg border bg-background px-3 py-2 text-sm">
              <option value="">No maximum</option>
              {[1, 2, 3, 4, 5, 6, 7].map((days) => <option key={days} value={days}>{days} {days ===1 ? 'day' : 'days'}</option>)}
            </select>
          </label>
          <label className="block space-y-2 text-sm font-medium" htmlFor="gap-importance">
            <span className="flex justify-between gap-3"><span>Importance of minimizing gaps</span><span>{gapImportance.toFixed(2)}×</span></span>
            <input id="gap-importance" type="range" min="0" max="5" step="0.25" value={gapImportance} onChange={(event) => usePreferences.getState().setGapImportance(Number(event.target.value))} className="w-full accent-primary" />
            <span className="flex justify-between text-xs font-normal text-muted-foreground"><span>Ignore gaps</span><span>High importance</span></span>
          </label>
        </Card>
      </div>
      <div className="space-y-6">
        <Card className="p-6">
          <h2 className="flex items-center gap-2 text-lg font-semibold"><UserRound size={19} />Your account</h2>
          <dl className="mt-6 space-y-4 text-sm">
            <div><dt className="text-xs text-muted-foreground">Name</dt><dd className="mt-1 font-medium">{user?.displayName}</dd></div>
            <div><dt className="text-xs text-muted-foreground">Email</dt><dd className="mt-1 break-all font-medium">{user?.email}</dd></div>
            <div><dt className="text-xs text-muted-foreground">Access</dt><dd className="mt-2"><Badge><ShieldCheck size={12} />{user?.role ==='ADMIN' ? 'Administrator' : 'Student'}</Badge></dd></div>
          </dl>
          <p className="mt-6 border-t pt-4 text-xs leading-relaxed text-muted-foreground">Account details are managed by the university.This workspace does not change your email,password,or role.</p>
          {expires && <p className="mt-3 text-xs text-muted-foreground">Current session expires at {new Date(expires).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}.</p>}
        </Card>
        <HealthCheck />
      </div>
    </div>
  </>
}

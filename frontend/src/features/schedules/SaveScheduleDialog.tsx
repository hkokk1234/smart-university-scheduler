import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router'
import { CircleCheck, Save } from 'lucide-react'
import { api } from '@/lib/api'
import type { CalendarSection } from '@/lib/types'
import { useSession } from "@/features/auth/sessionStore"
import { Dialog } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ErrorState } from "@/components/shared/RequestStates"

export function SaveScheduleDialog({ entries, semesterId, disabled =false }: { entries: CalendarSection[]; semesterId: string; disabled?: boolean }) {
  const [open, setOpen] =useState(false)
  const client =useQueryClient()
  const user =useSession((state) => state.user)
  const mutation =useMutation({ mutationFn: (name: string) => api.saveSchedule(semesterId, name, entries.map((entry) => entry.section.id)),
    onSuccess: () => void client.invalidateQueries({ queryKey: ['private', user?.id, 'schedules'] }) })
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); mutation.mutate(String(new FormData(event.currentTarget).get('name')).trim()) }
  return <><Button onClick={() => { mutation.reset(); setOpen(true) }} disabled={disabled || !semesterId}><Save size={16} />Save schedule</Button><Dialog open={open} onOpenChange={setOpen} title={mutation.isSuccess ? 'Your plan is saved' : 'Give your schedule a name'} description={mutation.isSuccess ? 'You can revisit this plan from Saved schedules.' : `Save ${entries.length} selected sections, including every weekly meeting.`}>
    {mutation.isSuccess ? <div className="space-y-5"><p role="status" className="flex items-center gap-2 text-sm text-primary"><CircleCheck size={18} />Schedule saved successfully.</p><Button asChild className="w-full"><Link to="/saved" onClick={() => setOpen(false)}>View saved schedules</Link></Button></div> : <form onSubmit={submit} className="space-y-5"><div><label htmlFor="schedule-name" className="mb-2 block text-sm font-medium">Schedule name</label><Input id="schedule-name" name="name" placeholder="e.g.My preferred week" required maxLength={120} autoFocus /></div>{mutation.isError && <ErrorState error={mutation.error} />}<div className="flex justify-end gap-3"><Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? 'Saving…' : 'Save schedule'}</Button></div></form>}
  </Dialog></>
}

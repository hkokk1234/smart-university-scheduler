import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { ArrowRight, Check, Eye, EyeOff, GraduationCap, CalendarDays } from 'lucide-react'
import { api, ApiError } from '@/lib/api'
import { useSession } from "./sessionStore"
import { usePlanner } from "@/features/planner/plannerStore"
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

export default function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const register =mode ==='register'
  const [visible, setVisible] =useState(false)
  const [clientError, setClientError] =useState('')
  const user =useSession((state) => state.user)
  const navigate =useNavigate()
  const location =useLocation()
  const mutation =useMutation({
    mutationFn: ({ email, name, password }: { email: string; name: string; password: string }) => register ? api.register(email, name, password) : api.login(email, password),
    onSuccess: (response) => {
      usePlanner.getState().setOwner(response.user.id)
      useSession.getState().start(response)
      const target =(location.state as { from?: string } | null)?.from
      navigate(target?.startsWith('/') && !target.startsWith('//') && !target.startsWith('/login') ? target : '/', { replace: true })
    },
  })
  if (user) return <Navigate to="/" replace />
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setClientError('')
    const form =new FormData(event.currentTarget)
    const password =String(form.get('password') ?? '')
    if (new TextEncoder().encode(password).length > 72) { setClientError("Your password must fit within 72 UTF 8 bytes.Try fewer accented characters or emoji."); return }
    mutation.mutate({ email: String(form.get('email')).trim(), name: String(form.get('name') ?? '').trim(), password })
  }
  const fields =mutation.error instanceof ApiError ? mutation.error.fields : {}
  return <main className="grid min-h-dvh lg:grid-cols-[1fr_1fr]">
    <section className="relative hidden overflow-hidden bg-[#481721] p-14 text-white lg:flex lg:flex-col">
      <Link to="/login" className="relative z-10 flex items-center gap-3 text-sm font-semibold"><span className="rounded-xl bg-white/10 p-2.5"><GraduationCap size={25} /></span>Smart University Scheduler</Link>
      <div className="relative z-10 my-auto max-w-xl py-14"><p className="mb-6 text-xs font-semibold uppercase tracking-[0.2em] text-[#edc1c0]/80">Make room for what matters</p><h1 className="text-5xl font-medium leading-[1.15] tracking-tight">A better semester<br />starts with<br /><span className="text-[#edc1c0]">a better plan.</span></h1><p className="mt-6 max-w-sm text-sm leading-7 text-white/70">Find your courses,compare your options,and build a week that leaves space for life beyond the lecture hall.</p><div className="mt-9 space-y-3 text-sm text-white/85">{['Every lecture and lab in one place', 'See timetable conflicts before you commit', 'Save the schedules that work for you'].map((text) => <p key={text} className="flex items-center gap-3"><span className="rounded-full bg-white/10 p-1"><Check size={13} /></span>{text}</p>)}</div></div>
      <div className="pointer-events-none absolute -bottom-28 -right-28 size-[460px] rounded-full border-[70px] border-white/5" /><div className="pointer-events-none absolute -right-24 top-28 size-72 rounded-full border border-white/10" />
      <p className="relative z-10 text-xs text-white/45">Thoughtful planning.More possibilities.</p>
    </section>
    <section className="flex items-center justify-center px-6 py-14 sm:px-12"><div className="w-full max-w-md">
      <Link to="/login" className="mb-10 flex items-center gap-3 font-semibold lg:hidden"><GraduationCap className="text-primary" />Smart University Scheduler</Link>
      <div className="mb-7 inline-flex size-12 items-center justify-center rounded-2xl bg-secondary text-primary"><CalendarDays size={23} /></div>
      <h2 className="text-3xl font-semibold tracking-tight">{register ? 'Create your workspace' : 'Welcome back.'}</h2>
      <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{register ? "One account.All your semester possibilities." : 'Sign in and pick up where your planning left off.'}</p>
      <form onSubmit={submit} className="mt-8 space-y-5">
        {register && <div><label htmlFor="name" className="mb-2 block text-sm font-medium">Your name</label><Input id="name" name="name" autoComplete="name" placeholder="Your full name" required maxLength={120} aria-invalid={!!fields.displayName} />{fields.displayName && <p className="mt-1 text-xs text-destructive">{fields.displayName}</p>}</div>}
        <div><label htmlFor="email" className="mb-2 block text-sm font-medium">Email address</label><Input id="email" name="email" type="email" autoComplete="username" placeholder="you@university.edu" required maxLength={254} aria-invalid={!!fields.email} />{fields.email && <p className="mt-1 text-xs text-destructive">{fields.email}</p>}</div>
        <div><label htmlFor="password" className="mb-2 block text-sm font-medium">Password</label><div className="relative"><Input id="password" name="password" type={visible ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} placeholder={register ? 'At least 12 characters' : 'Enter your password'} required minLength={register ? 12 : 1} maxLength={72} aria-invalid={!!fields.password} className="pr-12" /><button type="button" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible(!visible)} className="absolute right-3 top-3 text-muted-foreground">{visible ? <EyeOff size={19} /> : <Eye size={19} />}</button></div>{fields.password && <p className="mt-1 text-xs text-destructive">{fields.password}</p>}</div>
        {(clientError || mutation.error) && <p role="alert" className="rounded-xl bg-destructive/5 p-3 text-sm text-destructive">{clientError || mutation.error?.message}</p>}
        <Button type="submit" className="h-12 w-full" disabled={mutation.isPending}>{mutation.isPending ? 'Please wait…' : register ? 'Create student account' : 'Sign in to your workspace'}<ArrowRight size={16} /></Button>
      </form>
      <p className="mt-7 text-center text-sm text-muted-foreground">{register ? 'Already have an account?' : 'New to the workspace?'} <Link className="font-semibold text-primary hover:underline" to={register ? '/login' : '/register'}>{register ? 'Sign in' : 'Create an account'}</Link></p>
      <p className="mt-12 text-center text-xs leading-6 text-muted-foreground">Your session is kept in memory for security.<br />Refreshing the page requires signing in again.</p>
    </div></section>
  </main>
}

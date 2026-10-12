import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Bot, Send, Sparkles, Plus, RotateCcw } from 'lucide-react'
import { api } from '@/lib/api'
import type { CopilotReply, CopilotTurn } from '@/lib/types'
import { useSession } from "@/features/auth/sessionStore"
import { usePlanner } from "@/features/planner/plannerStore"
import { usePreferences } from "@/features/settings/preferencesStore"
import { useSelectedCourses } from "@/lib/catalogQueries"
import { SemesterSelect } from '@/components/shared/SemesterSelect'
import { PageHeading } from '@/components/shared/PageHeading'
import { ErrorState, LoadingState } from "@/components/shared/RequestStates"
import { Button } from '@/components/ui/button'
import { Card, Badge } from '@/components/ui/card'
import { Textarea } from '@/components/ui/input'

interface Message extends CopilotTurn { id: number; reply?: CopilotReply }
const starters=['Which courses fit my interests?', 'Review the workload of my selected courses.', 'How can I reduce gaps and campus days?']

export default function CopilotPage() {
  const user=useSession((state)=>state.user)
  const semesterId=usePlanner((state)=>state.semesterId)
  const courseIds=usePlanner((state)=>state.courseIds)
  const courses=useSelectedCourses()
  const [messages,setMessages]=useState<Message[]>([])
  const [question,setQuestion]=useState('')
  const [sending,setSending]=useState(false)
  const [error,setError]=useState<Error | null>(null)
  const active=useRef<AbortController | null>(null)
  const serial=useRef(0)
  const bottom=useRef<HTMLDivElement | null>(null)
  const status=useQuery({ queryKey:['private',user?.id,'copilot-status'], queryFn:({signal})=>api.copilotStatus(signal), staleTime:30_000, retry:false })
  useEffect(()=>{ active.current?.abort(); setSending(false); setMessages([]); setError(null); return ()=>active.current?.abort() },[semesterId])
  useEffect(()=>{ bottom.current?.scrollIntoView({block:'nearest'}) },[messages,sending])
  async function send(event: FormEvent) {
    event.preventDefault()
    const content=question.trim()
    if (!content || sending || !semesterId || !status.data?.available) return
    const controller=new AbortController(); active.current=controller
    const promptId=++serial.current
    const history=messages.slice(-6).map(({role,content})=>({role,content}))
    setMessages((previous)=>[...previous,{id:promptId,role:'USER',content}]); setQuestion(''); setSending(true); setError(null)
    const prefs=usePreferences.getState()
    const validWindow=prefs.earliestStart && prefs.latestEnd && prefs.earliestStart<prefs.latestEnd
    try {
      const reply=await api.copilotChat({ semesterId,courseIds,message:content,history,preferences:{
        ...(validWindow?{preferredStart:prefs.earliestStart,preferredEnd:prefs.latestEnd}:{}),
        preferredFreeDays:prefs.preferredFreeDays,maximumCampusDays:prefs.maximumCampusDays,preferredPeriod:prefs.preferredPeriod,
        weights:{gaps:prefs.gapImportance,campusDays:1,preferredTimes:1,freeDays:1,balance:0.2},
      } },controller.signal)
      if (!controller.signal.aborted) setMessages((previous)=>[...previous,{id:++serial.current,role:'ASSISTANT',content:reply.answer,reply}])
    } catch (failure) {
      if (!controller.signal.aborted) {
        setError(failure instanceof Error?failure:new Error('The advisor could not answer.'))
        setMessages((previous)=>previous.filter((message)=>message.id!==promptId)); setQuestion(content)
      }
    } finally { if (active.current===controller) setSending(false) }
  }
  return <>
    <PageHeading eyebrow="Your academic planning partner" title="AI Academic Copilot" description="Explore courses,understand your workload,and plan a week around your priorities." action={<SemesterSelect />} />
    <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_300px]">
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between gap-3 border-b bg-primary/5 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-primary p-2.5 text-primary-foreground"><Bot size={22} /></span><div><h2 className="font-semibold">Let’s think through your semester</h2><p className="mt-1 text-xs text-muted-foreground">Advice grounded in your catalog and timetable solver.</p></div></div><Button size="sm" variant="ghost" disabled={sending || !messages.length} aria-label="Clear conversation" onClick={()=>{setMessages([]);setError(null)}}><RotateCcw size={16}/></Button></div>
        {status.isPending?<LoadingState label="Connecting to your advisor…"/>:status.isError?<div className="p-5"><ErrorState error={status.error} retry={()=>void status.refetch()}/></div>:!status.data.available?<div className="space-y-3 p-6"><Badge>Not connected</Badge><h3 className="font-semibold">Your AI advisor is not enabled yet</h3><p className="text-sm text-muted-foreground">Ask your administrator to connect the AI service.Your catalog and schedule builder are ready to use.</p><Button asChild variant="outline"><Link to="/builder">Open schedule builder</Link></Button><Button variant="ghost" onClick={()=>void status.refetch()}>Check again</Button></div>:<>
          <div role="log" aria-label="Academic advice conversation" aria-live="polite" className="max-h-[600px] min-h-64 space-y-5 overflow-y-auto p-5 sm:p-6">
            {!messages.length && <div className="space-y-5 py-6"><Sparkles size={30} className="text-primary"/><h3 className="text-lg font-semibold">What would make this semester work for you?</h3><p className="max-w-lg text-sm leading-relaxed text-muted-foreground">Tell me your interests or ask about your selected courses.I can explain credits,scheduling tradeoffs,and ways to make your week more manageable.</p><div className="flex flex-wrap gap-2">{starters.map((text)=><Button key={text} size="sm" variant="outline" onClick={()=>setQuestion(text)}>{text}</Button>)}</div></div>}
            {messages.map((message)=><div key={message.id} className={message.role==='USER'?'ml-auto max-w-[90%] rounded-2xl bg-primary/10 p-4':"space-y-3 rounded-2xl border bg-background p-4"}><p className="mb-2 text-xs font-semibold text-primary">{message.role==='USER'?'You':'Academic Copilot'}</p><p className="whitespace-pre-wrap text-sm leading-relaxed">{message.content}</p>{message.reply?.recommendedCourses.map((course)=><div key={course.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border bg-card p-3"><Link to={'/catalog/'+course.id} className="text-sm hover:text-primary"><strong>{course.code}</strong> · {course.title}<span className="ml-2 text-xs text-muted-foreground">{course.credits} credits</span></Link><Button size="sm" variant="outline" disabled={courseIds.includes(course.id) || courseIds.length>=8} onClick={()=>usePlanner.getState().addCourse(course.id)}><Plus size={14}/>{courseIds.includes(course.id)?'Selected':'Add course'}</Button></div>)}{message.reply?.scheduleCheck && <p className="text-xs text-muted-foreground">Timetable check: {message.reply.scheduleCheck.status}.{message.reply.scheduleCheck.metrics.complete?'Search completed.':'Search incomplete.'} <Link to="/builder" className="text-primary underline">Compare options in the builder</Link></p>}{message.reply?.catalogTruncated && <p className="text-xs text-muted-foreground">Advice used the first 50 catalog courses plus your selected courses.Browse the catalog for the full list.</p>}</div>)}
            {sending && <p role="status" className="animate-pulse text-sm text-muted-foreground">Checking your courses and thinking through options…</p>}<div ref={bottom}/>
          </div>
          <form onSubmit={(event)=>void send(event)} className="space-y-3 border-t p-5">{error && <ErrorState error={error}/>}<label htmlFor="copilot-question" className="text-sm font-medium">Ask your academic advisor</label><Textarea id="copilot-question" value={question} onChange={(event)=>setQuestion(event.target.value)} placeholder="I’m interested in programming and want fewer campus days.What are my options?" maxLength={2000} rows={3} required disabled={sending}/><div className="flex items-center justify-between gap-3"><p className="text-[11px] text-muted-foreground">{question.length}/2000 · Conversation stays in this session.</p>{sending?<Button type="button" variant="outline" onClick={()=>active.current?.abort()}>Cancel</Button>:<Button type="submit" disabled={!question.trim() || !semesterId}><Send size={15}/>Ask Copilot</Button>}</div><p className="text-[11px] leading-relaxed text-muted-foreground">Your question,recent conversation,course choices and planning preferences are sent to the AI provider.Account details and saved schedules are not included.</p></form>
        </>}
      </Card>
      <div className="space-y-4"><Card className="space-y-4 p-5"><h2 className="text-sm font-semibold">Your planning context</h2><Badge>{courseIds.length} selected courses</Badge>{courses.flatMap((query)=>query.data?[query.data.course]:[]).map((course)=><p key={course.id} className="text-xs"><strong>{course.code}</strong><span className="mt-1 block text-muted-foreground">{course.title} · {course.credits} credits</span></p>)}{!courseIds.length && <p className="text-xs text-muted-foreground">Add courses for a workload and timetable review,or start by sharing your interests.</p>}<Button asChild variant="outline" className="w-full"><Link to="/catalog">Browse courses</Link></Button></Card><Card className="space-y-3 p-5"><h2 className="text-sm font-semibold">Advice that fits your priorities</h2><p className="text-xs leading-relaxed text-muted-foreground">Copilot uses your preferred class times,free days and gap importance.Your department’s degree requirements are not part of the current catalog.</p><Link to="/settings" className="text-xs font-semibold text-primary">Adjust planning preferences →</Link></Card></div>
    </div>
  </>
}

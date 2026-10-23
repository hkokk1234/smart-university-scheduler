import { Link, useSearchParams } from 'react-router'
import { ArrowUpRight, BookOpen, Check, Plus, Search } from 'lucide-react'
import { useCourses, useSemesters } from "@/lib/catalogQueries"
import { useDebouncedValue } from "@/lib/useDebouncedValue"
import { usePlanner } from "@/features/planner/plannerStore"
import { PageHeading } from '@/components/shared/PageHeading'
import { SemesterSelect } from '@/components/shared/SemesterSelect'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorState, EmptyState, LoadingState } from "@/components/shared/RequestStates"
import { Button } from '@/components/ui/button'
import { Card, Badge } from '@/components/ui/card'
import { Input, Select } from '@/components/ui/input'

export default function CatalogPage() {
  const [params, setParams] =useSearchParams()
  const semesterId =usePlanner((state) => state.semesterId)
  const selected =usePlanner((state) => state.courseIds)
  const add =usePlanner((state) => state.addCourse)
  const semesterQuery =useSemesters()
  const q =useDebouncedValue(params.get('q') ?? '')
  const page =Math.max(0, Math.min(1_000_000, Number(params.get('page')) || 0))
  const sort =params.get('sort') ?? 'code,asc'
  const query =useCourses({ semesterId: semesterId || undefined, q, page, size: 12, sort })
  function change(key: string, value: string) { const next =new URLSearchParams(params); if (value) next.set(key, value); else next.delete(key); if (key !=='page') next.delete('page'); setParams(next, { replace: key ==='q' }) }
  return <>
    <PageHeading eyebrow="Explore your semester" title="Course catalog" description="Discover what you’ll learn next.Add courses to your builder and compare their lecture and lab options." action={<SemesterSelect />} />
    <Card className="flex flex-wrap items-center gap-3 p-4"><div className="relative min-w-52 flex-1"><Search size={17} className="absolute left-3.5 top-3.5 text-muted-foreground" /><Input aria-label="Search courses by title or code" placeholder="Search by course title or code…" className="pl-10" value={params.get('q') ?? ''} onChange={(event) => change('q', event.target.value)} maxLength={100} /></div><Select aria-label="Sort courses" value={sort} onChange={(event) => change('sort', event.target.value)} className="sm:w-48"><option value="code,asc">Course code · A–Z</option><option value="title,asc">Title · A–Z</option><option value="credits,desc">Credits · high to low</option></Select><Badge className="h-9 px-3">{selected.length} selected</Badge></Card>
    {semesterQuery.isError && <ErrorState error={semesterQuery.error} retry={() => void semesterQuery.refetch()} />}
    {query.isPending ? <LoadingState label="Finding courses…" /> : query.isError ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : !query.data.content.length ? <EmptyState title="No courses found" description="Try a different search or semester.If your catalog is empty,your administrator needs to publish courses." action={q ? <Button variant="outline" onClick={() => change('q', '')}>Clear search</Button> : undefined} /> : <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{query.data.content.map((course) => <Card key={course.id} className="flex flex-col p-6"><div className="mb-6 flex items-start justify-between"><span className="rounded-xl bg-primary/8 p-3 text-primary"><BookOpen size={22} /></span><Badge>{course.credits} credits</Badge></div><p className="text-xs font-semibold tracking-wide text-primary">{course.code}</p><Link to={'/catalog/' + course.id} className="mt-2 text-lg font-semibold leading-snug hover:text-primary">{course.title}</Link><p className="mt-3 text-xs leading-relaxed text-muted-foreground">View section groups,meeting times,instructors,and available rooms.</p><div className="mt-auto flex items-center justify-between gap-2 pt-6"><Link to={'/catalog/' + course.id} className="inline-flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-primary">View details<ArrowUpRight size={14} /></Link><Button size="sm" variant={selected.includes(course.id) ? 'secondary' : 'outline'} disabled={selected.includes(course.id) || selected.length >=8 || (!!semesterId && course.semesterId !==semesterId)} onClick={() => { if (!semesterId) usePlanner.getState().setSemester(course.semesterId); add(course.id) }}>{selected.includes(course.id) ? <><Check size={14} />Selected</> : <><Plus size={14} />Add course</>}</Button></div></Card>)}</div>}
    {query.data && <Pagination page={page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} change={(next) => change('page', String(next))} />}
    <p className="text-xs text-muted-foreground">Choose up to 8 courses per plan.Times are shown in your semester’s local time zone.</p>
  </>
}

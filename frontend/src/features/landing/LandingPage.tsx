import { useEffect } from 'react'
import { Link } from 'react-router'
import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  CalendarCheck,
  Check,
  Clock3,
  GraduationCap,
  Sparkles,
  Target,
} from 'lucide-react'
import { Button } from '@/components/ui/button'

const days = [
  {
    name: 'MON',
    blocks: [
      { title: 'Data structures', time: '9:00 – 10:15', color: 'bg-[#f2d8d5] text-[#712f35]' },
      null,
      { title: 'Calculus II', time: '1:00 – 2:15', color: 'bg-[#dce8d8] text-[#385440]' },
      null,
    ],
  },
  {
    name: 'TUE',
    blocks: [
      null,
      { title: 'World history', time: '11:00 – 12:15', color: 'bg-[#e9e0f1] text-[#59466e]' },
      null,
      { title: 'Studio lab', time: '3:00 – 4:30', color: 'bg-[#f5e5c7] text-[#775a2c]' },
    ],
  },
  {
    name: 'WED',
    blocks: [
      { title: 'Data structures', time: '9:00 – 10:15', color: 'bg-[#f2d8d5] text-[#712f35]' },
      null,
      { title: 'Calculus II', time: '1:00 – 2:15', color: 'bg-[#dce8d8] text-[#385440]' },
      null,
    ],
  },
  {
    name: 'THU',
    blocks: [
      null,
      { title: 'World history', time: '11:00 – 12:15', color: 'bg-[#e9e0f1] text-[#59466e]' },
      null,
      null,
    ],
  },
  {
    name: 'FRI',
    blocks: [
      { title: 'Data structures', time: '9:00 – 10:15', color: 'bg-[#f2d8d5] text-[#712f35]' },
      null,
      null,
      null,
    ],
  },
]

const features = [
  {
    icon: BookOpen,
    number: '01',
    title: 'Start with your courses',
    description: 'Browse the catalog and add the courses you need for the semester.',
  },
  {
    icon: Target,
    number: '02',
    title: 'Tell us what matters',
    description: "Set your preferred hours,free days,and the kind of week you want.",
  },
  {
    icon: CalendarCheck,
    number: '03',
    title: 'Compare clear options',
    description: "Generate conflict free schedules and see why each option ranks.",
  },
]

export default function LandingPage() {
  useEffect(() => {
    document.title = 'Plan a better semester · Smart University Scheduler'
  }, [])

  return (
    <main className="min-h-dvh overflow-hidden bg-[#fbf8f4] text-[#292522]">
      <header className="mx-auto flex max-w-[1440px] items-center justify-between px-5 py-5 sm:px-10 lg:px-16">
        <Link to="/" className="flex items-center gap-3" aria-label="Smart University Scheduler home">
          <span className="flex size-10 items-center justify-center rounded-xl bg-[#761f2b] text-white">
            <GraduationCap size={22} />
          </span>
          <span className="text-sm font-semibold leading-tight tracking-tight">
            Smart University
            <span className="block text-xs font-normal text-[#817972]">Scheduler</span>
          </span>
        </Link>
        <nav aria-label="Main navigation" className="flex items-center gap-3 sm:gap-7">
          <a href="#how-it-works" className="hidden text-sm text-[#665f58] transition-colors hover:text-[#761f2b] sm:inline">
            How it works
          </a>
          <Link to="/login" className="text-sm font-medium text-[#514a44] hover:text-[#761f2b]">
            Sign in
          </Link>
          <Button asChild className="rounded-full bg-[#761f2b] px-5 text-white hover:bg-[#5d1822]">
            <Link to="/register">Get started <ArrowUpRight size={15} /></Link>
          </Button>
        </nav>
      </header>

      <section className="mx-auto grid max-w-[1440px] items-center gap-12 px-5 pb-20 pt-12 sm:px-10 sm:pt-16 lg:grid-cols-[0.92fr_1.08fr] lg:gap-8 lg:px-16 lg:pb-28 lg:pt-20">
        <div className="relative z-10 max-w-2xl">
          <p className="mb-6 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.19em] text-[#761f2b]">
            <span className="size-2 rounded-full bg-[#bd806f]" />
            A more thoughtful way to plan
          </p>
          <h1 className="font-serif text-[3.25rem] font-medium leading-[1.03] tracking-[-0.045em] text-[#2d2925] sm:text-6xl lg:text-[5.25rem]">
            Make your
            <br />
            semester work
            <br />
            <span className="italic text-[#8a2934]">for you.</span>
          </h1>
          <p className="mt-7 max-w-lg text-base leading-7 text-[#6d655e] sm:text-lg sm:leading-8">
            Bring your courses together,find schedules without conflicts,and make room for more than just class.
          </p>
          <div className="mt-8 flex flex-wrap items-center gap-4">
            <Button asChild size="lg" className="h-12 rounded-full bg-[#761f2b] px-6 text-white hover:bg-[#5d1822]">
              <Link to="/register">Build your schedule <ArrowRight size={17} /></Link>
            </Button>
            <a href="#how-it-works" className="inline-flex h-12 items-center gap-2 px-3 text-sm font-medium text-[#514a44] hover:text-[#761f2b]">
              See how it works <ArrowDown size={15} />
            </a>
          </div>
          <div className="mt-9 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-[#71685f]">
            <span className="flex items-center gap-2"><Check size={14} className="text-[#812b35]" />Compare section times</span>
            <span className="flex items-center gap-2"><Check size={14} className="text-[#812b35]" />Keep your plans private</span>
          </div>
        </div>

        <div className="relative mx-auto w-full max-w-[650px] lg:ml-auto">
          <div className="absolute -right-8 -top-12 size-40 rounded-full border border-[#8a2934]/10 sm:-right-14 sm:size-56" aria-hidden="true" />
          <div className="absolute -bottom-8 -left-7 size-36 rounded-full bg-[#eee1d3] sm:-left-12 sm:size-48" aria-hidden="true" />
          <div className="relative rounded-[2rem] border border-[#e8dfd5] bg-[#f3eee7] p-3 shadow-[0_30px_90px_-45px_rgba(77,47,37,0.3)] sm:p-5">
            <div className="rounded-[1.45rem] bg-white p-4 sm:p-6">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#928981]">A week with room to breathe</p>
                  <h2 className="mt-1 font-serif text-xl text-[#322b27] sm:text-2xl">Your schedule,in view</h2>
                </div>
                <span className="rounded-full bg-[#f8f4ef] px-2.5 py-1 text-[9px] font-medium text-[#857a71] sm:px-3 sm:text-[10px]">Illustrative preview</span>
              </div>
              <div role="img" aria-label="Illustrative weekly timetable with classes Monday through Friday and no overlapping meetings" className="mt-6 grid grid-cols-[34px_repeat(5,minmax(0,1fr))] gap-1.5 sm:grid-cols-[42px_repeat(5,minmax(0,1fr))] sm:gap-2">
                <div className="pt-1" />
                {days.map(({ name }) => (
                  <div key={name} className="pb-2 text-center text-[8px] font-semibold tracking-[0.12em] text-[#958b82] sm:text-[9px]">{name}</div>
                ))}
                <div className="grid grid-rows-4 gap-2 text-[8px] text-[#a2988e] sm:text-[9px]">
                  {['9 AM', '11 AM', '1 PM', '3 PM'].map((time) => <span key={time} className="pt-2">{time}</span>)}
                </div>
                {days.map(({ name, blocks }) => (
                  <div key={name} className="grid grid-rows-4 gap-2">
                    {blocks.map((block, index) => block ? (
                      <div key={`${name}-${block.title}`} className={`min-h-0 rounded-lg p-1.5 sm:rounded-xl sm:p-2 ${block.color}`}>
                        <p className="line-clamp-2 text-[8px] font-semibold leading-tight sm:text-[10px]">{block.title}</p>
                        <p className="mt-1 hidden text-[8px] opacity-75 sm:block">{block.time}</p>
                      </div>
                    ) : <div key={`${name}-free-${index}`} className="rounded-lg border border-dashed border-[#eee8e1] bg-[#fcfbf9]" />)}
                  </div>
                ))}
              </div>
              <div className="mt-4 flex items-center justify-between border-t border-[#f0ebe5] pt-4">
                <span className="flex items-center gap-2 text-[10px] text-[#736a62] sm:text-xs"><Clock3 size={14} className="text-[#8a2934]" /> Free Friday afternoon</span>
                <span className="flex items-center gap-1.5 text-[10px] font-medium text-[#517354] sm:text-xs"><Check size={14} /> No overlaps</span>
              </div>
            </div>
          </div>
          <div className="absolute -left-3 top-[29%] flex items-center gap-2 rounded-xl border border-[#efe8df] bg-white px-3 py-2.5 shadow-lg shadow-[#543a2e]/10 sm:-left-9 sm:px-4">
            <span className="flex size-8 items-center justify-center rounded-lg bg-[#f5e9e2] text-[#8a2934]"><Sparkles size={16} /></span>
            <span><span className="block text-[10px] font-semibold text-[#39322d] sm:text-xs">Made around you</span><span className="mt-0.5 block text-[9px] text-[#8b8178] sm:text-[10px]">Your preferences matter</span></span>
          </div>
        </div>
      </section>

      <section id="how-it-works" className="scroll-mt-8 border-y border-[#eae2d9] bg-[#f6f1eb]">
        <div className="mx-auto max-w-[1440px] px-5 py-16 sm:px-10 sm:py-20 lg:px-16">
          <div className="grid gap-5 md:grid-cols-[0.8fr_1.2fr] md:items-end">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#8a2934]">A clearer way forward</p>
              <h2 className="mt-4 max-w-md font-serif text-3xl leading-tight tracking-tight text-[#302a26] sm:text-4xl">From course list to a week that fits.</h2>
            </div>
            <p className="max-w-xl text-sm leading-7 text-[#71685f] md:justify-self-end">
              Plan with the details that matter: section choices,class times,and the breathing room you want between them.
            </p>
          </div>
          <div className="mt-10 grid gap-4 md:grid-cols-3">
            {features.map(({ icon: Icon, number, title, description }) => (
              <article key={number} className="group rounded-2xl border border-[#e8dfd5] bg-[#fbf9f6] p-6 transition-transform duration-200 hover:-translate-y-1 sm:p-7">
                <div className="flex items-start justify-between">
                  <span className="flex size-11 items-center justify-center rounded-xl bg-[#f1e4df] text-[#812b35]"><Icon size={20} /></span>
                  <span className="font-serif text-sm italic text-[#b5a99e]">{number}</span>
                </div>
                <h3 className="mt-7 text-base font-semibold text-[#38312c]">{title}</h3>
                <p className="mt-2 max-w-xs text-sm leading-6 text-[#7b726a]">{description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto grid max-w-[1440px] gap-8 px-5 py-16 sm:px-10 sm:py-20 md:grid-cols-[1fr_auto] md:items-center lg:px-16">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#8a2934]">Your next semester starts here</p>
          <h2 className="mt-3 max-w-2xl font-serif text-3xl leading-tight tracking-tight text-[#302a26] sm:text-4xl">A little planning can open up your whole week.</h2>
        </div>
        <Button asChild size="lg" className="h-12 w-fit rounded-full bg-[#761f2b] px-6 text-white hover:bg-[#5d1822]">
          <Link to="/register">Create your schedule <ArrowRight size={17} /></Link>
        </Button>
      </section>

      <footer className="border-t border-[#eae2d9]">
        <div className="mx-auto flex max-w-[1440px] flex-wrap items-center justify-between gap-4 px-5 py-6 text-xs text-[#857b72] sm:px-10 lg:px-16">
          <Link to="/" className="flex items-center gap-2 font-medium text-[#554c44]"><GraduationCap size={17} className="text-[#812b35]" /> Smart University Scheduler</Link>
          <span>Thoughtful planning.More possibilities.</span>
        </div>
      </footer>
    </main>
  )
}

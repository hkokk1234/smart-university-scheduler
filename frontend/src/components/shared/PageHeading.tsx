import type { ReactNode } from 'react'
export function PageHeading({ eyebrow, title, description, action }: { eyebrow?: string; title: string; description: string; action?: ReactNode }) {
  return <div className="flex flex-wrap items-end justify-between gap-4">
    <div>{eyebrow && <p className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-primary">{eyebrow}</p>}<h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h1><p className="mt-2 max-w-2xl text-sm leading-relaxed text-muted-foreground">{description}</p></div>
    {action}
  </div>
}

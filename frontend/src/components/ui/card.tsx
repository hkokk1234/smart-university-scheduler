import type { ComponentProps } from 'react'
import { mergeClassNames } from "@/lib/classNames"
export function Card({ className, ...props }: ComponentProps<'div'>) {
  return <div className={mergeClassNames('rounded-2xl border bg-card text-card-foreground shadow-sm shadow-black/[0.02]', className)} {...props} />
}
export function Badge({ className, ...props }: ComponentProps<'span'>) {
  return <span className={mergeClassNames("inline-flex items-center gap-1 rounded-md bg-secondary px-2 py-1 text-xs font-medium text-secondary-foreground", className)} {...props} />
}
export function Skeleton({ className, ...props }: ComponentProps<'div'>) {
  return <div aria-hidden="true" className={mergeClassNames("animate-pulse rounded-lg bg-muted motion-reduce:animate-none", className)} {...props} />
}

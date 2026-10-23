import type { ComponentProps } from 'react'
import { mergeClassNames } from "@/lib/classNames"

export function Input({ className, ...props }: ComponentProps<'input'>) {
  return <input className={mergeClassNames('flex h-11 w-full rounded-xl border border-input bg-background px-3.5 text-sm outline-none transition-shadow placeholder:text-muted-foreground focus:ring-2 focus:ring-ring/30 disabled:opacity-50', className)} {...props} />
}
export function Select({ className, children, ...props }: ComponentProps<'select'>) {
  return <select className={mergeClassNames('h-11 w-full rounded-xl border border-input bg-background px-3.5 text-sm outline-none focus:ring-2 focus:ring-ring/30', className)} {...props}>{children}</select>
}
export function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
  return <textarea className={mergeClassNames('w-full rounded-xl border border-input bg-background p-3 text-sm outline-none focus:ring-2 focus:ring-ring/30', className)} {...props} />
}

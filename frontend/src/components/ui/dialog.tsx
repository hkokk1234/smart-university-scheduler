import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { mergeClassNames } from "@/lib/classNames"

export function Dialog({ open, onOpenChange, title, description, children, className }: {
  open: boolean; onOpenChange: (value: boolean) => void; title: string; description?: string; children: ReactNode; className?: string
}) {
  return <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-slate-950/45 backdrop-blur-sm" />
      <DialogPrimitive.Content className={mergeClassNames('fixed left-1/2 top-1/2 z-50 max-h-[90dvh] w-[calc(100%_-_2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-auto rounded-2xl border bg-card p-6 text-card-foreground shadow-2xl', className)}>
        <DialogPrimitive.Title className="pr-8 text-xl font-semibold">{title}</DialogPrimitive.Title>
        <DialogPrimitive.Description className={mergeClassNames("mt-2 text-sm leading-relaxed text-muted-foreground", !description && 'sr-only')}>{description ?? title}</DialogPrimitive.Description>
        <DialogPrimitive.Close className="absolute right-4 top-4 rounded-lg p-2 text-muted-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" aria-label="Close dialog"><X size={18} /></DialogPrimitive.Close>
        <div className="mt-6">{children}</div>
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  </DialogPrimitive.Root>
}

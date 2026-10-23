import { AlertCircle, FolderOpen, LoaderCircle } from 'lucide-react'
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { Card, Skeleton } from '@/components/ui/card'

export function LoadingState({ label ='Loading your workspace…' }: { label?: string }) {
  return <div role="status" className="space-y-5 py-4"><div className="flex items-center gap-2 text-sm text-muted-foreground"><LoaderCircle className="animate-spin motion-reduce:animate-none" size={16} />{label}</div><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{[0, 1, 2].map((key) => <Skeleton key={key} className="h-40" />)}</div></div>
}
export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <Card className="flex min-h-64 flex-col items-center justify-center p-8 text-center"><div className="mb-5 rounded-2xl bg-secondary p-4 text-primary"><FolderOpen size={26} /></div><h2 className="text-lg font-semibold">{title}</h2><p className="mt-2 max-w-md text-sm leading-relaxed text-muted-foreground">{description}</p>{action && <div className="mt-6">{action}</div>}</Card>
}
export function ErrorState({ error, retry }: { error: unknown; retry?: () => void }) {
  const message =error instanceof Error ? error.message : "Something went wrong.Please try again."
  return <div role="alert" className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-destructive/20 bg-destructive/5 p-5"><div className="flex gap-3"><AlertCircle className="mt-0.5 shrink-0 text-destructive" size={20} /><div><p className="text-sm font-semibold">We couldn’t complete that request</p><p className="mt-1 text-sm text-muted-foreground">{message}</p></div></div>{retry && <Button variant="outline" onClick={retry}>Try again</Button>}</div>
}

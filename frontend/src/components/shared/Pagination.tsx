import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
export function Pagination({ page, totalPages, totalElements, change }: { page: number; totalPages: number; totalElements: number; change: (page: number) => void }) {
  return <div className="flex flex-wrap items-center justify-between gap-4 border-t pt-5 text-sm"><p className="text-muted-foreground">{totalElements} result{totalElements ===1 ? '' : 's'} · Page {totalPages ===0 ? 0 : page + 1} of {totalPages}</p><div className="flex gap-2"><Button variant="outline" size="sm" disabled={page ===0} onClick={() => change(page - 1)}><ChevronLeft size={16} />Previous</Button><Button variant="outline" size="sm" disabled={page + 1 >=totalPages} onClick={() => change(page + 1)}>Next<ChevronRight size={16} /></Button></div></div>
}

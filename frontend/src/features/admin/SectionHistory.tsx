import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { useSession } from '@/features/auth/sessionStore'
import { ChangeDetails } from '@/features/notifications/ChangeDetails'
import { ErrorState, LoadingState, EmptyState } from '@/components/shared/RequestStates'
import { Pagination } from '@/components/shared/Pagination'

export function SectionHistory({ sectionId }: { sectionId: string }) {
  const user =useSession(state => state.user)
  const [page, setPage] =useState(0)
  const query =useQuery({ queryKey: ['private', user?.id, 'section-history', sectionId, page], queryFn: ({ signal }) => api.sectionHistory(sectionId, page, signal) })
  if (query.isPending) return <LoadingState />
  if (query.isError) return <ErrorState error={query.error} retry={() => void query.refetch()} />
  return <div className="space-y-6">{!query.data.content.length ? <EmptyState title="No changes recorded yet" description="Section edits made after change history was enabled will appear here." /> : query.data.content.map(change => <div key={change.id} className="border-b pb-5"><ChangeDetails change={change} /></div>)}<Pagination page={page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} change={setPage} /></div>
}

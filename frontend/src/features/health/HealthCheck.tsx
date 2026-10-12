import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { checkHealth } from './api'

export function HealthCheck() {
  const [pending, setPending] =useState(false)
  const [message, setMessage] =useState('Check that the backend and database are available.')

  async function check() {
    setPending(true)
    try {
      setMessage(await checkHealth())
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Unable to reach the backend.')
    } finally {
      setPending(false)
    }
  }

  return (
    <section aria-labelledby="health-title" className="space-y-4 rounded-xl border bg-card p-6 text-card-foreground">
      <h2 id="health-title" className="text-lg font-semibold">Environment health</h2>
      <p role="status" aria-live="polite" className="text-sm text-muted-foreground">{message}</p>
      <Button onClick={check} disabled={pending}>{pending ? 'Checking…' : 'Check connection'}</Button>
    </section>
  )
}

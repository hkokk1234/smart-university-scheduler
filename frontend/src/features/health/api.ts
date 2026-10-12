export async function checkHealth(): Promise<string> {
  const response =await fetch('/actuator/health/readiness', {
    signal: AbortSignal.timeout(5000),
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new Error(`Backend is unavailable (HTTP ${response.status}).`)
  const health: unknown =await response.json()
  if (typeof health !=='object' || health ===null || !('status' in health) || health.status !=='UP') {
    throw new Error('Backend is not ready.')
  }
  return 'Backend and database are ready.'
}

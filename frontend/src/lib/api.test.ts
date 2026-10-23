import { afterEach, expect, it, vi } from 'vitest'
import { api, streamNotifications } from './api'
import { useSession } from '@/features/auth/sessionStore'

afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); useSession.getState().clear() })

function pendingFetch() {
  vi.stubGlobal('fetch', vi.fn((_url: string, options: RequestInit) => new Promise((_resolve, reject) => {
    options.signal?.addEventListener('abort', () => reject(options.signal?.reason), { once: true })
  })))
}

it('keeps the request deadline when a query supplies its own cancellation signal', async () => {
  const deadline =new AbortController()
  vi.spyOn(AbortSignal, 'timeout').mockReturnValue(deadline.signal)
  pendingFetch()
  const caller =new AbortController()
  const result =api.courses({}, caller.signal)
  deadline.abort(new DOMException('Timed out', 'TimeoutError'))
  await expect(result).rejects.toMatchObject({ status: 0, message: "The request took too long.Please try again." })
  expect(caller.signal.aborted).toBe(false)
})

it('preserves explicit cancellation without presenting it as a connection failure', async () => {
  pendingFetch()
  const caller =new AbortController()
  const result =api.courses({}, caller.signal)
  caller.abort()
  await expect(result).rejects.toMatchObject({ name: 'AbortError' })
})

it('requests a private repair preview with exclusions and preserves the server change report', async () => {
  useSession.setState({ token: 'test-token' })
  const response ={ originalScheduleId: 'saved-week', status: 'SOLVED', schedules: [], metrics: { complete: true } }
  const fetchMock =vi.fn().mockResolvedValue(new Response(JSON.stringify(response), { status: 200 }))
  vi.stubGlobal('fetch', fetchMock)
  const preferences ={ preferredFreeDays: [], maximumCampusDays: 5, preferredPeriod: 'ANY' as const,
    weights: { gaps: 1, campusDays: 1, preferredTimes: 1, freeDays: 1, balance: 0.2 } }
  await expect(api.repairSchedule('saved-week', ['cancelled-lab'], preferences)).resolves.toEqual(response)
  const [url, options] =fetchMock.mock.calls[0] as [string, RequestInit]
  expect(url).toBe('/api/v1/schedules/saved-week/repair')
  expect(options.method).toBe('POST')
  expect(new Headers(options.headers).get('Authorization')).toBe('Bearer test-token')
  expect(options.credentials).toBe('omit')
  expect(JSON.parse(options.body as string)).toEqual({ excludedSectionIds: ['cancelled-lab'], maxResults: 5, preferences })
})

it('does not fetch a saved schedule when the session has ended', async () => {
  const fetchMock =vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  await expect(api.schedule('saved-week')).rejects.toMatchObject({ status: 401 })
  expect(fetchMock).not.toHaveBeenCalled()
})

it('uploads a private multipart document without overriding the browser boundary', async () => {
  useSession.setState({ token: 'test-admin-token' })
  const fetchMock =vi.fn().mockResolvedValue(new Response(JSON.stringify({ tables: [], warnings: [] })))
  vi.stubGlobal('fetch', fetchMock)
  const file =new File(['test-content'], 'timetable.pdf', { type: 'application/pdf' })
  await api.previewImport(file)
  const [url, options] =fetchMock.mock.calls[0] as [string, RequestInit]
  expect(url).toBe('/api/v1/admin/catalog-import/preview')
  expect(options.body).toBeInstanceOf(FormData)
  expect((options.body as FormData).get('file')).toMatchObject({ name: 'timetable.pdf', size: file.size })
  expect(new Headers(options.headers).has('Content-Type')).toBe(false)
  expect(new Headers(options.headers).get('Authorization')).toBe('Bearer test-admin-token')
})

it('opens live updates using the bearer header and never puts the token in a URL', async () => {
  useSession.setState({ token: 'stream-test-token' })
  const fetchMock =vi.fn().mockResolvedValue(new Response('event: ready\ndata: {}\n\n'))
  vi.stubGlobal('fetch', fetchMock)
  const events: string[] =[]
  await streamNotifications(new AbortController().signal, event => events.push(event.name))
  const [url, options] =fetchMock.mock.calls[0] as [string, RequestInit]
  expect(url).toBe('/api/v1/notifications/stream')
  expect(url).not.toContain('stream-test-token')
  expect(new Headers(options.headers).get('Authorization')).toBe('Bearer stream-test-token')
  expect(options.credentials).toBe('omit')
  expect(events).toEqual(['ready'])
})

it('ends the local session if a stream reconnect receives an expired-token response', async () => {
  useSession.setState({ token: 'expired-token' })
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 401 })))
  await expect(streamNotifications(new AbortController().signal, () => {})).rejects.toMatchObject({ status: 401 })
  expect(useSession.getState().token).toBeNull()
})

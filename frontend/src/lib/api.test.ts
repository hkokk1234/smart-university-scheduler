import { afterEach, expect, it, vi } from 'vitest'
import { api } from './api'

afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals() })

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

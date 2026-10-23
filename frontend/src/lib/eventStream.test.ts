import { expect, it } from 'vitest'
import { consumeEventStream, type StreamEvent } from './eventStream'

function response(chunks: Uint8Array[]) {
  return new Response(new ReadableStream({ start(controller) {
    for (const chunk of chunks) controller.enqueue(chunk)
    controller.close()
  } }))
}
it('decodes fragmented UTF-8, CRLF frames and multi-line data without treating comments as events', async () => {
  const bytes =new TextEncoder().encode(': heartbeat\r\n\r\nevent: ready\r\ndata: Δευτέρα\r\ndata: second line\r\n\r\nevent: catalog-changed\ndata: {"courseId":"id"}\n\n')
  const chunks =Array.from(bytes, byte => Uint8Array.of(byte))
  const events: StreamEvent[] =[]
  await consumeEventStream(response(chunks), new AbortController().signal, event => events.push(event))
  expect(events).toEqual([{ name: 'ready', data: 'Δευτέρα\nsecond line' }, { name: 'catalog-changed', data: '{"courseId":"id"}' }])
})
it('stops reading when the caller leaves the page', async () => {
  const controller =new AbortController()
  let cancelled =false
  const stream =new ReadableStream({ cancel() { cancelled =true } })
  const reading =consumeEventStream(new Response(stream), controller.signal, () => { throw new Error('Unexpected event') })
  controller.abort()
  await reading
  expect(cancelled).toBe(true)
})
it('rejects oversized frames instead of buffering an unbounded message', async () => {
  await expect(consumeEventStream(response([new TextEncoder().encode('data: ' + 'x'.repeat(65537))]),
    new AbortController().signal, () => {})).rejects.toThrow('too large')
})

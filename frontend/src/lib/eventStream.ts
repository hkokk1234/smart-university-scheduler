export interface StreamEvent { name: string; data: string }

/** Consume fragmented UTF-8 SSE frames, including CRLF and multi-line data fields. */
export async function consumeEventStream(response: Response, signal: AbortSignal, onEvent: (event: StreamEvent) => void) {
  if (!response.body) throw new Error('The server returned no event stream.')
  const reader =response.body.getReader()
  const decoder =new TextDecoder()
  let buffer =''
  const abort =() => { void reader.cancel().catch(() => {}) }
  signal.addEventListener('abort', abort, { once: true })
  try {
    while (!signal.aborted) {
      const { done, value } =await reader.read()
      if (done) break
      buffer =(buffer + decoder.decode(value, { stream: true })).replace(/\r\n/g, '\n')
      let boundary: number
      while ((boundary =buffer.indexOf('\n\n')) >=0) {
        const frame =buffer.slice(0, boundary); buffer =buffer.slice(boundary + 2)
        if (frame.length > 65536) throw new Error('The event stream frame is too large.')
        let name ='message'
        const data: string[] =[]
        for (const line of frame.split('\n')) {
          if (line.startsWith('event:')) name =line.slice(6).trim()
          if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
        }
        if (data.length && !signal.aborted) onEvent({ name, data: data.join('\n') })
      }
      if (buffer.length > 65536) throw new Error('The event stream frame is too large.')
    }
  } finally {
    signal.removeEventListener('abort', abort)
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}

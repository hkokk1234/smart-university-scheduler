import { useEffect, useState } from 'react'
// Wait until the value stops changing before updating search results.
export function useDebouncedValue<T>(value: T, delayMilliseconds =300) {
  const [debouncedValue, setDebouncedValue] =useState(value)
  useEffect(() => { const timeoutId =setTimeout(() => setDebouncedValue(value), delayMilliseconds); return () => clearTimeout(timeoutId) }, [value, delayMilliseconds])
  return debouncedValue
}

import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

// Resolve conflicting Tailwind utilities after combining conditional class names.
export function mergeClassNames(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

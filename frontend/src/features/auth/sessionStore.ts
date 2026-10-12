import { create } from 'zustand'
import type { AuthResponse, User } from '@/lib/types'

interface Session {
  token: string | null
  user: User | null
  expiresAt: number | null
  start: (response: AuthResponse) => void
  clear: () => void
}
// Deliberately memory-only: passwords and JWTs are never written to browser storage.
export const useSession =create<Session>((set) => ({
  token: null, user: null, expiresAt: null,
  start: (response) => set({ token: response.accessToken, user: response.user, expiresAt: Date.now() + response.expiresIn * 1000 }),
  clear: () => set({ token: null, user: null, expiresAt: null }),
}))

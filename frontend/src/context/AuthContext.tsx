import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../api/client'
import type { AuthResponse, User } from '../types'

type AuthCtx = {
  user: User | null
  token: string | null
  login: (email: string, password: string) => Promise<void>
  register: (data: {name:string;email:string;password:string;role?:string;inviteCode?:string;acceptTerms?:boolean;acceptPrivacy?:boolean}) => Promise<void>
  logout: () => void
}

const Ctx = createContext<AuthCtx>(null!)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const raw = localStorage.getItem('user'); return raw ? JSON.parse(raw) : null
  })
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'))

  const apply = (res: AuthResponse) => {
    setToken(res.token); setUser(res.user)
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(res.user))
  }

  const login = async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
    apply(data)
  }
  const register = async (payload: any) => {
    const { data } = await api.post<AuthResponse>('/auth/register', payload)
    apply(data)
  }
  const logout = () => { setUser(null); setToken(null); localStorage.clear() }

  useEffect(() => { if (token) api.get('/auth/me').catch(() => logout()) }, [])

  return <Ctx.Provider value={{ user, token, login, register, logout }}>{children}</Ctx.Provider>
}
export const useAuth = () => useContext(Ctx)

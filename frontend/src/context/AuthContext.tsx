import { createContext, useContext, useState, ReactNode } from 'react'
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/client'

interface AuthState {
  email: string | null
  fullName: string | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, fullName: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [email, setEmail] = useState<string | null>(localStorage.getItem('email'))
  const [fullName, setFullName] = useState<string | null>(localStorage.getItem('fullName'))

  function persist(accessToken: string, refreshToken: string, userEmail: string, userFullName: string) {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('email', userEmail)
    localStorage.setItem('fullName', userFullName)
    setEmail(userEmail)
    setFullName(userFullName)
  }

  async function login(loginEmail: string, password: string) {
    const response = await apiLogin(loginEmail, password)
    persist(response.accessToken, response.refreshToken, response.email, response.fullName)
  }

  async function register(registerEmail: string, password: string, userFullName: string) {
    const response = await apiRegister(registerEmail, password, userFullName)
    persist(response.accessToken, response.refreshToken, response.email, response.fullName)
  }

  async function logout() {
    await apiLogout()
    setEmail(null)
    setFullName(null)
  }

  return (
    <AuthContext.Provider value={{ email, fullName, isAuthenticated: !!email, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}

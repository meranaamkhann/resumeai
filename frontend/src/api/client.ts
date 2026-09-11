const API_BASE = '/api'

function getAccessToken(): string | null {
  return localStorage.getItem('accessToken')
}

function getRefreshToken(): string | null {
  return localStorage.getItem('refreshToken')
}

function storeTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
}

function clearTokens() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('email')
  localStorage.removeItem('fullName')
}

let refreshInFlight: Promise<boolean> | null = null

async function tryRefreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await fetch(`${API_BASE}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        })
        if (!response.ok) {
          clearTokens()
          return false
        }
        const data = await response.json()
        storeTokens(data.accessToken, data.refreshToken)
        return true
      } catch {
        return false
      } finally {
        refreshInFlight = null
      }
    })()
  }
  return refreshInFlight
}

async function request<T>(path: string, options: RequestInit = {}, isRetry = false): Promise<T> {
  const token = getAccessToken()
  const headers: Record<string, string> = {
    ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers as Record<string, string> | undefined)
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })

  if (response.status === 401 && !isRetry && !path.startsWith('/auth/')) {
    const refreshed = await tryRefreshAccessToken()
    if (refreshed) {
      return request<T>(path, options, true)
    }
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(body.message || `Request failed with status ${response.status}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  email: string
  fullName: string
}

export function register(email: string, password: string, fullName: string) {
  return request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, fullName })
  })
}

export function login(email: string, password: string) {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  })
}

export async function logout() {
  const refreshToken = getRefreshToken()
  if (refreshToken) {
    try {
      await fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      })
    } catch {
      clearTokens()
    }
  }
  clearTokens()
}

export { storeTokens }

export interface ResumeUploadResponse {
  resumeId: string
  documentId: string
  status: string
  parsingConfidence: number
  parsingConfidenceWarning: boolean
}

export function uploadResume(file: File, label?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (label) formData.append('label', label)
  return request<ResumeUploadResponse>('/resumes/upload', { method: 'POST', body: formData })
}

export function deleteResume(resumeId: string) {
  return request<void>(`/resumes/${resumeId}`, { method: 'DELETE' })
}

export interface IssueDto {
  category: string
  severity: string
  title: string
  explanation: string
  recommendation: string
}

export interface AnalysisResponse {
  analysisId: string
  documentId: string
  overallScore: number
  categoryScores: {
    atsCompatibility: number
    keywordAlignment: number
    structure: number
    contentQuality: number
    experienceRelevance: number
    impact: number
    formatting: number
  }
  issues: IssueDto[]
  detectedSections: string[]
  disclaimer: string
  createdAt: string
}

export function analyzeDocument(documentId: string) {
  return request<AnalysisResponse>(`/analysis/documents/${documentId}`, { method: 'POST' })
}

export function getLatestAnalysis(documentId: string) {
  return request<AnalysisResponse>(`/analysis/documents/${documentId}/latest`)
}

export interface DashboardResponse {
  latestOverallScore: number | null
  resumeCount: number
  analysisCount: number
  topImprovementAreas: string[]
  recentAnalyses: { analysisId: string; resumeId: string; documentId: string; overallScore: number; createdAt: string }[]
}

export function getDashboard() {
  return request<DashboardResponse>('/dashboard')
}

export interface JobDescriptionResponse {
  id: string
  jobTitle: string | null
  seniority: string | null
  minYearsExperience: number | null
  requiredSkills: string[]
  preferredSkills: string[]
}

export function submitJobDescription(rawText: string) {
  return request<JobDescriptionResponse>('/job-descriptions', {
    method: 'POST',
    body: JSON.stringify({ rawText })
  })
}

export interface JobMatchResponse {
  matchId: string
  documentId: string
  jobDescriptionId: string
  matchScore: number
  requiredSkills: { matched: number; total: number; matchedSkills: string[]; missingSkills: string[] }
  preferredSkills: { matched: number; total: number; matchedSkills: string[]; missingSkills: string[] }
  experienceMatchLabel: string
  disclaimer: string
  createdAt: string
}

export function runJobMatch(documentId: string, jobDescriptionId: string) {
  return request<JobMatchResponse>(`/job-match/${documentId}/${jobDescriptionId}`, { method: 'POST' })
}

export interface BulletRewriteResponse {
  originalBullet: string
  rewrittenBullet: string
  factualConsistencyPassed: boolean
  factualConsistencyNote: string | null
}

export function rewriteBullet(bulletText: string, mode: string) {
  return request<BulletRewriteResponse>('/bullets/rewrite', {
    method: 'POST',
    body: JSON.stringify({ bulletText, mode })
  })
}

export interface RecruiterViewResponse {
  firstImpression: string
  strengths: string[]
  concerns: string[]
  label: string
}

export function getRecruiterView(documentId: string) {
  return request<RecruiterViewResponse>(`/recruiter-view/${documentId}`)
}

export interface ApplicationResponse {
  id: string
  company: string
  role: string
  jobUrl: string | null
  location: string | null
  applicationDate: string | null
  resumeVersionId: string | null
  status: string
  notes: string | null
  createdAt: string
  updatedAt: string
}

export function listApplications() {
  return request<ApplicationResponse[]>('/applications')
}

export function createApplication(data: Partial<ApplicationResponse>) {
  return request<ApplicationResponse>('/applications', { method: 'POST', body: JSON.stringify(data) })
}

export function updateApplication(id: string, data: Partial<ApplicationResponse>) {
  return request<ApplicationResponse>(`/applications/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export function deleteApplication(id: string) {
  return request<void>(`/applications/${id}`, { method: 'DELETE' })
}

export async function downloadExport(documentId: string, format: 'pdf' | 'docx') {
  const token = localStorage.getItem('accessToken')
  const response = await fetch(`/api/export/${documentId}.${format}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) throw new Error('Export failed')
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `resume-ats-safe.${format}`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export function verifyEmail(token: string) {
  return request<void>(`/auth/verify-email?token=${encodeURIComponent(token)}`)
}

export function requestPasswordReset(email: string) {
  return request<void>('/auth/request-password-reset', {
    method: 'POST',
    body: JSON.stringify({ email })
  })
}

export function resetPassword(token: string, newPassword: string) {
  return request<void>('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword })
  })
}

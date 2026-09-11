import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyEmail } from '../api/client'

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const [status, setStatus] = useState<'checking' | 'success' | 'error'>('checking')
  const [error, setError] = useState('')

  useEffect(() => {
    const token = searchParams.get('token')
    if (!token) {
      setStatus('error')
      setError('No verification token was provided.')
      return
    }
    verifyEmail(token)
      .then(() => setStatus('success'))
      .catch(err => {
        setStatus('error')
        setError(err instanceof Error ? err.message : 'Verification failed')
      })
  }, [searchParams])

  return (
    <div className="auth-page">
      <div className="auth-form" role="status" aria-live="polite">
        <h1>Email verification</h1>
        {status === 'checking' && <p>Verifying your email...</p>}
        {status === 'success' && (
          <>
            <p className="status">Your email has been verified.</p>
            <Link to="/dashboard">Go to dashboard</Link>
          </>
        )}
        {status === 'error' && <p className="error">{error}</p>}
      </div>
    </div>
  )
}

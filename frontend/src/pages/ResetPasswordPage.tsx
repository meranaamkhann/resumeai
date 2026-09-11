import { FormEvent, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../api/client'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const token = searchParams.get('token')
    if (!token) {
      setError('No reset token was provided.')
      return
    }
    setError('')
    setLoading(true)
    try {
      await resetPassword(token, password)
      setDone(true)
      setTimeout(() => navigate('/login'), 2000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Reset failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form onSubmit={handleSubmit} className="auth-form">
        <h1>Set a new password</h1>
        {error && <p className="error" role="alert">{error}</p>}
        {done ? (
          <p className="status" role="status">Password updated. Redirecting to log in...</p>
        ) : (
          <>
            <label>
              New password (min 10 characters)
              <input type="password" minLength={10} value={password} onChange={e => setPassword(e.target.value)} required />
            </label>
            <button type="submit" disabled={loading}>{loading ? 'Saving...' : 'Save new password'}</button>
          </>
        )}
        <p><Link to="/login">Back to log in</Link></p>
      </form>
    </div>
  )
}

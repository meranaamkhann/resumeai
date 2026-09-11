import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '../api/client'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await requestPasswordReset(email)
      setSubmitted(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form onSubmit={handleSubmit} className="auth-form">
        <h1>Reset password</h1>
        {error && <p className="error" role="alert">{error}</p>}
        {submitted ? (
          <p className="status" role="status">
            If an account exists for that email, a reset link has been sent.
          </p>
        ) : (
          <>
            <label>
              Email
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
            </label>
            <button type="submit" disabled={loading}>{loading ? 'Sending...' : 'Send reset link'}</button>
          </>
        )}
        <p><Link to="/login">Back to log in</Link></p>
      </form>
    </div>
  )
}

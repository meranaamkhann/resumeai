import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { submitJobDescription, runJobMatch, JobMatchResponse } from '../api/client'

export default function JobMatchPage() {
  const { documentId } = useParams<{ documentId: string }>()
  const [jdText, setJdText] = useState('')
  const [match, setMatch] = useState<JobMatchResponse | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleMatch() {
    if (!documentId || !jdText.trim()) return
    setBusy(true)
    setError('')
    try {
      const jd = await submitJobDescription(jdText)
      const result = await runJobMatch(documentId, jd.id)
      setMatch(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Job match failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <h1>Job Match</h1>
      <p>Paste a job description to see how this resume compares.</p>
      <textarea
        className="jd-textarea"
        rows={10}
        value={jdText}
        onChange={e => setJdText(e.target.value)}
        placeholder="Paste the full job description here..."
      />
      <button onClick={handleMatch} disabled={busy || !jdText.trim()}>
        {busy ? 'Matching...' : 'Run Job Match'}
      </button>
      {error && <p className="error">{error}</p>}

      {match && (
        <div className="match-result">
          <div className="overall-score">
            <span className="score-number">{match.matchScore}</span>
            <span>%</span>
          </div>
          <p className="disclaimer">{match.disclaimer}</p>

          <h2>Required skills</h2>
          <p>{match.requiredSkills.matched}/{match.requiredSkills.total} matched</p>
          {match.requiredSkills.missingSkills.length > 0 && (
            <p>Missing: {match.requiredSkills.missingSkills.join(', ')}</p>
          )}

          <h2>Preferred skills</h2>
          <p>{match.preferredSkills.matched}/{match.preferredSkills.total} matched</p>
          {match.preferredSkills.missingSkills.length > 0 && (
            <p>Missing: {match.preferredSkills.missingSkills.join(', ')}</p>
          )}

          <h2>Experience match</h2>
          <p>{match.experienceMatchLabel}</p>
        </div>
      )}
    </div>
  )
}

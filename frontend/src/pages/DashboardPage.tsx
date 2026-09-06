import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDashboard, deleteResume, DashboardResponse } from '../api/client'

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState('')
  const [deletingId, setDeletingId] = useState<string | null>(null)

  function load() {
    getDashboard().then(setDashboard).catch(err => setError(err.message))
  }

  useEffect(() => {
    load()
  }, [])

  async function handleDelete(resumeId: string) {
    if (!confirm('Delete this resume and its analyses? This cannot be undone.')) return
    setDeletingId(resumeId)
    setError('')
    try {
      await deleteResume(resumeId)
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed')
    } finally {
      setDeletingId(null)
    }
  }

  if (error && !dashboard) return <div className="page"><p className="error">{error}</p></div>
  if (!dashboard) return <div className="page"><p>Loading...</p></div>

  return (
    <div className="page">
      <h1>Dashboard</h1>
      {error && <p className="error">{error}</p>}
      <div className="dashboard-cards">
        <div className="dashboard-card">
          <span>Resume Score</span>
          <strong>{dashboard.latestOverallScore ?? '—'}</strong>
        </div>
        <div className="dashboard-card">
          <span>Resumes uploaded</span>
          <strong>{dashboard.resumeCount}</strong>
        </div>
        <div className="dashboard-card">
          <span>Analyses run</span>
          <strong>{dashboard.analysisCount}</strong>
        </div>
      </div>

      {dashboard.topImprovementAreas.length > 0 && (
        <>
          <h2>Top improvement areas</h2>
          <ul>
            {dashboard.topImprovementAreas.map(area => <li key={area}>{area}</li>)}
          </ul>
        </>
      )}

      <h2>Recent analyses</h2>
      {dashboard.recentAnalyses.length === 0 ? (
        <p>No analyses yet. <Link to="/upload">Upload a resume</Link> to get started.</p>
      ) : (
        <ul className="resume-list">
          {dashboard.recentAnalyses.map(a => (
            <li key={a.analysisId} className="resume-list-item">
              <Link to={`/analysis/${a.documentId}`}>Score {a.overallScore}/100 — {new Date(a.createdAt).toLocaleString()}</Link>
              <button onClick={() => handleDelete(a.resumeId)} disabled={deletingId === a.resumeId}>
                {deletingId === a.resumeId ? 'Deleting...' : 'Delete'}
              </button>
            </li>
          ))}
        </ul>
      )}

      <Link to="/upload" className="button-link">Upload another resume</Link>
    </div>
  )
}

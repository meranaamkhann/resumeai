import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  AnalysisResponse, getLatestAnalysis, getRecruiterView, RecruiterViewResponse,
  rewriteBullet, BulletRewriteResponse, downloadExport
} from '../api/client'

const severityLabel: Record<string, string> = {
  CRITICAL: 'Critical', HIGH: 'High', MEDIUM: 'Medium', LOW: 'Low', GOOD: 'Good'
}

const REWRITE_MODES = [
  ['ATS_OPTIMIZED', 'ATS Optimized'],
  ['RECRUITER_FRIENDLY', 'Recruiter Friendly'],
  ['CONCISE', 'Concise'],
  ['TECHNICAL', 'Technical'],
  ['ACHIEVEMENT_FOCUSED', 'Achievement Focused']
] as const

export default function AnalysisPage() {
  const { documentId } = useParams<{ documentId: string }>()
  const [analysis, setAnalysis] = useState<AnalysisResponse | null>(null)
  const [error, setError] = useState('')
  const [recruiterView, setRecruiterView] = useState<RecruiterViewResponse | null>(null)
  const [bulletInput, setBulletInput] = useState('')
  const [rewriteMode, setRewriteMode] = useState('ATS_OPTIMIZED')
  const [rewriteResult, setRewriteResult] = useState<BulletRewriteResponse | null>(null)
  const [rewriteError, setRewriteError] = useState('')

  useEffect(() => {
    if (!documentId) return
    getLatestAnalysis(documentId).then(setAnalysis).catch(err => setError(err.message))
  }, [documentId])

  async function handleShowRecruiterView() {
    if (!documentId) return
    try {
      setRecruiterView(await getRecruiterView(documentId))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load recruiter view')
    }
  }

  async function handleRewrite() {
    if (!bulletInput.trim()) return
    setRewriteError('')
    setRewriteResult(null)
    try {
      setRewriteResult(await rewriteBullet(bulletInput, rewriteMode))
    } catch (err) {
      setRewriteError(err instanceof Error ? err.message : 'Rewrite failed')
    }
  }

  async function handleExport(format: 'pdf' | 'docx') {
    if (!documentId) return
    try {
      await downloadExport(documentId, format)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed')
    }
  }

  if (error) return <div className="page"><p className="error">{error}</p></div>
  if (!analysis) return <div className="page"><p>Loading analysis...</p></div>

  const categories = [
    ['ATS Compatibility', analysis.categoryScores.atsCompatibility],
    ['Keyword Alignment', analysis.categoryScores.keywordAlignment],
    ['Structure', analysis.categoryScores.structure],
    ['Content Quality', analysis.categoryScores.contentQuality],
    ['Experience Relevance', analysis.categoryScores.experienceRelevance],
    ['Impact', analysis.categoryScores.impact],
    ['Formatting', analysis.categoryScores.formatting]
  ] as const

  return (
    <div className="page">
      <h1>Resume Analysis</h1>
      <div className="overall-score">
        <span className="score-number">{analysis.overallScore}</span>
        <span>/100</span>
      </div>
      <p className="disclaimer">{analysis.disclaimer}</p>

      <div className="action-row">
        <Link to={`/job-match/${documentId}`} className="button-link">Match against a job</Link>
        <button onClick={handleShowRecruiterView}>Recruiter view</button>
        <button onClick={() => handleExport('pdf')}>Export PDF</button>
        <button onClick={() => handleExport('docx')}>Export DOCX</button>
      </div>

      {recruiterView && (
        <div className="recruiter-view">
          <h2>Recruiter view</h2>
          <p className="disclaimer">{recruiterView.label}</p>
          <p>{recruiterView.firstImpression}</p>
          <strong>Strengths</strong>
          <ul>{recruiterView.strengths.map((s, i) => <li key={i}>{s}</li>)}</ul>
          <strong>Concerns</strong>
          <ul>{recruiterView.concerns.map((c, i) => <li key={i}>{c}</li>)}</ul>
        </div>
      )}

      <h2>Score breakdown</h2>
      <div className="score-grid">
        {categories.map(([label, value]) => (
          <div key={label} className="score-card">
            <span>{label}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>

      <h2>Detected sections</h2>
      <p>{analysis.detectedSections.length > 0 ? analysis.detectedSections.join(', ') : 'None confidently detected'}</p>

      <h2>Issues & recommendations</h2>
      <ul className="issue-list">
        {analysis.issues.map((issue, i) => (
          <li key={i} className={`issue issue-${issue.severity.toLowerCase()}`}>
            <span className="issue-severity">{severityLabel[issue.severity] ?? issue.severity}</span>
            <strong>{issue.title}</strong>
            <p>{issue.explanation}</p>
            <p className="recommendation">{issue.recommendation}</p>
          </li>
        ))}
      </ul>

      <h2>Rewrite a bullet</h2>
      <textarea
        className="jd-textarea"
        rows={3}
        value={bulletInput}
        onChange={e => setBulletInput(e.target.value)}
        placeholder="Paste a resume bullet to rewrite..."
      />
      <select value={rewriteMode} onChange={e => setRewriteMode(e.target.value)}>
        {REWRITE_MODES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select>
      <button onClick={handleRewrite} disabled={!bulletInput.trim()}>Rewrite</button>
      {rewriteError && <p className="error">{rewriteError}</p>}
      {rewriteResult && (
        <div className="rewrite-result">
          {rewriteResult.factualConsistencyPassed ? (
            <p>{rewriteResult.rewrittenBullet}</p>
          ) : (
            <p className="error">Rewrite rejected: {rewriteResult.factualConsistencyNote}</p>
          )}
        </div>
      )}
    </div>
  )
}

import { ChangeEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { uploadResume, analyzeDocument, getLatestAnalysis } from '../api/client'

export default function UploadPage() {
  const navigate = useNavigate()
  const [file, setFile] = useState<File | null>(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    setFile(e.target.files?.[0] ?? null)
    setError('')
  }

  async function handleUpload() {
    if (!file) return
    setBusy(true)
    setError('')
    try {
      setStatus('Uploading...')
      const uploadResult = await uploadResume(file)

      if (uploadResult.status === 'DUPLICATE_OF_EXISTING') {
        setStatus('You already uploaded this exact file. Showing the existing analysis.')
        try {
          await getLatestAnalysis(uploadResult.documentId)
          navigate(`/analysis/${uploadResult.documentId}`)
          return
        } catch {
          setStatus('Re-running analysis on your previous upload...')
        }
      } else if (uploadResult.parsingConfidenceWarning) {
        setStatus(`Uploaded, but parsing confidence is low (${uploadResult.parsingConfidence}%). Some recommendations may be inaccurate.`)
      } else {
        setStatus('Parsed successfully. Running analysis...')
      }

      const analysis = await analyzeDocument(uploadResult.documentId)
      navigate(`/analysis/${analysis.documentId}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
      setStatus('')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <h1>Upload your resume</h1>
      <p>Supported formats: PDF, DOCX, TXT. Max size 10MB.</p>
      <input type="file" accept=".pdf,.docx,.txt" onChange={handleFileChange} />
      <button onClick={handleUpload} disabled={!file || busy}>
        {busy ? 'Processing...' : 'Upload & Analyze'}
      </button>
      {status && <p className="status">{status}</p>}
      {error && <p className="error">{error}</p>}
    </div>
  )
}

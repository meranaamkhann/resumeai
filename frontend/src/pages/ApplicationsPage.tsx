import { FormEvent, useEffect, useState } from 'react'
import { listApplications, createApplication, updateApplication, deleteApplication, ApplicationResponse } from '../api/client'

const STATUSES = ['SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN']

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [company, setCompany] = useState('')
  const [role, setRole] = useState('')
  const [error, setError] = useState('')

  function load() {
    listApplications().then(setApplications).catch(err => setError(err.message))
  }

  useEffect(() => {
    load()
  }, [])

  async function handleAdd(e: FormEvent) {
    e.preventDefault()
    if (!company.trim() || !role.trim()) return
    try {
      await createApplication({ company, role, status: 'SAVED' })
      setCompany('')
      setRole('')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not add application')
    }
  }

  async function handleStatusChange(app: ApplicationResponse, status: string) {
    try {
      await updateApplication(app.id, { ...app, status })
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not update application')
    }
  }

  async function handleDelete(id: string) {
    try {
      await deleteApplication(id)
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete application')
    }
  }

  return (
    <div className="page">
      <h1>Applications</h1>
      {error && <p className="error">{error}</p>}

      <form onSubmit={handleAdd} className="inline-form">
        <input placeholder="Company" value={company} onChange={e => setCompany(e.target.value)} />
        <input placeholder="Role" value={role} onChange={e => setRole(e.target.value)} />
        <button type="submit">Add</button>
      </form>

      <ul className="resume-list">
        {applications.map(app => (
          <li key={app.id} className="resume-list-item">
            <div>
              <strong>{app.company}</strong> — {app.role}
            </div>
            <div className="app-controls">
              <select value={app.status} onChange={e => handleStatusChange(app, e.target.value)}>
                {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
              <button onClick={() => handleDelete(app.id)}>Delete</button>
            </div>
          </li>
        ))}
      </ul>
      {applications.length === 0 && <p>No applications tracked yet.</p>}
    </div>
  )
}

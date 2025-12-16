import React, { useState } from 'react'
import { Routes, Route, Link } from 'react-router-dom'

import StudentLookup from './StudentLookup'

export default function App() {
  return (
    <div className="page">
      <h1>ScholarSync</h1>

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/professor" element={<ProfessorView />} />
        <Route path="/student" element={<StudentLookup />} />
      </Routes>
    </div>
  )
}

function Home() {
  return (
    <div>
      <h2>Welcome</h2>
      <p>This is the ScholarSync landing page. Use the specific URLs to access student or professor pages.</p>
    </div>
  )
}

function ProfessorView() {
  const [courseId, setCourseId] = useState(1)
  const [file, setFile] = useState(null)
  const [status, setStatus] = useState('')
  const [keyInput, setKeyInput] = useState('')
  const [authorized, setAuthorized] = useState(false)
  const [authError, setAuthError] = useState('')

  const checkKey = async () => {
    setAuthError('')
    try {
      const res = await fetch('http://localhost:8080/api/auth/professor/check', { headers: { 'X-Professor-Key': keyInput } })
      if (!res.ok) {
        setAuthError('Invalid key')
      } else {
        sessionStorage.setItem('profKey', keyInput)
        setAuthorized(true)
      }
    } catch (err) {
      setAuthError('Network error')
    }
  }

  const upload = async () => {
    if (!file) return setStatus('Please choose a file')
    setStatus('Uploading...')
    const fd = new FormData()
    fd.append('file', file, file.name)
    const headers = {}
    const stored = sessionStorage.getItem('profKey')
    if (stored) headers['X-Professor-Key'] = stored
    try {
      const res = await fetch(`http://localhost:8080/api/groups/import?courseId=${encodeURIComponent(courseId)}`, {
        method: 'POST', body: fd, headers
      })
      const txt = await res.text()
      if (!res.ok) {
        setStatus(`Error ${res.status}: ${txt}`)
      } else {
        try { setStatus(JSON.stringify(JSON.parse(txt), null, 2)) }
        catch(e) { setStatus(txt) }
      }
    } catch (err) {
      setStatus('Network error: ' + err.message)
    }
  }

  if (!authorized) {
    return (
      <div>
        <h2>Professor Login</h2>
        <div>Enter the professor key to access the import form.</div>
        <input value={keyInput} onChange={e => setKeyInput(e.target.value)} />
        <div style={{marginTop:8}}>
          <button onClick={checkKey}>Unlock</button>
        </div>
        <div style={{color:'red', marginTop:8}}>{authError}</div>
      </div>
    )
  }

  return (
    <div>
      <h2>Group Import</h2>
      <label>Course ID</label>
      <input type="number" value={courseId} onChange={e => setCourseId(e.target.value)} />

      <label>Excel file (.xlsx)</label>
      <input type="file" accept=".xlsx" onChange={e => setFile(e.target.files[0])} />

      <div style={{marginTop: '1rem'}}>
        <button onClick={upload}>Upload and Import</button>
      </div>

      <pre className="result">{status}</pre>
    </div>
  )
}

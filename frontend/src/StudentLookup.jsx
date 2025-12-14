import React, { useState, useEffect, useRef } from 'react'

export default function StudentLookup() {
  const [studentId, setStudentId] = useState('')
  const [courseId, setCourseId] = useState(1)
  const [status, setStatus] = useState('')
  const [groupName, setGroupName] = useState('')
  const [members, setMembers] = useState([])
  const [available, setAvailable] = useState([])
  const [search, setSearch] = useState('')
  const [studentObj, setStudentObj] = useState(null)
  const [groupObj, setGroupObj] = useState(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createStatus, setCreateStatus] = useState('')
  const searchTimer = useRef(null)

  // Student lookup removed — the student enters their Student ID inside the Create Group modal instead.
  // My Group will be displayed after creating a group (or if you manually fetch via the API).


  const fetchSuggestions = async (q) => {
    try {
      const res = await fetch(`http://localhost:8080/api/students?courseId=${encodeURIComponent(courseId)}&q=${encodeURIComponent(q||'')}`)
      if (res.ok) {
        const json = await res.json()
        setAvailable(json)
      }
    } catch (err) {
      console.error('suggestions error', err)
    }
  }

  useEffect(() => {
    if (searchTimer.current) clearTimeout(searchTimer.current)
    searchTimer.current = setTimeout(() => fetchSuggestions(search), 300)
    return () => clearTimeout(searchTimer.current)
  }, [search, courseId])

  const createGroup = async () => {
    // The first member in the members list becomes the group leader
    const leader = members.length > 0 ? members[0] : ''
    const memberIds = [...new Set(members)]
    if (!groupName) return setCreateStatus('Group name required')
    if (!leader) return setCreateStatus('You must add at least one member and include yourself')
    if (memberIds.length === 0) return setCreateStatus('At least one member is required')
    setCreateStatus('Creating group...')
    try {
      const res = await fetch('http://localhost:8080/api/groups/manual', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ groupName, leaderStudentId: leader, courseId: Number(courseId), memberStudentIds: memberIds })
      })
      const txt = await res.text()
      if (!res.ok) {
        setCreateStatus(`Error ${res.status}: ${txt}`)
      } else {
        try {
          const created = JSON.parse(txt)
          setCreateStatus(JSON.stringify(created, null, 2));
          // set the local My Group display to the new group and student
          setGroupName(''); setMembers([]); setAvailable([]); setSearch(''); setShowCreateModal(false);
          setStudentObj({ studentId: leader })
          setGroupObj(created)
        } catch(e) { setCreateStatus(txt) }
      }
    } catch (err) {
      setCreateStatus('Network error: ' + err.message)
    }
  }

  return (
    <div>
      <h2>Student</h2>
      <p>Use the <strong>Create Group</strong> button below to create a group. Add yourself to the members list in the modal (the first member added will become the leader). After creating a group it will appear in the My Group section.</p>

      <hr style={{margin: '1.5rem 0'}} />

      <h2>My Group</h2>
      {studentObj ? (
        groupObj ? (
          <div style={{border:'1px solid #ddd', padding: 12}}>
            <div><strong>{groupObj.groupName}</strong></div>
            <div>Leader: {groupObj.leaderStudentId}</div>
            <div>Members:</div>
            <ul>
              {groupObj.memberStudentIds.map(m => <li key={m}>{m}</li>)}
            </ul>
          </div>
        ) : (
          <div style={{color:'#666'}}>You are not currently in a group for this course.</div>
        )
      ) : (
        <div style={{color:'#666'}}>Create a group (click "Create Group") to see it here. Add yourself to the members list in the modal.</div>
      )}

      <div style={{marginTop: 12}}>
        <button onClick={() => setShowCreateModal(true)}>Create Group</button>
      </div>

      {showCreateModal && (
        <div style={{position:'fixed', left:0, top:0, right:0, bottom:0, background:'rgba(0,0,0,0.4)', display:'flex', alignItems:'center', justifyContent:'center'}}>
          <div style={{background:'#fff', padding:20, width:600, maxWidth:'95%', borderRadius:6}}>
            <h3>Create Group</h3>
            <label style={{marginTop:8}}>Course ID</label>
            <input type="number" value={courseId} onChange={e => setCourseId(e.target.value)} style={{width:'100%'}} />

            <label style={{marginTop:8}}>Group Name</label>
            <input value={groupName} onChange={e => setGroupName(e.target.value)} style={{width:'100%'}} />

            <div style={{fontSize:12, color:'#666', marginTop:6}}>Add yourself to the members list (search and click); the first member added will become the group leader.</div>

            <label style={{marginTop:8}}>Members (search and click to add)</label>
            <input placeholder="Search students by id, name, or email" value={search} onChange={e => setSearch(e.target.value)} style={{width: '100%'}} />
            <div style={{border: '1px solid #ddd', maxHeight: 200, overflow: 'auto', marginTop: 6}}>
              {available.map(s => (
                <div key={s.studentId} style={{padding: 6, cursor: 'pointer'}} onClick={() => { if (!members.includes(s.studentId)) setMembers([...members, s.studentId]) }}>
                  {s.studentId} — {s.firstName} {s.lastName} <span style={{color:'#666'}}>({s.email})</span>
                </div>
              ))}
            </div>
            <div style={{marginTop: 8}}>
              {members.map(m => (
                <span key={m} style={{display:'inline-block', padding:'4px 8px', margin:4, border:'1px solid #ccc', borderRadius:8}}>
                  {m} <button style={{marginLeft:8}} onClick={() => setMembers(members.filter(x => x !== m))}>x</button>
                </span>
              ))}
            </div>

            <div style={{marginTop: 12, display:'flex', gap:8}}>
              <button onClick={createGroup}>Create</button>
              <button onClick={() => setShowCreateModal(false)}>Cancel</button>
            </div>
            <pre className="result">{createStatus}</pre>
          </div>
        </div>
      )}
    </div>
  )
}

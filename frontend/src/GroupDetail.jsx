import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'

export default function GroupDetail({ user }) {
  const { groupId } = useParams()
  const navigate = useNavigate()
  const [group, setGroup] = useState(null)
  const [tasks, setTasks] = useState([])
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [showTaskModal, setShowTaskModal] = useState(false)
  const [editingTask, setEditingTask] = useState(null)
  const [isMember, setIsMember] = useState(false)
  
  // Task form state
  const [taskTitle, setTaskTitle] = useState('')
  const [taskDesc, setTaskDesc] = useState('')
  const [taskStart, setTaskStart] = useState('')
  const [taskEnd, setTaskEnd] = useState('')
  const [taskProgress, setTaskProgress] = useState(0)

  useEffect(() => {
    if (groupId) {
      fetchGroupDetails()
    }
  }, [groupId])

  const fetchGroupDetails = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/groups/${groupId}`, {
        credentials: 'include'
      })
      if (response.ok) {
        const data = await response.json()
        setGroup(data)
        
        // Check if current user is a member
        const userInstitutionalId = user?.institutionalId
        if (userInstitutionalId) {
          const memberIds = data.members?.map(m => m.institutionalId) || []
          const isGroupMember = memberIds.includes(userInstitutionalId) || 
                              data.leader?.institutionalId === userInstitutionalId
          setIsMember(isGroupMember)
        }
        
        fetchTasks()
        fetchLogs()
      }
    } catch (err) {
      console.error('Error fetching group details:', err)
    } finally {
      setLoading(false)
    }
  }

  const fetchTasks = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/groups/${groupId}/tasks`, {
        credentials: 'include'
      })
      if (response.ok) {
        const data = await response.json()
        setTasks(data)
      }
    } catch (err) {
      console.error('Error fetching tasks:', err)
    }
  }

  const fetchLogs = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/groups/${groupId}/tasks/logs`, {
        credentials: 'include'
      })
      if (response.ok) {
        const data = await response.json()
        setLogs(data)
      }
    } catch (err) {
      console.error('Error fetching logs:', err)
    }
  }

  const handleCreateTask = async (e) => {
    e.preventDefault()
    try {
      const response = await fetch(`http://localhost:8080/api/groups/${groupId}/tasks`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          gctaskTitle: taskTitle,
          gctaskDesc: taskDesc,
          gctaskStart: taskStart,
          gctaskEnd: taskEnd
        })
      })
      
      if (response.ok) {
        setShowTaskModal(false)
        resetTaskForm()
        fetchTasks()
        fetchLogs()
      } else {
        const error = await response.json()
        alert(error.error || 'Failed to create task')
      }
    } catch (err) {
      console.error('Error creating task:', err)
      alert('Failed to create task')
    }
  }

  const handleUpdateTask = async (e) => {
    e.preventDefault()
    try {
      const response = await fetch(`http://localhost:8080/api/groups/${groupId}/tasks/${editingTask.gctaskId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          gctaskTitle: taskTitle,
          gctaskDesc: taskDesc,
          gctaskStart: taskStart,
          gctaskEnd: taskEnd,
          gctaskProgress: taskProgress
        })
      })
      
      if (response.ok) {
        setShowTaskModal(false)
        setEditingTask(null)
        resetTaskForm()
        fetchTasks()
        fetchLogs()
      } else {
        const error = await response.json()
        alert(error.error || 'Failed to update task')
      }
    } catch (err) {
      console.error('Error updating task:', err)
      alert('Failed to update task')
    }
  }

  const handleDeleteTask = async (taskId) => {
    if (!window.confirm('Are you sure you want to delete this task?')) return
    
    try {
      const response = await fetch(`http://localhost:8080/api/groups/${groupId}/tasks/${taskId}`, {
        method: 'DELETE',
        credentials: 'include'
      })
      
      if (response.ok) {
        fetchTasks()
        fetchLogs()
      } else {
        alert('Failed to delete task')
      }
    } catch (err) {
      console.error('Error deleting task:', err)
      alert('Failed to delete task')
    }
  }

  const resetTaskForm = () => {
    setTaskTitle('')
    setTaskDesc('')
    setTaskStart('')
    setTaskEnd('')
    setTaskProgress(0)
  }

  const openEditTask = (task) => {
    setEditingTask(task)
    setTaskTitle(task.gctaskTitle)
    setTaskDesc(task.gctaskDesc)
    setTaskStart(task.gctaskStart ? task.gctaskStart.substring(0, 16) : '')
    setTaskEnd(task.gctaskEnd ? task.gctaskEnd.substring(0, 16) : '')
    setTaskProgress(task.gctaskProgress || 0)
    setShowTaskModal(true)
  }

  const closeTaskModal = () => {
    setShowTaskModal(false)
    setEditingTask(null)
    resetTaskForm()
  }

  const formatDate = (dateString) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleDateString('en-US', { month: '2-digit', day: '2-digit' })
  }

  const formatDateTime = (dateString) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleString('en-US', { 
      month: '2-digit', 
      day: '2-digit', 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: true 
    })
  }

  const getMemberProgress = () => {
    if (!tasks || tasks.length === 0) return {}
    
    const memberProgress = {}
    tasks.forEach(task => {
      const owner = task.gctaskOwner
      if (!memberProgress[owner]) {
        memberProgress[owner] = { total: 0, count: 0 }
      }
      memberProgress[owner].total += task.gctaskProgress || 0
      memberProgress[owner].count += 1
    })
    
    const result = {}
    Object.keys(memberProgress).forEach(owner => {
      result[owner] = Math.round(memberProgress[owner].total / memberProgress[owner].count)
    })
    return result
  }

  const memberProgress = getMemberProgress()
  const allMembers = group ? [
    group.leader,
    ...(group.members || [])
  ].filter((m, i, arr) => arr.findIndex(x => x.institutionalId === m.institutionalId) === i) : []

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <div>Loading...</div>
      </div>
    )
  }

  if (!group) {
    return (
      <div style={{ padding: '2rem' }}>
        <p>Group not found</p>
        <button onClick={() => navigate('/dashboard')}>Back to Dashboard</button>
      </div>
    )
  }

  return (
    <div className="group-detail-container">
      <div className="group-detail-header">
        <button className="group-detail-back-btn" onClick={() => navigate('/dashboard')}>
          ← Back
        </button>
        <div className="group-detail-title-section">
          <h1 className="group-detail-name">{group.groupName}</h1>
          <p className="group-detail-subject">Subject Code: {group.courseId}</p>
        </div>
        {isMember && (
          <button className="group-detail-add-task-btn" onClick={() => setShowTaskModal(true)}>
            Add Task
          </button>
        )}
      </div>

      {/* Group Members Section */}
      <div className="group-detail-members-section">
        <h2 className="group-detail-section-title">Group Members</h2>
        <div className="group-detail-members-grid">
          {allMembers.map((member, idx) => (
            <div key={idx} className="group-detail-member-card">
              <div className="group-detail-member-avatar">
                {member.displayName ? member.displayName.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="group-detail-member-info">
                <div className="group-detail-member-name">
                  {member.displayName || 'Unknown'}
                  {group.leader?.institutionalId === member.institutionalId && (
                    <span className="group-detail-leader-badge">Leader</span>
                  )}
                </div>
                <div className="group-detail-member-id">{member.institutionalId || 'N/A'}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Progress Section */}
      <div className="group-detail-progress-section">
        <h2 className="group-detail-section-title">Progress</h2>
        <div className="group-detail-progress-content">
          {/* Members Progress */}
          <div className="group-detail-members-progress">
            <h3 className="group-detail-progress-subtitle">Members Progress</h3>
            {tasks.length === 0 ? (
              <p className="group-detail-empty-message">No tasks yet</p>
            ) : (
              <div className="group-detail-progress-list">
                {allMembers.map((member, idx) => {
                  const progress = memberProgress[member.institutionalId] || 0
                  return (
                    <div key={idx} className="group-detail-progress-item">
                      <div className="group-detail-progress-member-name">
                        {member.displayName || member.institutionalId}
                      </div>
                      <div className="group-detail-progress-bar-container">
                        <div 
                          className="group-detail-progress-bar-fill"
                          style={{ width: `${progress}%` }}
                        />
                      </div>
                      <div className="group-detail-progress-percentage">{progress}%</div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* Task Numbers Analytics */}
          <div className="group-detail-task-numbers">
            <h3 className="group-detail-progress-subtitle">Task Numbers</h3>
            {tasks.length === 0 ? (
              <p className="group-detail-empty-message">No tasks yet</p>
            ) : (
              <div className="group-detail-task-numbers-grid">
                {tasks.slice(0, 4).map(task => (
                  <div key={task.gctaskId} className="group-detail-task-number-card">
                    <div className="group-detail-task-number-title">{task.gctaskTitle}</div>
                    <div className="group-detail-task-number-progress">{task.gctaskProgress || 0}%</div>
                  </div>
                ))}
                {tasks.length > 4 && (
                  <div className="group-detail-view-all">View All ▸</div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bottom Section: Project Logs and Deadlines */}
      <div className="group-detail-bottom-section">
        {/* Project Logs */}
        <div className="group-detail-project-logs">
          <div className="group-detail-section-header">
            <h3 className="group-detail-section-title-small">Project Logs</h3>
            <span className="group-detail-section-arrow">→</span>
          </div>
          <div className="group-detail-logs-list">
            {logs.length === 0 ? (
              <p className="group-detail-empty-message">No activity yet</p>
            ) : (
              logs.slice(0, 5).map(log => (
                <div key={log.logId} className="group-detail-log-item">
                  <div className="group-detail-log-date">{formatDate(log.createdAt)}</div>
                  <div className="group-detail-log-description">{log.actionDescription}</div>
                  <div className="group-detail-log-time">{formatDateTime(log.createdAt).split(', ')[1]}</div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Deadlines */}
        <div className="group-detail-deadlines">
          <div className="group-detail-section-header">
            <h3 className="group-detail-section-title-small">Deadlines</h3>
            <span className="group-detail-section-arrow">→</span>
          </div>
          <div className="group-detail-deadlines-list">
            {tasks.length === 0 ? (
              <p className="group-detail-empty-message">No deadlines yet</p>
            ) : (
              tasks.map(task => (
                <div key={task.gctaskId} className="group-detail-deadline-item">
                  <div className="group-detail-deadline-title">{task.gctaskTitle}</div>
                  <div className="group-detail-deadline-date">{formatDate(task.gctaskEnd)}</div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Task Modal */}
      {showTaskModal && (
        <div className="group-detail-modal-overlay" onClick={closeTaskModal}>
          <div className="group-detail-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="group-detail-modal-header">
              <h2>{editingTask ? 'Edit Task' : 'Create New Task'}</h2>
              <button className="group-detail-modal-close" onClick={closeTaskModal}>×</button>
            </div>
            <form onSubmit={editingTask ? handleUpdateTask : handleCreateTask}>
              <div className="group-detail-form-group">
                <label>Task Title</label>
                <input
                  type="text"
                  value={taskTitle}
                  onChange={(e) => setTaskTitle(e.target.value)}
                  required
                  maxLength={60}
                  className="create-group-input"
                />
              </div>
              <div className="group-detail-form-group">
                <label>Description</label>
                <textarea
                  value={taskDesc}
                  onChange={(e) => setTaskDesc(e.target.value)}
                  required
                  rows={4}
                  className="create-group-input"
                />
              </div>
              <div className="group-detail-form-group">
                <label>Start Date & Time</label>
                <input
                  type="datetime-local"
                  value={taskStart}
                  onChange={(e) => setTaskStart(e.target.value)}
                  required
                  className="create-group-input"
                />
              </div>
              <div className="group-detail-form-group">
                <label>End Date & Time (Deadline)</label>
                <input
                  type="datetime-local"
                  value={taskEnd}
                  onChange={(e) => setTaskEnd(e.target.value)}
                  required
                  className="create-group-input"
                />
              </div>
              {editingTask && (
                <div className="group-detail-form-group">
                  <label>Progress (%)</label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={taskProgress}
                    onChange={(e) => setTaskProgress(parseInt(e.target.value) || 0)}
                    className="create-group-input"
                  />
                </div>
              )}
              <div className="group-detail-modal-actions">
                <button type="button" onClick={closeTaskModal} className="group-detail-btn-cancel">
                  Cancel
                </button>
                <button type="submit" className="group-detail-btn-submit">
                  {editingTask ? 'Update Task' : 'Create Task'}
                </button>
              </div>
            </form>
            {editingTask && (
              <div className="group-detail-modal-actions" style={{ marginTop: '1rem' }}>
                <button
                  type="button"
                  onClick={() => handleDeleteTask(editingTask.gctaskId)}
                  className="group-detail-btn-delete"
                >
                  Delete Task
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { EditGroupModal } from './App'

export default function CourseDetail({ user }) {
  const { courseId } = useParams()
  const navigate = useNavigate()
  const [course, setCourse] = useState(null)
  const [groups, setGroups] = useState([])
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [showAddStudentModal, setShowAddStudentModal] = useState(false)
  const [studentIdInput, setStudentIdInput] = useState('')
  const [addingStudent, setAddingStudent] = useState(false)
  const [addStudentError, setAddStudentError] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const studentsPerPage = 6
  const [editingGroup, setEditingGroup] = useState(null)
  const [deletingGroupId, setDeletingGroupId] = useState(null)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [studentSearchResults, setStudentSearchResults] = useState([])
  const [searchingStudents, setSearchingStudents] = useState(false)

  useEffect(() => {
    fetchCourseDetails()
    fetchStudents()
  }, [courseId])

  // Search for students as teacher types
  useEffect(() => {
    if (studentIdInput.length >= 2) {
      const searchTimer = setTimeout(async () => {
        setSearchingStudents(true)
        try {
          const response = await fetch(`http://localhost:8080/api/users/search?q=${encodeURIComponent(studentIdInput)}`, {
            credentials: 'include'
          })
          if (response.ok) {
            const data = await response.json()
            // Filter to only show students (not teachers)
            const students = Array.isArray(data) ? data.filter(user => user.role === 'STUDENT' || !user.role) : []
            setStudentSearchResults(students)
          } else {
            setStudentSearchResults([])
          }
        } catch (err) {
          console.error('Student search error:', err)
          setStudentSearchResults([])
        } finally {
          setSearchingStudents(false)
        }
      }, 300)
      return () => clearTimeout(searchTimer)
    } else {
      setStudentSearchResults([])
    }
  }, [studentIdInput])

  const handleSelectStudent = (student) => {
    setStudentIdInput(student.institutionalId || student.id || '')
    setStudentSearchResults([])
  }

  const fetchCourseDetails = async () => {
    try {
      setLoading(true)
      
      // Fetch course details
      const courseResponse = await fetch(`http://localhost:8080/api/courses/${courseId}`, {
        credentials: 'include'
      })
      
      if (!courseResponse.ok) {
        throw new Error('Failed to fetch course')
      }
      
      const courseData = await courseResponse.json()
      setCourse(courseData)
      
      // Fetch groups for this course
      await fetchGroups()
    } catch (err) {
      console.error('Error fetching course details:', err)
    } finally {
      setLoading(false)
    }
  }

  const fetchGroups = async () => {
    try {
      const groupsResponse = await fetch(`http://localhost:8080/api/courses/${courseId}/groups`, {
        credentials: 'include'
      })
      
      if (groupsResponse.ok) {
        const groupsData = await groupsResponse.json()
        setGroups(Array.isArray(groupsData) ? groupsData : [])
      } else {
        setGroups([])
      }
    } catch (err) {
      console.error('Error fetching groups:', err)
      setGroups([])
    }
  }

  const fetchStudents = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/students?courseId=${courseId}`, {
        credentials: 'include'
      })
      
      if (response.ok) {
        const studentsData = await response.json()
        setStudents(Array.isArray(studentsData) ? studentsData : [])
      } else {
        setStudents([])
      }
    } catch (err) {
      console.error('Error fetching students:', err)
      setStudents([])
    }
  }

  const handleAddStudent = async (e) => {
    e.preventDefault()
    if (!studentIdInput.trim()) {
      setAddStudentError('Please enter a student ID')
      return
    }

    setAddingStudent(true)
    setAddStudentError('')

    try {
      const response = await fetch(`http://localhost:8080/api/courses/${courseId}/students`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          institutionalId: studentIdInput.trim()
        })
      })

      const data = await response.json()

      if (!response.ok) {
        setAddStudentError(data.error || 'Failed to add student')
        return
      }

      // Success - refresh students list and close modal
      setStudentIdInput('')
      setStudentSearchResults([])
      setShowAddStudentModal(false)
      fetchStudents()
      fetchGroups() // Refresh groups in case student was added to a group
    } catch (err) {
      console.error('Error adding student:', err)
      setAddStudentError('Failed to add student. Please try again.')
    } finally {
      setAddingStudent(false)
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return ''
    try {
      const date = new Date(dateString)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${month}/${day}/${year}`
    } catch (e) {
      return dateString
    }
  }

  const getGroupInitial = (groupName) => {
    if (!groupName) return 'G'
    return groupName.charAt(0).toUpperCase()
  }

  const handleDeleteGroup = async () => {
    if (!deletingGroupId) return
    
    setDeleting(true)
    setDeleteError('')
    try {
      const response = await fetch(`http://localhost:8080/api/groups/${deletingGroupId}`, {
        method: 'DELETE',
        credentials: 'include'
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Failed to delete group' }))
        setDeleteError(errorData.error || 'Failed to delete group')
        setDeleting(false)
        return
      }
      
      // Refresh groups list
      await fetchGroups()
      setDeletingGroupId(null)
      setShowDeleteConfirm(false)
      setDeleting(false)
    } catch (err) {
      console.error('Delete group error:', err)
      setDeleteError('Network error: ' + err.message)
      setDeleting(false)
    }
  }

  const handleEditGroup = (group) => {
    // Enrich group with courseId for EditGroupModal
    const enrichedGroup = {
      ...group,
      courseId: courseId ? parseInt(courseId) : null
    }
    setEditingGroup(enrichedGroup)
  }

  const totalPages = Math.ceil(students.length / studentsPerPage)
  const startIndex = (currentPage - 1) * studentsPerPage
  const endIndex = startIndex + studentsPerPage
  const currentStudents = students.slice(startIndex, endIndex)

  const handlePreviousPage = () => {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1)
    }
  }

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1)
    }
  }

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading course details...</div>
      </div>
    )
  }

  if (!course) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        flexDirection: 'column',
        gap: '1rem'
      }}>
        <div>Course not found</div>
        <button onClick={() => navigate('/courses')} className="dashboard-create-btn">
          Back to Courses
        </button>
      </div>
    )
  }

  return (
    <div className="course-detail-wrapper">
      <div className="course-detail-header-section">
        <button 
          onClick={() => navigate('/courses')} 
          className="course-detail-back-btn"
        >
          ← Back
        </button>
        <div className="course-detail-title-section">
          <h1 className="course-detail-title">{course.courseName}</h1>
          <p className="course-detail-meta">
            {course.courseCode} - {formatDate(course.courseDur)}
            {course.courseInv && (
              <span className="course-detail-inv-code"> | Code: {course.courseInv}</span>
            )}
          </p>
          <div className="course-detail-separator"></div>
          <p className="course-detail-description">{course.courseDesc}</p>
        </div>
      </div>

      {/* Students Section */}
      {user && user.role === 'TEACHER' && (
        <div className="course-detail-students-section">
          <div className="course-detail-students-header">
            <h2 className="course-detail-students-title">Enrolled Students</h2>
            <button 
              onClick={() => setShowAddStudentModal(true)}
              className="course-detail-add-student-btn"
            >
              + Add Student
            </button>
          </div>
          {students.length === 0 ? (
            <div className="course-detail-empty-message">
              No students enrolled in this course yet.
            </div>
          ) : (
            <>
              <div className="course-detail-students-list">
                {currentStudents.map(student => (
                  <div key={student.studentId} className="course-detail-student-card">
                    <div className="course-detail-student-avatar">
                      {student.firstName ? student.firstName.charAt(0).toUpperCase() : 
                       student.lastName ? student.lastName.charAt(0).toUpperCase() : 
                       student.studentId ? student.studentId.charAt(0).toUpperCase() : 'S'}
                    </div>
                    <div className="course-detail-student-info">
                      <h3 className="course-detail-student-name">
                        {student.firstName && student.lastName 
                          ? `${student.firstName} ${student.lastName}`
                          : student.firstName || student.lastName || 'Unknown'}
                      </h3>
                      <p className="course-detail-student-id">ID: {student.studentId}</p>
                      {student.email && (
                        <p className="course-detail-student-email">{student.email}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              {totalPages > 1 && (
                <div className="course-detail-students-pagination">
                  <button
                    onClick={handlePreviousPage}
                    disabled={currentPage === 1}
                    className="course-detail-pagination-btn"
                  >
                    Previous
                  </button>
                  <span className="course-detail-pagination-info">
                    Page {currentPage} of {totalPages}
                  </span>
                  <button
                    onClick={handleNextPage}
                    disabled={currentPage === totalPages}
                    className="course-detail-pagination-btn"
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* Add Student Modal */}
      {showAddStudentModal && (
        <div className="course-detail-modal-overlay" onClick={() => setShowAddStudentModal(false)}>
          <div className="course-detail-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="course-detail-modal-header">
              <h3>Add Student to Course</h3>
              <button 
                className="course-detail-modal-close"
                onClick={() => {
                  setShowAddStudentModal(false)
                  setStudentIdInput('')
                  setAddStudentError('')
                  setStudentSearchResults([])
                }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleAddStudent} className="course-detail-modal-form">
              <div className="course-detail-modal-field" style={{ position: 'relative' }}>
                <label htmlFor="studentId">Student ID Number or Name</label>
                <input
                  id="studentId"
                  type="text"
                  value={studentIdInput}
                  onChange={(e) => setStudentIdInput(e.target.value)}
                  placeholder="Search by student ID or name"
                  disabled={addingStudent}
                  autoFocus
                />
                {searchingStudents && (
                  <div style={{ 
                    position: 'absolute', 
                    top: '100%', 
                    left: 0, 
                    right: 0, 
                    background: '#f8f9fa', 
                    padding: '0.5rem',
                    fontSize: '13px',
                    color: '#666',
                    zIndex: 1000
                  }}>
                    Searching...
                  </div>
                )}
                {studentSearchResults.length > 0 && !searchingStudents && (
                  <div style={{ 
                    position: 'absolute', 
                    top: '100%', 
                    left: 0, 
                    right: 0, 
                    background: 'white', 
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    marginTop: '4px',
                    maxHeight: '300px',
                    overflowY: 'auto',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                    zIndex: 1000
                  }}>
                    {studentSearchResults.map((student, idx) => (
                      <div
                        key={student.id || student.institutionalId || idx}
                        onClick={() => handleSelectStudent(student)}
                        style={{
                          padding: '12px',
                          cursor: 'pointer',
                          borderBottom: idx < studentSearchResults.length - 1 ? '1px solid #eee' : 'none',
                          transition: 'background-color 0.2s'
                        }}
                        onMouseEnter={(e) => e.target.style.backgroundColor = '#f0f0f0'}
                        onMouseLeave={(e) => e.target.style.backgroundColor = 'white'}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <div style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: '50%',
                            background: '#1e4487',
                            color: 'white',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 'bold',
                            fontSize: '16px',
                            flexShrink: 0
                          }}>
                            {student.displayName ? student.displayName.charAt(0).toUpperCase() : 
                             student.institutionalId ? student.institutionalId.charAt(0).toUpperCase() : 'S'}
                          </div>
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ fontWeight: '600', color: '#333', marginBottom: '2px' }}>
                              {student.displayName || 'Unknown Student'}
                            </div>
                            <div style={{ fontSize: '13px', color: '#666' }}>
                              ID: {student.institutionalId || 'N/A'}
                            </div>
                            {student.email && (
                              <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>
                                {student.email}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                {studentIdInput.length >= 2 && studentSearchResults.length === 0 && !searchingStudents && (
                  <div style={{ 
                    position: 'absolute', 
                    top: '100%', 
                    left: 0, 
                    right: 0, 
                    background: '#fff3cd', 
                    border: '1px solid #ffc107',
                    borderRadius: '4px',
                    marginTop: '4px',
                    padding: '8px',
                    fontSize: '13px',
                    color: '#856404',
                    zIndex: 1000
                  }}>
                    No students found matching "{studentIdInput}"
                  </div>
                )}
              </div>
              {addStudentError && (
                <div className="course-detail-modal-error">{addStudentError}</div>
              )}
              <div className="course-detail-modal-actions">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddStudentModal(false)
                    setStudentIdInput('')
                    setAddStudentError('')
                    setStudentSearchResults([])
                  }}
                  className="course-detail-modal-cancel"
                  disabled={addingStudent}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="course-detail-modal-submit"
                  disabled={addingStudent || !studentIdInput.trim()}
                >
                  {addingStudent ? 'Adding...' : 'Add Student'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="course-detail-groups-section">
        <h2 className="course-detail-groups-title">Groups in this Course</h2>
        {groups.length === 0 ? (
          <div className="course-detail-empty-message">
            No groups registered in this course yet.
          </div>
        ) : (
          <div className="course-detail-groups-list">
            {groups.map(group => (
              <div 
                key={group.groupId} 
                className="course-detail-group-card"
                onClick={() => navigate(`/groups/${group.groupId}`)}
              >
                <div className="course-detail-group-avatar">
                  {getGroupInitial(group.groupName)}
                </div>
                <div className="course-detail-group-info">
                  <h3 className="course-detail-group-name">{group.groupName}</h3>
                  <p className="course-detail-group-subject">{course.courseCode}</p>
                  <p className="course-detail-group-members">
                    Members: {group.memberStudentIds ? group.memberStudentIds.length + 1 : 1}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && deletingGroupId && (
        <div className="course-detail-modal-overlay" onClick={() => {
          setShowDeleteConfirm(false)
          setDeletingGroupId(null)
          setDeleteError('')
        }}>
          <div className="course-detail-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="course-detail-modal-header">
              <h3>Delete Group</h3>
              <button 
                className="course-detail-modal-close"
                onClick={() => {
                  setShowDeleteConfirm(false)
                  setDeletingGroupId(null)
                  setDeleteError('')
                }}
              >
                ×
              </button>
            </div>
            <div style={{ padding: '1rem' }}>
              <p style={{ marginBottom: '1rem', color: '#c33' }}>
                Are you sure you want to delete this group? This action cannot be undone and will remove the group for all students and teachers.
              </p>
              {deleteError && (
                <div className="course-detail-modal-error" style={{ marginBottom: '1rem' }}>{deleteError}</div>
              )}
              <div className="course-detail-modal-actions">
                <button
                  type="button"
                  onClick={() => {
                    setShowDeleteConfirm(false)
                    setDeletingGroupId(null)
                    setDeleteError('')
                  }}
                  className="course-detail-modal-cancel"
                  disabled={deleting}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleDeleteGroup}
                  className="course-detail-modal-submit"
                  disabled={deleting}
                  style={{ background: '#dc3545' }}
                >
                  {deleting ? 'Deleting...' : 'Delete Group'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Edit Group Modal */}
      {editingGroup && (
        <EditGroupModal
          group={editingGroup}
          user={user}
          onClose={() => {
            setEditingGroup(null)
          }}
          onSuccess={() => {
            setEditingGroup(null)
            fetchGroups()
          }}
        />
      )}
    </div>
  )
}

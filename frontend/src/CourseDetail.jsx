import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'

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

  useEffect(() => {
    fetchCourseDetails()
    fetchStudents()
  }, [courseId])

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
                }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleAddStudent} className="course-detail-modal-form">
              <div className="course-detail-modal-field">
                <label htmlFor="studentId">Student ID Number</label>
                <input
                  id="studentId"
                  type="text"
                  value={studentIdInput}
                  onChange={(e) => setStudentIdInput(e.target.value)}
                  placeholder="Enter student ID number"
                  disabled={addingStudent}
                  autoFocus
                />
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
    </div>
  )
}

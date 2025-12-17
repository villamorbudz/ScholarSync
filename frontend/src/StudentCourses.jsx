import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

export default function StudentCourses({ user }) {
  const navigate = useNavigate()
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [showEnrollModal, setShowEnrollModal] = useState(false)
  const [courseCodeInput, setCourseCodeInput] = useState('')
  const [enrolling, setEnrolling] = useState(false)
  const [enrollError, setEnrollError] = useState('')

  useEffect(() => {
    fetchCourses()
  }, [])

  const fetchCourses = async () => {
    try {
      setLoading(true)
      const response = await fetch('http://localhost:8080/api/courses/my-courses', {
        credentials: 'include'
      })
      
      if (response.ok) {
        const coursesData = await response.json()
        setCourses(Array.isArray(coursesData) ? coursesData : [])
      } else {
        setCourses([])
      }
    } catch (err) {
      console.error('Error fetching courses:', err)
      setCourses([])
    } finally {
      setLoading(false)
    }
  }

  const handleEnroll = async (e) => {
    e.preventDefault()
    if (!courseCodeInput.trim()) {
      setEnrollError('Please enter a course code')
      return
    }

    if (courseCodeInput.trim().length !== 6) {
      setEnrollError('Course code must be 6 characters')
      return
    }

    setEnrolling(true)
    setEnrollError('')

    try {
      const response = await fetch('http://localhost:8080/api/courses/enroll', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          courseInv: courseCodeInput.trim().toUpperCase()
        })
      })

      const data = await response.json()

      if (!response.ok) {
        setEnrollError(data.error || 'Failed to enroll in course')
        return
      }

      // Success - refresh courses list and close modal
      setCourseCodeInput('')
      setShowEnrollModal(false)
      fetchCourses()
    } catch (err) {
      console.error('Error enrolling in course:', err)
      setEnrollError('Failed to enroll in course. Please try again.')
    } finally {
      setEnrolling(false)
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

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading courses...</div>
      </div>
    )
  }

  return (
    <div className="student-courses-wrapper">
      <div className="student-courses-header">
        <div>
          <h2 className="student-courses-title">My Courses</h2>
          <p className="student-courses-subtitle">Courses you are enrolled in.</p>
        </div>
        <button 
          onClick={() => setShowEnrollModal(true)}
          className="student-courses-enroll-btn"
        >
          + Self Enrollment
        </button>
      </div>

      {courses.length === 0 ? (
        <div className="student-courses-empty">
          <p>You are not enrolled in any courses yet.</p>
          <p>Click "Self Enrollment" to join a course using a 6-digit course code.</p>
        </div>
      ) : (
        <div className="student-courses-list">
          {courses.map(course => (
            <div 
              key={course.courseId} 
              className="student-course-card"
              onClick={() => navigate(`/courses/${course.courseId}`)}
            >
              <div className="student-course-content">
                <h3 className="student-course-name">{course.courseName}</h3>
                <p className="student-course-code">{course.courseCode}</p>
                <p className="student-course-desc">{course.courseDesc}</p>
                <div className="student-course-meta">
                  <span className="student-course-adviser">Adviser: {course.courseAdviser}</span>
                  <span className="student-course-date">{formatDate(course.courseDur)}</span>
                </div>
              </div>
              <span className={`student-course-badge ${course.courseStat === 1 ? 'student-course-badge-active' : 'student-course-badge-inactive'}`}>
                {course.courseStat === 1 ? 'Active' : 'Inactive'}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Enroll Modal */}
      {showEnrollModal && (
        <div className="student-courses-modal-overlay" onClick={() => setShowEnrollModal(false)}>
          <div className="student-courses-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="student-courses-modal-header">
              <h3>Enroll in Course</h3>
              <button 
                className="student-courses-modal-close"
                onClick={() => {
                  setShowEnrollModal(false)
                  setCourseCodeInput('')
                  setEnrollError('')
                }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleEnroll} className="student-courses-modal-form">
              <div className="student-courses-modal-field">
                <label htmlFor="courseCode">Course Code (6 characters)</label>
                <input
                  id="courseCode"
                  type="text"
                  value={courseCodeInput}
                  onChange={(e) => {
                    const value = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6)
                    setCourseCodeInput(value)
                  }}
                  placeholder="Enter 6-digit course code"
                  disabled={enrolling}
                  autoFocus
                  maxLength={6}
                />
              </div>
              {enrollError && (
                <div className="student-courses-modal-error">{enrollError}</div>
              )}
              <div className="student-courses-modal-actions">
                <button
                  type="button"
                  onClick={() => {
                    setShowEnrollModal(false)
                    setCourseCodeInput('')
                    setEnrollError('')
                  }}
                  className="student-courses-modal-cancel"
                  disabled={enrolling}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="student-courses-modal-submit"
                  disabled={enrolling || courseCodeInput.trim().length !== 6}
                >
                  {enrolling ? 'Enrolling...' : 'Enroll'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

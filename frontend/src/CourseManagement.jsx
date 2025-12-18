import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

function CreateCourseModal({ onClose, onSuccess }) {
  const [courseName, setCourseName] = useState('')
  const [courseCode, setCourseCode] = useState('')
  const [courseDetails, setCourseDetails] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    
    if (!courseName.trim()) {
      setError('Course name is required')
      return
    }
    
    if (!courseCode.trim()) {
      setError('Course code is required')
      return
    }
    
    if (!courseDetails.trim()) {
      setError('Course details are required')
      return
    }
    
    if (!startDate || !endDate) {
      setError('Both start and end dates are required')
      return
    }

  
    const startDateTime = startDate ? `${startDate}T00:00:00` : null
    
    console.log('Creating course with data:', {
      courseName,
      courseCode,
      courseDesc: courseDetails,
      courseDur: startDateTime,
      startDate,
      endDate
    })
    
    setLoading(true)
    try {
      const response = await fetch('http://localhost:8080/api/courses', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          courseName: courseName.trim(),
          courseCode: courseCode.trim(),
          courseDesc: courseDetails.trim(),
          courseDur: startDateTime,
          courseCap: 0,
          courseStat: 1
        })
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Failed to create course' }))
        setError(errorData.error || 'Failed to create course')
        setLoading(false)
        return
      }
      
      const data = await response.json()

      
      setCourseName('')
      setCourseCode('')
      setCourseDetails('')
      setStartDate('')
      setEndDate('')
      
      setLoading(false)
      onSuccess(data)
      onClose()
    } catch (err) {
      console.error('Create course error:', err)
      setError('Network error: ' + err.message)
      setLoading(false)
    }
  }

  return (
    <div className="create-group-modal-overlay" onClick={onClose}>
      <div className="create-group-modal" onClick={(e) => e.stopPropagation()}>
        <button className="create-group-modal-close" onClick={onClose}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>

        <div className="create-group-modal-header">
          <h2 className="create-group-modal-title">Create Course</h2>
          <p className="create-group-modal-subtitle">Start creating your course.</p>
        </div>

        <form className="create-group-form" onSubmit={handleSubmit}>
          <div className="create-group-field">
            <label className="create-group-label">Enter Course Name</label>
            <input
              type="text"
              className="create-group-input"
              placeholder="Enter course name"
              value={courseName}
              onChange={(e) => setCourseName(e.target.value)}
              required
            />
          </div>

          <div className="create-group-field">
            <label className="create-group-label">Enter Course Code</label>
            <input
              type="text"
              className="create-group-input"
              placeholder="Enter course code"
              value={courseCode}
              onChange={(e) => setCourseCode(e.target.value)}
              required
            />
          </div>

          <div className="create-group-field">
            <label className="create-group-label">Enter Course Details</label>
            <textarea
              className="create-group-textarea"
              placeholder="Enter course details"
              value={courseDetails}
              onChange={(e) => setCourseDetails(e.target.value)}
              rows="4"
              required
            />
          </div>

          <div className="create-group-field">
            <label className="create-group-label">Enter Course Duration</label>
            <div className="date-inputs-container">
              <div className="date-input-wrapper">
                <input
                  type="date"
                  className="course-form-input date-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                />
                <span className="date-icon">📅</span>
              </div>
              <span className="date-separator">to</span>
              <div className="date-input-wrapper">
                <input
                  type="date"
                  className="course-form-input date-input"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                />
                <span className="date-icon">📅</span>
              </div>
            </div>
          </div>

          {error && (
            <div className="create-group-error">{error}</div>
          )}

          <div className="create-group-actions">
            <button
              type="submit"
              className="create-group-complete-btn"
              disabled={loading}
            >
              {loading ? 'Creating...' : 'COMPLETE'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function EditCourseModal({ course, onClose, onSuccess }) {
  const [courseName, setCourseName] = useState(course?.courseName || '')
  const [courseCode, setCourseCode] = useState(course?.courseCode || '')
  const [courseDetails, setCourseDetails] = useState(course?.courseDesc || '')
  const [startDate, setStartDate] = useState(course?.courseDur ? course.courseDur.split('T')[0] : '')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    
    if (!courseName.trim()) {
      setError('Course name is required')
      return
    }
    
    if (!courseCode.trim()) {
      setError('Course code is required')
      return
    }
    
    if (!courseDetails.trim()) {
      setError('Course details are required')
      return
    }
    
    if (!startDate) {
      setError('Start date is required')
      return
    }

    const startDateTime = startDate ? `${startDate}T00:00:00` : null
    
    setLoading(true)
    try {
      const response = await fetch(`http://localhost:8080/api/courses/${course.courseId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          courseName: courseName.trim(),
          courseCode: courseCode.trim(),
          courseDesc: courseDetails.trim(),
          courseDur: startDateTime,
          courseCap: course.courseCap || 0,
          courseStat: course.courseStat || 1
        })
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Failed to update course' }))
        setError(errorData.error || 'Failed to update course')
        setLoading(false)
        return
      }
      
      const data = await response.json()
      setLoading(false)
      onSuccess(data)
      onClose()
    } catch (err) {
      console.error('Update course error:', err)
      setError('Network error: ' + err.message)
      setLoading(false)
    }
  }

  const handleDelete = async () => {
    setDeleting(true)
    setError('')
    try {
      const response = await fetch(`http://localhost:8080/api/courses/${course.courseId}`, {
        method: 'DELETE',
        credentials: 'include'
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Failed to delete course' }))
        setError(errorData.error || 'Failed to delete course')
        setDeleting(false)
        return
      }
      
      setDeleting(false)
      onSuccess(null) // null indicates deletion
      onClose()
    } catch (err) {
      console.error('Delete course error:', err)
      setError('Network error: ' + err.message)
      setDeleting(false)
    }
  }

  return (
    <div className="create-group-modal-overlay" onClick={onClose}>
      <div className="create-group-modal" onClick={(e) => e.stopPropagation()}>
        <button className="create-group-modal-close" onClick={onClose}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>

        <div className="create-group-modal-header">
          <h2 className="create-group-modal-title">Edit Course</h2>
          <p className="create-group-modal-subtitle">Update course information.</p>
        </div>

        {showDeleteConfirm ? (
          <div className="delete-confirm-section">
            <p style={{ marginBottom: '1rem', color: '#c33' }}>Are you sure you want to delete this course? This action cannot be undone.</p>
            {error && <div className="create-group-error">{error}</div>}
            <div className="create-group-actions" style={{ gap: '1rem' }}>
              <button
                type="button"
                className="create-group-cancel-btn"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="create-group-delete-btn"
                onClick={handleDelete}
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Delete Course'}
              </button>
            </div>
          </div>
        ) : (
          <form className="create-group-form" onSubmit={handleSubmit}>
            <div className="create-group-field">
              <label className="create-group-label">Enter Course Name</label>
              <input
                type="text"
                className="create-group-input"
                placeholder="Enter course name"
                value={courseName}
                onChange={(e) => setCourseName(e.target.value)}
                required
              />
            </div>

            <div className="create-group-field">
              <label className="create-group-label">Enter Course Code</label>
              <input
                type="text"
                className="create-group-input"
                placeholder="Enter course code"
                value={courseCode}
                onChange={(e) => setCourseCode(e.target.value)}
                required
              />
            </div>

            <div className="create-group-field">
              <label className="create-group-label">Enter Course Details</label>
              <textarea
                className="create-group-textarea"
                placeholder="Enter course details"
                value={courseDetails}
                onChange={(e) => setCourseDetails(e.target.value)}
                rows="4"
                required
              />
            </div>

            <div className="create-group-field">
              <label className="create-group-label">Enter Course Duration</label>
              <div className="date-inputs-container">
                <div className="date-input-wrapper">
                  <input
                    type="date"
                    className="course-form-input date-input"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                  />
                  <span className="date-icon">📅</span>
                </div>
              </div>
            </div>

            {error && (
              <div className="create-group-error">{error}</div>
            )}

            <div className="create-group-actions" style={{ display: 'flex', gap: '1rem' }}>
              <button
                type="button"
                className="create-group-delete-btn"
                onClick={() => setShowDeleteConfirm(true)}
                disabled={loading}
              >
                Delete
              </button>
              <button
                type="submit"
                className="create-group-complete-btn"
                disabled={loading}
              >
                {loading ? 'Updating...' : 'UPDATE'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

export default function CourseManagement() {
  const navigate = useNavigate()
  const [courses, setCourses] = useState([])
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [editingCourse, setEditingCourse] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchCourses()
  }, [])

  const fetchCourses = async () => {
    setLoading(true)
    try {
      const response = await fetch('http://localhost:8080/api/courses', {
        credentials: 'include'
      })
      if (response.ok) {
        const data = await response.json()
        setCourses(Array.isArray(data) ? data : [])
      }
    } catch (err) {
      console.error('Error fetching courses:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreateSuccess = (newCourse) => {
    // Refresh courses list
    fetchCourses()
  }

  const handleEditSuccess = (updatedCourse) => {
    // Refresh courses list
    fetchCourses()
    setEditingCourse(null)
  }

  const handleEditClick = (e, course) => {
    e.stopPropagation() // Prevent navigation to course detail
    setEditingCourse(course)
  }

  return (
    <>
      <div className="dashboard-content-header">
        <div>
          <h2 className="dashboard-content-title">Course Management</h2>
          <p className="dashboard-content-subtitle">List of registered courses.</p>
        </div>
        <div className="dashboard-content-actions">
          <button className="dashboard-filter-btn">Filter</button>
          <button className="dashboard-create-btn" onClick={() => setShowCreateModal(true)}>
            Create Course
          </button>
        </div>
      </div>

      {loading ? (
        <div style={{ padding: '2rem', textAlign: 'center' }}>Loading courses...</div>
      ) : courses.length === 0 ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#666' }}>
          No courses created yet. Click "Create Course" to get started.
        </div>
      ) : (
        <div className="dashboard-groups-section">
          <div className="dashboard-groups-category">
            <h3 className="dashboard-groups-category-title">Created Courses</h3>
            <div className="dashboard-groups-list">
              {courses.map(course => (
                <div 
                  key={course.courseId} 
                  className="dashboard-group-card"
                  style={{ cursor: 'pointer', position: 'relative' }}
                  onClick={() => navigate(`/courses/${course.courseId}`)}
                >
                  <div className="dashboard-group-card-content">
                    <h4 className="dashboard-group-name">{course.courseName}</h4>
                    <p className="dashboard-group-subject">{course.courseCode}</p>
                    <p className="dashboard-group-adviser" style={{ marginTop: '0.5rem' }}>
                      {course.courseDesc}
                    </p>
                    <p className="dashboard-group-adviser" style={{ marginTop: '0.5rem' }}>
                      Adviser: {course.courseAdviser}
                    </p>
                  </div>
                  {/* Active status at top right */}
                  <span className={`dashboard-group-badge ${course.courseStat === 1 ? 'dashboard-group-badge-blue' : 'dashboard-group-badge-pink'}`} style={{ position: 'absolute', top: '1rem', right: '1rem' }}>
                    {course.courseStat === 1 ? 'Active' : 'Inactive'}
                  </span>
                  {/* Edit icon at bottom right */}
                  <button
                    className="course-edit-icon-btn"
                    onClick={(e) => handleEditClick(e, course)}
                    title="Edit course"
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                    </svg>
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {showCreateModal && (
        <CreateCourseModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleCreateSuccess}
        />
      )}

      {editingCourse && (
        <EditCourseModal
          course={editingCourse}
          onClose={() => setEditingCourse(null)}
          onSuccess={handleEditSuccess}
        />
      )}
    </>
  )
}

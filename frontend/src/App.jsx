import React, { useState, useEffect } from 'react'
import { Routes, Route, Link, Navigate, useNavigate } from 'react-router-dom'

import StudentLookup from './StudentLookup'
import CourseManagement from './CourseManagement'
import GroupDetail from './GroupDetail'
import CourseDetail from './CourseDetail'
import StudentCourses from './StudentCourses'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/auth/success" element={<AuthSuccess />} />
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/home" element={
        <div className="page">
          <h1>ScholarSync</h1>
          <Home />
        </div>
      } />
      <Route path="/professor" element={
        <div className="page">
          <h1>ScholarSync</h1>
          <ProfessorView />
        </div>
      } />
      <Route path="/student" element={
        <div className="page">
          <h1>ScholarSync</h1>
          <StudentLookup />
        </div>
      } />
      <Route path="/courses" element={<CourseManagementPage />} />
      <Route path="/courses/:courseId" element={<CourseDetailPage />} />
      <Route path="/my-courses" element={<StudentCoursesPage />} />
      <Route path="/groups/:groupId" element={<GroupDetailPage />} />
    </Routes>
  )
}

function GroupDetailPage() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        console.error('Error parsing user:', e)
      }
    }
    setLoading(false)
  }, [])

  if (loading) {
    return <div>Loading...</div>
  }

  return <GroupDetail user={user} />
}

function StudentCoursesPage() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        console.error('Error parsing user:', e)
      }
    }
    setLoading(false)
  }, [])

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading...</div>
      </div>
    )
  }

  const handleLogout = async () => {
    try {
      await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      })
      localStorage.removeItem('user')
      window.location.href = '/login'
    } catch (err) {
      console.error('Logout error:', err)
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
  }

  const getUserInitial = () => {
    if (!user || !user.displayName) return 'U'
    return user.displayName.charAt(0).toUpperCase()
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="dashboard-header-content">
          <div className="dashboard-logo-section">
            <div className="dashboard-logo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <rect x="2" y="2" width="10" height="10" fill="#0078d4"/>
                <rect x="12" y="2" width="10" height="10" fill="#f0713d"/>
                <rect x="2" y="12" width="10" height="10" fill="#d13438"/>
                <rect x="12" y="12" width="10" height="10" fill="#ffb900"/>
              </svg>
            </div>
            <h1 className="dashboard-title">ScholarSync</h1>
          </div>
          <div className="dashboard-header-right">
            {user && (
              <>
                <div className="dashboard-user-avatar-small">
                  {getUserInitial()}
                </div>
                <span className="dashboard-user-name-header">{user.displayName || 'User'}</span>
                <button onClick={handleLogout} className="dashboard-logout-btn">
                  Logout
                </button>
                <div className="dashboard-icon-btn">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="#f0713d" stroke="#f0713d" strokeWidth="2">
                    <circle cx="12" cy="12" r="5" fill="#f0713d"/>
                    <line x1="12" y1="1" x2="12" y2="3" stroke="#f0713d"/>
                    <line x1="12" y1="21" x2="12" y2="23" stroke="#f0713d"/>
                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" stroke="#f0713d"/>
                    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" stroke="#f0713d"/>
                    <line x1="1" y1="12" x2="3" y2="12" stroke="#f0713d"/>
                    <line x1="21" y1="12" x2="23" y2="12" stroke="#f0713d"/>
                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" stroke="#f0713d"/>
                    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" stroke="#f0713d"/>
                  </svg>
                </div>
                <div className="dashboard-notification-btn">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f0713d" strokeWidth="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                  </svg>
                  <span className="dashboard-notification-badge">3</span>
                </div>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        <nav className="dashboard-sidebar">
          <div className="dashboard-sidebar-user">
            <div className="dashboard-user-avatar">
              {getUserInitial()}
            </div>
            <div className="dashboard-sidebar-user-info">
              <div className="dashboard-sidebar-project">IT332 Capstone 1 | GO1-G02</div>
              <div className="dashboard-sidebar-team">Team 01</div>
            </div>
          </div>
          <div className="dashboard-sidebar-nav">
            {user && user.role === 'STUDENT' ? (
              <>
                <button 
                  className="dashboard-sidebar-button"
                  onClick={() => window.location.href = '/dashboard'}
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  <span>Create Group</span>
                </button>
                <Link to="/my-courses" className="dashboard-sidebar-button active">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                  </svg>
                  <span>Courses</span>
                </Link>
              </>
            ) : null}
          </div>
        </nav>

        <div className="dashboard-content-area">
          <StudentCourses user={user} />
        </div>
      </main>
    </div>
  )
}

function CourseDetailPage() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        console.error('Error parsing user:', e)
      }
    }
    setLoading(false)
  }, [])

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading...</div>
      </div>
    )
  }

  const handleLogout = async () => {
    try {
      await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      })
      localStorage.removeItem('user')
      window.location.href = '/login'
    } catch (err) {
      console.error('Logout error:', err)
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
  }

  const getUserInitial = () => {
    if (!user || !user.displayName) return 'U'
    return user.displayName.charAt(0).toUpperCase()
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="dashboard-header-content">
          <div className="dashboard-logo-section">
            <div className="dashboard-logo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <rect x="2" y="2" width="10" height="10" fill="#0078d4"/>
                <rect x="12" y="2" width="10" height="10" fill="#f0713d"/>
                <rect x="2" y="12" width="10" height="10" fill="#d13438"/>
                <rect x="12" y="12" width="10" height="10" fill="#ffb900"/>
              </svg>
            </div>
            <h1 className="dashboard-title">ScholarSync</h1>
          </div>
          <div className="dashboard-header-right">
            {user && (
              <>
                <div className="dashboard-user-avatar-small">
                  {getUserInitial()}
                </div>
                <span className="dashboard-user-name-header">{user.displayName || 'User'}</span>
                <button onClick={handleLogout} className="dashboard-logout-btn">
                  Logout
                </button>
                <div className="dashboard-icon-btn">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="#f0713d" stroke="#f0713d" strokeWidth="2">
                    <circle cx="12" cy="12" r="5" fill="#f0713d"/>
                    <line x1="12" y1="1" x2="12" y2="3" stroke="#f0713d"/>
                    <line x1="12" y1="21" x2="12" y2="23" stroke="#f0713d"/>
                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" stroke="#f0713d"/>
                    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" stroke="#f0713d"/>
                    <line x1="1" y1="12" x2="3" y2="12" stroke="#f0713d"/>
                    <line x1="21" y1="12" x2="23" y2="12" stroke="#f0713d"/>
                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" stroke="#f0713d"/>
                    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" stroke="#f0713d"/>
                  </svg>
                </div>
                <div className="dashboard-notification-btn">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f0713d" strokeWidth="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                  </svg>
                  <span className="dashboard-notification-badge">3</span>
                </div>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        <nav className="dashboard-sidebar">
          <div className="dashboard-sidebar-user">
            <div className="dashboard-user-avatar">
              {getUserInitial()}
            </div>
            <div className="dashboard-sidebar-user-info">
              <div className="dashboard-sidebar-project">IT332 Capstone 1 | GO1-G02</div>
              <div className="dashboard-sidebar-team">Team 01</div>
            </div>
          </div>
          <div className="dashboard-sidebar-nav">
            {user && user.role === 'STUDENT' ? (
              <button 
                className="dashboard-sidebar-button"
                onClick={() => window.location.href = '/dashboard'}
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                <span>Create Group</span>
              </button>
            ) : (
              <>
                <button 
                  className="dashboard-sidebar-button"
                  onClick={() => window.location.href = '/dashboard'}
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  <span>Create Group</span>
                </button>
                <Link to="/courses" className="dashboard-sidebar-button active">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                  </svg>
                  <span>Course Management</span>
                </Link>
              </>
            )}
          </div>
        </nav>

        <div className="dashboard-content-area">
          <CourseDetail user={user} />
        </div>
      </main>
    </div>
  )
}

function AuthSuccess() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    // Fetch user data from backend
    const fetchUserData = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/auth/success', {
          credentials: 'include' // Include cookies for session
        })
        
        if (!response.ok) {
          throw new Error('Failed to fetch user data')
        }
        
        const data = await response.json()
        
        if (data.success) {
          // Store user data in localStorage for later use
          localStorage.setItem('user', JSON.stringify(data.user))
          // Redirect to dashboard
          navigate('/dashboard', { replace: true })
        } else {
          setError(data.message || 'Authentication failed')
          setLoading(false)
        }
      } catch (err) {
        console.error('Error fetching user data:', err)
        setError('Failed to authenticate. Please try logging in again.')
        setLoading(false)
        // Redirect to login after 3 seconds
        setTimeout(() => {
          navigate('/login', { replace: true })
        }, 3000)
      }
    }

    fetchUserData()
  }, [navigate])

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        flexDirection: 'column',
        gap: '1rem'
      }}>
        <div style={{ fontSize: '18px' }}>Signing you in...</div>
        <div style={{ width: '40px', height: '40px', border: '4px solid #f3f3f3', borderTop: '4px solid #0078d4', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        flexDirection: 'column',
        gap: '1rem',
        color: '#d32f2f'
      }}>
        <div style={{ fontSize: '18px' }}>{error}</div>
        <button onClick={() => navigate('/login')} style={{ padding: '10px 20px', cursor: 'pointer' }}>
          Return to Login
        </button>
      </div>
    )
  }

  return null
}

function Login() {
  return (
    <div className="login-container">
      {/* Background Image - Full Screen */}
      <div className="login-bg-image"></div>
      
      {/* Animated Color Overlay */}
      <div className="login-color-overlay">
        <div className="color-blob blob-1"></div>
        <div className="color-blob blob-2"></div>
        <div className="color-blob blob-3"></div>
        <div className="color-blob blob-4"></div>
      </div>

      {/* Left Side Content - Logo and Text */}
      <div className="login-left-content">
        {/* Logo */}
        <div className="login-logo-container">
          <img src="/ScholarSync_Light.png" alt="ScholarSync Logo" className="login-logo-img" />
        </div>

        {/* Greeting Text */}
        <div className="login-greeting-text">
          Welcome to ScholarSync
        </div>

        {/* Other Text */}
        <div className="login-description-text">
          Connect, collaborate, and manage your academic groups seamlessly. 
          Sign in with your Microsoft account to get started.
        </div>

        {/* Sign In Button */}
        <div className="login-button-container">
          <a 
            href="http://localhost:8080/login/oauth2/authorization/microsoft" 
            className="microsoft-login-button"
          >
            <span className="microsoft-icon"></span>
            <span>Sign in with Microsoft</span>
          </a>
        </div>
      </div>
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

function Dashboard() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [createdGroups, setCreatedGroups] = useState([])
  const [assignedGroups, setAssignedGroups] = useState([])
  const [showCreateModal, setShowCreateModal] = useState(false)

  const fetchGroups = async () => {
    // TODO: Replace with actual API endpoint when available
    // For now, groups will be added via the create modal
    // Keep mock data for initial display
    setCreatedGroups([
      { id: 1, groupName: 'Group_Name', subjectCode: 'Subject_Code', adviser: 'Dr. Cheryl Pantaleon', members: 5 }
    ])
    setAssignedGroups([
      { id: 2, groupName: 'Group_Name', subjectCode: 'Subject_Code', adviser: 'Dr. Jane Smith', members: 4 }
    ])
  }

  useEffect(() => {
    // Get user data from localStorage (set by AuthSuccess)
    const userData = localStorage.getItem('user')
    if (userData) {
      setUser(JSON.parse(userData))
    }
    setLoading(false)
    
    fetchGroups()
  }, [])

  const handleLogout = async () => {
    try {
      await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      })
      localStorage.removeItem('user')
      window.location.href = '/login'
    } catch (err) {
      console.error('Logout error:', err)
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
  }

  const getUserInitial = () => {
    if (!user || !user.displayName) return 'D'
    return user.displayName.charAt(0).toUpperCase()
  }

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading...</div>
      </div>
    )
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="dashboard-header-content">
          <div className="dashboard-logo-section">
            <div className="dashboard-logo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <rect x="2" y="2" width="10" height="10" fill="#0078d4"/>
                <rect x="12" y="2" width="10" height="10" fill="#f0713d"/>
                <rect x="2" y="12" width="10" height="10" fill="#d13438"/>
                <rect x="12" y="12" width="10" height="10" fill="#ffb900"/>
              </svg>
            </div>
            <h1 className="dashboard-title">ScholarSync</h1>
          </div>
          <div className="dashboard-header-right">
            {user && (
              <>
                <div className="dashboard-user-avatar-small">
                  {getUserInitial()}
                </div>
                <span className="dashboard-user-name-header">{user.displayName || 'Dev Student'}</span>
                <button onClick={handleLogout} className="dashboard-logout-btn">
                  Logout
                </button>
                <div className="dashboard-icon-btn">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="#f0713d" stroke="#f0713d" strokeWidth="2">
                    <circle cx="12" cy="12" r="5" fill="#f0713d"/>
                    <line x1="12" y1="1" x2="12" y2="3" stroke="#f0713d"/>
                    <line x1="12" y1="21" x2="12" y2="23" stroke="#f0713d"/>
                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" stroke="#f0713d"/>
                    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" stroke="#f0713d"/>
                    <line x1="1" y1="12" x2="3" y2="12" stroke="#f0713d"/>
                    <line x1="21" y1="12" x2="23" y2="12" stroke="#f0713d"/>
                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" stroke="#f0713d"/>
                    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" stroke="#f0713d"/>
                  </svg>
                </div>
                <div className="dashboard-notification-btn">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f0713d" strokeWidth="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                  </svg>
                  <span className="dashboard-notification-badge">3</span>
                </div>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        <nav className="dashboard-sidebar">
          <div className="dashboard-sidebar-user">
            <div className="dashboard-user-avatar">
              {getUserInitial()}
            </div>
            <div className="dashboard-sidebar-user-info">
              <div className="dashboard-sidebar-project">IT332 Capstone 1 | GO1-G02</div>
              <div className="dashboard-sidebar-team">Team 01</div>
            </div>
          </div>
          <div className="dashboard-sidebar-nav">
            {user && user.role === 'STUDENT' ? (
              <>
                <button 
                  className="dashboard-sidebar-button"
                  onClick={() => setShowCreateModal(true)}
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  <span>Create Group</span>
                </button>
                <Link to="/my-courses" className="dashboard-sidebar-button">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                  </svg>
                  <span>Courses</span>
                </Link>
              </>
            ) : (
              <>
                <button 
                  className="dashboard-sidebar-button"
                  onClick={() => setShowCreateModal(true)}
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  <span>Create Group</span>
                </button>
                <Link to="/courses" className="dashboard-sidebar-button">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                  </svg>
                  <span>Course Management</span>
                </Link>
              </>
            )}
          </div>
        </nav>

        <div className="dashboard-content-area">
          <div className="dashboard-content-header">
            <div>
              <h2 className="dashboard-content-title">My Groups</h2>
              <p className="dashboard-content-subtitle">List of registered groups.</p>
            </div>
            <div className="dashboard-content-actions">
              <button className="dashboard-filter-btn">Filter</button>
              <button className="dashboard-create-btn" onClick={() => setShowCreateModal(true)}>Create Group</button>
            </div>
          </div>

          <div className="dashboard-groups-section">
            <div className="dashboard-groups-category">
              <h3 className="dashboard-groups-category-title">Created Groups</h3>
              <div className="dashboard-groups-list">
                {createdGroups.map(group => (
                  <div 
                    key={group.id} 
                    className="dashboard-group-card"
                    style={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/groups/${group.groupId}`)}
                  >
                    <div className="dashboard-group-card-content">
                      <div className="dashboard-group-name">{group.groupName}</div>
                      <div className="dashboard-group-subject">{group.subjectCode}</div>
                      <div className="dashboard-group-adviser">Adviser: {group.adviser}</div>
                    </div>
                    <div className="dashboard-group-badge dashboard-group-badge-blue">
                      {group.members} members
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="dashboard-groups-category">
              <h3 className="dashboard-groups-category-title">Assigned Groups</h3>
              <div className="dashboard-groups-list">
                {assignedGroups.map(group => (
                  <div 
                    key={group.id} 
                    className="dashboard-group-card"
                    style={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/groups/${group.groupId}`)}
                  >
                    <div className="dashboard-group-card-content">
                      <div className="dashboard-group-name">{group.groupName}</div>
                      <div className="dashboard-group-subject">{group.subjectCode}</div>
                      <div className="dashboard-group-adviser">Adviser: {group.adviser}</div>
                    </div>
                    <div className="dashboard-group-badge dashboard-group-badge-pink">
                      {group.members} members
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </main>
      {showCreateModal && (
        <CreateGroupModal
          user={user}
          onClose={() => setShowCreateModal(false)}
          onSuccess={(newGroup) => {
            // Add the new group to created groups
            setCreatedGroups(prev => [...prev, newGroup])
            setShowCreateModal(false)
            // Refresh groups list
            fetchGroups()
          }}
        />
      )}
    </div>
  )
}

function CreateGroupModal({ user, onClose, onSuccess }) {
  const [groupName, setGroupName] = useState('')
  const [groupDetails, setGroupDetails] = useState('')
  const [memberSearch, setMemberSearch] = useState('')
  const [members, setMembers] = useState([]) // Array of user objects: { institutionalId, displayName, email }
  const [searchResults, setSearchResults] = useState([])
  const [selectedLeader, setSelectedLeader] = useState('') // Selected leader institutional ID
  const [courseId, setCourseId] = useState('') // Selected course ID
  const [courses, setCourses] = useState([]) // Available courses
  const [loading, setLoading] = useState(false)
  const [loadingCourses, setLoadingCourses] = useState(true)
  const [error, setError] = useState('')
  
  // Determine if user is a teacher (can assign leader) or student (auto-assigned as leader)
  const isTeacher = user && user.role === 'TEACHER'
  const isStudent = user && user.role === 'STUDENT'

  // Fetch courses on mount
  useEffect(() => {
    const fetchCourses = async () => {
      try {
        setLoadingCourses(true)
        if (isStudent) {
          // For students, fetch their enrolled courses
          const response = await fetch('http://localhost:8080/api/courses/my-courses', {
            credentials: 'include'
          })
          if (response.ok) {
            const coursesData = await response.json()
            setCourses(Array.isArray(coursesData) ? coursesData : [])
            // Set default to first course if available
            if (coursesData && coursesData.length > 0) {
              setCourseId(coursesData[0].courseId.toString())
            }
          }
        } else if (isTeacher) {
          // For teachers, fetch all courses
          const response = await fetch('http://localhost:8080/api/courses', {
            credentials: 'include'
          })
          if (response.ok) {
            const coursesData = await response.json()
            setCourses(Array.isArray(coursesData) ? coursesData : [])
            // Set default to first course if available
            if (coursesData && coursesData.length > 0) {
              setCourseId(coursesData[0].courseId.toString())
            }
          }
        }
      } catch (err) {
        console.error('Error fetching courses:', err)
      } finally {
        setLoadingCourses(false)
      }
    }
    fetchCourses()
  }, [isStudent, isTeacher])

  // Search for users (by name or institutional ID)
  useEffect(() => {
    if (memberSearch.length >= 2) {
      const searchTimer = setTimeout(async () => {
        try {
          const response = await fetch(`http://localhost:8080/api/users/search?q=${encodeURIComponent(memberSearch)}`, {
            credentials: 'include'
          })
          if (response.ok) {
            const data = await response.json()
            setSearchResults(Array.isArray(data) ? data : [])
          }
        } catch (err) {
          console.error('User search error:', err)
        }
      }, 300)
      return () => clearTimeout(searchTimer)
    } else {
      setSearchResults([])
    }
  }, [memberSearch])

  const handleAddMember = (userToAdd) => {
    // Check if user is already in members list
    const isAlreadyAdded = members.some(m => 
      (m.institutionalId && userToAdd.institutionalId && m.institutionalId === userToAdd.institutionalId) ||
      (m.id && userToAdd.id && m.id === userToAdd.id)
    )
    
    if (!isAlreadyAdded) {
      const newMember = {
        institutionalId: userToAdd.institutionalId || userToAdd.id,
        displayName: userToAdd.displayName,
        email: userToAdd.email,
        id: userToAdd.id
      }
      setMembers([...members, newMember])
      
      // If student and this is the first member (or no leader selected), auto-set creator as leader
      if (isStudent && (!selectedLeader || members.length === 0)) {
        // Check if the creator is being added, or set creator as leader
        if (userToAdd.institutionalId === user?.institutionalId || !selectedLeader) {
          setSelectedLeader(user?.institutionalId || '')
        }
      }
      
      // If teacher and no leader selected yet, set first member as default leader
      if (isTeacher && !selectedLeader) {
        setSelectedLeader(newMember.institutionalId)
      }
      
      setMemberSearch('')
      setSearchResults([])
    }
  }

  const handleRemoveMember = (memberToRemove) => {
    setMembers(members.filter(m => 
      !((m.institutionalId && memberToRemove.institutionalId && m.institutionalId === memberToRemove.institutionalId) ||
        (m.id && memberToRemove.id && m.id === memberToRemove.id))
    ))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    
    if (!groupName.trim()) {
      setError('Group name is required')
      return
    }
    
    if (!courseId) {
      setError('Please select a course')
      return
    }
    
    if (members.length === 0) {
      setError('At least one member is required')
      return
    }

    // Extract institutional IDs from member objects
    const memberInstitutionalIds = members.map(m => m.institutionalId || m.id).filter(Boolean)
    
    // Determine leader based on user role
    let leaderStudentId = ''
    
    if (isStudent) {
      // STUDENT: Creator automatically becomes leader
      leaderStudentId = user?.institutionalId || ''
      if (!leaderStudentId) {
        setError('Unable to determine your institutional ID. Please ensure you are logged in.')
        return
      }
      // Ensure creator is in members list
      if (!memberInstitutionalIds.includes(leaderStudentId)) {
        memberInstitutionalIds.unshift(leaderStudentId)
      }
    } else if (isTeacher) {
      // TEACHER: Can assign any member as leader
      if (!selectedLeader) {
        setError('Please select a group leader')
        return
      }
      leaderStudentId = selectedLeader
      // Ensure selected leader is in members list
      if (!memberInstitutionalIds.includes(leaderStudentId)) {
        memberInstitutionalIds.unshift(leaderStudentId)
      }
    } else {
      // Fallback: Use first member or user's ID
      leaderStudentId = selectedLeader || memberInstitutionalIds[0] || (user?.institutionalId || '')
    }
    
    if (!leaderStudentId) {
      setError('Group leader is required')
      return
    }

    setLoading(true)
    try {
      const response = await fetch('http://localhost:8080/api/groups/manual', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          groupName: groupName.trim(),
          leaderStudentId: leaderStudentId,
          courseId: parseInt(courseId),
          memberStudentIds: memberInstitutionalIds
        })
      })

      const data = await response.json()
      
      if (!response.ok) {
        setError(data.errors ? data.errors.join(', ') : 'Failed to create group')
        setLoading(false)
        return
      }

      // Success - format the response for display
      const newGroup = {
        id: data.groupId || Date.now(),
        groupName: data.groupName || groupName,
        subjectCode: data.courseId ? `Course ${data.courseId}` : 'Subject_Code',
        adviser: data.adviserId || 'TBD',
        members: (data.memberStudentIds && data.memberStudentIds.length) || members.length
      }

      setLoading(false)
      onSuccess(newGroup)
      
      // Reset form
      setGroupName('')
      setGroupDetails('')
      setMemberSearch('')
      setMembers([])
      setSearchResults([])
      setSelectedLeader('')
      setCourseId(courses.length > 0 ? courses[0].courseId.toString() : '')
      setError('')
    } catch (err) {
      console.error('Create group error:', err)
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
          <h2 className="create-group-modal-title">Create Group</h2>
          <p className="create-group-modal-subtitle">Start creating your group.</p>
        </div>

        <form className="create-group-form" onSubmit={handleSubmit}>
          <div className="create-group-field">
            <label className="create-group-label">Select Course *</label>
            {loadingCourses ? (
              <div style={{ padding: '0.75rem', color: '#666' }}>Loading courses...</div>
            ) : courses.length === 0 ? (
              <div style={{ padding: '0.75rem', color: '#c33' }}>
                {isStudent ? 'You are not enrolled in any courses yet.' : 'No courses available.'}
              </div>
            ) : (
              <select
                className="create-group-input"
                value={courseId}
                onChange={(e) => setCourseId(e.target.value)}
                required
              >
                <option value="">-- Select a course --</option>
                {courses.map(course => (
                  <option key={course.courseId} value={course.courseId}>
                    {course.courseCode} - {course.courseName}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="create-group-field">
            <label className="create-group-label">Enter Group Name</label>
            <input
              type="text"
              className="create-group-input"
              placeholder="Enter group name"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
              required
            />
          </div>

          <div className="create-group-field">
            <label className="create-group-label">Enter Group Details</label>
            <textarea
              className="create-group-textarea"
              placeholder="Enter group details"
              value={groupDetails}
              onChange={(e) => setGroupDetails(e.target.value)}
              rows="4"
            />
          </div>

          <div className="create-group-field">
            <label className="create-group-label">Add Member to Group</label>
            <div className="create-group-member-input-wrapper">
              <svg className="create-group-search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8"/>
                <path d="M21 21l-4.35-4.35"/>
              </svg>
              <input
                type="text"
                className="create-group-input create-group-member-input"
                placeholder="Search by name or ID (e.g., 22-5689-375 or John Doe)"
                value={memberSearch}
                onChange={(e) => setMemberSearch(e.target.value)}
              />
            </div>
            
            {searchResults.length > 0 && (
              <div className="create-group-search-results">
                {searchResults.map((user, idx) => (
                  <div
                    key={user.id || idx}
                    className="create-group-search-result-item"
                    onClick={() => handleAddMember(user)}
                  >
                    {user.institutionalId || 'N/A'} - {user.displayName || user.email || 'Unknown User'}
                    {user.role && <span style={{ fontSize: '12px', color: '#666', marginLeft: '8px' }}>({user.role})</span>}
                  </div>
                ))}
              </div>
            )}

            {members.length > 0 && (
              <div className="create-group-members-list">
                {members.map((member, idx) => (
                  <div key={idx} className="create-group-member-tag">
                    <span>
                      {member.institutionalId || member.id} - {member.displayName || member.email || 'Unknown'}
                      {isStudent && member.institutionalId === user?.institutionalId && (
                        <span style={{ fontSize: '12px', color: '#1e4487', marginLeft: '8px', fontWeight: '600' }}>
                          (You - Leader)
                        </span>
                      )}
                      {isTeacher && selectedLeader === member.institutionalId && (
                        <span style={{ fontSize: '12px', color: '#1e4487', marginLeft: '8px', fontWeight: '600' }}>
                          (Leader)
                        </span>
                      )}
                    </span>
                    <button
                      type="button"
                      className="create-group-member-remove"
                      onClick={() => {
                        // If removing the selected leader, clear selection
                        if (selectedLeader === member.institutionalId) {
                          setSelectedLeader('')
                        }
                        handleRemoveMember(member)
                      }}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Leader Selection (only for TEACHER role) */}
          {isTeacher && members.length > 0 && (
            <div className="create-group-field">
              <label className="create-group-label">Select Group Leader</label>
              <select
                className="create-group-input"
                style={{ 
                  appearance: 'none',
                  backgroundImage: 'url("data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'12\' height=\'12\' viewBox=\'0 0 12 12\'%3E%3Cpath fill=\'%23333\' d=\'M6 9L1 4h10z\'/%3E%3C/svg%3E")',
                  backgroundRepeat: 'no-repeat',
                  backgroundPosition: 'right 16px center',
                  paddingRight: '40px',
                  cursor: 'pointer'
                }}
                value={selectedLeader}
                onChange={(e) => setSelectedLeader(e.target.value)}
                required
              >
                <option value="">--- Select Leader ---</option>
                {members.map((member, idx) => (
                  <option key={idx} value={member.institutionalId || member.id}>
                    {member.institutionalId || member.id} - {member.displayName || member.email || 'Unknown'}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Info message for STUDENT role */}
          {isStudent && members.length > 0 && (
            <div style={{ 
              padding: '12px', 
              backgroundColor: '#e8f0fe', 
              borderRadius: '8px', 
              fontSize: '14px', 
              color: '#1e4487',
              marginBottom: '1rem'
            }}>
              ℹ️ You will automatically be assigned as the group leader.
            </div>
          )}

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

function CourseManagementPage() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const userData = localStorage.getItem('user')
    if (userData) {
      const userObj = JSON.parse(userData)
      setUser(userObj)
      // Redirect if not a teacher
      if (userObj.role !== 'TEACHER') {
        window.location.href = '/dashboard'
      }
    }
    setLoading(false)
  }, [])

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading...</div>
      </div>
    )
  }

  if (!user || user.role !== 'TEACHER') {
    return null
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="dashboard-header-content">
          <div className="dashboard-logo-section">
            <div className="dashboard-logo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <rect x="2" y="2" width="10" height="10" fill="#0078d4"/>
                <rect x="12" y="2" width="10" height="10" fill="#f0713d"/>
                <rect x="2" y="12" width="10" height="10" fill="#d13438"/>
                <rect x="12" y="12" width="10" height="10" fill="#ffb900"/>
              </svg>
            </div>
            <h1 className="dashboard-title">ScholarSync</h1>
          </div>
          <div className="dashboard-header-right">
            <div className="dashboard-user-avatar-small">
              {user.displayName ? user.displayName.charAt(0).toUpperCase() : 'T'}
            </div>
            <span className="dashboard-user-name-header">{user.displayName || 'Teacher'}</span>
            <button onClick={() => window.location.href = '/login'} className="dashboard-logout-btn">
              Logout
            </button>
            <div className="dashboard-icon-btn">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="#f0713d" stroke="#f0713d" strokeWidth="2">
                <circle cx="12" cy="12" r="5" fill="#f0713d"/>
                <line x1="12" y1="1" x2="12" y2="3" stroke="#f0713d"/>
                <line x1="12" y1="21" x2="12" y2="23" stroke="#f0713d"/>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" stroke="#f0713d"/>
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" stroke="#f0713d"/>
                <line x1="1" y1="12" x2="3" y2="12" stroke="#f0713d"/>
                <line x1="21" y1="12" x2="23" y2="12" stroke="#f0713d"/>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" stroke="#f0713d"/>
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" stroke="#f0713d"/>
              </svg>
            </div>
            <div className="dashboard-notification-btn">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f0713d" strokeWidth="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
              <span className="dashboard-notification-badge">3</span>
            </div>
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        <nav className="dashboard-sidebar">
          <div className="dashboard-sidebar-user">
            <div className="dashboard-user-avatar">
              {user.displayName ? user.displayName.charAt(0).toUpperCase() : 'T'}
            </div>
            <div className="dashboard-sidebar-user-info">
              <div className="dashboard-sidebar-project">IT332 Capstone 1 | GO1-G02</div>
              <div className="dashboard-sidebar-team">Team 01</div>
            </div>
          </div>
          <div className="dashboard-sidebar-nav">
            <button 
              className="dashboard-sidebar-button"
              onClick={() => window.location.href = '/dashboard'}
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
              <span>Create Group</span>
            </button>
            <Link to="/courses" className="dashboard-sidebar-button active">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
              </svg>
              <span>Course Management</span>
            </Link>
          </div>
        </nav>

        <div className="dashboard-content-area">
          <CourseManagement />
        </div>
      </main>
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

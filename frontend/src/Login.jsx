import React from 'react'

export default function Login() {
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

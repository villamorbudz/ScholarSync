import React, { useState } from 'react';
import CreateCourse from './components/CreateCourse';
import CourseList from './components/CourseList';
import './App.css'; // Import global styles

function App() {
  // State to trigger a refresh in CourseList after a new course is created/updated/deleted
  const [refreshKey, setRefreshKey] = useState(0); 
  
  const handleDataChange = () => {
    setRefreshKey(prevKey => prevKey + 1);
  };

  return (
    <div className="App-Layout">
        {/* Mock Header from Wireframe */}
        <header className="header">
            <span className="logo">ScholarSync Professor</span>
            <nav>
                <a href="#home">Home</a>
                <a href="#courses">Courses</a>
                <a href="#schedule">Schedule</a>
            </nav>
            <div className="user-info">Julio M. Dela Pinas</div>
        </header>

        <main className="main-content-container">
            <aside className="sidebar-menu">
                {/* Mock Sidebar Navigation */}
                <h4>Course Management</h4>
                <ul>
                    <li>My Groups</li>
                    <li>Project Progress</li>
                </ul>
            </aside>

            <section className="main-panel">
                <CreateCourse onSuccessfulCreate={handleDataChange} /> 
                <hr className="divider"/>
                {/* Use the refreshKey to tell CourseList when to re-fetch data */}
                <CourseList refreshKey={refreshKey} /> 
            </section>
        </main>
    </div>
  );
}

export default App;
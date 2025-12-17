import React, { useState, useEffect } from 'react';
import axios from 'axios';
import UpdateCourseModal from './UpdateCourseModal'; 

const API_BASE_URL = 'http://localhost:8080/api/courses';

const CourseList = ({ refreshKey }) => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [currentCourse, setCurrentCourse] = useState(null);
  
  // R - Fetch Data (Read)
  const fetchCourses = async () => {
    // 1. Set loading state before starting the fetch
    setLoading(true); 

    try {
      const response = await axios.get(API_BASE_URL);
      
      // 2. SUCCESS: Set data and immediately set loading to false, and clear any error
      setCourses(response.data);
      setError(null);
    } catch (err) {
      // 3. ERROR: Set error message, and set courses to empty array
      console.error('Fetch error:', err);
      setError("Failed to fetch courses. Ensure Spring Boot backend is running.");
      setCourses([]); 
    } finally {
      // 4. Always set loading to false after the operation finishes (success or error)
      setLoading(false);
    }
  };

  useEffect(() => {
    // The effect's sole purpose is to synchronize with the external system (API)
    // when the refreshKey prop changes.
    fetchCourses();
  }, [refreshKey]); 

  // U - Open Update Modal
  const handleEdit = (course) => {
    setCurrentCourse({
        ...course,
        startDate: course.startDate, 
        endDate: course.endDate,
    });
    setIsModalOpen(true);
  };
  
  // U - Handle Modal Input Change (passes data up to CourseList state)
  const handleModalChange = (name, value) => {
      setCurrentCourse(prev => ({ ...prev, [name]: value }));
  };

  // U - Submit Update
  const handleUpdateSubmit = async (e) => {
    e.preventDefault();
    try {
        await axios.put(`${API_BASE_URL}/${currentCourse.id}`, currentCourse);
        alert('Course updated successfully!');
        setIsModalOpen(false);
        // Trigger a refresh by leveraging the refreshKey mechanism
        // Instead of calling fetchCourses() directly here, rely on the main useEffect 
        // which will be triggered by a change in refreshKey.
        // NOTE: Since we don't have the refreshKey setter here, 
        // we'll stick to direct call for simplicity in this component, but 
        // the original fetchCourses() call is generally fine in a success handler.
        fetchCourses(); 

    } catch (err) {
        alert('Error updating course: ' + (err.response?.data?.message || err.message));
    }
  };

  // D - Delete Course
  const handleDelete = async (id) => {
    if (window.confirm("CONFIRM DELETION: Are you sure you want to delete this course?")) {
        try {
            await axios.delete(`${API_BASE_URL}/${id}`);
            alert('Course deleted successfully!');
            fetchCourses(); // Direct call is acceptable in event handlers
        // eslint-disable-next-line no-unused-vars
        } catch (err) {
             alert('Error deleting course. Check backend logs.');
        }
    }
  };

  if (loading) return <p className="loading-message">Loading active courses...</p>;
  if (error) return <p className="error-message">{error}</p>;

  return (
    <div className="course-list-container">
      <h3>Course List (Read, Update, Delete)</h3>
      
      <UpdateCourseModal 
        isOpen={isModalOpen}
        course={currentCourse}
        onUpdate={handleUpdateSubmit}
        onClose={() => setIsModalOpen(false)}
        onChange={handleModalChange}
      />

      <table className="course-table">
        <thead>
          <tr>
            <th>Code</th>
            <th>Course Name</th>
            <th>Duration</th>
            <th>Adviser</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {courses.length > 0 ? courses.map((course) => (
            <tr key={course.id}>
              <td>{course.courseCode}</td>
              <td>{course.courseName}</td>
              <td>{course.startDate} to {course.endDate}</td>
              <td>{course.adviserName}</td>
              <td className="action-buttons">
                <button className="edit-btn" onClick={() => handleEdit(course)}>Manage (U)</button>
                <button className="delete-btn" onClick={() => handleDelete(course.id)}>Delete (D)</button>
              </td>
            </tr>
          )) : (
              <tr><td colSpan="5">No courses found. Create one above!</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default CourseList;
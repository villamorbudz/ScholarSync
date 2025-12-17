import React, { useState } from 'react';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/courses';
const ADVISERS = [
  { id: 1, name: 'Dr. John Smith' },
  { id: 2, name: 'Prof. Mary Jones' },
];

const CreateCourse = ({ onSuccessfulCreate }) => {
  const [formData, setFormData] = useState({
    courseName: '',
    courseCode: '',
    courseDetails: '',
    startDate: '',
    endDate: '',
    adviserName: '',
  });
  const [message, setMessage] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('Creating course...');
    setIsSuccess(false);

    // --- START OF UPDATE: Ensure data payload is clean and correct ---
    const payload = {
        // Use spread operator for all other simple text fields
        ...formData,
        // Explicitly format date strings to guarantee YYYY-MM-DD format (ISO 8601), 
        // matching the @JsonFormat in Course.java
        startDate: formData.startDate.substring(0, 10), 
        endDate: formData.endDate.substring(0, 10),
    };

    try {
      // Send the guaranteed correctly-formatted payload
      const response = await axios.post(API_BASE_URL, payload);
      // --- END OF UPDATE ---
      
      setMessage(`Course '${response.data.courseName}' created successfully!`);
      setIsSuccess(true);
      
      // Call the callback function to trigger the CourseList refresh
      onSuccessfulCreate();

      // Reset form
      setFormData({
        courseName: '',
        courseCode: '',
        courseDetails: '',
        startDate: '',
        endDate: '',
        adviserName: '',
      });

    } catch (error) {
      // Log the specific response data for better debugging, if available
      console.error('Error creating course:', error.response?.data || error.message);
      setMessage("Connection lost. Unable to save changes. Please check network/backend.");
      setIsSuccess(false);
    }
  };

  return (
    <div className="create-course-container">
      <h2>Create Course</h2>
      {message && (
        <div className={`message ${isSuccess ? 'success' : 'error'}`}>{message}</div>
      )}

      <form onSubmit={handleSubmit} className="course-form">
        <div className="form-group">
          <label htmlFor="courseName">Enter Course Name</label>
          <input type="text" id="courseName" name="courseName" value={formData.courseName} onChange={handleChange} required />
        </div>

        <div className="form-group">
          <label htmlFor="courseCode">Enter Course Code</label>
          <input type="text" id="courseCode" name="courseCode" value={formData.courseCode} onChange={handleChange} required />
        </div>

        <div className="form-group">
          <label htmlFor="courseDetails">Enter Course Details</label>
          <textarea id="courseDetails" name="courseDetails" value={formData.courseDetails} onChange={handleChange} required rows="5" />
        </div>

        <div className="form-group date-duration-group">
          <label>Enter Course Duration</label>
          <div className="date-inputs">
            {/* The input type="date" ensures the YYYY-MM-DD format is in the value */}
            <input type="date" name="startDate" value={formData.startDate} onChange={handleChange} required />
            <span>to</span>
            <input type="date" name="endDate" value={formData.endDate} onChange={handleChange} required />
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="adviserName">Select Adviser</label>
          <select id="adviserName" name="adviserName" value={formData.adviserName} onChange={handleChange} required >
            <option value="" disabled>--- Select ---</option>
            {ADVISERS.map(adviser => (
              <option key={adviser.id} value={adviser.name}>{adviser.name}</option>
            ))}
          </select>
        </div>

        <div className="form-actions">
          <button type="submit" className="create-course-button">Create Course</button>
        </div>
      </form>
    </div>
  );
};

export default CreateCourse;
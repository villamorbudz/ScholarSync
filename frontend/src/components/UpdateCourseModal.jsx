import React from 'react';

const UpdateCourseModal = ({ isOpen, course, onUpdate, onClose, onChange }) => {
    if (!isOpen || !course) return null;

    const handleModalChange = (e) => {
        const { name, value } = e.target;
        onChange(name, value);
    };

    return (
        <div className="modal-backdrop">
            <form className="modal-content" onSubmit={onUpdate}>
                <h3>Manage Course: {course.courseName}</h3>
                
                <label>Name: <input type="text" name="courseName" value={course.courseName} onChange={handleModalChange} required /></label>
                <label>Code: <input type="text" name="courseCode" value={course.courseCode} onChange={handleModalChange} required /></label>
                <label>Details: <textarea name="courseDetails" value={course.courseDetails} onChange={handleModalChange} rows="3" required /></label>
                <label>Start Date: <input type="date" name="startDate" value={course.startDate} onChange={handleModalChange} required /></label>
                <label>End Date: <input type="date" name="endDate" value={course.endDate} onChange={handleModalChange} required /></label>
                
                <div className="modal-actions">
                    <button type="submit" className="button-primary">Save Changes</button>
                    <button type="button" className="button-secondary" onClick={onClose}>Cancel</button>
                </div>
            </form>
        </div>
    );
};

export default UpdateCourseModal;
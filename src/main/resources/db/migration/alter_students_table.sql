-- Migration script to change students table primary key from single to composite
-- This allows students to enroll in multiple courses

-- Step 1: Drop the existing primary key constraint
ALTER TABLE `students` DROP PRIMARY KEY;

-- Step 2: Add a composite primary key on (student_id, course_id)
ALTER TABLE `students` ADD PRIMARY KEY (`student_id`, `course_id`);

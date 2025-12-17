-- Migration script to fix Student table to support multiple course enrollments
-- This changes the PRIMARY KEY from just student_id to a composite key (student_id, course_id)

-- Step 1: Drop the existing PRIMARY KEY constraint
ALTER TABLE `students` DROP PRIMARY KEY;

-- Step 2: Add a new composite PRIMARY KEY on (student_id, course_id)
ALTER TABLE `students` ADD PRIMARY KEY (`student_id`, `course_id`);

-- Note: If you have existing data, make sure there are no duplicate (student_id, course_id) pairs
-- If duplicates exist, you'll need to remove them first before running this script

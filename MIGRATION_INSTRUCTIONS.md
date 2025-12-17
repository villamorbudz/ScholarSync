# Database Migration Instructions

## Problem
The `students` table currently has `student_id` as the PRIMARY KEY, which prevents students from enrolling in multiple courses. We need to change it to a composite primary key of `(student_id, course_id)`.

## Solution
Run the following SQL commands in your MySQL database:

```sql
USE scholarsync;

-- Drop the existing primary key constraint
ALTER TABLE `students` DROP PRIMARY KEY;

-- Add a composite primary key on (student_id, course_id)
ALTER TABLE `students` ADD PRIMARY KEY (`student_id`, `course_id`);
```

## How to Run
1. Open MySQL Command Line Client or MySQL Workbench
2. Connect to your MySQL server
3. Select the `scholarsync` database: `USE scholarsync;`
4. Run the ALTER TABLE commands above
5. Restart your Spring Boot application

## Verification
After running the migration, verify the change:
```sql
SHOW CREATE TABLE students;
```

You should see the PRIMARY KEY defined as `PRIMARY KEY (student_id, course_id)` instead of just `PRIMARY KEY (student_id)`.

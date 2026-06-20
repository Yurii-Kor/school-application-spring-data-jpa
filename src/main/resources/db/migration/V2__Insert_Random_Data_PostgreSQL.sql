INSERT INTO groups (group_name)
SELECT CONCAT(
         chr(65 + floor(random() * 26)::int),
         chr(65 + floor(random() * 26)::int),
         '-',
         lpad(floor(random() * 100)::text, 2, '0')
       )
FROM generate_series(1,10);

INSERT INTO courses (course_name, course_description) VALUES
  ('Mathematics', 'Course covering fundamental mathematical concepts.'),
  ('Biology', 'Introduction to the study of living organisms.'),
  ('Chemistry', 'Basics of chemical reactions and properties.'),
  ('Physics', 'Fundamentals of matter, energy, and forces.'),
  ('History', 'Overview of historical events and trends.'),
  ('Geography', 'Study of the Earth and its features.'),
  ('Literature', 'Exploration of literary works and genres.'),
  ('Computer Science', 'Introduction to computer programming and algorithms.'),
  ('Economics', 'Principles of economic theory and practice.'),
  ('Art', 'Appreciation and study of visual arts.');

DO $$ 
DECLARE 
    start_group_id BIGINT := 100; 
    start_student_id BIGINT := 1000;
    total_students INT := 200;
    total_groups INT := 10;
    max_students_per_group INT := 30;
    min_courses_per_student INT := 1;
    max_courses_per_student INT := 3; 

    new_student_id BIGINT;
    group_counts INT[] := array_fill(0, ARRAY[10]);
    group_index INT;
    group_id BIGINT;
    course_count INT;
BEGIN 
    FOR i IN 0..total_students-1 LOOP
        LOOP
            group_index := floor(random() * total_groups)::INT;  
            EXIT WHEN group_counts[group_index] < max_students_per_group;
        END LOOP;

        group_counts[group_index] := group_counts[group_index] + 1;
        group_id := group_index + start_group_id;

        INSERT INTO students (group_id, first_name, last_name)
        VALUES (
            group_id,
            (SELECT unnest(array[
                'Alice','Bob','Charlie','David','Eva','Frank','Grace','Hannah','Ian','Jane',
                'Kevin','Laura','Michael','Nina','Oliver','Pam','Quentin','Rachel','Steve','Tina'
            ]) ORDER BY random() LIMIT 1),
            (SELECT unnest(array[
                'Anderson','Brown','Clark','Davis','Evans','Franklin','Garcia','Harris','Ivanov','Johnson',
                'King','Lewis','Martinez','Nelson','Olsen','Perez','Quinn','Roberts','Smith','Turner'
            ]) ORDER BY random() LIMIT 1)
        );
    END LOOP;
    
    FOR i IN start_student_id..(start_student_id + total_students - 1) LOOP
        INSERT INTO students_courses (student_id, course_id)
        SELECT i::BIGINT, course_id 
        FROM (
            SELECT course_id FROM courses ORDER BY random() LIMIT (floor(random() * (max_courses_per_student - min_courses_per_student + 1)) + min_courses_per_student)
        ) AS subquery
        ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

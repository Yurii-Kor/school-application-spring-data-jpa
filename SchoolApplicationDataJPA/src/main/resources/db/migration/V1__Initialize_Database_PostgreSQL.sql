DROP TABLE IF EXISTS student_courses;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS groups;

DROP SEQUENCE IF EXISTS group_id_seq;
DROP SEQUENCE IF EXISTS student_id_seq;
DROP SEQUENCE IF EXISTS course_id_seq;

CREATE SEQUENCE group_id_seq START 100;
CREATE TABLE groups (
    group_id BIGINT PRIMARY KEY DEFAULT nextval('group_id_seq'),
    group_name VARCHAR(255) NOT NULL UNIQUE
);

CREATE SEQUENCE student_id_seq START 1000;
CREATE TABLE students (
    student_id BIGINT PRIMARY KEY DEFAULT nextval('student_id_seq'),
    group_id BIGINT REFERENCES groups(group_id) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL
);

CREATE SEQUENCE course_id_seq START 10;
CREATE TABLE courses (
    course_id BIGINT PRIMARY KEY DEFAULT nextval('course_id_seq'),
    course_name VARCHAR(255) NOT NULL UNIQUE,
    course_description TEXT
);

CREATE TABLE students_courses (
    student_id BIGINT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(course_id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, course_id)
);

package ua.foxminded.schoolapplication.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ua.foxminded.schoolapplication.model.domain.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

}

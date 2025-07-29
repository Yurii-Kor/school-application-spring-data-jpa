package ua.foxminded.schoolapplication.testutil;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.model.repository.CourseRepository;
import ua.foxminded.schoolapplication.model.repository.GroupRepository;
import ua.foxminded.schoolapplication.model.repository.StudentRepository;

@Component
public class TestDataInitializer {

	private final GroupRepository groupRepository;
	private final StudentRepository studentRepository;
	private final CourseRepository courseRepository;

	public TestDataInitializer(GroupRepository groupRepository, StudentRepository studentRepository,
			CourseRepository courseRepository) {

		this.groupRepository = groupRepository;
		this.studentRepository = studentRepository;
		this.courseRepository = courseRepository;
	}

	@Transactional
	public TestEntities initialize(List<Group> rawGroups, List<Course> rawCourses, List<Student> rawStudents) {
		List<Group> savedGroups = rawGroups.isEmpty() ? List.of() : groupRepository.saveAll(rawGroups);
		List<Course> savedCourses = rawCourses.isEmpty() ? List.of() : courseRepository.saveAll(rawCourses);
		List<Student> savedStudents = rawStudents.isEmpty() ? List.of() : studentRepository.saveAll(rawStudents);

		return new TestEntities(savedGroups, savedCourses, savedStudents);
	}
}

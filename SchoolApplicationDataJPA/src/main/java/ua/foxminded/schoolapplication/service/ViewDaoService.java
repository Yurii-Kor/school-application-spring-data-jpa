package ua.foxminded.schoolapplication.service;

import java.util.List;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.model.repository.CourseRepository;
import ua.foxminded.schoolapplication.model.repository.GroupRepository;
import ua.foxminded.schoolapplication.model.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(value = TxType.REQUIRES_NEW)
public class ViewDaoService {

	private static final Logger logger = LoggerFactory.getLogger(ViewDaoService.class);

	private final GroupRepository groupRepository;
	private final StudentRepository studentRepository;
	private final CourseRepository courseRepository;

	private final EntityValidationService validator;

	public ViewDaoService(GroupRepository groupRepository, StudentRepository studentRepository,
			CourseRepository courseRepository, EntityValidationService validator) {

		this.groupRepository = groupRepository;
		this.studentRepository = studentRepository;
		this.courseRepository = courseRepository;
		this.validator = validator;
	}

	public List<Group> findGroupsWithStudentCountLessOrEqual(String maxStudentsInput) {
		validateInputString(maxStudentsInput, "Maximum students input must not be null or empty");
		logger.debug("Received input for maxStudents: '{}'", maxStudentsInput);

		long parsed = parseLong(maxStudentsInput);
		validateNonNegativeLong(parsed, String.format("Maximum number of students cannot be negative: %d", parsed));
		int maxStudents = parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;

		List<Group> result = groupRepository.findGroupsWithStudentCountLessThanOrEqualTo(maxStudents);
		logger.debug("Found {} groups with student count <= {}", result.size(), maxStudents);

		return result;
	}

	public Set<Student> findStudentsByCourseName(String courseNameInput) {
		validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received input for course name: '{}'", courseNameInput);

		Course course = courseRepository.findByCourseName(courseNameInput).orElseThrow(() -> {
			logger.warn("No course found with name '{}'", courseNameInput);
			throw new EntityNotFoundException(
					String.format("Could not find students because course '%s' seems to have vanished mysteriously",
							courseNameInput));
		});

		Set<Student> students = course.getStudents();
		logger.debug("Found {} student(s) enrolled in course '{}'", students.size(), courseNameInput);

		return students;
	}

	public Student addStudent(Student student) {
		validator.validateEntities(List.of(student));

		return studentRepository.save(student);
	}

	public Student deleteStudentById(String studentIdInput) {
		validateInputString(studentIdInput, "Student ID input must not be null or empty");

		logger.debug("Received input to delete student by ID: '{}'", studentIdInput);

		Long parsedId = parseLong(studentIdInput);
		validateNonNegativeLong(parsedId, String.format("Student ID cannot be negative: %d", parsedId));

		Student studentToDelete = studentRepository.findById(parsedId).orElseThrow(() -> {
			logger.warn("Student with ID {} does not exist", parsedId);
			return new EntityNotFoundException(
					String.format("Cannot delete student: no student found with ID %d", parsedId));
		});

		studentRepository.delete(studentToDelete);

		return studentToDelete;
	}

	public Course addStudentToCourse(String studentIdInput, String courseNameInput) {
		validateInputString(studentIdInput, "Student ID input must not be null or empty");
		validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received request to add student '{}' to course '{}'", studentIdInput, courseNameInput);

		long parsedStudentId = parseLong(studentIdInput);
		validateNonNegativeLong(parsedStudentId, String.format("Student ID cannot be negative: %d", parsedStudentId));

		Course course = courseRepository.findByCourseName(courseNameInput).orElseThrow(() -> {
			logger.warn("Course with name '{}' does not exist", courseNameInput);
			return new EntityNotFoundException(
					String.format("Cannot add student to course: course '%s' not found", courseNameInput));
		});

		Student student = studentRepository.getReferenceById(parsedStudentId);

		logger.debug("Adding student '{}' to course '{}'", student, course);
		course.getStudents().add(student);
		logger.info("Student with ID {} successfully added to course '{}'", student.getId(), courseNameInput);

		return course;
	}

	public Course removeStudentFromCourse(String studentIdInput, String courseNameInput) {
		validateInputString(studentIdInput, "Student ID input must not be null or empty");
		validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received request to remove student '{}' from course '{}'", studentIdInput, courseNameInput);

		long parsedStudentId = parseLong(studentIdInput);
		validateNonNegativeLong(parsedStudentId, String.format("Student ID cannot be negative: %d", parsedStudentId));

		Course course = courseRepository.findByCourseName(courseNameInput).orElseThrow(() -> {
			logger.warn("Course with name '{}' does not exist", courseNameInput);
			return new EntityNotFoundException(
					String.format("Cannot remove student from course: course '%s' not found", courseNameInput));
		});

		logger.debug("Removing student id '{}' from course '{}'", studentIdInput, course);
		boolean removed = course.getStudents().removeIf(student -> student.getId().equals(parsedStudentId));

		if (removed) {
			logger.info("Student with ID {} successfully removed from course '{}'", parsedStudentId, courseNameInput);
		} else {
			logger.warn("Student with ID {} was not found in course '{}'", parsedStudentId, courseNameInput);
		}

		return course;
	}

	private Long parseLong(String input) {
		try {
			return Long.parseLong(input);
		} catch (NumberFormatException e) {
			logger.error("Failed to parse input '{}' as number", input, e);
			throw new IllegalArgumentException(String.format("Invalid Input: '%s' is not a number.", input), e);
		}
	}

	private void validateInputString(String input, String errorMessage) {
		if (input == null || input.trim().isEmpty()) {
			logger.warn("Provided input string is null or empty");
			throw new IllegalArgumentException(errorMessage);
		}
	}

	private void validateNonNegativeLong(Long id, String errorMessage) {
		if (id < 0) {
			logger.warn("Provided ID is negative: {}", id);
			throw new IllegalArgumentException(errorMessage);
		}
	}
}

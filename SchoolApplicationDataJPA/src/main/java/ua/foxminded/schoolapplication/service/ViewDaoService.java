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
	private final InputPreprocessingService inputPreprocessor;

	public ViewDaoService(GroupRepository groupRepository, StudentRepository studentRepository,
			CourseRepository courseRepository, EntityValidationService validator,
			InputPreprocessingService inputPreprocessor) {

		this.groupRepository = groupRepository;
		this.studentRepository = studentRepository;
		this.courseRepository = courseRepository;
		this.validator = validator;
		this.inputPreprocessor = inputPreprocessor;
	}

	public List<Group> findGroupsWithStudentCountLessOrEqual(String maxStudentsInput) {
		inputPreprocessor.validateInputString(maxStudentsInput, "Maximum students input must not be null or empty");
		logger.debug("Received input for maxStudents: '{}'", maxStudentsInput);

		Long parsed = inputPreprocessor.parseLong(maxStudentsInput);
		inputPreprocessor.validateNonNegativeLong(parsed,
				String.format("Maximum number of students cannot be negative: %d", parsed));
		Integer maxStudents = inputPreprocessor.parseInteger(parsed);

		List<Long> resultIds = groupRepository.findGroupIdsByStudentCountLessThanOrEqualTo(maxStudents);
		if (resultIds.isEmpty()) {
			logger.debug("No Groups Found with student count <= {}", maxStudents);
			return List.of();
		}

		List<Group> result = groupRepository.findAllById(resultIds);
		logger.debug("Found {} groups with student count <= {}", result.size(), maxStudents);

		return result;
	}

	public Set<Student> findStudentsByCourseName(String courseNameInput) {
		inputPreprocessor.validateInputString(courseNameInput, "Course name must not be null or empty");
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
		inputPreprocessor.validateInputString(studentIdInput, "Student ID input must not be null or empty");

		logger.debug("Received input to delete student by ID: '{}'", studentIdInput);

		Long parsedId = inputPreprocessor.parseLong(studentIdInput);
		inputPreprocessor.validateNonNegativeLong(parsedId,
				String.format("Student ID cannot be negative: %d", parsedId));

		Student studentToDelete = studentRepository.findById(parsedId).orElseThrow(() -> {
			logger.warn("Student with ID {} does not exist", parsedId);
			return new EntityNotFoundException(
					String.format("Cannot delete student: no student found with ID %d", parsedId));
		});

		studentRepository.delete(studentToDelete);

		return studentToDelete;
	}

	public Course addStudentToCourse(String studentIdInput, String courseNameInput) {
		inputPreprocessor.validateInputString(studentIdInput, "Student ID input must not be null or empty");
		inputPreprocessor.validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received request to add student '{}' to course '{}'", studentIdInput, courseNameInput);

		Long parsedStudentId = inputPreprocessor.parseLong(studentIdInput);
		inputPreprocessor.validateNonNegativeLong(parsedStudentId,
				String.format("Student ID cannot be negative: %d", parsedStudentId));

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
		inputPreprocessor.validateInputString(studentIdInput, "Student ID input must not be null or empty");
		inputPreprocessor.validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received request to remove student '{}' from course '{}'", studentIdInput, courseNameInput);

		Long parsedStudentId = inputPreprocessor.parseLong(studentIdInput);
		inputPreprocessor.validateNonNegativeLong(parsedStudentId,
				String.format("Student ID cannot be negative: %d", parsedStudentId));

		Course course = courseRepository.findByCourseName(courseNameInput).orElseThrow(() -> {
			logger.warn("Course with name '{}' does not exist", courseNameInput);
			return new EntityNotFoundException(
					String.format("Cannot remove student from course: course '%s' not found", courseNameInput));
		});

		logger.debug("Removing student id '{}' from course '{}'", studentIdInput, course);
		Boolean removed = course.getStudents().removeIf(student -> student.getId().equals(parsedStudentId));

		if (removed) {
			logger.info("Student with ID {} successfully removed from course '{}'", parsedStudentId, courseNameInput);
		} else {
			logger.warn("Student with ID {} was not found in course '{}'", parsedStudentId, courseNameInput);
		}

		return course;
	}
}

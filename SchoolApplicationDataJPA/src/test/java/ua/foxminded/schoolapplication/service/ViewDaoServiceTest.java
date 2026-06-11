package ua.foxminded.schoolapplication.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.schoolapplication.config.ValidatorConfig;
import ua.foxminded.schoolapplication.exception.FieldConstraintException;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.testutil.TestDataInitializer;
import ua.foxminded.schoolapplication.testutil.TestEntities;
import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

@DataJpaTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ ViewDaoService.class, EntityValidationService.class, TestcontainersConfiguration.class,
		TestDataInitializer.class, ValidatorConfig.class, InputPreprocessingService.class })

class ViewDaoServiceTest {

	static final String GROUP_NAME = "serviceTestGroup-11";
	static final String FIRST_NAME = "John";
	static final String LAST_NAME = "Doe";
	static final String HISTORY = "History";

	static final String NON_EXISTENT = "NonExistent";
	static final Long NON_EXISTENT_ID = 999L;

	static final String STUDENTS = "5";
	static final String NEGATIVE_NUMBER = "-5";
	static final String NOT_NUMBER = "abc";
	static final String EMPTY_STRING = "   ";
	static final int ONE_GROUP = 1;
	static final int ONE_STUDENT = 1;

	@Autowired
	private TestDataInitializer initializer;

	@Autowired
	private ViewDaoService viewDaoService;

	private Group testGroup;
	private Course testCourse;
	private Student testStudent;
	private Student studentToDelede;
	private Student shouldBeAddedToCourse;
	private Student shouldBeRemovedFromCourse;

	@BeforeAll
	void setup() {
		testGroup = Group.builder().groupName(GROUP_NAME).build();
		testCourse = Course.builder().courseName(HISTORY).build();
		testStudent = Student.builder()
				.group(testGroup)
				.firstName(FIRST_NAME)
				.lastName(LAST_NAME)
				.courses(Set.of(testCourse))
				.build();
		studentToDelede = Student.builder().group(testGroup).firstName(FIRST_NAME).lastName(LAST_NAME).build();
		shouldBeAddedToCourse = Student.builder().group(testGroup).firstName(FIRST_NAME).lastName(LAST_NAME).build();
		shouldBeRemovedFromCourse = Student.builder()
				.group(testGroup)
				.firstName(FIRST_NAME)
				.lastName(LAST_NAME)
				.courses(Set.of(testCourse))
				.build();

		TestEntities entities = initializer.initialize(List.of(testGroup),
				List.of(testCourse),
				List.of(testStudent, studentToDelede, shouldBeAddedToCourse, shouldBeRemovedFromCourse));

		testGroup = entities.groups().get(0);
		testCourse = entities.courses().get(0);
		testStudent = entities.students().get(0);
		studentToDelede = entities.students().get(1);
		shouldBeAddedToCourse = entities.students().get(2);
		shouldBeRemovedFromCourse = entities.students().get(3);
	}

	@Test
	@DisplayName("Should find groups with student count <= " + STUDENTS)
	void findGroupsWithStudentCountLessOrEqualShouldReturnValidGroup() {
		List<Group> result = viewDaoService.findGroupsWithStudentCountLessOrEqual(STUDENTS);

		assertEquals(ONE_GROUP, result.size(), "Should return one group");
		assertEquals(testGroup.getId(), result.get(0).getId(), "Returned group ID should match test group ID");
	}

	@Test
	@DisplayName("Should throw exception for negative student count")
	void findGroupsWithNegativeStudentCountShouldThrowException() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.findGroupsWithStudentCountLessOrEqual(NEGATIVE_NUMBER));

		assertTrue(exception.getMessage().contains("cannot be negative"),
				"Exception message should indicate negative input is invalid");
	}

	@Test
	@DisplayName("Should throw exception for non-numeric input")
	void findGroupsWithNonNumericInputShouldThrowException() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.findGroupsWithStudentCountLessOrEqual(NOT_NUMBER));

		assertTrue(exception.getMessage().contains("is not a number"),
				"Exception message should indicate non-numeric input");
	}

	@Test
	@DisplayName("Should return students enrolled in course")
	void findStudentsByCourseNameShouldReturnStudents() {
		Set<Student> result = viewDaoService.findStudentsByCourseName(testCourse.getCourseName());

		assertTrue(result.size() > ONE_STUDENT, "Should return more then one student");
		assertTrue(result.contains(testStudent), "Returned student set should consist test student");
	}

	@Test
	@DisplayName("Should throw exception if course name does not exist")
	void findStudentsByNonexistentCourseNameShouldThrowException() {
		EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
				() -> viewDaoService.findStudentsByCourseName(NON_EXISTENT));

		assertTrue(exception.getMessage().contains("vanished mysteriously"),
				"Exception message should indicate missing course");
	}

	@Test
	@DisplayName("Should successfully save a valid student")
	void addStudentShouldSucceed() {
		Student newStudent = Student.builder().group(testGroup).firstName(FIRST_NAME).lastName(LAST_NAME).build();

		Student saved = assertDoesNotThrow(() -> viewDaoService.addStudent(newStudent));
		assertNotNull(saved.getId(), "Student ID should be assigned after saving");
		assertEquals(FIRST_NAME, saved.getFirstName());
	}

	@Test
	@DisplayName("Should throw exception when student fails validation")
	void addStudentWithInvalidDataShouldThrowValidationException() {
		Student invalidStudent = Student.builder().group(testGroup).firstName(null).lastName(LAST_NAME).build();

		assertThrows(FieldConstraintException.class, () -> viewDaoService.addStudent(invalidStudent));
	}

	@Test
	@DisplayName("Should throw exception when saving student with non-existent group")
	void addStudentWithNonExistentGroupShouldThrowConstraintViolationException() {
		Group detachedGroup = Group.builder().id(NON_EXISTENT_ID).groupName(NON_EXISTENT).build();

		Student invalidStudent = Student.builder()
				.group(detachedGroup)
				.firstName(FIRST_NAME)
				.lastName(LAST_NAME)
				.build();

		assertThrows(DataIntegrityViolationException.class, () -> viewDaoService.addStudent(invalidStudent));
	}

	@Test
	@DisplayName("Should delete existing student by ID and return the student")
	void deleteStudentByIdShouldSucceed() {
		String studentIdStr = studentToDelede.getId().toString();

		Student deletedStudent = assertDoesNotThrow(() -> viewDaoService.deleteStudentById(studentIdStr));

		assertNotNull(deletedStudent, "Deleted student should not be null");
		assertEquals(studentToDelede.getId(), deletedStudent.getId(), "IDs should match");
		assertEquals(studentToDelede.getFirstName(), deletedStudent.getFirstName(), "First names should match");
		assertEquals(studentToDelede.getLastName(), deletedStudent.getLastName(), "Last names should match");
	}

	@Test
	@DisplayName("Should throw exception when student ID does not exist")
	void deleteStudentWithNonexistentIdShouldThrowException() {
		EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
				() -> viewDaoService.deleteStudentById(NON_EXISTENT_ID.toString()));

		assertTrue(exception.getMessage().toLowerCase().contains("no student"),
				"Exception message should indicate that student was not found");
	}

	@Test
	@DisplayName("Should throw exception when student ID is not a number")
	void deleteStudentWithNonNumericIdShouldThrowException() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.deleteStudentById(NOT_NUMBER));

		assertTrue(exception.getMessage().toLowerCase().contains("not a number"),
				"Exception message should indicate non-numeric input");
	}

	@Test
	@DisplayName("Should throw exception when student ID is negative")
	void deleteStudentWithNegativeIdShouldThrowException() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.deleteStudentById(NEGATIVE_NUMBER));

		assertTrue(exception.getMessage().toLowerCase().contains("cannot be negative"),
				"Exception message should indicate negative ID");
	}

	@Test
	@DisplayName("Should throw exception when student ID input is empty")
	void deleteStudentWithEmptyIdShouldThrowException() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.deleteStudentById(EMPTY_STRING));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"),
				"Exception message should indicate empty input");
	}

	@Test
	@DisplayName("Should throw exception when student ID input is null")
	void deleteStudentWithNullIdShouldThrowException() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.deleteStudentById(null));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"),
				"Exception message should indicate null input");
	}

	@Test
	@DisplayName("Should successfully add student to course and return updated course")
	void addStudentToCourseShouldSucceed() {
		Course updatedCourse = assertDoesNotThrow(() -> viewDaoService
				.addStudentToCourse(shouldBeAddedToCourse.getId().toString(), testCourse.getCourseName()));

		assertNotNull(updatedCourse.getId(), "Returned course should have an ID");

		boolean studentAdded = updatedCourse.getStudents()
				.stream()
				.anyMatch(student -> student.equals(shouldBeAddedToCourse));

		assertTrue(studentAdded, "Student should be present in the updated course's students set");
	}

	@Test
	@DisplayName("Should throw exception when adding non-existent student to course")
	void addNonExistentStudentToCourseShouldThrowException() {
		String nonExistentStudentId = NON_EXISTENT_ID.toString();

		assertThrows(EntityNotFoundException.class,
				() -> viewDaoService.addStudentToCourse(nonExistentStudentId, testCourse.getCourseName()));
	}

	@Test
	@DisplayName("Should throw exception when adding student to non-existent course")
	void addStudentToNonExistentCourseShouldThrowException() {
		String nonExistentCourseName = NON_EXISTENT;

		EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
				() -> viewDaoService.addStudentToCourse(shouldBeAddedToCourse.getId().toString(),
						nonExistentCourseName));

		assertTrue(exception.getMessage().contains("course"), "Exception message should indicate missing course");
	}

	@Test
	@DisplayName("Should throw exception when student ID is null")
	void addStudentToCourseShouldFailForNullStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.addStudentToCourse(null, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

	@Test
	@DisplayName("Should throw exception when student ID is empty")
	void addStudentToCourseShouldFailForEmptyStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.addStudentToCourse(EMPTY_STRING, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

	@Test
	@DisplayName("Should throw exception when student ID is not a number")
	void addStudentToCourseShouldFailForNonNumericStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.addStudentToCourse(NOT_NUMBER, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("not a number"));
	}

	@Test
	@DisplayName("Should throw exception when student ID is negative")
	void addStudentToCourseShouldFailForNegativeStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.addStudentToCourse(NEGATIVE_NUMBER, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("cannot be negative"));
	}

	@Test
	@DisplayName("Should throw exception when course name is null")
	void addStudentToCourseShouldFailForNullCourseName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.addStudentToCourse(String.valueOf(testStudent.getId()), null));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

	@Test
	@DisplayName("Should throw exception when course name is empty")
	void addStudentToCourseShouldFailForEmptyCourseName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.addStudentToCourse(String.valueOf(testStudent.getId()), EMPTY_STRING));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

	@Test
	@DisplayName("Should successfully remove student from course and return updated course")
	void removeStudentFromCourseShouldSucceed() {
		Course updatedCourse = assertDoesNotThrow(() -> viewDaoService
				.removeStudentFromCourse(shouldBeRemovedFromCourse.getId().toString(), testCourse.getCourseName()));

		assertNotNull(updatedCourse.getId(), "Returned course should have an ID");

		boolean studentStillPresent = updatedCourse.getStudents()
				.stream()
				.anyMatch(student -> student.equals(shouldBeRemovedFromCourse));

		assertFalse(studentStillPresent, "Student should be removed from the updated course's students set");
	}

	@Test
	@DisplayName("Should throw exception when removing student from non-existent course")
	void removeStudentFromNonExistentCourseShouldThrowException() {
		String nonExistentCourseName = NON_EXISTENT;

		EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
				() -> viewDaoService.removeStudentFromCourse(shouldBeRemovedFromCourse.getId().toString(),
						nonExistentCourseName));

		assertTrue(exception.getMessage().contains("course"), "Exception message should indicate missing course");
	}

	@Test
	@DisplayName("Should throw exception when student ID is null")
	void removeStudentFromCourseShouldFailForNullStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.removeStudentFromCourse(null, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

	@Test
	@DisplayName("Should throw exception when student ID is empty")
	void removeStudentFromCourseShouldFailForEmptyStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.removeStudentFromCourse(EMPTY_STRING, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

	@Test
	@DisplayName("Should throw exception when student ID is not a number")
	void removeStudentFromCourseShouldFailForNonNumericStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.removeStudentFromCourse(NOT_NUMBER, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("not a number"));
	}

	@Test
	@DisplayName("Should throw exception when student ID is negative")
	void removeStudentFromCourseShouldFailForNegativeStudentId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.removeStudentFromCourse(NEGATIVE_NUMBER, testCourse.getCourseName()));

		assertTrue(exception.getMessage().toLowerCase().contains("cannot be negative"));
	}

	@Test
	@DisplayName("Should throw exception when course name is null")
	void removeStudentFromCourseShouldFailForNullCourseName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.removeStudentFromCourse(String.valueOf(testStudent.getId()), null));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

	@Test
	@DisplayName("Should throw exception when course name is empty")
	void removeStudentFromCourseShouldFailForEmptyCourseName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> viewDaoService.removeStudentFromCourse(String.valueOf(testStudent.getId()), EMPTY_STRING));

		assertTrue(exception.getMessage().toLowerCase().contains("must not be null or empty"));
	}

}

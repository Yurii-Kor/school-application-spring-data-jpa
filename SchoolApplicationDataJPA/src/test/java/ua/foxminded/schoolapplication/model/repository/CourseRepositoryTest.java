package ua.foxminded.schoolapplication.model.repository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.testutil.TestDataInitializer;
import ua.foxminded.schoolapplication.testutil.TestEntities;
import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, TestDataInitializer.class })

class CourseRepositoryTest {

	static final String MATHEMATICS = "Mathematics";
	static final String BIOLOGY = "Biology";
	static final String DEFAULT_GROUP_NAME = "TestGroupCourse-11";
	static final String DEFAULT_FIRST_NAME = "Alice";
	static final String DEFAULT_LAST_NAME = "Smith";
	static final Long NON_EXISTENT_ID = 999L;
	static final int ONE_STUDENT_RELATED = 1;

	@Autowired
	private TestDataInitializer initializer;

	@Autowired
	private CourseRepository courseRepository;

	private Course testCourse;
	private Student testStudent;
	private Group testGroup;

	@BeforeAll
	void setup() {
		testGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();
		testCourse = Course.builder().courseName(MATHEMATICS).build();
		testStudent = Student.builder()
				.group(testGroup)
				.firstName(DEFAULT_FIRST_NAME)
				.lastName(DEFAULT_LAST_NAME)
				.build();

		TestEntities entities = initializer.initialize(List.of(testGroup), List.of(testCourse), List.of(testStudent));

		testGroup = entities.groups().get(0);
		testCourse = entities.courses().get(0);
		testStudent = entities.students().get(0);
	}

	@Test
	@DisplayName("should save and retrieve saved course by ID")
	void shouldSaveAndFindById() {
		Course saved = courseRepository.save(Course.builder().courseName(BIOLOGY).build());

		Course retrieved = courseRepository.findById(saved.getId()).orElseThrow();

		assertThat(retrieved.getCourseName()).isEqualTo(BIOLOGY);
	}

	@Test
	@DisplayName("Saving course with duplicate name should throw Exception")
	void saveDuplicateCourseNameShouldThrowException() {
		Course duplicate = Course.builder().courseName(MATHEMATICS).build();

		assertThrows(DataIntegrityViolationException.class, () -> {
			courseRepository.save(duplicate);
			courseRepository.flush();
		});
	}

	@Test
	@DisplayName("Saving list of courses with duplicate names should throw Exception")
	void saveAllWithDuplicateNamesShouldThrowException() {
		Course courseOriginal = Course.builder().courseName(BIOLOGY).build();
		Course courseDuplicate = Course.builder().courseName(BIOLOGY).build();

		assertThrows(DataIntegrityViolationException.class, () -> {
			courseRepository.saveAll(List.of(courseOriginal, courseDuplicate));
			courseRepository.flush();
		});
	}

	@Test
	@DisplayName("Update should modify existing course")
	void updateShouldModifyCourse() {
		testCourse.setCourseName(BIOLOGY);

		Course updated = courseRepository.save(testCourse);
		Course fetched = courseRepository.findById(updated.getId()).orElseThrow();

		assertThat(fetched.getCourseName()).isEqualTo(BIOLOGY);
	}

	@Test
	@DisplayName("Updating course with duplicate name should throw Exception")
	void updateCourseWithDuplicateNameShouldThrowException() {
		Course conflicting = courseRepository.save(Course.builder().courseName(BIOLOGY).build());

		conflicting.setCourseName(MATHEMATICS);

		assertThrows(DataIntegrityViolationException.class, () -> {
			courseRepository.save(conflicting);
			courseRepository.flush();
		});
	}

	@Test
	@DisplayName("delete should remove existing course")
	void deleteShouldRemoveCourse() {
		Course toDelete = courseRepository.save(Course.builder().courseName(BIOLOGY).build());
		Long id = toDelete.getId();

		courseRepository.delete(toDelete);
		courseRepository.flush();

		Optional<Course> result = courseRepository.findById(id);
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByCourseName should return course with given name")
	void findByCourseNameShouldReturnCourse() {
		Optional<Course> result = courseRepository.findByCourseName(MATHEMATICS);

		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(testCourse.getId());
		assertThat(result.get().getCourseName()).isEqualTo(MATHEMATICS);
	}
}

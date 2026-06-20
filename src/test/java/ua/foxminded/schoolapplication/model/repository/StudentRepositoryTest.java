package ua.foxminded.schoolapplication.model.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;

import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.testutil.TestDataInitializer;
import ua.foxminded.schoolapplication.testutil.TestEntities;
import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, TestDataInitializer.class })

class StudentRepositoryTest {

	static final String DEFAULT_GROUP_NAME = "TestGroupStudent-11";
	static final String NON_EXISTENT_GROUP_NAME = "NonExistentGroup-22";
	static final String DEFAULT_COURSE_NAME = "Cyber Security";
	static final String NON_EXISTENT_COURSE_NAME = "NonExistentCourse";
	static final String DEFAULT_FIRST_NAME = "John";
	static final String UPDATED_FIRST_NAME = "UpdatedJohn";
	static final String DEFAULT_LAST_NAME = "Doe";
	static final String UPDATED_LAST_NAME = "UpdatedDoe";

	static final Long NON_EXISTENT_ID = 999L;
	static final int ONE_COURSE_PROCESSED = 1;

	@Autowired
	private TestDataInitializer initializer;

	@Autowired
	private StudentRepository studentRepository;

	private Group testGroup;
	private Student testStudent;
	private Course testCourse;

	@BeforeAll
	void setup() {
		testGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();
		testCourse = Course.builder().courseName(DEFAULT_COURSE_NAME).build();
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
	@DisplayName("should save and retrieve student by ID")
	void shouldSaveAndFindById() {
		Student saved = studentRepository.save(
				Student.builder().group(testGroup).firstName(DEFAULT_FIRST_NAME).lastName(DEFAULT_LAST_NAME).build());

		Optional<Student> retrieved = studentRepository.findById(saved.getId());
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getFirstName()).isEqualTo(DEFAULT_FIRST_NAME);
	}

	@Test
	@DisplayName("Saving student with non-existent group should throw Exception")
	void saveStudentWithNonExistentGroupShouldThrowException() {
		Group detachedGroup = Group.builder().id(NON_EXISTENT_ID).groupName(NON_EXISTENT_GROUP_NAME).build();

		Student student = Student.builder()
				.group(detachedGroup)
				.firstName(DEFAULT_FIRST_NAME)
				.lastName(DEFAULT_LAST_NAME)
				.build();

		Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
			studentRepository.save(student);
			studentRepository.flush();
		}, "Expected DataIntegrityViolationException when saving student with non-existent group");
	}

	@Test
	@DisplayName("Update should modify existing student")
	void updateShouldModifyStudent() {
		testStudent.setFirstName(UPDATED_FIRST_NAME);
		testStudent.setLastName(UPDATED_LAST_NAME);

		Student updated = studentRepository.save(testStudent);

		Optional<Student> fetched = studentRepository.findById(updated.getId());

		assertThat(fetched).isPresent();
		assertThat(fetched.get().getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
		assertThat(fetched.get().getLastName()).isEqualTo(UPDATED_LAST_NAME);
	}

	@Test
	@DisplayName("Updating student with non-existent group should throw exception")
	void updateStudentWithNonExistentGroupShouldThrowException() {
		Group detachedGroup = Group.builder().id(NON_EXISTENT_ID).groupName(NON_EXISTENT_GROUP_NAME).build();

		testStudent.setGroup(detachedGroup);

		assertThrows(JpaObjectRetrievalFailureException.class, () -> {
			studentRepository.save(testStudent);
			studentRepository.flush();
		});

		testStudent.setGroup(testGroup);
	}

	@Test
	@DisplayName("findById should return Optional.empty() for non-existent student")
	void findByIdShouldReturnEmptyForNonExistentStudent() {
		Optional<Student> result = studentRepository.findById(NON_EXISTENT_ID);

		assertThat(result).isNotPresent();
	}

	@Test
	@DisplayName("delete should remove existing student")
	void deleteShouldRemoveStudent() {
		Student toDelete = studentRepository.save(
				Student.builder().firstName(DEFAULT_FIRST_NAME).lastName(DEFAULT_LAST_NAME).group(testGroup).build());

		Long id = toDelete.getId();

		studentRepository.delete(toDelete);
		studentRepository.flush();

		Optional<Student> result = studentRepository.findById(id);
		assertThat(result).isEmpty();
	}
}

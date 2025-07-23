package ua.foxminded.schoolapplication.model.repository;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;

import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.testutil.TestDataInitializer;
import ua.foxminded.schoolapplication.testutil.TestEntities;
import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@TestInstance(Lifecycle.PER_CLASS)
@Testcontainers
@Import({ TestcontainersConfiguration.class, TestDataInitializer.class })

class GroupRepositoryTest {

	static final String EMPTY_GROUP_NAME = "EmptyGroup-11";
	static final String FULL_GROUP_NAME = "FullGroup-22";
	static final String DEFAULT_GROUP_NAME = "TestGroup-33";
	static final String UPDATED_GROUP_NAME = "UpdatedGroup-44";
	static final String[] STUDENT_FIRST_NAMES = { "John", "Jane" };
	static final String[] STUDENT_LAST_NAMES = { "Doe", "Smith" };
	static final String UNSAVED_NAME = null;

	static final Long NON_EXISTENT_ID = 999L;
	static final int ONE_STUDENT_PROCESSED = 1;

	@Autowired
	private TestDataInitializer initializer;

	@Autowired
	private GroupRepository groupRepository;

	private Group emptyGroup;
	private Group fullGroup;

	@BeforeAll
	void setup() {
		List<Group> inputGroups = List.of(Group.builder().groupName(EMPTY_GROUP_NAME).build(),
				Group.builder().groupName(FULL_GROUP_NAME).build());

		List<Student> inputStudents = List.of(
				Student.builder()
						.group(inputGroups.get(1))
						.firstName(STUDENT_FIRST_NAMES[0])
						.lastName(STUDENT_LAST_NAMES[0])
						.build(),

				Student.builder()
						.group(inputGroups.get(1))
						.firstName(STUDENT_FIRST_NAMES[1])
						.lastName(STUDENT_LAST_NAMES[1])
						.build());

		TestEntities entities = initializer.initialize(inputGroups, List.of(), inputStudents);

		emptyGroup = entities.groups().get(0);
		fullGroup = entities.groups().get(1);
	}

	@Test
	@DisplayName("should save and retrieve saved group by ID")
	void shouldSaveAndFindById() {
		Group saved = groupRepository.save(Group.builder().groupName(DEFAULT_GROUP_NAME).build());

		Group retrieved = groupRepository.findById(saved.getId()).orElseThrow();

		assertThat(retrieved.getGroupName()).isEqualTo(DEFAULT_GROUP_NAME);
	}

	@Test
	@DisplayName("Saving group with duplicate name should throw Exception")
	void saveDuplicateGroupNameShouldThrowException() {
		Group duplicate = Group.builder().groupName(EMPTY_GROUP_NAME).build();

		assertThrows(DataIntegrityViolationException.class, () -> {
			groupRepository.save(duplicate);
			groupRepository.flush();
		});
	}

	@Test
	@DisplayName("Saving list of groups with duplicate names should throw Exception")
	void saveAllWithDuplicateGroupNamesShouldThrowException() {
		Group groupOriginal = Group.builder().groupName(DEFAULT_GROUP_NAME).build();
		Group groupDuplicate = Group.builder().groupName(DEFAULT_GROUP_NAME).build();

		assertThrows(DataIntegrityViolationException.class, () -> {
			groupRepository.saveAll(List.of(groupOriginal, groupDuplicate));
			groupRepository.flush();
		});
	}

	@Test
	@DisplayName("Update should modify existing group")
	void updateShouldModifyGroup() {
		emptyGroup.setGroupName(UPDATED_GROUP_NAME);

		Group updated = groupRepository.save(emptyGroup);
		Group fetched = groupRepository.findById(updated.getId()).orElseThrow();

		assertThat(fetched.getGroupName()).isEqualTo(UPDATED_GROUP_NAME);
	}

	@Test
	@DisplayName("Updating group with duplicate name should throw Exception")
	void updateGroupWithDuplicateNameShouldThrowException() {
		emptyGroup.setGroupName(FULL_GROUP_NAME);

		assertThrows(DataIntegrityViolationException.class, () -> {
			groupRepository.save(emptyGroup);
			groupRepository.flush();
		});
	}

	@Test
	@DisplayName("delete should remove empty group")
	void deleteShouldRemoveEmptyGroup() {
		Long id = emptyGroup.getId();

		groupRepository.delete(emptyGroup);
		groupRepository.flush();

		assertThat(groupRepository.findById(id)).isEmpty();
	}

	@Test
	@DisplayName("deleting group with students should throw Exception")
	void deleteGroupWithStudentsShouldThrowException() {
		assertThrows(DataIntegrityViolationException.class, () -> {
			groupRepository.delete(fullGroup);
			groupRepository.flush();
		});
	}

	@Test
	@DisplayName("findGroupsWithStudentCountLessOrEqual should return only groups with student count <= max")
	void findGroupsWithStudentCountLessOrEqualShouldReturnCorrectGroups() {
		List<Long> resultIds = groupRepository.findGroupIdsByStudentCountLessThanOrEqualTo(ONE_STUDENT_PROCESSED);
		List<Group> result = groupRepository.findAllById(resultIds);

		assertThat(result).contains(emptyGroup);
		assertThat(result).doesNotContain(fullGroup);
	}
}

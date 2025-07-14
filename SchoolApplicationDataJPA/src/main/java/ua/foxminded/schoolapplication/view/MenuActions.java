package ua.foxminded.schoolapplication.view;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.schoolapplication.exception.FieldConstraintException;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.service.ViewDaoService;

import java.util.List;
import java.util.Scanner;
import java.util.Set;

@Component
public class MenuActions {

	private Scanner scanner;
	private ViewDaoService viewDaoService;

	public MenuActions(ViewDaoService viewDaoService, Scanner scanner) {
		this.viewDaoService = viewDaoService;
		this.scanner = scanner;
	}

	public void findGroupsByStudentCount() {
		System.out.print("Enter the max number of students: ");
		String maxInput = scanner.nextLine();

		try {
			List<Group> groups = viewDaoService.findGroupsWithStudentCountLessOrEqual(maxInput);

			if (groups.isEmpty()) {
				System.out.println("No groups found with student count ≤ " + maxInput);
			} else {
				System.out.println("Groups with student count ≤ " + maxInput + ":");
				groups.forEach(System.out::println);
			}
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void findStudentsByCourseName() {
		System.out.print("Enter course name: ");
		String courseName = scanner.nextLine().trim();

		try {
			Set<Student> students = viewDaoService.findStudentsByCourseName(courseName);

			if (students.isEmpty()) {
				System.out.println("No students found for course: " + courseName);
			} else {
				System.out.println("Students enrolled in course '" + courseName + "':");
				students.forEach(System.out::println);
			}
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void addNewStudent() {
		System.out.print("Enter group ID: ");
		Long groupId = scanner.nextLong();
		Group group = Group.builder().id(groupId).build();
		scanner.nextLine();

		System.out.print("Enter first name: ");
		String firstName = scanner.nextLine().trim();

		System.out.print("Enter last name: ");
		String lastName = scanner.nextLine().trim();

		Student student = Student.builder().group(group).firstName(firstName).lastName(lastName).build();

		try {
			Student saved = viewDaoService.addStudent(student);
			System.out.printf("Student added successfully: %s%n", saved);
		} catch (FieldConstraintException e) {
			System.err.println("Cannot add student due to validation errors.");
			System.err.println(e.getMessage());
		} catch (DataIntegrityViolationException dbex) {
			System.err.println("Error: Cannot add student because group may not exist or violates DB constraints. "
					+ dbex.getMessage());
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void deleteStudentById() {
		System.out.print("Enter STUDENT_ID to delete: ");
		String studentId = scanner.nextLine();

		try {
			Student deleted = viewDaoService.deleteStudentById(studentId);
			System.out.printf("Student deleted: %s%n", deleted);
		} catch (EntityNotFoundException e) {
			System.err.printf("Student with id: %s - Not Existed", studentId);
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void addStudentToCourse() {
		System.out.print("Enter STUDENT_ID: ");
		String studentId = scanner.nextLine();

		System.out.print("Enter course name: ");
		String courseName = scanner.nextLine();

		try {
			viewDaoService.addStudentToCourse(studentId, courseName);
			System.out.printf("Student with ID %s successfully added to course '%s'%n", studentId, courseName);
		} catch (JpaObjectRetrievalFailureException e) {
			if (e.getCause() instanceof EntityNotFoundException) {
				System.err.println("Entity not found: " + e.getCause().getMessage());
			} else {
				throw e;
			}
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void removeStudentFromCourse() {
		System.out.print("Enter STUDENT_ID: ");
		String studentId = scanner.nextLine();

		System.out.print("Enter course name to remove: ");
		String courseName = scanner.nextLine();

		try {
			viewDaoService.removeStudentFromCourse(studentId, courseName);
			System.out.printf("Student with ID %s removed from course '%s'%n", studentId, courseName);
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
}

package ua.foxminded.schoolapplication.view;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class MainMenu implements CommandLineRunner {

	private Scanner scanner;
	private MenuActions menuActions;

	public MainMenu(MenuActions menuActions, Scanner scanner) {
		this.menuActions = menuActions;
		this.scanner = scanner;
	}

	@Override
	public void run(String... args) {
		while (true) {
			printMenu();

			String choice = scanner.nextLine().trim().toLowerCase();

			switch (choice) {
			case "a" -> menuActions.findGroupsByStudentCount();
			case "b" -> menuActions.findStudentsByCourseName();
			case "c" -> menuActions.addNewStudent();
			case "d" -> menuActions.deleteStudentById();
			case "e" -> menuActions.addStudentToCourse();
			case "f" -> menuActions.removeStudentFromCourse();
			case "q" -> {
				System.out.println("Exiting application...");
				return;
			}

			default -> System.out.println("Invalid choice. Please select a valid option.");
			}
		}
	}

	private void printMenu() {
		String lineSeparator = System.lineSeparator();
		StringBuilder menu = new StringBuilder();
		menu.append(lineSeparator)
				.append("=== Main Menu ===")
				.append(lineSeparator)
				.append(" a. Find all groups with less or equal students' number")
				.append(lineSeparator)
				.append(" b. Find all students related to the course with the given name")
				.append(lineSeparator)
				.append(" c. Add a new student")
				.append(lineSeparator)
				.append(" d. Delete a student by STUDENT_ID")
				.append(lineSeparator)
				.append(" e. Add a student to the course")
				.append(lineSeparator)
				.append(" f. Remove the student from one of their courses")
				.append(lineSeparator)
				.append(" q. Exit")
				.append(lineSeparator)
				.append("Choose an option: ");

		System.out.print(menu);
	}
}

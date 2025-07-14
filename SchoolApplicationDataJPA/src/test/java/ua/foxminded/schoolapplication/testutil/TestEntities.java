package ua.foxminded.schoolapplication.testutil;

import java.util.List;

import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;

public record TestEntities(List<Group> groups, List<Course> courses, List<Student> students) {
}

package ua.foxminded.schoolapplication.model.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.foxminded.schoolapplication.config.ValidatorConfig;
import ua.foxminded.schoolapplication.model.domain.Course;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class CourseValidatorTest {

	static final String VALID_COURSE_NAME = "Mathematics 101";
	static final String VALID_COURSE_DESCRIPTION = "An introductory course to mathematics.";

	static final String NULL = "null";
	static final String EMPTY = "";
	static final String TOO_SHORT = "A";
	static final String INVALID_CHARS = "Math@101";
	static final String TOO_LONG_NAME = "This is a very long course name that is intended to exceed the maximum allowed length of one hundred characters for courses";

	static final String COURSE_PATTERN = "courseName: \"{0}\", courseDescription: \"{1}\" | Expected valid: {2}";

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = COURSE_PATTERN)
	@MethodSource("provideCoursesForValidation")
	@DisplayName("Course entity validation should behave as expected")
	void validateEntities_ShouldBehaveAsExpected(String courseName, String courseDescription, boolean shouldPass) {
	    String validatedCourseName = NULL.equals(courseName) ? null : courseName;
	    String validatedCourseDescription = NULL.equals(courseDescription) ? null : courseDescription;

	    Course course = Course.builder()
	            .courseName(validatedCourseName)
	            .courseDescription(validatedCourseDescription)
	            .build();

	    Set<ConstraintViolation<Course>> violations = validator.validate(course);

	    if (shouldPass) {
	        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
	    } else {
	        assertFalse(violations.isEmpty(), "Expected violations, but got none");
	    }
	}

	static Stream<Arguments> provideCoursesForValidation() {
		return Stream.of(Arguments.of(VALID_COURSE_NAME, VALID_COURSE_DESCRIPTION, true),
				Arguments.of(VALID_COURSE_NAME, EMPTY, true),
				Arguments.of(NULL, VALID_COURSE_DESCRIPTION, false),
				Arguments.of(EMPTY, VALID_COURSE_DESCRIPTION, false),
				Arguments.of(TOO_SHORT, VALID_COURSE_DESCRIPTION, false),
				Arguments.of(INVALID_CHARS, VALID_COURSE_DESCRIPTION, false),
				Arguments.of(TOO_LONG_NAME, VALID_COURSE_DESCRIPTION, false));
	}
}

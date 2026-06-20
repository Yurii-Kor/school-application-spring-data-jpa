package ua.foxminded.schoolapplication.model.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.foxminded.schoolapplication.config.ValidatorConfig;
import ua.foxminded.schoolapplication.model.domain.Group;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class GroupValidatorTest {

	static final String VALID_GROUP_NAME_SIMPLE = "AB-12";
	static final String VALID_GROUP_NAME_LONG = "Mathematics-101";

	static final String INVALID_GROUP_NAME_NO_HYPHEN = "AB12";
	static final String INVALID_GROUP_NAME_NON_DIGIT_AFTER_HYPHEN = "AB-1A";
	static final String INVALID_GROUP_NAME_EMPTY = "";
	static final String NULL = "null";
	static final String INVALID_GROUP_NAME_HYPHEN_WITHOUT_DIGITS = "AB-";
	static final String INVALID_GROUP_NAME_HYPHEN_WITHOUT_LETTERS = "-12";
	static final String INVALID_GROUP_NAME_ADDITIONAL_SYMBOLS = "AB-12-34";

	static final String GROUP_PATTERN = "GroupName: {0} | Expected valid: {1}";

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = GROUP_PATTERN)
	@MethodSource("provideGroupsForValidation")
	@DisplayName("Group entity validation should behave as expected")
	void validate_GroupName_ShouldBehaveAsExpected(String groupName, boolean shouldPass) {
	    String actualGroupName = NULL.equals(groupName) ? null : groupName;

	    Group testedGroup = Group.builder()
	        .groupName(actualGroupName)
	        .build();

	    Set<ConstraintViolation<Group>> violations = validator.validate(testedGroup);

	    if (shouldPass) {
	        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
	    } else {
	        assertFalse(violations.isEmpty(), "Expected violations, but got none");
	    }
	}

	static Stream<Arguments> provideGroupsForValidation() {
		return Stream.of(Arguments.of(VALID_GROUP_NAME_SIMPLE, true),
				Arguments.of(VALID_GROUP_NAME_LONG, true),
				Arguments.of(INVALID_GROUP_NAME_NO_HYPHEN, false),
				Arguments.of(INVALID_GROUP_NAME_NON_DIGIT_AFTER_HYPHEN, false),
				Arguments.of(INVALID_GROUP_NAME_EMPTY, false),
				Arguments.of(NULL, false),
				Arguments.of(INVALID_GROUP_NAME_HYPHEN_WITHOUT_DIGITS, false),
				Arguments.of(INVALID_GROUP_NAME_HYPHEN_WITHOUT_LETTERS, false),
				Arguments.of(INVALID_GROUP_NAME_ADDITIONAL_SYMBOLS, false));
	}
}

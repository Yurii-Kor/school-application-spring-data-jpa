package ua.foxminded.schoolapplication.exception;

import jakarta.validation.ConstraintViolation;

import java.util.Set;
import java.util.stream.Collectors;

public class FieldConstraintException extends RuntimeException {

	private final Set<ConstraintViolation<?>> violations;

	public FieldConstraintException(Set<ConstraintViolation<?>> violations) {
		super("Validation failed with " + violations.size() + " constraint violations");
		this.violations = violations;
	}

	@Override
	public String getMessage() {
		return violations.stream()
				.map(v -> String.format("[%s: %s -> %s]", v.getPropertyPath(), v.getInvalidValue(), v.getMessage()))
				.collect(Collectors.joining(System.lineSeparator()));
	}
}

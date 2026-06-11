package ua.foxminded.schoolapplication.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.foxminded.schoolapplication.exception.FieldConstraintException;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EntityValidationService {

	private final Validator validator;

	public EntityValidationService(Validator validator) {
		this.validator = validator;
	}

	public <T> void validateEntities(List<T> entities) {
		if (entities == null || entities.isEmpty()) {
			throw new IllegalArgumentException("The list of entities must not be null or empty");
		}

		Set<ConstraintViolation<?>> allViolations = new HashSet<>();

		for (T entity : entities) {
			Set<? extends ConstraintViolation<?>> violations = validator.validate(entity);
			allViolations.addAll(violations);
		}

		if (!allViolations.isEmpty()) {
			throw new FieldConstraintException(allViolations);
		}
	}
}

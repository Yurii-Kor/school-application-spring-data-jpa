package ua.foxminded.schoolapplication.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InputPreprocessingService {

	private static final Logger logger = LoggerFactory.getLogger(InputPreprocessingService.class);

	public Long parseLong(String input) {
		try {
			return Long.parseLong(input);
		} catch (NumberFormatException e) {
			logger.error("Failed to parse input '{}' as number", input, e);
			throw new IllegalArgumentException(String.format("Invalid Input: '%s' is not a number.", input), e);
		}
	}

	public Integer parseInteger(String input) {
		try {
			return Integer.parseInt(input);
		} catch (NumberFormatException e) {
			logger.error("Failed to parse input '{}' as number", input, e);
			throw new IllegalArgumentException(
					String.format("Invalid Input: '%s' is not a number or too large num.", input), e);
		}
	}

	public void validateInputString(String input, String errorMessage) {
		if (input == null || input.trim().isEmpty()) {
			logger.warn("Provided input string is null or empty");
			throw new IllegalArgumentException(errorMessage);
		}
	}

	public void validateNonNegativeNum(Number number, String errorMessage) {
		long value = number.longValue();
		if (value < 0) {
			logger.warn("Provided ID is negative: {}", value);
			throw new IllegalArgumentException(errorMessage);
		}
	}
}

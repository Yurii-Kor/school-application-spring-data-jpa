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

	public Integer parseInteger(Long parsed) {
		if (parsed > Integer.MAX_VALUE) {
			logger.error("Failed to parse integer '{}'", parsed);
			throw new IllegalArgumentException(
					String.format("Invalid Long Number: '%s' is too large for Integer.", parsed));
		}
		return parsed.intValue();
	}

	public void validateInputString(String input, String errorMessage) {
		if (input == null || input.trim().isEmpty()) {
			logger.warn("Provided input string is null or empty");
			throw new IllegalArgumentException(errorMessage);
		}
	}

	public void validateNonNegativeLong(Long id, String errorMessage) {
		if (id < 0) {
			logger.warn("Provided ID is negative: {}", id);
			throw new IllegalArgumentException(errorMessage);
		}
	}
}

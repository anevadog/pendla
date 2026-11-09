package io.github.anevadog.pendla.jobsearch;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Thrown when the search asks for a city that is not in SupportedCity. Mapped to 400.
 * The message lists the valid cities so the client knows what to send.
 */
public class UnsupportedCityException extends RuntimeException {

	public UnsupportedCityException(String city) {
		super("Unsupported city '" + city + "'. Supported cities: " + supportedNames());
	}

	private static String supportedNames() {
		return Arrays.stream(SupportedCity.values())
				.map(SupportedCity::getDisplayName)
				.collect(Collectors.joining(", "));
	}

}

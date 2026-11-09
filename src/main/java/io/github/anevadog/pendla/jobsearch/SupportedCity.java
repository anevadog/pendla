package io.github.anevadog.pendla.jobsearch;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cities the search accepts, with their JobTech municipality id (verified against JobTech on 2026-11-09).
 * A fixed list keeps the filter exact; see docs/decisiones.md D2.
 */
public enum SupportedCity {

	STOCKHOLM("Stockholm", "AvNB_uwa_6n6"),
	GOTEBORG("Göteborg", "PVZL_BQT_XtL"),
	MALMO("Malmö", "oYPt_yRA_Smm"),
	UPPSALA("Uppsala", "otaF_bQY_4ZD");

	private final String displayName;
	private final String municipalityConceptId;

	SupportedCity(String displayName, String municipalityConceptId) {
		this.displayName = displayName;
		this.municipalityConceptId = municipalityConceptId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getMunicipalityConceptId() {
		return municipalityConceptId;
	}

	/** Finds a city by its Swedish name, ignoring case and surrounding spaces ("göteborg" works). */
	public static Optional<SupportedCity> fromName(String name) {
		String trimmed = name.trim();
		return Arrays.stream(values())
				.filter(city -> city.displayName.equalsIgnoreCase(trimmed))
				.findFirst();
	}

}

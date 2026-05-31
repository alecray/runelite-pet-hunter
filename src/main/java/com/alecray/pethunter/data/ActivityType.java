package com.alecray.pethunter.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Whether a pet's source activity is done alone, in a group, or works either way.
 */
@Getter
@RequiredArgsConstructor
public enum ActivityType
{
	SOLO("Solo"),
	GROUP("Group"),
	BOTH("Solo or group");

	private final String displayName;

	/**
	 * Whether a pet of this activity type should be shown when the panel filter is set to
	 * {@code filter}. BOTH pets match any filter; a pure SOLO/GROUP pet only matches its own.
	 */
	public boolean matchesFilter(ActivityType filter)
	{
		if (filter == null || this == BOTH)
		{
			return true;
		}
		return this == filter || filter == BOTH;
	}
}

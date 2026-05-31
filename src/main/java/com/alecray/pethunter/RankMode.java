package com.alecray.pethunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How {@link TaskService} orders un-obtained pets when suggesting the next target.
 */
@Getter
@RequiredArgsConstructor
public enum RankMode
{
	/** Lowest drop-rate denominator first — the most likely / quickest pets to get. */
	EASIEST_FIRST("Easiest first"),
	/** Highest drop-rate denominator first — the rarest grinds. */
	RAREST_FIRST("Rarest first"),
	/** Keep the dataset's natural order. */
	DATASET_ORDER("Dataset order");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}

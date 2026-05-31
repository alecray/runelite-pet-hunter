package com.alecray.pethunter.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * A single collection-log pet and its source activity. The static fields are loaded from the
 * bundled {@code pets.json}; the mutable runtime state ({@link #obtained}, {@link #customTags})
 * is merged on by {@link com.alecray.pethunter.PetDataManager}.
 */
@Data
public class Pet
{
	/** Display name, e.g. "Pet general graardor". */
	private String name;

	/** Item id of the pet, used to match against collection-log item widgets. */
	private int itemId;

	/** Human-readable source activity/boss, e.g. "General Graardor (GWD)". */
	private String source;

	/** Solo / group / both classification of the source activity. */
	private ActivityType type;

	/** Drop-rate denominator (the N in 1/N). Use 0 when not a fixed rate. */
	private int rarity;

	/** Old School Wiki page for the pet. */
	private String wikiUrl;

	/** Built-in tags shipped in pets.json (e.g. "boss", "gwd", "raid"). */
	private List<String> defaultTags = new ArrayList<>();

	// ----- runtime state, not part of pets.json -----

	/** Whether the player has obtained this pet (set from the collection log). */
	private transient boolean obtained;

	/** User-defined tags, persisted in config keyed by item id. */
	private transient List<String> customTags = new ArrayList<>();

	/** All tags (default + custom) for filtering/display. */
	public List<String> allTags()
	{
		List<String> all = new ArrayList<>();
		if (defaultTags != null)
		{
			all.addAll(defaultTags);
		}
		if (customTags != null)
		{
			for (String t : customTags)
			{
				if (!all.contains(t))
				{
					all.add(t);
				}
			}
		}
		return all;
	}

	public boolean hasTag(String tag)
	{
		if (tag == null)
		{
			return true;
		}
		return allTags().stream().anyMatch(t -> t.equalsIgnoreCase(tag));
	}
}

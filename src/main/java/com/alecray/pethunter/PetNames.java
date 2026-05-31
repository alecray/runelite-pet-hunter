package com.alecray.pethunter;

/**
 * Helpers for turning pet display names into a stable identity key. Pets are matched between the
 * bundled dataset, the collection-log interface, and persisted config by this normalized key so
 * differences in casing, spacing, and punctuation (apostrophes, hyphens) never break matching.
 */
public final class PetNames
{
	private PetNames()
	{
	}

	/**
	 * @return a lowercase, alphanumeric-only key for the given name, or "" if name is null/blank.
	 */
	public static String key(String name)
	{
		if (name == null)
		{
			return "";
		}
		return name.toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}

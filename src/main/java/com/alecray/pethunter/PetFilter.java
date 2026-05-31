package com.alecray.pethunter;

import com.alecray.pethunter.data.ActivityType;
import com.alecray.pethunter.data.Pet;
import lombok.Value;

/**
 * The panel's active filter state. {@code activityType}/{@code tag} of {@code null} mean "any".
 */
@Value
public class PetFilter
{
	boolean hideObtained;
	ActivityType activityType;
	String tag;

	public boolean matches(Pet pet)
	{
		if (hideObtained && pet.isObtained())
		{
			return false;
		}
		if (activityType != null && !pet.getType().matchesFilter(activityType))
		{
			return false;
		}
		return tag == null || pet.hasTag(tag);
	}
}

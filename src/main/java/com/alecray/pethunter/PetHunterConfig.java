package com.alecray.pethunter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PetHunterConfig.GROUP)
public interface PetHunterConfig extends Config
{
	String GROUP = "pethunter";

	@ConfigItem(
		keyName = "hideObtainedByDefault",
		name = "Hide obtained by default",
		description = "When the panel opens, start with pets you already own hidden.",
		position = 1
	)
	default boolean hideObtainedByDefault()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rankMode",
		name = "Suggestion ranking",
		description = "How the next-pet suggestion orders the pets you still need.",
		position = 2
	)
	default RankMode rankMode()
	{
		return RankMode.EASIEST_FIRST;
	}

	@ConfigItem(
		keyName = "autoSync",
		name = "Auto-sync from collection log",
		description = "Automatically read obtained pets whenever a collection log page is drawn.",
		position = 3
	)
	default boolean autoSync()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSyncButton",
		name = "Show Sync button in log",
		description = "Inject a 'Sync Pets' button into the collection log header.",
		position = 4
	)
	default boolean showSyncButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyOnNewPet",
		name = "Notify on new pet",
		description = "Show a notification when a new pet is added to your collection log.",
		position = 5
	)
	default boolean notifyOnNewPet()
	{
		return true;
	}
}

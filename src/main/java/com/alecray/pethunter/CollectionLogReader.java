package com.alecray.pethunter;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

/**
 * Reads obtained pets out of the open collection-log page. RuneLite cannot read the whole log at
 * once, so this runs whenever a collection-log page is drawn (and on the manual Sync button): it
 * walks the item widgets of the currently-open page, treats fully-opaque icons as obtained, and
 * feeds the matching pets into {@link PetDataManager}.
 *
 * <p>Reading is additive — opening the "All Pets" page captures everything at once, while opening
 * an individual boss page still picks up that boss's pet. All methods must run on the client thread.</p>
 */
@Slf4j
@Singleton
public class CollectionLogReader
{
	private final Client client;
	private final ItemManager itemManager;
	private final PetDataManager dataManager;

	@Inject
	public CollectionLogReader(Client client, ItemManager itemManager, PetDataManager dataManager)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.dataManager = dataManager;
	}

	/**
	 * Read the currently-open collection-log page and mark any obtained pets on it.
	 *
	 * @return the number of pet entries found obtained on this page, or -1 if the page wasn't open.
	 *         (This counts matches on the page, not newly-discovered pets.)
	 */
	public int readOpenPage()
	{
		Widget itemsContainer = client.getWidget(ComponentID.COLLECTION_LOG_ENTRY_ITEMS);
		if (itemsContainer == null)
		{
			return -1;
		}

		Widget[] children = itemsContainer.getDynamicChildren();
		if (children == null)
		{
			return 0;
		}

		Set<String> obtainedKeys = new HashSet<>();
		for (Widget child : children)
		{
			if (child == null || child.getItemId() <= 0)
			{
				continue;
			}
			// In the collection log, obtained items render fully opaque (opacity 0); unobtained
			// items are faded.
			if (child.getOpacity() != 0)
			{
				continue;
			}

			String name = itemManager.getItemComposition(child.getItemId()).getName();
			String key = PetNames.key(name);
			if (dataManager.getByKey(key) != null)
			{
				obtainedKeys.add(key);
			}
		}

		dataManager.markObtained(obtainedKeys);
		return obtainedKeys.size();
	}
}

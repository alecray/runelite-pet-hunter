package com.alecray.pethunter;

import com.alecray.pethunter.data.Pet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads the bundled pet dataset and merges in the player's runtime state (which pets are obtained
 * and any custom tags). It is the single source of truth queried by the panel and {@link TaskService},
 * and the sink the {@link CollectionLogReader} writes obtained pets into.
 *
 * <p>All persisted state is keyed by {@link PetNames#key(String)} so it survives item-id changes
 * and matches the collection-log interface regardless of casing/punctuation.</p>
 */
@Slf4j
@Singleton
public class PetDataManager
{
	private static final String PETS_RESOURCE = "/com/alecray/pethunter/pets.json";

	private final Gson gson;
	private final PetHunterConfigManager configStore;
	private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

	/** Immutable dataset loaded from pets.json, in file order. */
	private final List<Pet> pets = new ArrayList<>();
	/** Normalized name key -> Pet, for fast lookup from the collection log. */
	private final Map<String, Pet> petsByKey = new LinkedHashMap<>();

	@Inject
	public PetDataManager(Gson gson, PetHunterConfigManager configStore)
	{
		this.gson = gson;
		this.configStore = configStore;
		loadDataset();
	}

	private void loadDataset()
	{
		try (InputStream in = PetDataManager.class.getResourceAsStream(PETS_RESOURCE))
		{
			if (in == null)
			{
				log.error("Pet Hunter: could not find bundled {}", PETS_RESOURCE);
				return;
			}
			Type listType = new TypeToken<List<Pet>>()
			{
			}.getType();
			List<Pet> loaded = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), listType);
			if (loaded != null)
			{
				pets.addAll(loaded);
				for (Pet pet : loaded)
				{
					petsByKey.put(PetNames.key(pet.getName()), pet);
				}
			}
			log.debug("Pet Hunter: loaded {} pets from dataset", pets.size());
		}
		catch (IOException e)
		{
			log.error("Pet Hunter: failed to read pet dataset", e);
		}
	}

	/**
	 * Apply persisted obtained-state and custom tags onto the dataset. Call once after the config
	 * store is ready (e.g. plugin start) and whenever the active profile changes.
	 */
	public void reloadState()
	{
		Set<String> obtained = configStore.getObtained();
		Map<String, List<String>> tags = configStore.getCustomTags();
		for (Pet pet : pets)
		{
			String key = PetNames.key(pet.getName());
			pet.setObtained(obtained.contains(key));
			pet.setCustomTags(new ArrayList<>(tags.getOrDefault(key, Collections.emptyList())));
		}
		fireChanged();
	}

	public List<Pet> getPets()
	{
		return Collections.unmodifiableList(pets);
	}

	public Pet getByKey(String key)
	{
		return petsByKey.get(key);
	}

	public long obtainedCount()
	{
		return pets.stream().filter(Pet::isObtained).count();
	}

	public int total()
	{
		return pets.size();
	}

	/**
	 * Mark the given normalized pet keys as obtained (additive — does not clear pets already known).
	 * Unknown keys are ignored. Persists and notifies listeners only if something changed.
	 */
	public void markObtained(Collection<String> obtainedKeys)
	{
		boolean changed = false;
		for (String key : obtainedKeys)
		{
			Pet pet = petsByKey.get(key);
			if (pet != null && !pet.isObtained())
			{
				pet.setObtained(true);
				changed = true;
			}
		}
		if (changed)
		{
			persistObtained();
			fireChanged();
		}
	}

	public void addCustomTag(Pet pet, String tag)
	{
		String clean = tag == null ? "" : tag.trim();
		if (clean.isEmpty() || pet.getCustomTags().stream().anyMatch(t -> t.equalsIgnoreCase(clean)))
		{
			return;
		}
		pet.getCustomTags().add(clean);
		persistTags();
		fireChanged();
	}

	public void removeCustomTag(Pet pet, String tag)
	{
		if (pet.getCustomTags().removeIf(t -> t.equalsIgnoreCase(tag)))
		{
			persistTags();
			fireChanged();
		}
	}

	/**
	 * Replace a pet's custom tags wholesale (used by the card's tag editor). Blank entries are
	 * dropped and duplicates (case-insensitive) are collapsed.
	 */
	public void setCustomTags(Pet pet, List<String> tags)
	{
		List<String> cleaned = new ArrayList<>();
		for (String t : tags)
		{
			String clean = t == null ? "" : t.trim();
			if (!clean.isEmpty() && cleaned.stream().noneMatch(c -> c.equalsIgnoreCase(clean)))
			{
				cleaned.add(clean);
			}
		}
		pet.setCustomTags(cleaned);
		persistTags();
		fireChanged();
	}

	/** All tags in use across the dataset (default + custom), sorted, for the filter dropdown. */
	public List<String> allKnownTags()
	{
		Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (Pet pet : pets)
		{
			tags.addAll(pet.allTags());
		}
		return new ArrayList<>(tags);
	}

	private void persistObtained()
	{
		Set<String> keys = pets.stream()
			.filter(Pet::isObtained)
			.map(p -> PetNames.key(p.getName()))
			.collect(Collectors.toCollection(TreeSet::new));
		configStore.setObtained(keys);
	}

	private void persistTags()
	{
		Map<String, List<String>> tags = new LinkedHashMap<>();
		for (Pet pet : pets)
		{
			if (!pet.getCustomTags().isEmpty())
			{
				tags.put(PetNames.key(pet.getName()), new ArrayList<>(pet.getCustomTags()));
			}
		}
		configStore.setCustomTags(tags);
	}

	public void addChangeListener(Runnable listener)
	{
		changeListeners.add(listener);
	}

	public void removeChangeListener(Runnable listener)
	{
		changeListeners.remove(listener);
	}

	private void fireChanged()
	{
		for (Runnable listener : changeListeners)
		{
			listener.run();
		}
	}
}
